package corablue.stagehand.world;

import corablue.stagehand.Stagehand;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StageChestManager extends PersistentState {

    // Structure: Chest ID -> (Looter ID -> Set of Destroyed Item Instance IDs)
    private final Map<UUID, Map<UUID, Set<UUID>>> destroyedItems = new HashMap<>();

    public StageChestManager() {}

    // --- Core API ---

    public static void markItemDestroyed(MinecraftServer server, UUID chestId, UUID looterId, UUID itemInstanceId) {
        StageChestManager state = getServerState(server);

        state.destroyedItems
                .computeIfAbsent(chestId, k -> new HashMap<>())
                .computeIfAbsent(looterId, k -> new HashSet<>())
                .add(itemInstanceId);

        state.markDirty(); // Tells Minecraft to save this to disk on the next autosave
        Stagehand.LOGGER.info("Tracked item destroyed! Chest: " + chestId + ", Looter: " + looterId);
    }

    public static boolean isItemDestroyed(MinecraftServer server, UUID chestId, UUID looterId) {
        StageChestManager state = getServerState(server);

        Map<UUID, Set<UUID>> chestData = state.destroyedItems.get(chestId);
        if (chestData != null) {
            Set<UUID> looterData = chestData.get(looterId);
            // Returns true if the looter has at least one destroyed item registered from this chest
            return looterData != null && !looterData.isEmpty();
        }
        return false;
    }

    public static void resetDestroyedStatus(MinecraftServer server, UUID chestId, UUID looterId) {
        StageChestManager state = getServerState(server);

        Map<UUID, Set<UUID>> chestData = state.destroyedItems.get(chestId);
        if (chestData != null) {
            chestData.remove(looterId);
            state.markDirty();
        }
    }

    // --- State Saving and Loading ---

    public static StageChestManager getServerState(MinecraftServer server) {
        // We save this to the Overworld's data so it is universally accessible across dimensions
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        Type<StageChestManager> type = new Type<>(
                StageChestManager::new, // If it doesn't exist, create a new one
                StageChestManager::createFromNbt, // If it does exist, load it from NBT
                null
        );

        return persistentStateManager.getOrCreate(type, Stagehand.MOD_ID + "_stage_chest_manager");
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList chestList = new NbtList();

        for (Map.Entry<UUID, Map<UUID, Set<UUID>>> chestEntry : destroyedItems.entrySet()) {
            NbtCompound chestTag = new NbtCompound();
            chestTag.putUuid("ChestId", chestEntry.getKey());

            NbtList looterList = new NbtList();
            for (Map.Entry<UUID, Set<UUID>> looterEntry : chestEntry.getValue().entrySet()) {
                NbtCompound looterTag = new NbtCompound();
                looterTag.putUuid("LooterId", looterEntry.getKey());

                NbtList itemsList = new NbtList();
                for (UUID itemId : looterEntry.getValue()) {
                    NbtCompound itemTag = new NbtCompound();
                    itemTag.putUuid("ItemId", itemId);
                    itemsList.add(itemTag);
                }
                looterTag.put("DestroyedItems", itemsList);
                looterList.add(looterTag);
            }
            chestTag.put("Looters", looterList);
            chestList.add(chestTag);
        }

        nbt.put("ChestData", chestList);
        return nbt;
    }

    public static StageChestManager createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        StageChestManager state = new StageChestManager();

        if (nbt.contains("ChestData")) {
            NbtList chestList = nbt.getList("ChestData", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < chestList.size(); i++) {
                NbtCompound chestTag = chestList.getCompound(i);
                UUID chestId = chestTag.getUuid("ChestId");

                Map<UUID, Set<UUID>> looterMap = new HashMap<>();
                NbtList looterList = chestTag.getList("Looters", NbtElement.COMPOUND_TYPE);

                for (int j = 0; j < looterList.size(); j++) {
                    NbtCompound looterTag = looterList.getCompound(j);
                    UUID looterId = looterTag.getUuid("LooterId");

                    Set<UUID> itemSet = new HashSet<>();
                    NbtList itemsList = looterTag.getList("DestroyedItems", NbtElement.COMPOUND_TYPE);
                    for (int k = 0; k < itemsList.size(); k++) {
                        itemSet.add(itemsList.getCompound(k).getUuid("ItemId"));
                    }
                    looterMap.put(looterId, itemSet);
                }
                state.destroyedItems.put(chestId, looterMap);
            }
        }
        return state;
    }
}
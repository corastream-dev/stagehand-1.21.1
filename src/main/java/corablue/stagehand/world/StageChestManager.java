package corablue.stagehand.world;

import corablue.stagehand.Stagehand;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StageChestManager extends PersistentState {

    // Chest ID -> (Looter ID -> Set of Destroyed Item Instance IDs)
    private final Map<UUID, Map<UUID, Set<UUID>>> destroyedItems = new HashMap<>();

    // Chest ID -> (Looter ID -> Persistent Chest Inventory)
    private final Map<UUID, Map<UUID, DefaultedList<ItemStack>>> chestInventories = new HashMap<>();

    public StageChestManager() {}

    // --- Core API ---

    public static void markItemDestroyed(MinecraftServer server, UUID chestId, UUID looterId, UUID itemInstanceId) {
        StageChestManager state = getServerState(server);
        state.destroyedItems
                .computeIfAbsent(chestId, k -> new HashMap<>())
                .computeIfAbsent(looterId, k -> new HashSet<>())
                .add(itemInstanceId);
        state.markDirty();
    }

    public static boolean isItemInstanceDestroyed(MinecraftServer server, UUID chestId, UUID looterId, UUID itemInstanceId) {
        StageChestManager state = getServerState(server);
        Map<UUID, Set<UUID>> chestData = state.destroyedItems.get(chestId);
        if (chestData != null) {
            Set<UUID> looterData = chestData.get(looterId);
            return looterData != null && looterData.contains(itemInstanceId);
        }
        return false;
    }

    public static void resetSpecificDestroyedItem(MinecraftServer server, UUID chestId, UUID looterId, UUID itemInstanceId) {
        StageChestManager state = getServerState(server);
        Map<UUID, Set<UUID>> chestData = state.destroyedItems.get(chestId);
        if (chestData != null) {
            Set<UUID> looterData = chestData.get(looterId);
            if (looterData != null) {
                looterData.remove(itemInstanceId);

                // Keep the maps clean to prevent memory leaks!
                if (looterData.isEmpty()) {
                    chestData.remove(looterId);
                    if (chestData.isEmpty()) {
                        state.destroyedItems.remove(chestId);
                    }
                }
                state.markDirty();
            }
        }
    }

    public static void resetDestroyedStatus(MinecraftServer server, UUID chestId, UUID looterId) {
        StageChestManager state = getServerState(server);
        Map<UUID, Set<UUID>> chestData = state.destroyedItems.get(chestId);

        if (chestData != null) {
            chestData.remove(looterId);

            // Clean up the parent map if it's now empty
            if (chestData.isEmpty()) {
                state.destroyedItems.remove(chestId);
            }
            state.markDirty();
        }
    }

    public static void removeChest(MinecraftServer server, UUID chestId) {
        StageChestManager state = getServerState(server);
        boolean removed = false;

        if (state.destroyedItems.remove(chestId) != null) removed = true;
        if (state.chestInventories.remove(chestId) != null) removed = true;

        if (removed) {
            state.markDirty();
        }
    }

    // --- Inventory API ---

    public static DefaultedList<ItemStack> getOrCreatePlayerInventory(MinecraftServer server, UUID chestId, UUID looterId) {
        StageChestManager state = getServerState(server);
        return state.chestInventories
                .computeIfAbsent(chestId, k -> new HashMap<>())
                .computeIfAbsent(looterId, k -> DefaultedList.ofSize(27, ItemStack.EMPTY));
    }

    public static void savePlayerInventory(MinecraftServer server, UUID chestId, UUID looterId, DefaultedList<ItemStack> inventory) {
        StageChestManager state = getServerState(server);
        state.chestInventories
                .computeIfAbsent(chestId, k -> new HashMap<>())
                .put(looterId, inventory);
        state.markDirty();
    }

    // --- State Saving and Loading ---

    public static StageChestManager getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        Type<StageChestManager> type = new Type<>(StageChestManager::new, StageChestManager::createFromNbt, null);
        return persistentStateManager.getOrCreate(type, Stagehand.MOD_ID + "_stage_chest_manager");
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList chestList = new NbtList();

        Set<UUID> allChests = new HashSet<>();
        allChests.addAll(destroyedItems.keySet());
        allChests.addAll(chestInventories.keySet());

        for (UUID chestId : allChests) {
            NbtCompound chestTag = new NbtCompound();
            chestTag.putUuid("ChestId", chestId);

            NbtList looterList = new NbtList();
            Set<UUID> allLooters = new HashSet<>();
            if (destroyedItems.containsKey(chestId)) allLooters.addAll(destroyedItems.get(chestId).keySet());
            if (chestInventories.containsKey(chestId)) allLooters.addAll(chestInventories.get(chestId).keySet());

            for (UUID looterId : allLooters) {
                NbtCompound looterTag = new NbtCompound();
                looterTag.putUuid("LooterId", looterId);

                // Save Destroyed Items
                if (destroyedItems.containsKey(chestId) && destroyedItems.get(chestId).containsKey(looterId)) {
                    NbtList itemsList = new NbtList();
                    for (UUID itemId : destroyedItems.get(chestId).get(looterId)) {
                        NbtCompound itemTag = new NbtCompound();
                        itemTag.putUuid("ItemId", itemId);
                        itemsList.add(itemTag);
                    }
                    looterTag.put("DestroyedItems", itemsList);
                }

                // Save Inventory
                if (chestInventories.containsKey(chestId) && chestInventories.get(chestId).containsKey(looterId)) {
                    NbtCompound invTag = new NbtCompound();
                    Inventories.writeNbt(invTag, chestInventories.get(chestId).get(looterId), registryLookup);
                    looterTag.put("Inventory", invTag);
                }

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
                Map<UUID, DefaultedList<ItemStack>> invMap = new HashMap<>();

                NbtList looterList = chestTag.getList("Looters", NbtElement.COMPOUND_TYPE);

                for (int j = 0; j < looterList.size(); j++) {
                    NbtCompound looterTag = looterList.getCompound(j);
                    UUID looterId = looterTag.getUuid("LooterId");

                    if (looterTag.contains("DestroyedItems")) {
                        Set<UUID> itemSet = new HashSet<>();
                        NbtList itemsList = looterTag.getList("DestroyedItems", NbtElement.COMPOUND_TYPE);
                        for (int k = 0; k < itemsList.size(); k++) {
                            itemSet.add(itemsList.getCompound(k).getUuid("ItemId"));
                        }
                        looterMap.put(looterId, itemSet);
                    }

                    if (looterTag.contains("Inventory")) {
                        DefaultedList<ItemStack> inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);
                        Inventories.readNbt(looterTag.getCompound("Inventory"), inventory, registryLookup);
                        invMap.put(looterId, inventory);
                    }
                }
                if (!looterMap.isEmpty()) state.destroyedItems.put(chestId, looterMap);
                if (!invMap.isEmpty()) state.chestInventories.put(chestId, invMap);
            }
        }
        return state;
    }
}
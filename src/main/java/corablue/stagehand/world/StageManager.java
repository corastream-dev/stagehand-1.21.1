package corablue.stagehand.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StageManager extends PersistentState {

    // --- Global Hub & First Join Tracking ---
    private String hubDimension = "";
    private BlockPos hubPos = null;
    private final Set<UUID> knownPlayers = new HashSet<>();

    // --- NEW: Global Return Roster ---
    public record ReturnData(String dimension, double x, double y, double z, float yaw, float pitch) {}
    private final Map<UUID, ReturnData> playerReturns = new HashMap<>();

    public static StageManager createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        StageManager state = new StageManager();

        if (tag.contains("HubX")) {
            state.hubDimension = tag.getString("HubDim");
            state.hubPos = new BlockPos(tag.getInt("HubX"), tag.getInt("HubY"), tag.getInt("HubZ"));
        }

        if (tag.contains("KnownPlayers")) {
            NbtList list = tag.getList("KnownPlayers", NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size(); i++) {
                state.knownPlayers.add(UUID.fromString(list.getString(i)));
            }
        }

        // Load Global Return Data
        if (tag.contains("PlayerReturns")) {
            NbtList list = tag.getList("PlayerReturns", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound rTag = list.getCompound(i);
                state.playerReturns.put(
                        rTag.getUuid("UUID"),
                        new ReturnData(
                                rTag.getString("Dim"), rTag.getDouble("X"), rTag.getDouble("Y"), rTag.getDouble("Z"),
                                rTag.getFloat("Yaw"), rTag.getFloat("Pitch")
                        )
                );
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        if (hubPos != null) {
            nbt.putString("HubDim", hubDimension);
            nbt.putInt("HubX", hubPos.getX());
            nbt.putInt("HubY", hubPos.getY());
            nbt.putInt("HubZ", hubPos.getZ());
        }

        NbtList list = new NbtList();
        for (UUID uuid : knownPlayers) {
            list.add(NbtString.of(uuid.toString()));
        }
        nbt.put("KnownPlayers", list);

        // Save Global Return Data
        NbtList returnList = new NbtList();
        for (Map.Entry<UUID, ReturnData> entry : playerReturns.entrySet()) {
            NbtCompound rTag = new NbtCompound();
            rTag.putUuid("UUID", entry.getKey());
            rTag.putString("Dim", entry.getValue().dimension());
            rTag.putDouble("X", entry.getValue().x());
            rTag.putDouble("Y", entry.getValue().y());
            rTag.putDouble("Z", entry.getValue().z());
            rTag.putFloat("Yaw", entry.getValue().yaw());
            rTag.putFloat("Pitch", entry.getValue().pitch());
            returnList.add(rTag);
        }
        nbt.put("PlayerReturns", returnList);

        return nbt;
    }

    // --- Admin Hub Setters ---
    public void setGlobalHub(String dimension, BlockPos pos) {
        this.hubDimension = dimension;
        this.hubPos = pos;
        this.markDirty();
    }
    public String getHubDimension() { return this.hubDimension; }
    public BlockPos getHubPos() { return this.hubPos; }

    // --- First Join Check ---
    public boolean isNewPlayer(UUID uuid) {
        if (knownPlayers.contains(uuid)) return false;
        knownPlayers.add(uuid);
        this.markDirty();
        return true;
    }

    // --- NEW: Global Return Logic ---
    public void saveReturnData(UUID uuid, String dimension, double x, double y, double z, float yaw, float pitch) {
        this.playerReturns.put(uuid, new ReturnData(dimension, x, y, z, yaw, pitch));
        this.markDirty();
    }

    public ReturnData getReturnData(UUID uuid) {
        return this.playerReturns.get(uuid);
    }

    public void clearReturnData(UUID uuid) {
        this.playerReturns.remove(uuid);
        this.markDirty();
    }

    private static final PersistentState.Type<StageManager> TYPE = new PersistentState.Type<>(
            StageManager::new, StageManager::createFromNbt, null
    );

    public static StageManager getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return persistentStateManager.getOrCreate(TYPE, "stagehand_stage_manager");
    }
}
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

    // --- Global Return Roster ---
    public record ReturnData(String dimension, double x, double y, double z, float yaw, float pitch) {}
    private final Map<UUID, ReturnData> playerReturns = new HashMap<>();

    // --- NEW: Spiral Stage Allocation ---
    private int nextStageIndex = 0;
    private final Map<UUID, BlockPos> playerStages = new HashMap<>();

    // A 1000-block radius means each stage requires a 2000x2000 block cell to prevent bleeding.
    private static final int STAGE_SPACING = 2000;

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

        // --- NEW: Load Spiral Grid Data ---
        if (tag.contains("NextStageIndex")) {
            state.nextStageIndex = tag.getInt("NextStageIndex");
        }

        if (tag.contains("PlayerStages")) {
            NbtList list = tag.getList("PlayerStages", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound stageTag = list.getCompound(i);
                state.playerStages.put(
                        stageTag.getUuid("UUID"),
                        new BlockPos(stageTag.getInt("X"), stageTag.getInt("Y"), stageTag.getInt("Z"))
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

        // --- NEW: Save Spiral Grid Data ---
        nbt.putInt("NextStageIndex", nextStageIndex);

        NbtList stageList = new NbtList();
        for (Map.Entry<UUID, BlockPos> entry : playerStages.entrySet()) {
            NbtCompound stageTag = new NbtCompound();
            stageTag.putUuid("UUID", entry.getKey());
            stageTag.putInt("X", entry.getValue().getX());
            stageTag.putInt("Y", entry.getValue().getY());
            stageTag.putInt("Z", entry.getValue().getZ());
            stageList.add(stageTag);
        }
        nbt.put("PlayerStages", stageList);

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

    // --- Global Return Logic ---
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

    // --- NEW: Spiral Grid Logic ---

    /**
     * Retrieves the player's saved Stage coordinates, or assigns them a new location
     * on the spiral grid if they don't have one yet.
     */
    public BlockPos getOrCreatePlayerStage(UUID uuid) {
        if (playerStages.containsKey(uuid)) {
            return playerStages.get(uuid);
        }

        BlockPos newPos = calculateSpiralPos(nextStageIndex, STAGE_SPACING);
        playerStages.put(uuid, newPos);
        nextStageIndex++;
        this.markDirty();

        return newPos;
    }

    /**
     * Generates a square spiral expanding outwards from 0,0.
     * Maps index 0 -> (0,0), index 1 -> (1,0), index 2 -> (1,-1), index 3 -> (0,-1), etc.
     */
    private BlockPos calculateSpiralPos(int index, int spacing) {
        if (index == 0) return new BlockPos(0, 64, 0); // Assuming Y=64 is the baseline

        // Calculate which "ring" the index is on
        int r = (int) Math.floor((Math.sqrt(index + 1) - 1) / 2) + 1;

        // Find the maximum index of the previous ring
        int prevMax = (2 * r - 1) * (2 * r - 1) - 1;

        // Find where we are situated on the current ring
        int offset = index - prevMax;
        int sideLength = 2 * r;

        int x = 0;
        int z = 0;

        if (offset <= sideLength) {
            // Right side (moving up/negative Z)
            x = r;
            z = (r - 1) - (offset - 1);
        } else if (offset <= 2 * sideLength) {
            // Top side (moving left/negative X)
            x = r - (offset - sideLength);
            z = -r;
        } else if (offset <= 3 * sideLength) {
            // Left side (moving down/positive Z)
            x = -r;
            z = -r + (offset - 2 * sideLength);
        } else {
            // Bottom side (moving right/positive X)
            x = -r + (offset - 3 * sideLength);
            z = r;
        }

        return new BlockPos(x * spacing, 64, z * spacing);
    }

    private static final PersistentState.Type<StageManager> TYPE = new PersistentState.Type<>(
            StageManager::new, StageManager::createFromNbt, null
    );

    public static StageManager getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return persistentStateManager.getOrCreate(TYPE, "stagehand_stage_manager");
    }
}
package corablue.stagehand.block.entity;

import corablue.stagehand.world.StageManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import corablue.stagehand.block.ModBlocks;
import corablue.stagehand.world.ModDimensions;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

public class StageProjectorBlockEntity extends BlockEntity {
    private UUID owner = null;
    private String worldKey = "";
    private int warmupTimer = 0;
    private int teleportRadius = 0;

    //Animation
    public float hingeAngle = 0.0f;
    public float prevHingeAngle = 0.0f;
    public boolean clientWasPowered = false;

    private boolean hasGeneratedStage = false;
    private BlockPos stageSpawnPos = BlockPos.ORIGIN;
    private boolean isStageReady = false;

    public int getWarmupTimer() { return this.warmupTimer; }
    public void setWarmupTimer(int timer) { this.warmupTimer = timer; markDirty(); }
    public int getTeleportRadius() { return this.teleportRadius; }
    public void setTeleportRadius(int radius) { this.teleportRadius = radius; markDirty(); }

    public static void tick(net.minecraft.world.World world, BlockPos pos, BlockState state, StageProjectorBlockEntity be) {
        be.prevHingeAngle = be.hingeAngle;

        boolean isPowered = state.get(corablue.stagehand.block.custom.StageProjectorBlock.POWERED);

        if (world.isClient) {
            if (isPowered && !be.clientWasPowered) {
                be.warmupTimer = 40;
            }
            be.clientWasPowered = isPowered;
        }

        // --- The Hinge Animation ---
        boolean shouldBeOpen = isPowered || be.warmupTimer > 0;

        if (shouldBeOpen && be.hingeAngle > -1.5708f) {
            be.hingeAngle -= 0.8f; // Lowered from 0.6 to give the smoothing room to breathe
            if (be.hingeAngle < -1.5708f) be.hingeAngle = -1.5708f;
        }

        if (!shouldBeOpen && be.hingeAngle < 0.0f) {
            be.hingeAngle += 0.8f;
            if (be.hingeAngle > 0.0f) be.hingeAngle = 0.0f;
        }

        // --- The Countdown ---
        // Allow BOTH the client and the server to tick down the timer!
        if (be.warmupTimer > 0) {
            be.warmupTimer--;

            // When the countdown hits zero, Fire (but only on the server!)
            if (!world.isClient && be.warmupTimer == 0) {
                if (state.getBlock() instanceof corablue.stagehand.block.custom.StageProjectorBlock projectorBlock) {
                    projectorBlock.executeTeleport(world, pos, state, be);
                }
            }
        }
    }

    public StageProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STAGE_PROJECTOR_BE, pos, state);
    }

    public void setWorldKey(String key) {
        this.worldKey = key;
        this.markDirty();
    }

    public String getWorldKey() {
        return this.worldKey;
    }

    // --- The Deterministic Hash Math ---
    public BlockPos getStageCoordinate() {
        if (this.worldKey == null || this.worldKey.isEmpty()) return null;

        // Standardize the string so "MyBase" and "mybase" go to the same place
        String safeKey = this.worldKey.toLowerCase().trim();
        java.util.UUID hash = java.util.UUID.nameUUIDFromBytes(safeKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Modulo 29,000 keeps the coordinates safely within the 30M world border!
        int gridX = (int) (hash.getMostSignificantBits() % 29000);
        int gridZ = (int) (hash.getLeastSignificantBits() % 29000);

        return new BlockPos(gridX * 1000, 100, gridZ * 1000);
    }

    // Replace your old getOrGenerateStage method with this:
    public BlockPos getOrGenerateStage() {
        if (this.world == null || this.world.isClient) return null;

        BlockPos center = getStageCoordinate();
        if (center == null) return null;

        net.minecraft.server.world.ServerWorld stageDimension = this.world.getServer().getWorld(corablue.stagehand.world.ModDimensions.THE_STAGE);

        // Check if the platform exists by looking for the Config Block
        if (!(stageDimension.getBlockEntity(center) instanceof StageConfigBlockEntity)) {
            generatePlatform(stageDimension, center);
        }

        return center;
    }

    private void generatePlatform(net.minecraft.server.world.ServerWorld stageDimension, net.minecraft.util.math.BlockPos center) {
        // 1. Build a 31x31 Stage FLUSH with the center Config Block (Y offset is now 0)
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                net.minecraft.util.math.BlockPos pos = center.add(x, 0, z); // Changed from -1 to 0
                net.minecraft.block.BlockState state;

                int absX = Math.abs(x);
                int absZ = Math.abs(z);

                if (absX == 15 || absZ == 15) {
                    state = (absX == 15 && absZ == 15) ?
                            net.minecraft.block.Blocks.CHISELED_STONE_BRICKS.getDefaultState() : net.minecraft.block.Blocks.POLISHED_ANDESITE.getDefaultState();
                } else if (absX == 14 || absZ == 14) {
                    state = net.minecraft.block.Blocks.SPRUCE_WOOD.getDefaultState();
                } else if (absX <= 3 && absZ <= 3) {
                    state = net.minecraft.block.Blocks.OAK_PLANKS.getDefaultState();
                } else if (absX <= 7 && absZ <= 7) {
                    state = net.minecraft.block.Blocks.OAK_PLANKS.getDefaultState();
                } else {
                    state = net.minecraft.block.Blocks.OAK_PLANKS.getDefaultState();
                }

                stageDimension.setBlockState(pos, state);
            }
        }

        // 2. Add Corner Pillars & Basic Lighting (Shifted up to sit on the new floor)
        for (int cx : new int[]{-15, 15}) {
            for (int cz : new int[]{-15, 15}) {
                stageDimension.setBlockState(center.add(cx, 1, cz), Blocks.POLISHED_TUFF_WALL.getDefaultState());
                stageDimension.setBlockState(center.add(cx, 2, cz), net.minecraft.block.Blocks.TORCH.getDefaultState());
            }
        }

        // 3. Place the Config Block exactly in the center (it will overwrite the Andesite block seamlessly!)
        stageDimension.setBlockState(center, corablue.stagehand.block.ModBlocks.STAGE_CONFIG_BLOCK.getDefaultState());

        // 4. Inject the Owner UUID
        net.minecraft.block.entity.BlockEntity be = stageDimension.getBlockEntity(center);
        if (be instanceof StageConfigBlockEntity config) {
            config.setOwner(this.owner);
        }
    }

    // --- Getters & Setters ---
    public void setOwner(UUID uuid) { this.owner = uuid; markDirty(); }
    public boolean isOwner(PlayerEntity player) { return this.owner != null && this.owner.equals(player.getUuid()); }

    public boolean isStageReady() { return this.isStageReady; }
    public void setStageReady(boolean ready) { this.isStageReady = ready; markDirty(); }

    // --- Persistence ---

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (owner != null) nbt.putUuid("Owner", owner);
        nbt.putString("WorldKey", worldKey);
        nbt.putBoolean("HasGeneratedStage", hasGeneratedStage);
        nbt.putBoolean("IsStageReady", isStageReady);
        nbt.putInt("StageX", stageSpawnPos.getX());
        nbt.putInt("StageY", stageSpawnPos.getY());
        nbt.putInt("StageZ", stageSpawnPos.getZ());
        nbt.putInt("Warmup", warmupTimer);
        nbt.putInt("TeleportRadius", teleportRadius);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("Owner")) this.owner = nbt.getUuid("Owner");
        if (nbt.contains("WorldKey")) this.worldKey = nbt.getString("WorldKey");
        this.hasGeneratedStage = nbt.getBoolean("HasGeneratedStage");
        this.isStageReady = nbt.getBoolean("IsStageReady");
        int x = nbt.getInt("StageX");
        int y = nbt.getInt("StageY");
        int z = nbt.getInt("StageZ");
        this.stageSpawnPos = new BlockPos(x, y, z);
        this.warmupTimer = nbt.getInt("Warmup");
        this.teleportRadius = nbt.getInt("TeleportRadius");
    }
}
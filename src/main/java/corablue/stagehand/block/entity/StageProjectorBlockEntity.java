package corablue.stagehand.block.entity;

import corablue.stagehand.world.StageManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import corablue.stagehand.block.ModBlocks;
import corablue.stagehand.world.ModDimensions;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;
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

        //Structure manager
        StructureTemplateManager manager = stageDimension.getStructureTemplateManager();
        Identifier templateId = Identifier.of("stagehand", "stage_platform");
        Optional<StructureTemplate> templateOptional = manager.getTemplate(templateId);

        if (templateOptional.isPresent()) {
            StructureTemplate template = templateOptional.get();

            //Configure placement settings (Rotation, Mirroring, etc.)
            StructurePlacementData placementData = new StructurePlacementData()
                    .setMirror(BlockMirror.NONE)
                    .setRotation(BlockRotation.NONE)
                    .setIgnoreEntities(true); // Don't load saved cows/zombies

            // Structures place from the corner (lowest X, Y, Z), not the center.
            net.minecraft.util.math.Vec3i size = template.getSize();
            net.minecraft.util.math.BlockPos cornerPos = center.add(
                    -size.getX() / 2,
                    -6, // Adjust Y if you want the floor to be flush with the center
                    -size.getZ() / 2
            );

            //Place the structure!
            template.place(stageDimension, cornerPos, cornerPos, placementData, stageDimension.getRandom(), 2);
        }

        //Place the Config Block exactly in the center
        stageDimension.setBlockState(center, corablue.stagehand.block.ModBlocks.STAGE_CONFIG_BLOCK.getDefaultState());

        //Inject the Owner UUID
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
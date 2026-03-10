package corablue.stagehand.block.entity;

import corablue.stagehand.block.StageProjectorBlock;
import corablue.stagehand.world.StageManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import corablue.stagehand.block.ModBlocks;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtIo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;

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

        boolean isPowered = state.get(StageProjectorBlock.POWERED);

        if (world.isClient) {
            if (isPowered && !be.clientWasPowered) {
                be.warmupTimer = 40;
            }
            be.clientWasPowered = isPowered;
        }

        boolean shouldBeOpen = isPowered || be.warmupTimer > 0;
        if (shouldBeOpen && be.hingeAngle > -1.5708f) {
            be.hingeAngle -= 0.8f; // Lowered from 0.6 to give the smoothing room to breathe
            if (be.hingeAngle < -1.5708f) be.hingeAngle = -1.5708f;
        }
        if (!shouldBeOpen && be.hingeAngle < 0.0f) {
            be.hingeAngle += 0.8f;
            if (be.hingeAngle > 0.0f) be.hingeAngle = 0.0f;
        }

        if (be.warmupTimer > 0) {
            be.warmupTimer--;
            if (!world.isClient && be.warmupTimer == 0) {
                if (state.getBlock() instanceof StageProjectorBlock projectorBlock) {
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

    //Generates new stages in a spiral pattern starting at 0,0
    //This was a name to hash location % by Minecrafts maximum map size, 30M
    //But shaders and other mods did NOT like extreme coords
    //So we just have neighbors instead :)
    public BlockPos getStageCoordinate() {
        if (this.worldKey == null || this.worldKey.isEmpty() || this.world == null || this.world.isClient) {
            return null;
        }

        //Sanitize
        String safeKey = this.worldKey.toLowerCase().trim().replaceAll("[^a-z0-9_\\-]", "_");
        java.util.UUID hash = java.util.UUID.nameUUIDFromBytes(safeKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        return StageManager.getServerState(this.world.getServer()).getOrCreatePlayerStage(hash);
    }

    public BlockPos getOrGenerateStage() {
        if (this.world == null || this.world.isClient) return null;

        BlockPos center = getStageCoordinate();
        if (center == null) return null;

        net.minecraft.server.world.ServerWorld stageDimension = this.world.getServer().getWorld(corablue.stagehand.world.ModDimensions.THE_STAGE);

        // Check if the platform exists
        if (!(stageDimension.getBlockEntity(center) instanceof StageConfigBlockEntity)) {

            String safeKey = this.worldKey != null ? this.worldKey.toLowerCase().trim().replaceAll("[^a-z0-9_\\-]", "_") : "";
            generatePlatform(stageDimension, center, safeKey, this.owner);
        }

        return center;
    }

    public static void generatePlatform(net.minecraft.server.world.ServerWorld stageDimension, net.minecraft.util.math.BlockPos center, String safeKey, java.util.UUID owner) {
        StructureTemplateManager manager = stageDimension.getStructureTemplateManager();
        Optional<StructureTemplate> templateOptional = Optional.empty();

        if (!safeKey.isEmpty()) {

            //Check for NBT structures that match the world name
            Path configDir = FabricLoader.getInstance().getConfigDir().resolve("stagehand").resolve("structures");

            if (!Files.exists(configDir)) {
                try {
                    Files.createDirectories(configDir);
                } catch (Exception e) {
                    corablue.stagehand.Stagehand.LOGGER.warn("Could not create global structures directory.");
                }
            }

            Path globalStructurePath = configDir.resolve(safeKey + ".nbt");

            if (Files.exists(globalStructurePath)) {
                NbtCompound nbt = null;

                // First, try reading it as a standard compressed Minecraft Structure
                try (InputStream stream = Files.newInputStream(globalStructurePath)) {
                    nbt = NbtIo.readCompressed(stream, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
                } catch (Exception e) {
                    // If it fails (likely a ZipException), try reading it as an UNCOMPRESSED NBT file
                    try (InputStream stream2 = Files.newInputStream(globalStructurePath);
                         java.io.DataInputStream dataStream = new java.io.DataInputStream(stream2)) {

                        nbt = NbtIo.readCompound(dataStream, net.minecraft.nbt.NbtSizeTracker.ofUnlimitedBytes());
                    } catch (Exception e2) {
                        corablue.stagehand.Stagehand.LOGGER.error("Failed to load global structure (Compressed & Uncompressed): " + safeKey, e2);
                    }
                }

                if (nbt != null) {
                    try {
                        StructureTemplate template = manager.createTemplate(nbt);
                        templateOptional = Optional.of(template);
                    } catch (Exception e) {
                        corablue.stagehand.Stagehand.LOGGER.error("Failed to parse NBT data into a Structure Template for: " + safeKey, e);
                    }
                }
            } else {
                corablue.stagehand.Stagehand.LOGGER.warn("Global structure file not found at expected path: " + globalStructurePath.toAbsolutePath());
            }

            // Datapack fallback
            if (templateOptional.isEmpty()) {
                try {
                    Identifier customId = Identifier.of(safeKey);

                    if (!safeKey.contains(":")) {
                        Identifier stagehandId = Identifier.of("stagehand", safeKey);
                        templateOptional = manager.getTemplate(stagehandId);
                    }

                    if (templateOptional.isEmpty()) {
                        templateOptional = manager.getTemplate(customId);
                    }
                } catch (Exception e) {
                    //Aint NOTHIN
                }
            }
        }

        if (templateOptional.isEmpty()) {
            Identifier defaultId = Identifier.of("stagehand", "stage_platform");
            templateOptional = manager.getTemplate(defaultId);
        }

        //Right in the middle
        //If there's a config block in the NBT, place that at the center instead
        //It acts as a little 'spawn point'
        if (templateOptional.isPresent()) {
            StructureTemplate template = templateOptional.get();

            StructurePlacementData placementData = new StructurePlacementData()
                    .setMirror(BlockMirror.NONE)
                    .setRotation(BlockRotation.NONE)
                    .setIgnoreEntities(true);

            BlockPos configOffset = null;
            java.util.List<StructureTemplate.StructureBlockInfo> configBlocks = template.getInfosForBlock(
                    BlockPos.ORIGIN,
                    placementData,
                    ModBlocks.STAGE_CONFIG_BLOCK
                        );

            if (!configBlocks.isEmpty()) {
                configOffset = configBlocks.get(0).pos();
            }

            net.minecraft.util.math.BlockPos cornerPos;
            if (configOffset != null) {
                cornerPos = center.subtract(configOffset);
            } else {
                net.minecraft.util.math.Vec3i size = template.getSize();
                cornerPos = center.add(-size.getX() / 2, -6, -size.getZ() / 2);
            }

            template.place(stageDimension, cornerPos, cornerPos, placementData, stageDimension.getRandom(), 2);
        }

        stageDimension.setBlockState(center, corablue.stagehand.block.ModBlocks.STAGE_CONFIG_BLOCK.getDefaultState());

        net.minecraft.block.entity.BlockEntity be = stageDimension.getBlockEntity(center);
        if (be instanceof StageConfigBlockEntity config) {
            config.setOwner(owner);
        }
    }

    public void setOwner(UUID uuid) { this.owner = uuid; markDirty(); }
    public boolean isOwner(PlayerEntity player) { return this.owner != null && this.owner.equals(player.getUuid()); }

    public boolean isStageReady() { return this.isStageReady; }
    public void setStageReady(boolean ready) { this.isStageReady = ready; markDirty(); }

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
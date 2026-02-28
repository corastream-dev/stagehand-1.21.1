package corablue.stagehand.block.custom;

import com.mojang.serialization.MapCodec;
import corablue.stagehand.block.entity.StageConfigBlockEntity;
import corablue.stagehand.block.entity.StageProjectorBlockEntity;
import corablue.stagehand.world.ModDimensions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StageProjectorBlock extends BlockWithEntity {
    public static final net.minecraft.state.property.BooleanProperty POWERED = net.minecraft.state.property.Properties.POWERED;
    public static final net.minecraft.state.property.DirectionProperty FACING = net.minecraft.state.property.Properties.HORIZONTAL_FACING;
    public static final MapCodec<StageProjectorBlock> CODEC = createCodec(StageProjectorBlock::new);

    public StageProjectorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(POWERED, false).with(FACING, net.minecraft.util.math.Direction.NORTH));
    }

    @Override
    protected void appendProperties(net.minecraft.state.StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED, FACING);
    }

    @Override
    public net.minecraft.block.BlockRenderType getRenderType(BlockState state) {
        return net.minecraft.block.BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(net.minecraft.item.ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.block.entity.BlockEntityTicker<T> getTicker(World world, BlockState state, net.minecraft.block.entity.BlockEntityType<T> type) {
        return validateTicker(type, corablue.stagehand.block.entity.ModBlockEntities.STAGE_PROJECTOR_BE, StageProjectorBlockEntity::tick);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StageProjectorBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient || placer == null) return;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof StageProjectorBlockEntity projector) {
            projector.setOwner(placer.getUuid());

            // Check if they renamed the projector in an anvil!
            if (itemStack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)) {
                projector.setWorldKey(itemStack.getName().getString());
                placer.sendMessage(net.minecraft.text.Text.translatable("ui.stagehand.stage_projector.linked_custom", itemStack.getName().getString()));
            } else {
                // Default to their username
                projector.setWorldKey(placer.getName().getString());
                placer.sendMessage(net.minecraft.text.Text.translatable("ui.stagehand.stage_projector.linked"));
            }
        }
    }

    @Override
    protected net.minecraft.util.ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, net.minecraft.entity.player.PlayerEntity player, net.minecraft.util.Hand hand, net.minecraft.util.hit.BlockHitResult hit) {
        if (!world.isClient) {
            // Check if they are holding a renamed Name Tag
            if (stack.isOf(net.minecraft.item.Items.NAME_TAG) && stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof StageProjectorBlockEntity projector) {

                    // Only the owner (or an admin) can relink a projector
                    if (projector.isOwner(player) || player.hasPermissionLevel(2)) {
                        String newKey = stack.getName().getString();
                        projector.setWorldKey(newKey);
                        player.sendMessage(net.minecraft.text.Text.translatable("ui.stagehand.stage_projector.relinked", newKey), false);

                        // Consume the Name Tag in survival mode
                        if (!player.isCreative()) {
                            stack.decrement(1);
                        }
                        return net.minecraft.util.ItemActionResult.SUCCESS;
                    } else {
                        player.sendMessage(net.minecraft.text.Text.translatable("ui.stagehand.stage_projector.not_owner"), true);
                        return net.minecraft.util.ItemActionResult.FAIL;
                    }
                }
            }
        }
        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, net.minecraft.block.Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            boolean isCurrentlyPowered = state.get(POWERED);
            int power = world.getReceivedRedstonePower(pos);

            if (power > 0) {
                net.minecraft.block.entity.BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof corablue.stagehand.block.entity.StageProjectorBlockEntity projector) {

                    // 1. Rising Edge: Trigger state, start timer, and LOCK IN the initial radius!
                    if (!isCurrentlyPowered) {
                        world.setBlockState(pos, state.with(POWERED, true), 3);
                        projector.setTeleportRadius(power); // <-- Moved here!

                        if (projector.getWarmupTimer() == 0) {
                            projector.setWarmupTimer(30);
                            world.playSound(null, pos, corablue.stagehand.sound.ModSounds.TELEPORT_CHARGE, net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 1.0f);
                        }
                    }
                    // 2. We are already warming up. Only let the radius INCREASE, never step down!
                    else if (projector.getWarmupTimer() > 0) {
                        projector.setTeleportRadius(Math.max(projector.getTeleportRadius(), power));
                    }
                }
            }
            // 3. Falling Edge: The power dropped to 0
            else if (power == 0 && isCurrentlyPowered) {
                world.setBlockState(pos, state.with(POWERED, false), 3);
            }
        }
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
    }

    public void executeTeleport(World world, BlockPos pos, BlockState state, StageProjectorBlockEntity projector) {
        int power = projector.getTeleportRadius();
        if (power == 0) power = 15; // Ultimate fallback just in case of NBT weirdness

        BlockPos destination = projector.getOrGenerateStage();
        if (destination != null) {
            ServerWorld stageDimension = world.getServer().getWorld(ModDimensions.THE_STAGE);

            boolean isStageReady = false;
            StageConfigBlockEntity stageConfig = null;

            BlockEntity destBe = stageDimension.getBlockEntity(destination);
            if (destBe instanceof StageConfigBlockEntity config) {
                stageConfig = config;
                isStageReady = config.isStageReady();
            }

            //Max radius of teleport now 8 blocks
            Box teleportArea = new Box(pos).expand(power * 0.5);

            // Prepare spherical math
            net.minecraft.util.math.Vec3d center = pos.toCenterPos(); // Get exact center of block
            double radiusSquared = power * power; // Square it so we don't have to use slow Math.sqrt()

            // Get players, but filter out the corners of the box!
            List<ServerPlayerEntity> players = world.getEntitiesByClass(ServerPlayerEntity.class, teleportArea,
                    // Keep player IF: They are alive AND inside the sphere radius
                    p -> p.isAlive() && p.squaredDistanceTo(center) <= radiusSquared);

            for (ServerPlayerEntity player : players) {
                if (projector.isOwner(player) || isStageReady) {

                    if (!player.getWorld().getRegistryKey().equals(ModDimensions.THE_STAGE)) {
                        corablue.stagehand.world.StageManager manager = corablue.stagehand.world.StageManager.getServerState(world.getServer());
                        manager.saveReturnData(player.getUuid(), player.getWorld().getRegistryKey().getValue().toString(), player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
                    }

                    double offsetX = player.getX() - pos.getX();
                    double offsetY = player.getY() - pos.getY();
                    double offsetZ = player.getZ() - pos.getZ();

                    double destX = destination.getX() + offsetX;
                    double destY = destination.getY() + 1 + offsetY;
                    double destZ = destination.getZ() + offsetZ;

                    BlockPos footPos = BlockPos.ofFloored(destX, destY - 1, destZ);
                    boolean willFallToVoid = true;

                    BlockPos.Mutable checkPos = footPos.mutableCopy();
                    for (int y = footPos.getY(); y >= stageDimension.getBottomY(); y--) {
                        checkPos.setY(y);
                        if (!stageDimension.getBlockState(checkPos).isAir()) {
                            willFallToVoid = false;
                            break;
                        }
                    }

                    if (willFallToVoid) {
                        stageDimension.setBlockState(footPos, net.minecraft.block.Blocks.TINTED_GLASS.getDefaultState());
                    }

                    // --- TELEPORT & FIRE EFFECTS ---
                    player.teleport(stageDimension, destX, destY, destZ, player.getYaw(), player.getPitch());

                    // --- NEW: Enforce GameMode based on Whitelist ---
                    if (stageConfig != null) {
                        if (stageConfig.isBuilder(player)) {
                            // Owner or Builder -> Survival (Standard Build Mode)
                            if (!player.isCreative() && !player.isSpectator()) {
                                player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
                            }
                            player.sendMessage(net.minecraft.text.Text.translatable("ui.stagehand.stage_projector.welcome_builder"), true);
                        } else {
                            // Visitor -> Adventure Mode
                            if (!player.isCreative() && !player.isSpectator()) {
                                player.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
                            }
                            player.sendMessage(net.minecraft.text.Text.translatable("ui.stagehand.stage_projector.welcome_visitor"), true);
                        }
                    }

                    // 1. Play the "Boom" sound in the new dimension right where they landed
                    stageDimension.playSound(null, destX, destY, destZ, corablue.stagehand.sound.ModSounds.TELEPORT_FIRE, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);

                    // 2. Send the Network Packet to trigger the blinding white screen flash!
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new corablue.stagehand.network.FlashScreenPayload());
                }
            }
        }
    }
}
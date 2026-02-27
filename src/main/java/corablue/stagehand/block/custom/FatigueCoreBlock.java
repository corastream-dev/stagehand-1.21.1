package corablue.stagehand.block.custom;

import com.mojang.serialization.MapCodec;
import corablue.stagehand.block.entity.FatigueCoreBlockEntity;
import corablue.stagehand.block.entity.ModBlockEntities; // Make sure this matches your registry package!
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import corablue.stagehand.client.gui.FatigueCoreScreen;
import net.minecraft.client.MinecraftClient;

public class FatigueCoreBlock extends BlockWithEntity {
    public static final MapCodec<FatigueCoreBlock> CODEC = createCodec(FatigueCoreBlock::new);
    public static final DirectionProperty FACING = Properties.FACING;

    public FatigueCoreBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // getOpposite() makes the "front" face the player when placed
        return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    // --- 1. FIXED: Actually create the Block Entity ---
    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FatigueCoreBlockEntity(pos, state);
    }

    // --- 2. FIXED: Add the Ticker so the effect works ---
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // Only tick on the server side
        if (world.isClient) return null;

        // validateTicker ensures we are ticking the correct block entity type
        return validateTicker(type, ModBlockEntities.FATIGUE_CORE_BE, FatigueCoreBlockEntity::tick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof FatigueCoreBlockEntity fatigueCore) {
            // The security check should happen on both sides so the server doesn't
            // process ghost interactions, and the client doesn't open the GUI.
            if (!fatigueCore.isOwner(player)) {
                if (!world.isClient) {
                    player.sendMessage(Text.literal("§cYou do not own this Fatigue Core."), true);
                }
                return ActionResult.SUCCESS;
            }

            // Open GUI on the Client Side
            if (world.isClient) {
                MinecraftClient.getInstance().setScreen(
                        new FatigueCoreScreen(pos, fatigueCore.getRange(), fatigueCore.doesAffectOwner())
                );
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient || placer == null) return;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof FatigueCoreBlockEntity fatigueCore) {
            fatigueCore.setOwner(placer.getUuid());
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        // If the block is actually being removed/destroyed (not just updating blockstate properties)
        if (!state.isOf(newState.getBlock())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof FatigueCoreBlockEntity fatigueCore) {
                // Safely release all players trapped by this block!
                fatigueCore.releaseAllPlayers();
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }
}
package corablue.stagehand.block;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;
import corablue.stagehand.block.entity.FatigueCoreBlockEntity;
import corablue.stagehand.block.entity.ModBlockEntities;
import corablue.stagehand.client.gui.FatigueCoreScreen;
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
import net.minecraft.client.MinecraftClient;

//Fatigue Core gives nearby players mining fatigue
//Mimics the way many popular dungeon mods protect their structures
//But craftable and for builders, with a whitelist
//Many anti-griefing strategies that could be refined in the future

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

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FatigueCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return validateTicker(type, ModBlockEntities.FATIGUE_CORE_BE, FatigueCoreBlockEntity::tick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof FatigueCoreBlockEntity fatigueCore) {
            if (!fatigueCore.isOwner(player)) {
                if (!world.isClient) {
                    player.sendMessage(Text.translatable("ui.stagehand.fatigue_core.not_owner"), true);
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
        // If the block is actually being removed/destroyed
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
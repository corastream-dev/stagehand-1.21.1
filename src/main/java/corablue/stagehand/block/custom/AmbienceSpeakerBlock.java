package corablue.stagehand.block.custom;

import com.mojang.serialization.MapCodec;
import corablue.stagehand.Stagehand;
import corablue.stagehand.block.entity.AmbienceSpeakerBlockEntity;
import corablue.stagehand.block.entity.ModBlockEntities;
import corablue.stagehand.world.ModDimensions;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.block.Block;

import corablue.stagehand.client.gui.AmbienceSpeakerScreen;
import net.minecraft.client.MinecraftClient;

public class AmbienceSpeakerBlock extends BlockWithEntity {
    public static final MapCodec<AmbienceSpeakerBlock> CODEC = createCodec(AmbienceSpeakerBlock::new);
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    public AmbienceSpeakerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(POWERED, false));
    }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
    @Override public BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.isClient) return;
        boolean isPowered = world.isReceivingRedstonePower(pos);
        if (state.get(POWERED) != isPowered) {
            world.setBlockState(pos, state.with(POWERED, isPowered), 3);
        }
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // getOpposite() makes the "front" face the player when placed
        return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AmbienceSpeakerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) {
            return validateTicker(type, ModBlockEntities.AMBIENCE_SPEAKER_BE, AmbienceSpeakerBlockEntity::clientTick);
        }
        return null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof AmbienceSpeakerBlockEntity speaker) {
            if (!speaker.isOwner(player)) {
                if (!world.isClient) {
                    player.sendMessage(Text.literal("§cYou do not own this Ambience Speaker."), true);
                }
                return ActionResult.SUCCESS;
            }

            if (world.isClient) {
                MinecraftClient.getInstance().setScreen(
                        new AmbienceSpeakerScreen(pos, speaker.getCurrentSound(), speaker.getRange(), speaker.isPlaying())
                );
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient || placer == null) return;

        if (Stagehand.CONFIG.OnlyAllowSpeakerInStage() && !world.getRegistryKey().equals(ModDimensions.THE_STAGE)) {
            world.breakBlock(pos, true);
            if (placer instanceof PlayerEntity player) {
                player.sendMessage(Text.literal("§cAmbience Speakers can only be placed in The Stage."), true);
            }
            return;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof AmbienceSpeakerBlockEntity speaker) speaker.setOwner(placer.getUuid());
    }
}
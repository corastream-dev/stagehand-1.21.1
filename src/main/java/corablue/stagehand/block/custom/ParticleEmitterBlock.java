package corablue.stagehand.block.custom;

import com.mojang.serialization.MapCodec;
import corablue.stagehand.block.entity.ModBlockEntities;
import corablue.stagehand.block.entity.ParticleEmitterBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

// If you haven't made the screen yet, this import will show an error. That is expected for now!
import corablue.stagehand.client.gui.ParticleEmitterScreen;
import net.minecraft.client.MinecraftClient;

public class ParticleEmitterBlock extends BlockWithEntity {
    public static final MapCodec<ParticleEmitterBlock> CODEC = createCodec(ParticleEmitterBlock::new);
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    public ParticleEmitterBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.UP).with(POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }

    @Override
    public BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // If you click a wall (NORTH), getSide is NORTH.
        // The blue screen (model top) will now point NORTH (away from the wall).
        return this.getDefaultState()
                .with(FACING, ctx.getSide())
                .with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
    }


    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.isClient) return;
        boolean isPowered = world.isReceivingRedstonePower(pos);
        if (state.get(POWERED) != isPowered) {
            world.setBlockState(pos, state.with(POWERED, isPowered), 3);
        }
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ParticleEmitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // Particles ONLY spawn on the client side!
        if (world.isClient) {
            return validateTicker(type, ModBlockEntities.PARTICLE_EMITTER_BE, ParticleEmitterBlockEntity::clientTick);
        }
        return null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);

        if (be instanceof ParticleEmitterBlockEntity emitter) {
            // Keep your standard Stagehand security check
            if (!emitter.isOwner(player)) {
                if (!world.isClient) {
                    player.sendMessage(Text.literal("§cYou do not own this Particle Emitter."), true);
                }
                return ActionResult.SUCCESS;
            }

            // Open the GUI on the client side
            if (world.isClient) {
                MinecraftClient.getInstance().setScreen(new ParticleEmitterScreen(pos, emitter));
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient) return;
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ParticleEmitterBlockEntity emitter) {

            // 1. Read the saved custom data from the broken item
            NbtComponent customData = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            if (customData != null) {
                // Pass the NBT straight back into your existing readNbt logic!
                emitter.readNbt(customData.copyNbt(), world.getRegistryManager());
            }

            // 2. Set the owner (doing this after reading NBT ensures the *new* placer gets ownership)
            if (placer != null) {
                emitter.setOwner(placer.getUuid());
            }
        }
    }
}
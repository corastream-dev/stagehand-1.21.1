package corablue.stagehand.block.custom;

import com.mojang.serialization.MapCodec;
import corablue.stagehand.block.entity.StageConfigBlockEntity;
import corablue.stagehand.client.gui.StageConfigScreen;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class StageConfigBlock extends BlockWithEntity {
    public static final MapCodec<StageConfigBlock> CODEC = createCodec(StageConfigBlock::new);

    public StageConfigBlock(Settings settings) { super(settings); }
    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
    @Override public BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StageConfigBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof StageConfigBlockEntity config) {

            // Allow both Owner and Builders to open it
            if (!config.isBuilder(player)) {
                if (!world.isClient) player.sendMessage(Text.literal("§cOnly the Owner and Builders can access this terminal."), true);
                return ActionResult.SUCCESS;
            }

            if (world.isClient) {
                boolean isOwner = config.isOwner(player);
                MinecraftClient.getInstance().setScreen(
                        new StageConfigScreen(pos, isOwner, config.isStageReady(), config.getBuilderWhitelist())
                );
            }
        }
        return ActionResult.SUCCESS;
    }
}
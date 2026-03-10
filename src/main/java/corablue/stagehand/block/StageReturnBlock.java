package corablue.stagehand.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

//Very simple, when powered returns the player to the same place they left their previous world
//If set in config, returns player to their spawnpoint instead

public class StageReturnBlock extends Block {
    public static final net.minecraft.state.property.BooleanProperty POWERED = net.minecraft.state.property.Properties.POWERED;

    public StageReturnBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(POWERED, false));
    }

    @Override
    protected void appendProperties(net.minecraft.state.StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, net.minecraft.block.Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {

            if (!world.getRegistryKey().getValue().equals(net.minecraft.util.Identifier.of("stagehand", "the_stage"))) {
                return;
            }

            boolean isCurrentlyPowered = state.get(POWERED);
            int power = world.getReceivedRedstonePower(pos);

            if (power > 0 && !isCurrentlyPowered) {
                world.setBlockState(pos, state.with(POWERED, true), 3);

                net.minecraft.util.math.Box teleportArea = new net.minecraft.util.math.Box(pos).expand(power * 0.5f);
                java.util.List<ServerPlayerEntity> players = world.getEntitiesByClass(ServerPlayerEntity.class, teleportArea, net.minecraft.entity.Entity::isAlive);

                for (ServerPlayerEntity player : players) {
                    if (corablue.stagehand.world.StageReturnHandler.returnPlayer(player, corablue.stagehand.world.StageReturnHandler.FallbackMode.FORCE_OVERWORLD)) {
                        // TELEPORT SUCCESSFUL!
                        // Might wanna do something here later
                    }
                }
            }
            //Reset
            else if (power == 0 && isCurrentlyPowered) {
                world.setBlockState(pos, state.with(POWERED, false), 3);
            }
        }
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
    }
}
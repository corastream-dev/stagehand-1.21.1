package corablue.stagehand.block.custom;

import corablue.stagehand.block.entity.StageConfigBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class StageReturnBlock extends Block {
    public static final net.minecraft.state.property.BooleanProperty POWERED = net.minecraft.state.property.Properties.POWERED;

    public StageReturnBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(POWERED, false));
    }

    // Tell the block to register this property
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

            // Rising Edge: It just received power!
            if (power > 0 && !isCurrentlyPowered) {
                // Lock the block into the "Powered" state
                world.setBlockState(pos, state.with(POWERED, true), 3);

                // Grab players in the radius
                net.minecraft.util.math.Box teleportArea = new net.minecraft.util.math.Box(pos).expand(power);
                java.util.List<ServerPlayerEntity> players = world.getEntitiesByClass(ServerPlayerEntity.class, teleportArea, net.minecraft.entity.Entity::isAlive);

                for (ServerPlayerEntity player : players) {

                    // Attempt the return teleport
                    if (corablue.stagehand.world.StageReturnHandler.returnPlayer(player, corablue.stagehand.world.StageReturnHandler.FallbackMode.FORCE_OVERWORLD)) {

                        // TELEPORT SUCCESSFUL!
                        // Because returnPlayer already moved them, player.getWorld() and player.getX() are now their new Overworld coordinates!

                        // 1. Play the "Boom" sound at their arrival location
                        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), corablue.stagehand.sound.ModSounds.TELEPORT_FIRE, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);

                        // 2. Send the Network Packet for the instant screen flash
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new corablue.stagehand.network.FlashScreenPayload());
                    }
                }
            }
            // Falling Edge: The power dropped to 0 (button popped out)
            else if (power == 0 && isCurrentlyPowered) {
                // Unlock the block so it can fire again later
                world.setBlockState(pos, state.with(POWERED, false), 3);
            }
        }
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
    }
}
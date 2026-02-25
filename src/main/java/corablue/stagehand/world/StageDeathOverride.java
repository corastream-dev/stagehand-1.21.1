package corablue.stagehand.world;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class StageDeathOverride{

    public static void registerStageDeathOverride(){

        // --- Stage Dimension Death Override ---
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                if (player.getWorld().getRegistryKey().equals(ModDimensions.THE_STAGE)) {
                    if (player.getHealth() <= amount) {
                        // Attempt to rescue the player
                        boolean safelyReturned = corablue.stagehand.world.StageReturnHandler.returnPlayer(player, corablue.stagehand.world.StageReturnHandler.FallbackMode.RESPAWN_POINT);

                        if (safelyReturned) {
                            // TELEPORT SUCCESSFUL!
                            // They are now at their Bed or the Stage Hub.
                            // 1. Play sound at their arrival location
                            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), corablue.stagehand.sound.ModSounds.TELEPORT_FIRE, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);

                            // 2. Send the Network Packet for the instant screen flash
                            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new corablue.stagehand.network.FlashScreenPayload());

                            // 3. Cancel the actual death event!
                            return false;
                        }
                    }
                }
            }
            return true;
        });

    }

}
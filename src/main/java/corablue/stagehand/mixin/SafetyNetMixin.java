package corablue.stagehand.mixin;

import corablue.stagehand.network.FlashScreenPayload;
import corablue.stagehand.sound.ModSounds;
import corablue.stagehand.world.ModDimensions;
import corablue.stagehand.world.StageReturnHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SafetyNetMixin {

    @Inject(method = "tryUseTotem", at = @At("HEAD"), cancellable = true)
    private void stagehand$interceptDeath(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        // 1. Check if this is a Player
        if ((Object) this instanceof ServerPlayerEntity player) {

            // 2. Check if they are in The Stage
            if (player.getWorld().getRegistryKey().equals(ModDimensions.THE_STAGE)) {

                // 3. Attempt Rescue
                // We use your existing handler. 
                // Note: The handler MUST heal the player (setHealth) for this to work, 
                // otherwise they will just die again in the next tick.
                boolean rescued = StageReturnHandler.returnPlayer(player, StageReturnHandler.FallbackMode.RESPAWN_POINT);

                if (rescued) {
                    // 4. Effects (Sound/Flash)
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.TELEPORT_FIRE, SoundCategory.PLAYERS, 1.0f, 1.0f);

                    ServerPlayNetworking.send(player, new FlashScreenPayload());

                    // 5. CRITICAL: Tell the game "We handled the death"
                    // Returning 'true' makes the game think a Totem was used successfully,
                    // so it stops the death process. 
                    // Since we injected at HEAD, the vanilla code that consumes the item never runs.
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
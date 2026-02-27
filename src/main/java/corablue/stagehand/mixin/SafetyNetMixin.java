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
                boolean rescued = StageReturnHandler.returnPlayer(player, StageReturnHandler.FallbackMode.RESPAWN_POINT);

                if (rescued) {
                    //Set health to stop death logic
                    player.setHealth(player.getMaxHealth());
                    player.clearStatusEffects();

                    // 2. Schedule the teleport for the next tick to avoid recursion/crash
                    player.getServer().execute(() -> {
                        StageReturnHandler.returnPlayer(player, StageReturnHandler.FallbackMode.RESPAWN_POINT);
                    });

                    cir.setReturnValue(true);
                }
            }
        }
    }
}
package corablue.stagehand.mixin;

import corablue.stagehand.world.ModDimensions;
import corablue.stagehand.world.StageReturnHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class SafetyNetMixin {

    @Shadow public abstract float getHealth();
    @Shadow public abstract float getMaxHealth();

    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
    private void stagehand$interceptFatalDamage(DamageSource source, float amount, CallbackInfo ci) {

        //If this is a player...
        if ((Object) this instanceof ServerPlayerEntity player) {

            //Are we are in The Stage?
            if (player.getWorld().getRegistryKey().equals(ModDimensions.THE_STAGE)) {

                //Check if this damage is fatal (Current Health - Final Damage <= 0)
                if (this.getHealth() - amount <= 0) {

                    //Attempt Rescue
                    boolean rescued = StageReturnHandler.returnPlayer(player, StageReturnHandler.FallbackMode.RESPAWN_POINT);

                    if (rescued) {
                        //Heal them up so they don't arrive dead
                        player.setHealth(this.getMaxHealth());

                        //Sound and flash!
                        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), corablue.stagehand.sound.ModSounds.TELEPORT_FIRE, net.minecraft.sound.SoundCategory.PLAYERS, 1.0f, 1.0f);
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new corablue.stagehand.network.FlashScreenPayload());

                        //Don't waste totems!
                        ci.cancel();
                    }
                }
            }
        }
    }
}
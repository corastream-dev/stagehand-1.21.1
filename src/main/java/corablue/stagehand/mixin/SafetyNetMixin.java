package corablue.stagehand.mixin;

import corablue.stagehand.Stagehand;
import corablue.stagehand.world.ModDimensions;
import corablue.stagehand.world.StageReturnHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SafetyNetMixin {

    @Inject(method = "tryUseTotem", at = @At("HEAD"), cancellable = true)
    private void stagehand$interceptDeath(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        //Check if this is a Player
        if ((Object) this instanceof ServerPlayerEntity player) {

            //Check if they are in The Stage
            if (player.getWorld().getRegistryKey().equals(ModDimensions.THE_STAGE)) {

                StageReturnHandler.FallbackMode mode = Stagehand.CONFIG.StageDeathReturnsToSpawn()
                        ? StageReturnHandler.FallbackMode.RESPAWN_POINT
                        : StageReturnHandler.FallbackMode.FORCE_OVERWORLD;

                //Attempt Rescue
                boolean rescued = StageReturnHandler.returnPlayer(player, mode);

                if (rescued) {

                    //Achievement
                    corablue.stagehand.advancement.ModCriteria.STAGE_DEATH.trigger(player);

                    //Set health to stop death logic
                    player.setHealth(player.getMaxHealth());
                    player.clearStatusEffects();

                    //Schedule the teleport for the next tick to avoid recursion/crash
                    player.getServer().execute(() -> {
                        StageReturnHandler.returnPlayer(player, mode);
                    });

                    cir.setReturnValue(true);
                }
            }
        }
    }
}
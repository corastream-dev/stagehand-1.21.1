package corablue.stagehand.mixin;

import corablue.stagehand.Stagehand;
import corablue.stagehand.world.ModDimensions;
import corablue.stagehand.world.StageManager;
import corablue.stagehand.world.StageReturnHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SafetyNetMixin {

    @Inject(method = "tryUseTotem", at = @At("HEAD"), cancellable = true)
    private void stagehand$interceptDeath(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        // Check if this is a Player
        if ((Object) this instanceof ServerPlayerEntity player) {

            // Check if they are in The Stage
            if (player.getWorld().getRegistryKey().equals(ModDimensions.THE_STAGE)) {

                // Trigger the achievement
                corablue.stagehand.advancement.ModCriteria.STAGE_DEATH.trigger(player);

                // --- COMPLETELY RESET PLAYER STATE ---
                // Restore Health
                player.setHealth(player.getMaxHealth());
                player.clearStatusEffects();
                player.setFireTicks(0);

                // Schedule the teleport for the next tick to avoid recursion/crash
                player.getServer().execute(() -> {
                    StageManager manager = StageManager.getServerState(player.getServer());

                    StageReturnHandler.FallbackMode mode = Stagehand.CONFIG.StageDeathReturnsToSpawn()
                            ? StageReturnHandler.FallbackMode.RESPAWN_POINT
                            : StageReturnHandler.FallbackMode.FORCE_OVERWORLD;

                    // FIX: If the player has no return data, they spawned in the Hub and never left.
                    // Force them to use the Hub as their respawn point so they aren't kicked to the Overworld!
                    if (manager.getReturnData(player.getUuid()) == null) {
                        mode = StageReturnHandler.FallbackMode.RESPAWN_POINT;
                    }

                    // Execute the single, clean teleport
                    player.setHealth(player.getMaxHealth());
                    player.clearStatusEffects();
                    player.getHungerManager().setFoodLevel(20);
                    player.getHungerManager().setSaturationLevel(5.0f);
                    player.setFireTicks(0);
                    player.setAir(player.getMaxAir());
                    player.fallDistance = 0.0f;
                    StageReturnHandler.returnPlayer(player, mode);

                });

                player.getServer().execute(() -> {
                    player.setFireTicks(0);
                });

                // Cancel the vanilla death event
                cir.setReturnValue(true);
            }
        }
    }
}
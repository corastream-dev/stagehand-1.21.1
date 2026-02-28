package corablue.stagehand.mixin;

import corablue.stagehand.Stagehand;
import corablue.stagehand.world.ModDimensions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class StageTimeMixin {

    @Shadow public abstract RegistryKey<World> getRegistryKey();

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void stagehand$dynamicStageTime(CallbackInfoReturnable<Long> cir) {

        // Check if the current world is The Stage
        if (this.getRegistryKey().equals(ModDimensions.THE_STAGE)) {

            // If the config says we should NOT do the daylight cycle...
            if (!Stagehand.CONFIG.StageDoDaylightCycle()) {

                //Get time from config
                cir.setReturnValue((long) Stagehand.CONFIG.StageTimeOverride());
            }
        }
    }
}
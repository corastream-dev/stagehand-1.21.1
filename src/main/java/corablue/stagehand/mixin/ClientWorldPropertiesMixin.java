package corablue.stagehand.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.HeightLimitView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {

    @Inject(method = "getSkyDarknessHeight", at = @At("HEAD"), cancellable = true)
    private void stagehand$shiftHorizon(HeightLimitView world, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(-500.0);
    }
}
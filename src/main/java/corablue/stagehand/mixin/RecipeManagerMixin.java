package corablue.stagehand.mixin;

import com.google.gson.JsonElement;
import corablue.stagehand.Stagehand;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    // Intercepts the loading of recipes from JSONs
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At("HEAD"))
    private void stagehand$removeDisabledRecipes(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {

        if (Stagehand.CONFIG.DisableFatigueCore()) {
            // Remove the recipe
            map.remove(Identifier.of(Stagehand.MOD_ID, "fatigue_core"));
            map.remove(Identifier.of(Stagehand.MOD_ID, "fatigue_core_longer"));
        }
    }
}
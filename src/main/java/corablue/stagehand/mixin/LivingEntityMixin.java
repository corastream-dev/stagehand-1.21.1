package corablue.stagehand.mixin;

import corablue.stagehand.item.ModComponents;
import corablue.stagehand.item.StageChestTrackerComponent;
import corablue.stagehand.world.StageChestManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow public abstract ItemStack getActiveItem();

    // Hooks into the moment an entity finishes eating food or drinking a potion
    @Inject(method = "consumeItem", at = @At("HEAD"))
    private void onConsumeItem(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!entity.getWorld().isClient()) {
            ItemStack stack = this.getActiveItem();
            StageChestTrackerComponent tracker = stack.get(ModComponents.STAGE_CHEST_TRACKER);

            if (tracker != null && entity.getServer() != null) {
                StageChestManager.markItemDestroyed(
                        entity.getServer(),
                        tracker.chestId(),
                        tracker.ownerId(),
                        tracker.itemInstanceId()
                );
            }
        }
    }
}
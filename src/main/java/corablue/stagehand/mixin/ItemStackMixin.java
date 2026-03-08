package corablue.stagehand.mixin;

import corablue.stagehand.Stagehand;
import corablue.stagehand.item.ModComponents;
import corablue.stagehand.item.StageChestTrackerComponent;
import corablue.stagehand.world.StageChestManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "decrement", at = @At("HEAD"))
    private void onDecrement(int amount, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;

        // If this decrement reduces the stack to 0 or less, the item is gone!
        if (stack.getCount() - amount <= 0) {
            StageChestTrackerComponent tracker = stack.get(ModComponents.STAGE_CHEST_TRACKER);

            // Make sure the tracker exists AND the server is running
            if (tracker != null && Stagehand.SERVER != null) {
                StageChestManager.markItemDestroyed(
                        Stagehand.SERVER,
                        tracker.chestId(),
                        tracker.ownerId(),
                        tracker.itemInstanceId()
                );
            }
        }
    }
}
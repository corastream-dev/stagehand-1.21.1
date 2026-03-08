package corablue.stagehand.mixin;

import corablue.stagehand.item.ModComponents;
import corablue.stagehand.item.StageChestTrackerComponent;
import corablue.stagehand.world.StageChestManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        if (!entity.getWorld().isClient()) {
            ItemStack stack = null;

            // Handle dropped items (lava, despawning, void, etc.)
            if (entity instanceof ItemEntity itemEntity) {
                if (reason == Entity.RemovalReason.KILLED || (reason == Entity.RemovalReason.DISCARDED && !itemEntity.getStack().isEmpty())) {
                    stack = itemEntity.getStack();
                }
            }
            // Handle thrown consumable items (splash potions, ender pearls, snowballs)
            else if (entity instanceof net.minecraft.entity.projectile.thrown.ThrownItemEntity thrownEntity) {
                if (reason == Entity.RemovalReason.DISCARDED || reason == Entity.RemovalReason.KILLED) {
                    stack = thrownEntity.getStack();
                }
            }

            if (stack != null) {
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
}
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

        // Ensure we are on the server and the entity is an ItemEntity
        if (!entity.getWorld().isClient() && entity instanceof ItemEntity itemEntity) {

            // KILLED = Lava, Fire, Cactus, Explosions
            // DISCARDED = Despawn or Void (or picked up!)
            // If it was picked up, the stack will be empty. If it died/despawned naturally, it still has a count.
            if (reason == Entity.RemovalReason.KILLED || (reason == Entity.RemovalReason.DISCARDED && !itemEntity.getStack().isEmpty())) {
                ItemStack stack = itemEntity.getStack();
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
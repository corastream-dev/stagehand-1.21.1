package corablue.stagehand.mixin;

import corablue.stagehand.item.ModComponents;
import corablue.stagehand.item.StageChestTrackerComponent;
import corablue.stagehand.world.StageChestManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract boolean isDamageable();
    @Shadow public abstract int getDamage();
    @Shadow public abstract int getMaxDamage();

    // Hooks into the specific damage method used in 1.21.1 to process durability loss
    @Inject(method = "damage(ILnet/minecraft/server/world/ServerWorld;Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/function/Consumer;)V", at = @At("HEAD"))
    private void onDamage(int amount, ServerWorld world, ServerPlayerEntity player, Consumer<Item> breakCallback, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;

        // Check if this durability damage will cause the item to break
        if (this.isDamageable() && (this.getDamage() + amount >= this.getMaxDamage())) {
            StageChestTrackerComponent tracker = stack.get(ModComponents.STAGE_CHEST_TRACKER);

            if (tracker != null && world.getServer() != null) {
                StageChestManager.markItemDestroyed(
                        world.getServer(),
                        tracker.chestId(),
                        tracker.ownerId(),
                        tracker.itemInstanceId()
                );
            }
        }
    }
}
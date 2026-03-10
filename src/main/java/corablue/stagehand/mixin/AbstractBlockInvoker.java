package corablue.stagehand.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractBlock.class)
public interface AbstractBlockInvoker {

    // The @Invoker annotation binds this interface method to the protected vanilla method
    @Invoker("onEntityCollision")
    void invokeOnEntityCollision(BlockState state, World world, BlockPos pos, Entity entity);
}
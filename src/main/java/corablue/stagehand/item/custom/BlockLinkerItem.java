package corablue.stagehand.item.custom;

import corablue.stagehand.item.BlockLinkerComponent;
import corablue.stagehand.item.ModComponents;
import corablue.stagehand.world.ProxyStateManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

//Blocklinker creates a map of a source and target
//When you interact with the source, the target is invoked instead
//There are hooks to onUse and onSteppedOn
//Create a floors that burn or freeze the player by linking to magma/powdered snow
//Create an item frame that reads a lectern, etc

public class BlockLinkerItem extends Item {

    public BlockLinkerItem(Settings settings) { super(settings); }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || context.getWorld().isClient) return ActionResult.PASS;

        ServerWorld world = (ServerWorld) context.getWorld();
        BlockPos clickedPos = context.getBlockPos();
        ItemStack stack = context.getStack();

        // Initialize the state manager
        ProxyStateManager stateManager = ProxyStateManager.getServerState(world);

        // Sneak-click a block to clear its links AND the wand's memory
        if (player.isSneaking()) {
            stack.remove(ModComponents.BLOCK_LINKER);
            stateManager.removeLink(clickedPos);
            player.sendMessage(Text.literal("Cleared links for this block.").formatted(Formatting.YELLOW), true);
            return ActionResult.SUCCESS;
        }

        BlockLinkerComponent data = stack.get(ModComponents.BLOCK_LINKER);

        if (data == null) {
            // STEP 1: Clicked a Source Block (No component exists yet)
            stack.set(ModComponents.BLOCK_LINKER, new BlockLinkerComponent(clickedPos));
            player.sendMessage(Text.literal("Source selected. Now click the target to mimic.").formatted(Formatting.GREEN), true);

        } else {
            // STEP 2: Clicked a Target Block (Component has the source coordinates)
            BlockPos sourcePos = data.sourcePos();

            if (sourcePos.equals(clickedPos)) {
                player.sendMessage(Text.literal("Cannot link a block to itself!").formatted(Formatting.RED), true);
                stack.remove(ModComponents.BLOCK_LINKER);
                return ActionResult.SUCCESS;
            }

            // --- CYCLE DETECTION ---
            // Trace the chain of target blocks. If any of them point back to our source block, it's a loop!
            BlockPos currentCheck = clickedPos;
            while (currentCheck != null) {
                if (currentCheck.equals(sourcePos)) {
                    player.sendMessage(Text.literal("Cannot create a recursive link loop!").formatted(Formatting.RED), true);
                    stack.remove(ModComponents.BLOCK_LINKER);
                    return ActionResult.SUCCESS;
                }
                currentCheck = stateManager.getTarget(currentCheck);
            }
            // -----------------------

            // Save the link to the world file
            stateManager.addLink(sourcePos, clickedPos);

            player.sendMessage(Text.literal("Proxy Link established!").formatted(Formatting.GOLD), true);

            // Clear wand memory for the next link
            stack.remove(ModComponents.BLOCK_LINKER);
        }

        return ActionResult.SUCCESS;
    }
}
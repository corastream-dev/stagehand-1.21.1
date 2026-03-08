package corablue.stagehand.block.custom;

import com.mojang.serialization.MapCodec;
import corablue.stagehand.block.entity.StageChestBlockEntity;
import corablue.stagehand.world.StageChestManager;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class StageChestBlock extends BlockWithEntity {
    public static final MapCodec<StageChestBlock> CODEC = createCodec(StageChestBlock::new);

    public StageChestBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StageChestBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // Assuming you are doing a standard model. Change to ENTITYBLOCK_ANIMATED if you are making a custom chest lid animation
        return BlockRenderType.MODEL;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient && placer instanceof PlayerEntity player) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof StageChestBlockEntity stageChest) {
                stageChest.setOwner(player.getUuid());
            }
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof StageChestBlockEntity chest) {

            // Check if player is owner or server admin
            if (player.getUuid().equals(chest.getOwnerId()) || player.hasPermissionLevel(2)) {
                // Owner opens the config UI
                player.openHandledScreen(chest);
            } else {
                // Non-owner opens their instanced view of the chest
                handleLooting(chest, player, world);
            }
        }
        return ActionResult.CONSUME;
    }

    private void handleLooting(StageChestBlockEntity chest, PlayerEntity player, World world) {
        UUID pid = player.getUuid();
        StageChestBlockEntity.ChestMode mode = chest.getMode();

        boolean isEligible = false;
        boolean addTracker = false;
        String denyMessage = null;

        // 1. Determine if the player is allowed to see the loot
        switch (mode) {
            case SINGLE:
                if (!chest.hasPlayerLooted(pid)) {
                    isEligible = true;
                    chest.markPlayerLooted(pid);
                } else {
                    denyMessage = "This chest is empty.";
                }
                break;

            case INFINITE:
                isEligible = true;
                break;

            case TIMER:
                long lastLootTime = chest.getLastLootTime(pid);
                long currentTime = world.getTime();
                if (currentTime - lastLootTime >= chest.getTimerCooldownTicks()) {
                    isEligible = true;
                    chest.setLastLootTime(pid, currentTime);
                } else {
                    long remainingSeconds = (chest.getTimerCooldownTicks() - (currentTime - lastLootTime)) / 20;
                    denyMessage = "Try again in " + remainingSeconds + " seconds.";
                }
                break;

            case REFILL_ON_DESTROYED:
                boolean itemsLost = corablue.stagehand.world.StageChestManager.isItemDestroyed(world.getServer(), chest.getUuid(), pid);
                if (!chest.hasPlayerLooted(pid) || itemsLost) {
                    isEligible = true;
                    addTracker = true;
                    chest.markPlayerLooted(pid);
                    corablue.stagehand.world.StageChestManager.resetDestroyedStatus(world.getServer(), chest.getUuid(), pid);
                } else {
                    denyMessage = "You must lose your current items before getting more.";
                }
                break;
        }

        // Give them a heads up if they aren't getting new loot
        if (denyMessage != null) {
            player.sendMessage(Text.literal(denyMessage), true);
        }

        // 2. Open the UI
        openInstancedChest(chest, player, isEligible, addTracker);
    }

    private void openInstancedChest(StageChestBlockEntity chest, PlayerEntity player, boolean populateLoot, boolean addTracker) {
        // Create a temporary virtual inventory
        net.minecraft.inventory.SimpleInventory virtualInventory = new net.minecraft.inventory.SimpleInventory(27) {
            @Override
            public void onClose(PlayerEntity playerEntity) {
                super.onClose(playerEntity);
                // SAFETY NET: Drop anything left in the virtual chest when they close it.
                // Prevents losing accidental deposits or unlooted rewards.
                for (int i = 0; i < this.size(); i++) {
                    ItemStack stack = this.getStack(i);
                    if (!stack.isEmpty()) {
                        playerEntity.dropItem(stack, false);
                    }
                }
            }
        };

        // Fill it with the template items if they are eligible
        if (populateLoot) {
            net.minecraft.util.collection.DefaultedList<ItemStack> template = chest.getItems();
            for (int i = 0; i < template.size(); i++) {
                ItemStack templateStack = template.get(i);
                if (!templateStack.isEmpty()) {
                    ItemStack stackToGive = templateStack.copy(); // MUST COPY!

                    if (addTracker) {
                        stackToGive.set(corablue.stagehand.item.ModComponents.STAGE_CHEST_TRACKER,
                                new corablue.stagehand.item.StageChestTrackerComponent(chest.getUuid(), player.getUuid(), java.util.UUID.randomUUID())
                        );
                    }

                    virtualInventory.setStack(i, stackToGive);
                }
            }
        }

        // Play the chest open sound
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.5f, 1.0f);

        // Open the generic 9x3 chest UI for the player
        player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> net.minecraft.screen.GenericContainerScreenHandler.createGeneric9x3(syncId, playerInv, virtualInventory),
                chest.getDisplayName()
        ));
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof StageChestBlockEntity chestBlockEntity) {
                // Drop the owner's template items if the chest is broken
                ItemScatterer.spawn(world, pos, chestBlockEntity);
                world.updateComparators(pos, this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }
}
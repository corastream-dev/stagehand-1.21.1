package corablue.stagehand.block.custom;

import com.mojang.serialization.MapCodec;
import corablue.stagehand.block.entity.StageChestBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
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
            if (player.getUuid().equals(chest.getOwnerId()) || player.hasPermissionLevel(2)) {
                player.openHandledScreen(chest);
            } else {
                handleLooting(chest, player, world);
            }
        }
        return ActionResult.CONSUME;
    }

    private void handleLooting(StageChestBlockEntity chest, PlayerEntity player, World world) {
        UUID chestId = chest.getUuid();
        UUID pid = player.getUuid();

        // 1. Fetch persistent instanced inventory
        DefaultedList<ItemStack> pInvData = corablue.stagehand.world.StageChestManager.getOrCreatePlayerInventory(world.getServer(), chestId, pid);

        // 2. Perform Refill Logic
        boolean isFirstTime = !chest.hasPlayerLooted(pid);
        boolean dirty = false;

        if (isFirstTime) {
            for (int i = 0; i < chest.getItems().size(); i++) {
                ItemStack templateItem = chest.getItems().get(i);
                if (!templateItem.isEmpty()) {
                    ItemStack clone = templateItem.copy();
                    attachTrackerIfNeeded(clone, chest, pid);
                    pInvData.set(i, clone);
                }
            }
            chest.markPlayerLooted(pid);
            dirty = true;
        } else {
            boolean triggerRefill = false;

            switch (chest.getMode()) {
                case SINGLE:
                    break;
                case TIMER:
                    if (world.getTime() - chest.getLastLootTime(pid) >= chest.getTimerCooldownTicks()) {
                        triggerRefill = true;
                        chest.setLastLootTime(pid, world.getTime());
                    }
                    break;
                case REFILL_ON_DESTROYED:
                    if (corablue.stagehand.world.StageChestManager.isItemDestroyed(world.getServer(), chestId, pid)) {
                        triggerRefill = true;
                    }
                    break;
                case INFINITE:
                    triggerRefill = true;
                    break;
            }

            if (triggerRefill) {
                // Clear the tracker map for this player on this chest
                corablue.stagehand.world.StageChestManager.resetDestroyedStatus(world.getServer(), chestId, pid);

                for (ItemStack templateItem : chest.getItems()) {
                    if (templateItem.isEmpty()) continue;

                    // Safely handles duplicates of the same item in the template
                    long requiredCount = chest.getItems().stream().filter(s -> ItemStack.areItemsEqual(s, templateItem)).count();
                    long currentCount = pInvData.stream().filter(s -> ItemStack.areItemsEqual(s, templateItem)).count();

                    if (currentCount < requiredCount) {
                        for (int slot = 0; slot < pInvData.size(); slot++) {
                            if (pInvData.get(slot).isEmpty()) {
                                ItemStack clone = templateItem.copy();
                                attachTrackerIfNeeded(clone, chest, pid);
                                pInvData.set(slot, clone);
                                dirty = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (dirty) {
            corablue.stagehand.world.StageChestManager.savePlayerInventory(world.getServer(), chestId, pid, pInvData);
        }

        openInstancedChest(chest, player, pInvData);
    }

    private void openInstancedChest(StageChestBlockEntity chest, PlayerEntity player, DefaultedList<ItemStack> pInvData) {
        UUID chestId = chest.getUuid();
        UUID pid = player.getUuid();

        net.minecraft.inventory.SimpleInventory virtualInventory = new net.minecraft.inventory.SimpleInventory(27) {
            @Override
            public void markDirty() {
                super.markDirty();
                DefaultedList<ItemStack> currentItems = DefaultedList.ofSize(27, ItemStack.EMPTY);
                for(int i=0; i < 27; i++) {
                    currentItems.set(i, this.getStack(i));
                }
                corablue.stagehand.world.StageChestManager.savePlayerInventory(player.getServer(), chestId, pid, currentItems);
            }
        };

        for (int i = 0; i < pInvData.size(); i++) {
            virtualInventory.setStack(i, pInvData.get(i));
        }

        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.5f, 1.0f);

        player.openHandledScreen(new ExtendedScreenHandlerFactory<BlockPos>() {
            @Override
            public BlockPos getScreenOpeningData(net.minecraft.server.network.ServerPlayerEntity p) {
                return chest.getPos();
            }
            @Override
            public Text getDisplayName() {
                return chest.getDisplayName();
            }
            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity playerEntity) {
                // Pass a PropertyDelegate declaring they are NOT an owner (0)
                ArrayPropertyDelegate props = new ArrayPropertyDelegate(3);
                props.set(0, chest.getMode().ordinal());
                props.set(1, chest.getTimerCooldownTicks());
                props.set(2, 0);

                return new corablue.stagehand.screen.StageChestScreenHandler(syncId, playerInv, virtualInventory, props, chest.getPos());
            }
        });
    }

    private void attachTrackerIfNeeded(ItemStack stack, StageChestBlockEntity chest, UUID pid) {
        if (chest.getMode() == StageChestBlockEntity.ChestMode.REFILL_ON_DESTROYED || chest.getMode() == StageChestBlockEntity.ChestMode.SINGLE) {
            stack.set(corablue.stagehand.item.ModComponents.STAGE_CHEST_TRACKER,
                    new corablue.stagehand.item.StageChestTrackerComponent(chest.getUuid(), pid, java.util.UUID.randomUUID())
            );
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof StageChestBlockEntity chestBlockEntity) {
                ItemScatterer.spawn(world, pos, chestBlockEntity);
                world.updateComparators(pos, this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }
}
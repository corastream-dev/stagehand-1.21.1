package corablue.stagehand.block.custom;

import com.mojang.serialization.MapCodec;
import corablue.stagehand.block.entity.StageChestBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class StageChestBlock extends BlockWithEntity {
    public static final MapCodec<StageChestBlock> CODEC = createCodec(StageChestBlock::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public StageChestBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
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
    public BlockRenderType getRenderType(net.minecraft.block.BlockState state) {
        // This tells Minecraft: "Don't look for a JSON model, look for a BlockEntityRenderer!"
        return net.minecraft.block.BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
            world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.5f, 0.9f);

            if (player.getUuid().equals(chest.getOwnerId()) || player.isCreative()) {
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

        DefaultedList<ItemStack> pInvData = corablue.stagehand.world.StageChestManager.getOrCreatePlayerInventory(world.getServer(), chestId, pid);

        boolean isFirstTime = !chest.hasPlayerLooted(pid);
        boolean dirty = false;

        if (isFirstTime) {
            for (int i = 0; i < chest.getItems().size(); i++) {
                ItemStack templateItem = chest.getItems().get(i);
                if (!templateItem.isEmpty()) {
                    ItemStack clone = templateItem.copy();
                    attachTrackerIfNeeded(clone, chest, pid, getSlotUUID(chestId, pid, i));
                    pInvData.set(i, clone);
                }
            }
            chest.markPlayerLooted(pid);
            dirty = true;
        } else {
            // --- NEW: Surgical replacement for REFILL_ON_DESTROYED ---
            if (chest.getMode() == StageChestBlockEntity.ChestMode.REFILL) {
                for (int i = 0; i < chest.getItems().size(); i++) {
                    ItemStack templateItem = chest.getItems().get(i);
                    if (templateItem.isEmpty()) continue;

                    UUID slotUUID = getSlotUUID(chestId, pid, i);

                    // Only refill if THIS EXACT SLOT was destroyed
                    if (corablue.stagehand.world.StageChestManager.isItemInstanceDestroyed(world.getServer(), chestId, pid, slotUUID)) {
                        corablue.stagehand.world.StageChestManager.resetSpecificDestroyedItem(world.getServer(), chestId, pid, slotUUID);

                        for (int slot = 0; slot < pInvData.size(); slot++) {
                            if (pInvData.get(slot).isEmpty()) {
                                ItemStack clone = templateItem.copy();
                                attachTrackerIfNeeded(clone, chest, pid, slotUUID);
                                pInvData.set(slot, clone);
                                dirty = true;
                                break; // Move on to the next destroyed template item
                            }
                        }
                    }
                }
            }
            // --- Legacy batch replacement for TIMER and INFINITE ---
            else {
                boolean triggerRefill = false;

                switch (chest.getMode()) {
                    case TIMER:
                        if (world.getTime() - chest.getLastLootTime(pid) >= chest.getTimerCooldownTicks()) {
                            triggerRefill = true;
                            chest.setLastLootTime(pid, world.getTime());
                        }
                        break;
                    case INFINITE:
                        triggerRefill = true;
                        break;
                    default:
                        break;
                }

                if (triggerRefill) {
                    corablue.stagehand.world.StageChestManager.resetDestroyedStatus(world.getServer(), chestId, pid);

                    for (int i = 0; i < chest.getItems().size(); i++) {
                        ItemStack templateItem = chest.getItems().get(i);
                        if (templateItem.isEmpty()) continue;

                        long requiredCount = chest.getItems().stream().filter(s -> ItemStack.areItemsEqual(s, templateItem)).count();
                        long currentCount = pInvData.stream().filter(s -> ItemStack.areItemsEqual(s, templateItem)).count();

                        if (currentCount < requiredCount) {
                            for (int slot = 0; slot < pInvData.size(); slot++) {
                                if (pInvData.get(slot).isEmpty()) {
                                    ItemStack clone = templateItem.copy();
                                    attachTrackerIfNeeded(clone, chest, pid, getSlotUUID(chestId, pid, i));
                                    pInvData.set(slot, clone);
                                    dirty = true;
                                    break;
                                }
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

        // Use a local class so we can call a custom finish method easily
        class InstancedChestInventory extends net.minecraft.inventory.SimpleInventory {
            private boolean isInitializing = true; // Lock saves during setup

            public InstancedChestInventory() {
                super(27);
            }

            @Override
            public void markDirty() {
                super.markDirty();

                // Block saves while the loop is populating the chest
                if (isInitializing) return;

                DefaultedList<ItemStack> currentItems = DefaultedList.ofSize(27, ItemStack.EMPTY);
                for(int i = 0; i < 27; i++) {
                    currentItems.set(i, this.getStack(i));
                }
                corablue.stagehand.world.StageChestManager.savePlayerInventory(player.getServer(), chestId, pid, currentItems);
            }

            public void finishInitializing() {
                this.isInitializing = false;
            }
        }

        InstancedChestInventory virtualInventory = new InstancedChestInventory();

        // Populate the inventory WITHOUT triggering saves
        for (int i = 0; i < pInvData.size(); i++) {
            virtualInventory.setStack(i, pInvData.get(i));
        }

        // Unlock saves now that the inventory is safely loaded
        virtualInventory.finishInitializing();

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
                ArrayPropertyDelegate props = new ArrayPropertyDelegate(3);
                props.set(0, chest.getMode().ordinal());
                props.set(1, chest.getTimerCooldownTicks());
                props.set(2, 0);

                return new corablue.stagehand.screen.StageChestScreenHandler(syncId, playerInv, virtualInventory, props, chest.getPos());
            }
        });
    }

    private UUID getSlotUUID(UUID chestId, UUID pid, int slotIndex) {
        return java.util.UUID.nameUUIDFromBytes((chestId.toString() + pid.toString() + slotIndex).getBytes());
    }

    private void attachTrackerIfNeeded(ItemStack stack, StageChestBlockEntity chest, UUID pid, UUID instanceId) {
        if (chest.getMode() == StageChestBlockEntity.ChestMode.REFILL || chest.getMode() == StageChestBlockEntity.ChestMode.ONCE) {
            stack.set(corablue.stagehand.item.ModComponents.STAGE_CHEST_TRACKER,
                    new corablue.stagehand.item.StageChestTrackerComponent(chest.getUuid(), pid, instanceId)
            );
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof StageChestBlockEntity chestBlockEntity) {
                // Scatter the physical chest's template items
                ItemScatterer.spawn(world, pos, chestBlockEntity);

                // NEW: Purge the persistent data from the world save!
                if (!world.isClient && world.getServer() != null) {
                    corablue.stagehand.world.StageChestManager.removeChest(world.getServer(), chestBlockEntity.getUuid());
                }

                world.updateComparators(pos, this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }
}
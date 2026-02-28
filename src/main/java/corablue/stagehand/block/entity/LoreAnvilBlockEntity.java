package corablue.stagehand.block.entity;

import corablue.stagehand.screen.ModScreenHandlers;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LoreAnvilBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory<BlockPos> {
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(1, ItemStack.EMPTY);

    public LoreAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LORE_ANVIL_BE, pos, state);
    }

    @Override public DefaultedList<ItemStack> getItems() { return items; }
    @Override public Text getDisplayName() { return Text.literal("Lore Desk"); }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return this.pos; // Correctly sends position to the client
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInv, PlayerEntity player) {
        return new LoreAnvilScreenHandler(syncId, playerInv, this);
    }

    // This is the method that was appearing as "missing"
    public void applyEdit(String name, List<String> loreLines) { // Removed Rarity arg
        ItemStack stack = this.getStack(0);
        if (stack.isEmpty()) return;

        // 1. Name
        if (name != null && !name.isBlank()) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        } else {
            stack.remove(DataComponentTypes.CUSTOM_NAME);
        }

        // 2. Lore
        if (loreLines != null) {
            List<Text> textLore = new ArrayList<>();
            for (String line : loreLines) {
                if (!line.isBlank()) textLore.add(Text.literal(line));
            }
            stack.set(DataComponentTypes.LORE, new LoreComponent(textLore));
        }

        // Removed Rarity Logic

        this.markDirtyAndSync();
    }

    public void markDirtyAndSync() {
        this.markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
    }

    // --- NESTED SCREEN HANDLER (Only one definition!) ---
    public static class LoreAnvilScreenHandler extends ScreenHandler {
        private final LoreAnvilBlockEntity entity;

        public LoreAnvilScreenHandler(int syncId, PlayerInventory inventory, BlockPos pos) {
            this(syncId, inventory, (LoreAnvilBlockEntity) inventory.player.getWorld().getBlockEntity(pos));
        }

        public LoreAnvilScreenHandler(int syncId, PlayerInventory inventory, LoreAnvilBlockEntity entity) {
            super(ModScreenHandlers.LORE_ANVIL, syncId);
            this.entity = entity;

            // 1. INPUT SLOT (Center of the top area)
            // Let's move it down a bit to Y=45 so it sits nicely between Name and Lore
            this.addSlot(new Slot(entity, 0, 80, 45));

            // 2. PLAYER INVENTORY (Moved down for Generic 54 texture)
            // In generic_54, the inventory starts at Y = 140
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 9; ++j) {
                    this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 140 + i * 18));
                }
            }

            // 3. HOTBAR (Moved down)
            // Y = 198 is standard for generic_54
            for (int i = 0; i < 9; ++i) {
                this.addSlot(new Slot(inventory, i, 8 + i * 18, 198));
            }
        }

        @Override public boolean canUse(PlayerEntity player) { return true; }
        public LoreAnvilBlockEntity getEntity() { return entity; }

        @Override
        public ItemStack quickMove(PlayerEntity player, int invSlot) {
            ItemStack newStack = ItemStack.EMPTY;
            Slot slot = this.slots.get(invSlot);
            if (slot != null && slot.hasStack()) {
                ItemStack originalStack = slot.getStack();
                newStack = originalStack.copy();
                if (invSlot < 1) {
                    if (!this.insertItem(originalStack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
                } else if (!this.insertItem(originalStack, 0, 1, false)) return ItemStack.EMPTY;

                if (originalStack.isEmpty()) slot.setStack(ItemStack.EMPTY);
                else slot.markDirty();
            }
            return newStack;
        }
    }

    @Override public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, items, registryLookup);
    }
    @Override public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, items, registryLookup);
    }
}
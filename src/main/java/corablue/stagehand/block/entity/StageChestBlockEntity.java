package corablue.stagehand.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StageChestBlockEntity extends BlockEntity implements ImplementedInventory, ExtendedScreenHandlerFactory<BlockPos> {

    public enum ChestMode {
        SINGLE,
        INFINITE,
        TIMER,
        REFILL_ON_DESTROYED
    }

    private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(27, ItemStack.EMPTY);

    private UUID ownerId = null;
    private UUID chestId;
    private ChestMode mode = ChestMode.SINGLE;
    private int timerCooldownTicks = 24000; // 1 in-game day default

    // Stores UUID -> Timestamp (for timer) or 1 (for single/refill modes to mark as looted)
    private final Map<UUID, Long> playerLootData = new HashMap<>();

    protected final net.minecraft.screen.PropertyDelegate propertyDelegate = new net.minecraft.screen.PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> mode.ordinal();
                case 1 -> timerCooldownTicks;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> mode = ChestMode.values()[value];
                case 1 -> timerCooldownTicks = value;
            }
        }

        @Override
        public int size() {
            return 2; // We are tracking 2 variables: Mode and Timer
        }
    };

    public StageChestBlockEntity(BlockPos pos, BlockState state) {
        // NOTE: Make sure to add STAGE_CHEST to your ModBlockEntities class!
        super(ModBlockEntities.STAGE_CHEST_BE, pos, state);
    }

    // --- Core Logic ---
    public UUID getUuid() {
        if (this.chestId == null) {
            this.chestId = UUID.randomUUID(); // Generate a unique ID the first time it's asked
            this.markDirty();
        }
        return this.chestId;
    }

    public void setOwner(UUID uuid) {
        this.ownerId = uuid;
        this.markDirty();
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public void setMode(ChestMode mode) {
        this.mode = mode;
        this.markDirty();
    }

    public ChestMode getMode() {
        return this.mode;
    }

    public void setTimerCooldownTicks(int ticks) {
        this.timerCooldownTicks = ticks;
        this.markDirty();
    }

    public int getTimerCooldownTicks() {
        return this.timerCooldownTicks;
    }

    // --- Memory Logic ---

    public boolean hasPlayerLooted(UUID playerId) {
        return playerLootData.containsKey(playerId);
    }

    public void markPlayerLooted(UUID playerId) {
        playerLootData.put(playerId, 1L);
        this.markDirty();
    }

    public long getLastLootTime(UUID playerId) {
        return playerLootData.getOrDefault(playerId, 0L);
    }

    public void setLastLootTime(UUID playerId, long time) {
        playerLootData.put(playerId, time);
        this.markDirty();
    }

    // --- Inventory & GUI ---

    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.stagehand.stage_chest");
    }

    @Nullable
    @Override
    public net.minecraft.screen.ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new corablue.stagehand.screen.StageChestScreenHandler(syncId, playerInventory, this, this.propertyDelegate, this.pos);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return this.pos;
    }

    // --- NBT Serialization (1.21.1 format) ---

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);

        if (this.chestId != null) {
            nbt.putUuid("ChestId", this.chestId);
        }

        if (ownerId != null) {
            nbt.putUuid("Owner", ownerId);
        }
        nbt.putString("Mode", mode.name());
        nbt.putInt("TimerCooldown", timerCooldownTicks);

        NbtList memoryList = new NbtList();
        for (Map.Entry<UUID, Long> entry : playerLootData.entrySet()) {
            NbtCompound memoryTag = new NbtCompound();
            memoryTag.putUuid("PlayerId", entry.getKey());
            memoryTag.putLong("Data", entry.getValue());
            memoryList.add(memoryTag);
        }
        nbt.put("PlayerMemory", memoryList);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);

        if (nbt.contains("ChestId")) {
            this.chestId = nbt.getUuid("ChestId");
        }

        if (nbt.contains("Owner")) {
            this.ownerId = nbt.getUuid("Owner");
        }
        if (nbt.contains("Mode")) {
            this.mode = ChestMode.valueOf(nbt.getString("Mode"));
        }
        if (nbt.contains("TimerCooldown")) {
            this.timerCooldownTicks = nbt.getInt("TimerCooldown");
        }

        this.playerLootData.clear();
        if (nbt.contains("PlayerMemory")) {
            NbtList memoryList = nbt.getList("PlayerMemory", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < memoryList.size(); i++) {
                NbtCompound memoryTag = memoryList.getCompound(i);
                this.playerLootData.put(memoryTag.getUuid("PlayerId"), memoryTag.getLong("Data"));
            }
        }
    }
}
package corablue.stagehand.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class StageConfigBlockEntity extends BlockEntity {
    private UUID owner = null;
    private boolean isStageReady = false;
    private String builderWhitelist = "";

    public StageConfigBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STAGE_CONFIG_BE, pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        markDirtyAndSync();
    }

    public boolean isOwner(PlayerEntity player) {
        return this.owner != null && this.owner.equals(player.getUuid());
    }

    public boolean isBuilder(PlayerEntity player) {
        if (isOwner(player)) return true;
        String playerName = player.getName().getString().toLowerCase();
        for (String name : builderWhitelist.toLowerCase().split(",")) {
            if (name.trim().equals(playerName)) return true;
        }
        return false;
    }

    public boolean isStageReady() { return this.isStageReady; }
    public void setStageReady(boolean ready) { this.isStageReady = ready; markDirtyAndSync(); }

    public String getBuilderWhitelist() { return this.builderWhitelist; }
    public void setBuilderWhitelist(String whitelist) { this.builderWhitelist = whitelist; markDirtyAndSync(); }

    // --- NBT Saving & Loading ---
    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (owner != null) nbt.putUuid("Owner", owner);
        nbt.putBoolean("IsStageReady", isStageReady);
        nbt.putString("BuilderWhitelist", builderWhitelist);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("Owner")) this.owner = nbt.getUuid("Owner");
        this.isStageReady = nbt.getBoolean("IsStageReady");
        this.builderWhitelist = nbt.getString("BuilderWhitelist");
    }

    // --- Client Synchronization Methods ---
    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    private void markDirtyAndSync() {
        this.markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
    }
}
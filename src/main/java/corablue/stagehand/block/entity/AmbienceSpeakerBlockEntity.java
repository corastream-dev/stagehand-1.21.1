package corablue.stagehand.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AmbienceSpeakerBlockEntity extends BlockEntity {
    private UUID owner = null;
    private int range = 16;
    private float pitch = 1.0f;
    private boolean isPlaying = false;
    private Identifier currentSound = Identifier.of("minecraft", "ambient.cave");

    public AmbienceSpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AMBIENCE_SPEAKER_BE, pos, state); // Update registry reference!
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (owner != null) nbt.putUuid("Owner", owner);
        nbt.putInt("Range", range);
        nbt.putBoolean("IsPlaying", isPlaying);
        nbt.putFloat("Pitch", pitch); // Add this
        if (currentSound != null) nbt.putString("CurrentSound", currentSound.toString());
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("Owner")) this.owner = nbt.getUuid("Owner");
        this.range = nbt.getInt("Range");
        this.isPlaying = nbt.getBoolean("IsPlaying");
        if (nbt.contains("Pitch")) this.pitch = nbt.getFloat("Pitch"); else this.pitch = 1.0f; // Add this
        if (nbt.contains("CurrentSound")) this.currentSound = Identifier.of(nbt.getString("CurrentSound"));
    }

    public void setOwner(UUID uuid) { this.owner = uuid; markDirty(); }
    public boolean isOwner(PlayerEntity player) { return owner != null && owner.equals(player.getUuid()); }

    public float getPitch() { return this.pitch; }
    public int getRange() { return this.range; }
    public boolean isPlaying() { return this.isPlaying; }
    public Identifier getCurrentSound() { return this.currentSound; }

    public static void clientTick(World world, BlockPos pos, BlockState state, AmbienceSpeakerBlockEntity be) {
        corablue.stagehand.client.sound.SpeakerSoundManager.tickSpeaker(be);
    }

    public void updateSettings(Identifier sound, int range, boolean isPlaying, float pitch) {
        this.currentSound = sound;
        this.range = range;
        this.isPlaying = isPlaying;
        this.pitch = pitch;
        markDirtyAndSync();
    }

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
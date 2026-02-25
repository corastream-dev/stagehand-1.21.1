package corablue.stagehand.block.entity;

import corablue.stagehand.block.custom.AmbienceSpeakerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;


import java.util.UUID;

public class AmbienceSpeakerBlockEntity extends BlockEntity {
    private UUID owner = null;
    private int range = 16;
    private boolean isPlaying = false;
    private Identifier currentSound = Identifier.of("minecraft", "ambient.cave");

    public AmbienceSpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AMBIENCE_SPEAKER_BE, pos, state); // Update registry reference!
    }

    // --- Data Persistence ---
    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (owner != null) nbt.putUuid("Owner", owner);
        nbt.putInt("Range", range);
        nbt.putBoolean("IsPlaying", isPlaying);
        if (currentSound != null) nbt.putString("CurrentSound", currentSound.toString());
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("Owner")) this.owner = nbt.getUuid("Owner");
        this.range = nbt.getInt("Range");
        this.isPlaying = nbt.getBoolean("IsPlaying");
        if (nbt.contains("CurrentSound")) this.currentSound = Identifier.of(nbt.getString("CurrentSound"));
    }

    // --- Getters & Setters ---
    public void setOwner(UUID uuid) { this.owner = uuid; markDirty(); }
    public boolean isOwner(PlayerEntity player) { return owner != null && owner.equals(player.getUuid()); }

    public int getRange() { return this.range; }
    public boolean isPlaying() { return this.isPlaying; }
    public Identifier getCurrentSound() { return this.currentSound; }

    // --- The NEW Client-Side Tick ---
    public static void clientTick(World world, BlockPos pos, BlockState state, AmbienceSpeakerBlockEntity be) {
        // Call this EVERY tick so the manager can monitor and stop sounds if needed
        corablue.stagehand.client.sound.SpeakerSoundManager.tickSpeaker(be);
    }

    public void updateSettings(Identifier sound, int range, boolean isPlaying) {
        this.currentSound = sound;
        this.range = range;
        this.isPlaying = isPlaying;
        markDirtyAndSync();
    }

    // --- Sync ---
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
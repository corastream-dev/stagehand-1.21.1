package corablue.stagehand.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record OpenAmbienceSpeakerScreenPayload(BlockPos pos, Identifier sound, int range, boolean isPlaying, float pitch) implements CustomPayload {
    public static final CustomPayload.Id<OpenAmbienceSpeakerScreenPayload> ID = new CustomPayload.Id<>(Identifier.of("stagehand", "open_ambience_speaker"));

    public static final PacketCodec<RegistryByteBuf, OpenAmbienceSpeakerScreenPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeBlockPos(value.pos());
                buf.writeIdentifier(value.sound()); // Writes the Identifier safely
                buf.writeInt(value.range());
                buf.writeBoolean(value.isPlaying());
                buf.writeFloat(value.pitch());
            },
            buf -> new OpenAmbienceSpeakerScreenPayload(
                    buf.readBlockPos(),
                    buf.readIdentifier(), // Reads the Identifier back out
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readFloat()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
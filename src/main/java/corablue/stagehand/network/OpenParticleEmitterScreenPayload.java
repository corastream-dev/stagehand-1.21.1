package corablue.stagehand.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record OpenParticleEmitterScreenPayload(BlockPos pos) implements CustomPayload {
    public static final CustomPayload.Id<OpenParticleEmitterScreenPayload> ID = new CustomPayload.Id<>(Identifier.of("stagehand", "open_particle_emitter"));
    public static final PacketCodec<RegistryByteBuf, OpenParticleEmitterScreenPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeBlockPos(value.pos());
            },
            buf -> new OpenParticleEmitterScreenPayload(
                    buf.readBlockPos()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
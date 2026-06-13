package corablue.stagehand.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record OpenFatigueCoreScreenPayload(BlockPos pos, int range, boolean affectOwner) implements CustomPayload {
    public static final CustomPayload.Id<OpenFatigueCoreScreenPayload> ID = new CustomPayload.Id<>(Identifier.of("stagehand", "open_fatigue_core"));
    public static final PacketCodec<RegistryByteBuf, OpenFatigueCoreScreenPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeBlockPos(value.pos());
                buf.writeInt(value.range());
                buf.writeBoolean(value.affectOwner());
            },
            buf -> new OpenFatigueCoreScreenPayload(
                    buf.readBlockPos(),
                    buf.readInt(),
                    buf.readBoolean()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
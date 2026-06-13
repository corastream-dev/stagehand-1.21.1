package corablue.stagehand.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record OpenStageConfigScreenPayload(BlockPos pos, boolean isOwner, boolean isReady, String whitelist) implements CustomPayload {
    public static final CustomPayload.Id<OpenStageConfigScreenPayload> ID = new CustomPayload.Id<>(Identifier.of("stagehand", "open_stage_config"));
    public static final PacketCodec<RegistryByteBuf, OpenStageConfigScreenPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeBlockPos(value.pos());
                buf.writeBoolean(value.isOwner());
                buf.writeBoolean(value.isReady());
                buf.writeString(value.whitelist());
            },
            buf -> new OpenStageConfigScreenPayload(
                    buf.readBlockPos(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readString()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
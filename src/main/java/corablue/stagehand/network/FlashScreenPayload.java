package corablue.stagehand.network;

import corablue.stagehand.Stagehand;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FlashScreenPayload() implements CustomPayload {
    public static final CustomPayload.Id<FlashScreenPayload> ID = new CustomPayload.Id<>(Identifier.of(Stagehand.MOD_ID, "flash_screen"));
    // Since we just need the trigger and no extra data, we use PacketCodec.unit()
    public static final PacketCodec<RegistryByteBuf, FlashScreenPayload> CODEC = PacketCodec.unit(new FlashScreenPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
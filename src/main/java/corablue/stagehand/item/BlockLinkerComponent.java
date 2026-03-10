package corablue.stagehand.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;

public record BlockLinkerComponent(BlockPos sourcePos) {
    public static final Codec<BlockLinkerComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("sourcePos").forGetter(BlockLinkerComponent::sourcePos)
    ).apply(instance, BlockLinkerComponent::new));

    public static final PacketCodec<ByteBuf, BlockLinkerComponent> PACKET_CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, BlockLinkerComponent::sourcePos,
            BlockLinkerComponent::new
    );
}
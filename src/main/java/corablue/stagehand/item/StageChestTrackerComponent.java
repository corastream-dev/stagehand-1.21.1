package corablue.stagehand.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record StageChestTrackerComponent(UUID chestId, UUID ownerId, UUID itemInstanceId) {
    public static final Codec<StageChestTrackerComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Uuids.CODEC.fieldOf("chestId").forGetter(StageChestTrackerComponent::chestId),
            Uuids.CODEC.fieldOf("ownerId").forGetter(StageChestTrackerComponent::ownerId),
            Uuids.CODEC.fieldOf("itemInstanceId").forGetter(StageChestTrackerComponent::itemInstanceId)
    ).apply(instance, StageChestTrackerComponent::new));

    public static final PacketCodec<ByteBuf, StageChestTrackerComponent> PACKET_CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, StageChestTrackerComponent::chestId,
            Uuids.PACKET_CODEC, StageChestTrackerComponent::ownerId,
            Uuids.PACKET_CODEC, StageChestTrackerComponent::itemInstanceId,
            StageChestTrackerComponent::new
    );
}
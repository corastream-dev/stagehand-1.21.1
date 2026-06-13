package corablue.stagehand.network;

import net.minecraft.util.math.BlockPos;

public record OpenAmbienceSpeakerScreenPacket(BlockPos pos, net.minecraft.util.Identifier sound, int range, boolean isPlaying, float pitch) {}
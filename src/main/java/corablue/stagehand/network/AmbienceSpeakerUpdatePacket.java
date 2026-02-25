package corablue.stagehand.network;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record AmbienceSpeakerUpdatePacket(BlockPos pos, Identifier sound, int range, boolean isPlaying) {}
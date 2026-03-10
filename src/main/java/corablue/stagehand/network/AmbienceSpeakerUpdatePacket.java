package corablue.stagehand.network;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

// Added pitch float at the end
public record AmbienceSpeakerUpdatePacket(BlockPos pos, Identifier sound, int range, boolean isPlaying, float pitch) {}
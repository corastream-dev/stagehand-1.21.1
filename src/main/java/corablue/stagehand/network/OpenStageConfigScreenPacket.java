package corablue.stagehand.network;

import net.minecraft.util.math.BlockPos;

public record OpenStageConfigScreenPacket(BlockPos pos, boolean isOwner, boolean isReady, String whitelist) {}
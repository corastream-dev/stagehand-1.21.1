package corablue.stagehand.network;

import net.minecraft.util.math.BlockPos;

public record FatigueCoreUpdatePacket(BlockPos pos, int range, boolean affectOwner) {}
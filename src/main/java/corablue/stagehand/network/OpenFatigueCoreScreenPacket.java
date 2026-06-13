package corablue.stagehand.network;

import net.minecraft.util.math.BlockPos;

public record OpenFatigueCoreScreenPacket(BlockPos pos, int range, boolean affectOwner) {}
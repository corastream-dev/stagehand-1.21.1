package corablue.stagehand.network;
import net.minecraft.util.math.BlockPos;

public record StageConfigUpdatePacket(BlockPos pos, boolean isReady, String whitelist, String action) {}
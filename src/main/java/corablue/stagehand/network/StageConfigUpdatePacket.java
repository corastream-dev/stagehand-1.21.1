package corablue.stagehand.network;
import net.minecraft.util.math.BlockPos;

public record StageConfigUpdatePacket(BlockPos pos, boolean isReady, String whitelist, StageAction action) {

    public enum StageAction { RETURN, SAVE, GAMEMODE }

}
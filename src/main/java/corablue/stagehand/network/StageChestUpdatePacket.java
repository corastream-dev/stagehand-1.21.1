package corablue.stagehand.network;

import corablue.stagehand.block.entity.StageChestBlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

// owo-lib will automatically serialize/deserialize this record!
public record StageChestUpdatePacket(BlockPos pos, int modeOrdinal, int timerTicks) {

    // This is the method your ModNetwork.java is calling
    public static void handle(StageChestUpdatePacket message, ServerPlayerEntity player) {

        // Always queue world modifications on the main server thread
        player.server.execute(() -> {
            if (player.getWorld().getBlockEntity(message.pos()) instanceof StageChestBlockEntity chest) {

                // Security Check: Only the owner or an admin can change the chest settings
                if (player.getUuid().equals(chest.getOwnerId()) || player.hasPermissionLevel(2)) {

                    // Apply the new data
                    chest.setMode(StageChestBlockEntity.ChestMode.values()[message.modeOrdinal()]);
                    chest.setTimerCooldownTicks(message.timerTicks());

                    // Mark dirty so the server saves the new settings
                    chest.markDirty();
                }
            }
        });
    }
}
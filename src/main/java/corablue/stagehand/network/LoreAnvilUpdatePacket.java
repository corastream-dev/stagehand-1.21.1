package corablue.stagehand.network;

import corablue.stagehand.block.entity.LoreAnvilBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public record LoreAnvilUpdatePacket(BlockPos pos, String name, List<String> lore) {

    // Call this from your Network Receiver
    public static void handle(LoreAnvilUpdatePacket packet, ServerPlayerEntity player) {
        // FIXED: Use player.getServer(), not getServerWorld()
        if (player.getServer() != null) {
            player.getServer().execute(() -> {
                // Validation: Range check (ensure player is close to the block)
                if (player.squaredDistanceTo(packet.pos().toCenterPos()) > 64) return;

                BlockEntity be = player.getWorld().getBlockEntity(packet.pos());
                if (be instanceof LoreAnvilBlockEntity loreAnvil) {
                    loreAnvil.applyEdit(packet.name(), packet.lore());
                }
            });
        }
    }
}
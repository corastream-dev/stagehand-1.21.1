package corablue.stagehand.world;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.Objects;

public class StageReturnHandler {

    public enum FallbackMode {
        FORCE_OVERWORLD,
        RESPAWN_POINT,
        FAIL
    }

    public static boolean returnPlayer(ServerPlayerEntity player, FallbackMode mode) {

        // 1. Ask the Global Manager for their Return Data
        StageManager manager = StageManager.getServerState(player.getServer());
        StageManager.ReturnData returnData = manager.getReturnData(player.getUuid());

        // 2. Try to return them via their Global Memory Cache
        if (returnData != null) {
            RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(returnData.dimension()));
            ServerWorld targetWorld = player.getServer().getWorld(dimKey);

            if (targetWorld != null) {
                executeTeleport(player, targetWorld, returnData.x(), returnData.y(), returnData.z(), returnData.yaw(), returnData.pitch());

                // Clear their memory so they don't get stuck if they break things later!
                manager.clearReturnData(player.getUuid());
                return true;
            }
        }

        // --- 3. FALLBACK CONTINGENCIES (No cache found!) ---

        if (mode == FallbackMode.FORCE_OVERWORLD) {
            ServerWorld overworld = player.getServer().getWorld(World.OVERWORLD);
            if (overworld != null) {
                BlockPos spawnPos = overworld.getSpawnPos();
                executeTeleport(player, overworld, spawnPos.getX() + 0.5, spawnPos.getY() + 0.1, spawnPos.getZ() + 0.5, player.getYaw(), player.getPitch());
                return true;
            }

        } else if (mode == FallbackMode.RESPAWN_POINT) {
            if (manager.getHubPos() != null) {
                RegistryKey<World> hubDimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(manager.getHubDimension()));
                ServerWorld hubWorld = player.getServer().getWorld(hubDimKey);

                if (hubWorld != null) {
                    BlockPos hubPos = manager.getHubPos();
                    executeTeleport(player, hubWorld, hubPos.getX() + 0.5, hubPos.getY() + 0.1, hubPos.getZ() + 0.5, player.getYaw(), player.getPitch());
                    return true;
                }
            }

            RegistryKey<World> spawnDimKey = player.getSpawnPointDimension();
            ServerWorld targetWorld = player.getServer().getWorld(spawnDimKey);

            if (targetWorld != null) {
                BlockPos spawnPos = player.getSpawnPointPosition();
                if (spawnPos == null) spawnPos = targetWorld.getSpawnPos();
                executeTeleport(player, targetWorld, spawnPos.getX() + 0.5, spawnPos.getY() + 0.1, spawnPos.getZ() + 0.5, player.getYaw(), player.getPitch());
                return true;
            }
        }

        return false;
    }

    private static void executeTeleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {

        //Make sure we don't die
        player.setHealth(player.getMaxHealth());
        player.extinguish();
        player.setVelocity(0, 0, 0);
        player.fallDistance = 0.0f;

        //If the destination is NOT the stage dimension...
        if (!world.getRegistryKey().equals(ModDimensions.THE_STAGE)) {

            //...don't leave them stuck in Adventure Mode in the real world
            if (Objects.requireNonNull(player.interactionManager.getGameMode()) == GameMode.ADVENTURE) {
                player.changeGameMode(GameMode.SURVIVAL);
            }
        }

        // Perform the teleport
        player.teleport(world, x, y, z, yaw, pitch);
    }
}
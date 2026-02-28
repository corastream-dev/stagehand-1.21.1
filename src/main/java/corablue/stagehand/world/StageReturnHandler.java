package corablue.stagehand.world;

import corablue.stagehand.network.FlashScreenPayload;
import corablue.stagehand.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
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
        // 1. Reset state BEFORE teleport
        player.setHealth(player.getMaxHealth());
        player.extinguish();
        player.getAbilities().invulnerable = true; // Temporary safety
        player.setVelocity(0, 0, 0);
        player.fallDistance = 0.0f;

        // 2. Perform Teleport
        player.teleport(world, x, y, z, yaw, pitch);

        // 3. Update GameMode AFTER teleport is confirmed
        if (!world.getRegistryKey().equals(ModDimensions.THE_STAGE)) {
            if (player.interactionManager.getGameMode() == GameMode.ADVENTURE) {
                player.changeGameMode(GameMode.SURVIVAL);
            }
        }

        //SFX
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TELEPORT_FIRE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        ServerPlayNetworking.send(player, new FlashScreenPayload());

        player.getAbilities().invulnerable = false;
        player.sendAbilitiesUpdate();
    }
}
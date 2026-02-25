package corablue.stagehand.network;

import corablue.stagehand.block.entity.AmbienceSpeakerBlockEntity;
import corablue.stagehand.block.entity.FatigueCoreBlockEntity;
import corablue.stagehand.block.entity.LoreAnvilBlockEntity;
import corablue.stagehand.block.entity.StageConfigBlockEntity;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;

public class ModNetwork {
    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(Identifier.of("stagehand", "main"));

    public static void init() {

        //Lore Anvil Packet
        CHANNEL.registerServerbound(LoreAnvilUpdatePacket.class, (message, access) -> {
            var player = access.player();
            var world = player.getWorld();
            var be = world.getBlockEntity(message.pos());

            if (be instanceof LoreAnvilBlockEntity anvil) {
                if (player.squaredDistanceTo(message.pos().toCenterPos()) <= 64) {
                    // Just pass name and lore now
                    anvil.applyEdit(message.name(), message.lore());
                }
            }
        });

        //Particle Emitter Packet
        CHANNEL.registerServerbound(ParticleEmitterUpdatePacket.class, (message, access) -> {
            var world = access.player().getWorld();
            var be = world.getBlockEntity(message.pos());

            if (be instanceof corablue.stagehand.block.entity.ParticleEmitterBlockEntity emitter) {
                emitter.updateSettings(
                        message.particleType(),
                        message.r1(), message.g1(), message.b1(), // Color 1
                        message.r2(), message.g2(), message.b2(), // Color 2
                        message.scale(), message.gravity(), message.amount(), message.lifetime(), // Behavior & Timing
                        message.oX(), message.oY(), message.oZ(), // Offsets
                        message.aX(), message.aY(), message.aZ(), // Area Spread
                        message.minVX(), message.maxVX(),         // Velocity X
                        message.minVY(), message.maxVY(),         // Velocity Y
                        message.minVZ(), message.maxVZ()          // Velocity Z
                );
            }
        });

        //Teleport flash
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(corablue.stagehand.network.FlashScreenPayload.ID, corablue.stagehand.network.FlashScreenPayload.CODEC);

        //Fatigue Core Packet
        CHANNEL.registerServerbound(FatigueCoreUpdatePacket.class, (message, access) -> {
            // This runs on the server side
            BlockEntity be = access.player().getWorld().getBlockEntity(message.pos());

            if (be instanceof FatigueCoreBlockEntity fatigueCore) {
                // SECURITY: Ensure the player sending the packet actually owns the block!
                if (fatigueCore.isOwner(access.player())) {
                    fatigueCore.setRange(message.range());
                    fatigueCore.setAffectOwner(message.affectOwner());
                }
            }
        });

        //Ambience Speaker Packet
        CHANNEL.registerServerbound(AmbienceSpeakerUpdatePacket.class, (message, access) -> {
            // This runs on the server side
            BlockEntity be = access.player().getWorld().getBlockEntity(message.pos());

            if (be instanceof AmbienceSpeakerBlockEntity speaker) {
                // SECURITY: Check ownership again
                if (speaker.isOwner(access.player())) {
                    speaker.updateSettings(message.sound(), message.range(), message.isPlaying());
                }
            }
        });

        CHANNEL.registerServerbound(StageConfigUpdatePacket.class, (message, access) -> {
            ServerPlayerEntity player = access.player();
            BlockPos pos = message.pos();
            String action = message.action();

            // 1. Handle RETURN first.
            // It should not depend on the BlockEntity existing in the current world.
            if (action.equals("RETURN")) {
                corablue.stagehand.world.StageReturnHandler.returnPlayer(player, corablue.stagehand.world.StageReturnHandler.FallbackMode.FAIL);
                return; // Exit early so we don't process further logic after teleporting
            }

            // 2. Attempt to fetch the Block Entity for location-based actions (SAVE/GAMEMODE)
            BlockEntity be = player.getWorld().getBlockEntity(pos);
            if (be instanceof StageConfigBlockEntity config) {
                boolean isOwner = config.isOwner(player);
                boolean isBuilder = config.isBuilder(player);

                // Action: Save Config (Owner Only)
                if (action.equals("SAVE") && isOwner) {
                    config.setStageReady(message.isReady());
                    // Ensure we don't save a null string to the block entity
                    config.setBuilderWhitelist(message.whitelist() != null ? message.whitelist() : "");
                }

                // Action: Gamemode Swap (Owner & Builders)
                else if (action.equals("GAMEMODE") && isBuilder) {
                    if (player.interactionManager.getGameMode() == net.minecraft.world.GameMode.ADVENTURE) {
                        player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
                    } else {
                        player.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
                    }
                }
            }
        });

    }
}
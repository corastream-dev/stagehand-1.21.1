package corablue.stagehand.network;

import corablue.stagehand.block.entity.AmbienceSpeakerBlockEntity;
import corablue.stagehand.block.entity.FatigueCoreBlockEntity;
import corablue.stagehand.block.entity.LoreAnvilBlockEntity;
import corablue.stagehand.block.entity.StageConfigBlockEntity;
import corablue.stagehand.world.StageReturnHandler;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

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

        // Particle Emitter Packet
        CHANNEL.registerServerbound(ParticleEmitterUpdatePacket.class, (message, access) -> {
            var world = access.player().getWorld();
            var be = world.getBlockEntity(message.pos());

            if (be instanceof corablue.stagehand.block.entity.ParticleEmitterBlockEntity emitter) {
                emitter.updateSettings(
                        message.particleType(),
                        message.r1(), message.g1(), message.b1(),
                        message.r2(), message.g2(), message.b2(),
                        message.useLifetimeColor(), // Added
                        message.scale(), message.gravity(), message.amount(), message.lifetime(),
                        message.oX(), message.oY(), message.oZ(),
                        message.aX(), message.aY(), message.aZ(),
                        message.minVX(), message.maxVX(),
                        message.minVY(), message.maxVY(),
                        message.minVZ(), message.maxVZ(),
                        message.orbX(), message.orbY(), message.orbZ(),
                        message.rotate(),
                        message.emissive()
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

        //Stage Config Packet
        CHANNEL.registerServerbound(StageConfigUpdatePacket.class, (message, access) -> {
            ServerPlayerEntity player = access.player();
            BlockPos pos = message.pos();
            StageConfigUpdatePacket.StageAction action = message.action();

            //Get some info from the config block
            BlockEntity be = player.getWorld().getBlockEntity(pos);
            boolean isBuilder = false;
            boolean isOwner = false;
            if (be instanceof StageConfigBlockEntity config) {
                isOwner = config.isOwner(player);
                isBuilder = config.isBuilder(player);


                switch (action) {
                    case RETURN -> {
                        //Just return the player
                        corablue.stagehand.world.StageReturnHandler.returnPlayer(player, corablue.stagehand.world.StageReturnHandler.FallbackMode.FAIL);
                    }
                    case SAVE -> {
                        if (isOwner) {
                            //Save config data from block
                            config.setStageReady(message.isReady());
                            config.setBuilderWhitelist(message.whitelist() != null ? message.whitelist() : "");
                        }
                    }
                    case GAMEMODE -> {
                        if (isBuilder) {
                            switch (player.interactionManager.getGameMode()) {
                                case GameMode.ADVENTURE -> {
                                    player.changeGameMode(GameMode.SURVIVAL);
                                }
                                case GameMode.SURVIVAL -> {
                                    player.changeGameMode(GameMode.ADVENTURE);
                                }
                                case GameMode.CREATIVE -> {
                                    player.sendMessage(Text.literal("§cYou are in Creative Mode! Leave Creative Mode to toggle Adventure Mode."));
                                }
                                case GameMode.SPECTATOR -> {
                                    player.sendMessage(Text.literal("§cYou are in Spectator Mode! Leave Spectator Mode to toggle Adventure Mode."));
                                }
                            }
                        }
                    }
                }
            }
        });

    }
}
package corablue.stagehand;

import corablue.stagehand.block.ModBlocks;
import corablue.stagehand.block.entity.FatigueCoreBlockEntity;
import corablue.stagehand.block.entity.ModBlockEntities;
import corablue.stagehand.item.ModItemGroups;
import corablue.stagehand.item.ModItems;
import corablue.stagehand.network.ModNetwork;
import corablue.stagehand.screen.ModScreenHandlers;
import corablue.stagehand.sound.ModSounds;
import corablue.stagehand.world.ModDimensions;
import corablue.stagehand.world.StageDeathOverride;
import corablue.stagehand.world.StageManager;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import static net.minecraft.server.command.CommandManager.literal;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class Stagehand implements ModInitializer {
	public static final String MOD_ID = "stagehand";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Queue for delaying the first-join teleport by a few ticks to prevent client ghost chunks
	private static final Map<UUID, Integer> pendingHubTeleports = new HashMap<>();

	@Override
	public void onInitialize() {
		ModItemGroups.registerItemGroups();
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerBlockEntities();
		ModSounds.registerSounds();
		ModNetwork.init();
		ModDimensions.register();
		ModScreenHandlers.registerScreenHandlers();
		StageDeathOverride.registerStageDeathOverride();

		//Admin can now set Stages as worldspawns!
		//Detect new players and...
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			StageManager manager = StageManager.getServerState(server);

			// Add new players to the queue instead of teleporting instantly
			if (manager.isNewPlayer(player.getUuid()) && manager.getHubPos() != null) {
				pendingHubTeleports.put(player.getUuid(), 10);
			}
		});

		// Tick Event to process the delayed teleports
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!pendingHubTeleports.isEmpty()) {
				Iterator<Map.Entry<UUID, Integer>> it = pendingHubTeleports.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<UUID, Integer> entry = it.next();
					int ticksLeft = entry.getValue() - 1;

					if (ticksLeft <= 0) {
						ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
						if (player != null) {
							StageManager manager = StageManager.getServerState(server);
							if (manager.getHubPos() != null) {
								RegistryKey<World> hubDimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(manager.getHubDimension()));
								ServerWorld hubWorld = server.getWorld(hubDimKey);
								if (hubWorld != null) {
									BlockPos hubPos = manager.getHubPos();
									player.teleport(hubWorld, hubPos.getX() + 0.5, hubPos.getY() + 0.1, hubPos.getZ() + 0.5, player.getYaw(), player.getPitch());
								}
							}
						}
						it.remove();
					} else {
						entry.setValue(ticksLeft);
					}
				}
			}
		});

		//Return new players to hub on death if they have no spawn point yet
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive && newPlayer.getSpawnPointPosition() == null) {
				StageManager manager = StageManager.getServerState(newPlayer.getServer());
				if (manager.getHubPos() != null) {
					RegistryKey<World> hubDimKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(manager.getHubDimension()));
					ServerWorld hubWorld = newPlayer.getServer().getWorld(hubDimKey);
					if (hubWorld != null) {
						BlockPos hubPos = manager.getHubPos();
						newPlayer.teleport(hubWorld, hubPos.getX() + 0.5, hubPos.getY() + 0.1, hubPos.getZ() + 0.5, newPlayer.getYaw(), newPlayer.getPitch());
					}
				}
			}
		});

		// --- COMMANDS ---
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

			dispatcher.register(literal("setstagehub")
					.requires(source -> source.hasPermissionLevel(2))
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayer();
						if (player == null) return 0;
						StageManager manager = StageManager.getServerState(player.getServer());
						manager.setGlobalHub(player.getWorld().getRegistryKey().getValue().toString(), player.getBlockPos());
						player.sendMessage(Text.literal("§aGlobal Stage Hub has been set to your current location!"), false);
						return 1;
					}));

			dispatcher.register(literal("leavestage")
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayer();
						if (player == null) return 0;
						if (!player.getWorld().getRegistryKey().equals(ModDimensions.THE_STAGE)) {
							player.sendMessage(Text.literal("§cYou are not currently in a Stage!"), false);
							return 0;
						}
						// isDeath = false
						if (corablue.stagehand.world.StageReturnHandler.returnPlayer(player, corablue.stagehand.world.StageReturnHandler.FallbackMode.FAIL)) {
							return 1;
						} else {
							player.sendMessage(Text.literal("§cError: Could not find a safe return location!"), false);
							return 0;
						}
					}));

			//Might not need these anymore

			dispatcher.register(literal("ignorefatigue")
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayer();
						if (player == null) return 0;
						boolean successfullyOptedOut = false;
						for (FatigueCoreBlockEntity core : FatigueCoreBlockEntity.ACTIVE_CORES) {
							if (core.getWorld() == player.getWorld()) {
								if (core.isPlayerInRange(player)) {
									core.optOutPlayer(player.getUuid());
									successfullyOptedOut = true;
								}
							}
						}
						if (successfullyOptedOut) {
							player.sendMessage(Text.literal("§aYou have successfully opted out of this Fatigue Zone. Type /resumefatigue to opt back in."), false);
						} else {
							player.sendMessage(Text.literal("§cYou are not currently inside a Fatigue Zone."), false);
						}
						return 1;
					}));

			dispatcher.register(literal("resumefatigue")
					.executes(context -> {
						ServerPlayerEntity player = context.getSource().getPlayer();
						if (player == null) return 0;
						boolean successfullyOptedIn = false;
						for (FatigueCoreBlockEntity core : FatigueCoreBlockEntity.ACTIVE_CORES) {
							if (core.getWorld() == player.getWorld()) {
								double distance = Math.sqrt(core.getPos().getSquaredDistance(player.getPos()));
								if (distance <= core.getRange()) {
									core.optInPlayer(player.getUuid());
									successfullyOptedIn = true;
								}
							}
						}
						if (successfullyOptedIn) {
							player.sendMessage(Text.literal("§aYou have opted back into this Fatigue Zone."), false);
						} else {
							player.sendMessage(Text.literal("§cYou are not currently inside a Fatigue Zone."), false);
						}
						return 1;
					}));
		});
	}
}
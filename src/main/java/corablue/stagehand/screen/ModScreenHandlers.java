package corablue.stagehand.screen;

import corablue.stagehand.block.entity.LoreAnvilBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import corablue.stagehand.client.gui.StageChestScreen;

public class ModScreenHandlers {

    // 1. Define the Screen Handler Type
    // We use ExtendedScreenHandlerType because we need to pass a BlockPos from server to client
    public static final ScreenHandlerType<LoreAnvilBlockEntity.LoreAnvilScreenHandler> LORE_ANVIL =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of("modid", "lore_anvil"),
                    new ExtendedScreenHandlerType<>(LoreAnvilBlockEntity.LoreAnvilScreenHandler::new, BlockPos.PACKET_CODEC));

    public static final ScreenHandlerType<StageChestScreenHandler> STAGE_CHEST =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of("stagehand", "stage_chest"),
                    new ExtendedScreenHandlerType<>(StageChestScreenHandler::new, BlockPos.PACKET_CODEC));

    // 2. Call this method in your main Mod initializer
    public static void registerScreenHandlers() {
        // This just ensures the static field above is initialized
        System.out.println("Registering Screen Handlers for Stagehand");

        HandledScreens.register(ModScreenHandlers.STAGE_CHEST, StageChestScreen::new);
    }
}
package corablue.stagehand.screen;

import corablue.stagehand.block.entity.LoreAnvilBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ModScreenHandlers {

    // 1. Define the Screen Handler Type
    // We use ExtendedScreenHandlerType because we need to pass a BlockPos from server to client
    public static final ScreenHandlerType<LoreAnvilBlockEntity.LoreAnvilScreenHandler> LORE_ANVIL =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of("modid", "lore_anvil"),
                    new ExtendedScreenHandlerType<>(LoreAnvilBlockEntity.LoreAnvilScreenHandler::new, BlockPos.PACKET_CODEC));

    // 2. Call this method in your main Mod initializer
    public static void registerScreenHandlers() {
        // This just ensures the static field above is initialized
        System.out.println("Registering Screen Handlers for Stagehand");
    }
}
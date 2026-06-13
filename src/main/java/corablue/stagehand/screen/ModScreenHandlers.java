package corablue.stagehand.screen;

import corablue.stagehand.block.entity.LoreAnvilBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ModScreenHandlers {

    public static final ScreenHandlerType<LoreAnvilBlockEntity.LoreAnvilScreenHandler> LORE_ANVIL =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of("modid", "lore_anvil"),
                    new ExtendedScreenHandlerType<>(LoreAnvilBlockEntity.LoreAnvilScreenHandler::new, BlockPos.PACKET_CODEC));

    public static final ScreenHandlerType<StageChestScreenHandler> STAGE_CHEST =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of("stagehand", "stage_chest"),
                    new ExtendedScreenHandlerType<>(StageChestScreenHandler::new, BlockPos.PACKET_CODEC));

    public static void registerScreenHandlers() {
        System.out.println("Registering Screen Handlers for Stagehand");
        // REMOVED HandledScreens.register(...)
    }
}
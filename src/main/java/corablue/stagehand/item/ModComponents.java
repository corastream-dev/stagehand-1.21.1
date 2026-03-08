package corablue.stagehand.item;

import corablue.stagehand.Stagehand;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModComponents {

    // This is the actual component type we will attach to ItemStacks
    public static final ComponentType<StageChestTrackerComponent> STAGE_CHEST_TRACKER = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Stagehand.MOD_ID, "stage_chest_tracker"),
            ComponentType.<StageChestTrackerComponent>builder()
                    .codec(StageChestTrackerComponent.CODEC)
                    .packetCodec(StageChestTrackerComponent.PACKET_CODEC)
                    .build()
    );

    // Call this in your main Stagehand.java initialization!
    public static void registerComponentTypes() {
        Stagehand.LOGGER.info("Registering Data Component Types for " + Stagehand.MOD_ID);
    }
}
package corablue.stagehand.world;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class ModDimensions {
    // This creates a reference to our custom dimension
    public static final RegistryKey<World> THE_STAGE = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of("stagehand", "the_stage")
    );

    public static void register() {
        System.out.println("Registering Dimensions for Stagehand...");
    }
}
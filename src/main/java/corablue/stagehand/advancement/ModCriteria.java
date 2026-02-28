package corablue.stagehand.advancement;

import net.minecraft.advancement.criterion.TickCriterion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModCriteria {
    // Reuse TickCriterion for simple triggers! No custom Codecs or classes needed.
    public static final TickCriterion AUTHOR_ITEM = Registry.register(
            Registries.CRITERION,
            Identifier.of("stagehand", "author_item"),
            new TickCriterion()
    );

    public static final TickCriterion STAGE_DEATH = Registry.register(
            Registries.CRITERION,
            Identifier.of("stagehand", "stage_death"),
            new TickCriterion()
    );

    public static final TickCriterion STAGE_ENTRANCE = Registry.register(
            Registries.CRITERION,
            Identifier.of("stagehand", "stage_entrance"),
            new TickCriterion()
    );


    public static void initialize() {
        // Call this in your main init so the registry fires
    }
}
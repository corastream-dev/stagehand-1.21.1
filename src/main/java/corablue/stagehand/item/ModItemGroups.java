package corablue.stagehand.item;

import corablue.stagehand.Stagehand;
import corablue.stagehand.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

    public static final ItemGroup STAGEHAND_BLOCKS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(Stagehand.MOD_ID, "stagehand_blocks").toTranslationKey(),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModBlocks.STAGE_PROJECTOR_BLOCK))
                    .displayName(Text.translatable("itemgroup.stagehand.stagehand_blocks"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModBlocks.FATIGUE_CORE_BLOCK);
                        entries.add(ModBlocks.AMBIENCE_SPEAKER_BLOCK);
                        entries.add(ModBlocks.PARTICLE_EMITTER_BLOCK);
                        entries.add(ModBlocks.STAGE_PROJECTOR_BLOCK);
                        entries.add(ModBlocks.STAGE_RETURN_BLOCK);
                        entries.add(ModBlocks.LORE_ANVIL_BLOCK);
                    })
                    .build());



    public static void registerItemGroups(){
        Stagehand.LOGGER.info("Registering item groups for " + Stagehand.MOD_ID);
    }
}

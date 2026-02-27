package corablue.stagehand.block;

import corablue.stagehand.Stagehand;
import corablue.stagehand.block.custom.*;
import corablue.stagehand.block.entity.LoreAnvilBlockEntity;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.UniformIntProvider;

public class ModBlocks {

    public static final Block LORE_ANVIL_BLOCK = registerBlock("lore_anvil",
            new LoreAnvilBlock(AbstractBlock.Settings.create()
                    .strength(1)
                    .requiresTool()
                    .nonOpaque()));

    public static final Block FATIGUE_CORE_BLOCK = registerBlock("fatigue_core",
            new FatigueCoreBlock(AbstractBlock.Settings.create()
                    .strength(1)
                    .requiresTool()));

    public static final Block STAGE_PROJECTOR_BLOCK = registerBlock("stage_projector",
            new StageProjectorBlock(AbstractBlock.Settings.create()
                    .strength(1)
                    .requiresTool()
                    .nonOpaque()));

    public static final Block STAGE_CONFIG_BLOCK = registerBlock("stage_config",
            new StageConfigBlock(AbstractBlock.Settings.copy(Blocks.BEDROCK)
                    .luminance(state -> 5)));

    public static final Block STAGE_RETURN_BLOCK = registerBlock("stage_return",
            new StageReturnBlock(AbstractBlock.Settings.create()
                    .strength(1)
                    .requiresTool()
                    .nonOpaque()));

    public static final Block AMBIENCE_SPEAKER_BLOCK = registerBlock("ambience_speaker",
            new AmbienceSpeakerBlock(AbstractBlock.Settings.create()
                    .strength(1)
                    .requiresTool()));

    public static final Block PARTICLE_EMITTER_BLOCK = registerBlock("particle_emitter",
            new ParticleEmitterBlock(AbstractBlock.Settings.create()
                    .strength(1)
                    .requiresTool()
                    .nonOpaque()));


    private static Block registerBlock(String name, Block block){
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(Stagehand.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block){
        Registry.register(Registries.ITEM, Identifier.of(Stagehand.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
    }

    public static void registerModBlocks(){
        Stagehand.LOGGER.info("Registering blocks for " + Stagehand.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            //None for now
        });
    }
}

package corablue.stagehand.block.entity;

import corablue.stagehand.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static BlockEntityType<LoreAnvilBlockEntity> LORE_ANVIL_BE;
    public static BlockEntityType<FatigueCoreBlockEntity> FATIGUE_CORE_BE;
    public static BlockEntityType<AmbienceSpeakerBlockEntity> AMBIENCE_SPEAKER_BE;
    public static BlockEntityType<StageProjectorBlockEntity> STAGE_PROJECTOR_BE;
    public static BlockEntityType<StageConfigBlockEntity> STAGE_CONFIG_BE;
    public static BlockEntityType<ParticleEmitterBlockEntity> PARTICLE_EMITTER_BE;

    public static void registerBlockEntities() {


        LORE_ANVIL_BE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of("stagehand", "lore_anvil"),
                BlockEntityType.Builder.create(LoreAnvilBlockEntity::new, ModBlocks.LORE_ANVIL_BLOCK).build()
        );

        FATIGUE_CORE_BE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of("stagehand", "fatigue_core"),
                BlockEntityType.Builder.create(FatigueCoreBlockEntity::new, ModBlocks.FATIGUE_CORE_BLOCK).build()
        );

        AMBIENCE_SPEAKER_BE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of("stagehand", "ambience_speaker"),
                BlockEntityType.Builder.create(AmbienceSpeakerBlockEntity::new, ModBlocks.AMBIENCE_SPEAKER_BLOCK).build()
        );

        STAGE_PROJECTOR_BE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of("stagehand", "stage_projector"),
                BlockEntityType.Builder.create(StageProjectorBlockEntity::new, ModBlocks.STAGE_PROJECTOR_BLOCK).build()
        );

        STAGE_CONFIG_BE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of("stagehand", "stage_config"),
                BlockEntityType.Builder.create(StageConfigBlockEntity::new, ModBlocks.STAGE_CONFIG_BLOCK).build()
        );

        // 2. Initialize the Particle Emitter here
        // (Assuming your block is named PARTICLE_EMITTER_BLOCK in ModBlocks,
        //  change it if you named it something else!)
        PARTICLE_EMITTER_BE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of("stagehand", "particle_emitter"),
                BlockEntityType.Builder.create(ParticleEmitterBlockEntity::new, ModBlocks.PARTICLE_EMITTER_BLOCK).build()
        );
    }
}
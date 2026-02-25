package corablue.stagehand.client.model;

import corablue.stagehand.Stagehand;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModModelLayers {

    // Define the layer identifier here
    public static final EntityModelLayer PROJECTOR =
            new EntityModelLayer(Identifier.of(Stagehand.MOD_ID, "stage_projector"), "main");

    public static void registerModelLayers() {
        Stagehand.LOGGER.info("Registering Client Model Layers for " + Stagehand.MOD_ID);

        // Register the geometry data
        EntityModelLayerRegistry.registerModelLayer(PROJECTOR, StageProjectorModel::getTexturedModelData);
    }
}
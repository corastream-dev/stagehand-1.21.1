package corablue.stagehand.client.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class StageProjectorModel extends Model {
    // This is the texture you just placed!
    public static final Identifier TEXTURE = Identifier.of("stagehand", "textures/entity/projector_entity.png");

    private final ModelPart flashHinge;
    private final ModelPart body;

    public StageProjectorModel(ModelPart root) {
        super(RenderLayer::getEntitySolid); // Tells it how to render light and transparency
        this.flashHinge = root.getChild("flash_hinge");
        this.body = root.getChild("Body");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData partData = modelData.getRoot();

        // The translated Blockbench Geometry!
        ModelPartData flashHinge = partData.addChild("flash_hinge", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 16.0F, 0.0F));

        flashHinge.addChild("Flash_r1", ModelPartBuilder.create()
                        .uv(10, 21).mirrored().cuboid(-7.99F, -8.0F, -2.0F, 15.99F, 7.0F, 5.0F, Dilation.NONE),
                ModelTransform.of(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

        partData.addChild("Body", ModelPartBuilder.create()
                        .uv(0, 42).cuboid(-16.0F, -6.0F, 0.0F, 16.0F, 6.0F, 16.0F, Dilation.NONE)
                        .uv(6, 0).mirrored().cuboid(-16.0F, -11.0F, 7.0F, 16.0F, 5.0F, 9.0F, Dilation.NONE),
                ModelTransform.pivot(8.0F, 24.0F, -8.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    // We expose the hinge so the Renderer can rotate it!
    public void setHingeAngle(float angle) {
        this.flashHinge.pitch = angle;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        this.flashHinge.render(matrices, vertices, light, overlay, color);
        this.body.render(matrices, vertices, light, overlay, color);
    }
}
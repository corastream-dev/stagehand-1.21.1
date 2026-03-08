package corablue.stagehand;

import corablue.stagehand.block.ModBlocks;
import corablue.stagehand.client.gui.LoreAnvilScreen;
import corablue.stagehand.client.model.ModModelLayers;
import corablue.stagehand.network.FlashScreenPayload;
import corablue.stagehand.particles.ModParticles;
import corablue.stagehand.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.minecraft.util.Identifier;

public class StagehandClient implements ClientModInitializer {
    public static float flashAlpha = 0.0f;

    @Override
    public void onInitializeClient() {

        //Stage dimension effects file
        DimensionRenderingRegistry.registerDimensionEffects(
                Identifier.of("stagehand", "the_stage"),
                corablue.stagehand.client.StageDimensionEffects.INSTANCE
        );

        //Stage dimension void fade
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();

            // Only run this in The Stage
            if (client.world != null && client.world.getRegistryKey().getValue().toString().equals("stagehand:the_stage")) {
                if (client.player != null) {
                    double y = client.player.getY();

                    if (y < -40.0) {
                        // Formula: (-40 - currentY) / (Total Distance of 80)
                        float voidAlpha = (float) Math.max(0.0, Math.min(1.0, (-40.0 - y) / 80.0));

                        int alphaInt = (int) (voidAlpha * 255);
                        int whiteColor = (alphaInt << 24) | 0xFFFFFF;

                        drawContext.fill(0, 0, drawContext.getScaledWindowWidth(), drawContext.getScaledWindowHeight(), whiteColor);
                    }
                }
            }
        });

        //Lore Anvil cutout renderer (for feather)
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LORE_ANVIL_BLOCK, RenderLayer.getCutout());

        //Lore Anvil screen
        HandledScreens.register(ModScreenHandlers.LORE_ANVIL, LoreAnvilScreen::new);

        //Register model layers
        ModModelLayers.registerModelLayers();

        //Register stage projector renderer (client animation)
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                corablue.stagehand.block.entity.ModBlockEntities.STAGE_PROJECTOR_BE,
                corablue.stagehand.client.StageProjectorBlockEntityRenderer::new
        );

        //Client Particles
        ModParticles.registerClientFactories();

        //Screen Flash Logic
        ClientPlayNetworking.registerGlobalReceiver(FlashScreenPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                flashAlpha = 1.0f;
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (flashAlpha > 0.0f) {
                flashAlpha -= 0.025f;
                if (flashAlpha < 0.0f) flashAlpha = 0.0f;
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (flashAlpha > 0.0f) {
                int alphaInt = (int) (flashAlpha * 255);
                int color = (alphaInt << 24) | 0xFFFFFF;
                drawContext.fill(0, 0, drawContext.getScaledWindowWidth(), drawContext.getScaledWindowHeight(), color);
            }
        });
    }
}
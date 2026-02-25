package corablue.stagehand;

import corablue.stagehand.client.gui.LoreAnvilScreen;
import corablue.stagehand.client.model.ModModelLayers;
import corablue.stagehand.network.FlashScreenPayload;
import corablue.stagehand.particles.ModParticles;
import corablue.stagehand.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class StagehandClient implements ClientModInitializer {
    public static float flashAlpha = 0.0f;

    @Override
    public void onInitializeClient() {

        HandledScreens.register(ModScreenHandlers.LORE_ANVIL, LoreAnvilScreen::new);
        ModModelLayers.registerModelLayers();

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
package corablue.stagehand.client.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import corablue.stagehand.network.ModNetwork;
import corablue.stagehand.network.StageConfigUpdatePacket;

public class StageConfigScreen extends BaseOwoScreen<FlowLayout> {

    private final BlockPos blockPos;
    private final boolean isOwner;

    private boolean isReady;
    private String whitelist;
    private TextBoxComponent whitelistField;

    public StageConfigScreen(BlockPos blockPos, boolean isOwner, boolean isReady, String whitelist) {
        this.blockPos = blockPos;
        this.isOwner = isOwner;
        this.isReady = isReady;
        this.whitelist = whitelist;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {

    /* =======================
       COLOR DEFINITIONS
       ======================= */
        final int BORDER_COLOR = 0xFF404040;
        final int MAIN_BACKGROUND = 0xD9101010;
        final int TITLE_TEXT = 0xFFFFFF;
        final int SUBTEXT = 0xAAAAAA;
        final int BUTTON_TEXT = 0xD2953D;

        final int BUTTON_BASE = 0xFF2E180D;
        final int BUTTON_HOVER = 0xFF884D27;
        final int ACCENT = 0xFFD47A3D;
        final int SAVE_ACCENT = 0xFF4CAF50;

    /* =======================
       LAYOUT SETUP
       ======================= */
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        // --- BORDER WRAPPER ---
        FlowLayout borderWrapper = Containers.verticalFlow(Sizing.fixed(300), Sizing.content());
        borderWrapper.surface(Surface.flat(BORDER_COLOR));
        borderWrapper.padding(Insets.of(1));

        // --- MAIN CARD ---
        FlowLayout mainCard = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        mainCard.padding(Insets.of(15))
                .surface(Surface.flat(MAIN_BACKGROUND))
                .horizontalAlignment(HorizontalAlignment.CENTER);

        // --- HEADER ---
        mainCard.child(
                Components.label(Text.translatable("ui.stagehand.stage_config.title"))
                        .shadow(true)
                        .color(Color.ofRgb(TITLE_TEXT))
                        .margins(Insets.bottom(15))
        );

        // --- OWNER SECTION ---
        if (isOwner) {
            // Ready Toggle
            ButtonComponent readyBtn = Components.button(
                    Text.translatable("ui.stagehand.stage_config.status",
                                    // Inner translatable passes either READY or NOT READY to the %s
                                    isReady ? Text.translatable("ui.stagehand.stage_config.status.ready")
                                            : Text.translatable("ui.stagehand.stage_config.status.not_ready"))
                            .styled(s -> s.withColor(TextColor.fromRgb(BUTTON_TEXT))),
                    button -> {
                        this.isReady = !this.isReady;
                        button.setMessage(Text.translatable("ui.stagehand.stage_config.status",
                                        this.isReady ? Text.translatable("ui.stagehand.stage_config.status.ready")
                                                : Text.translatable("ui.stagehand.stage_config.status.not_ready"))
                                .styled(s -> s.withColor(TextColor.fromRgb(BUTTON_TEXT))));
                    }
            );
            readyBtn.sizing(Sizing.fill(100), Sizing.fixed(20));
            readyBtn.renderer(ButtonComponent.Renderer.flat(BUTTON_BASE, BUTTON_HOVER, ACCENT));
            readyBtn.margins(Insets.bottom(10));
            mainCard.child(readyBtn);

            // Whitelist Input Group
            mainCard.child(Components.label(Text.translatable("ui.stagehand.stage_config.whitelist"))
                    .color(Color.ofRgb(SUBTEXT))
                    .margins(Insets.bottom(4)));

            this.whitelistField = Components.textBox(Sizing.fill(100));
            this.whitelistField.text(this.whitelist);
            this.whitelistField.setMaxLength(200);
            this.whitelistField.margins(Insets.bottom(10));
            mainCard.child(this.whitelistField);

            // Save Settings Button
            ButtonComponent saveBtn = Components.button(
                    Text.translatable("ui.stagehand.stage_config.save")
                            .styled(s -> s.withColor(TextColor.fromRgb(BUTTON_TEXT))),
                    button -> sendAction(StageConfigUpdatePacket.StageAction.SAVE)
            );
            saveBtn.sizing(Sizing.fill(100), Sizing.fixed(20));
            saveBtn.renderer(ButtonComponent.Renderer.flat(BUTTON_BASE, BUTTON_HOVER, SAVE_ACCENT));
            saveBtn.margins(Insets.bottom(15));
            mainCard.child(saveBtn);
        }

        // --- ACTION ROW ---
        FlowLayout actionRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        actionRow.gap(10);

        String gmName = net.minecraft.client.MinecraftClient.getInstance().interactionManager.getCurrentGameMode().getName().toUpperCase();

        ButtonComponent gmBtn = Components.button(
                Text.translatable("ui.stagehand.stage_config.mode", gmName).styled(s -> s.withColor(TextColor.fromRgb(BUTTON_TEXT))),
                button -> {
                    sendAction(StageConfigUpdatePacket.StageAction.GAMEMODE);
                    this.close();
                }
        );

        gmBtn.sizing(Sizing.fill(50), Sizing.fixed(20));
        gmBtn.renderer(ButtonComponent.Renderer.flat(BUTTON_BASE, BUTTON_HOVER, ACCENT));

        ButtonComponent backBtn = Components.button(
                Text.translatable("ui.stagehand.stage_config.exit").styled(s -> s.withColor(TextColor.fromRgb(BUTTON_TEXT))),
                button -> { sendAction(StageConfigUpdatePacket.StageAction.RETURN); this.close(); }
        );
        backBtn.sizing(Sizing.fill(50), Sizing.fixed(20));
        backBtn.renderer(ButtonComponent.Renderer.flat(BUTTON_BASE, BUTTON_HOVER, ACCENT));

        actionRow.child(gmBtn).child(backBtn);
        mainCard.child(actionRow);

        borderWrapper.child(mainCard);
        rootComponent.child(borderWrapper);
    }

    private void sendAction(StageConfigUpdatePacket.StageAction action) {
        // If the field exists, get text; otherwise use the original string or empty
        String currentWhitelist = (this.whitelistField != null) ? this.whitelistField.getText() : (this.whitelist != null ? this.whitelist : "");

        ModNetwork.CHANNEL.clientHandle().send(
                new StageConfigUpdatePacket(this.blockPos, this.isReady, currentWhitelist, action)
        );
    }
}
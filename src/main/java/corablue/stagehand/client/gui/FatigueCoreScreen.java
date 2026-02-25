package corablue.stagehand.client.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import corablue.stagehand.network.ModNetwork;
import corablue.stagehand.network.FatigueCoreUpdatePacket;

public class FatigueCoreScreen extends BaseOwoScreen<FlowLayout> {

    private final BlockPos blockPos;
    private int currentRange;
    private boolean affectOwner;

    public FatigueCoreScreen(BlockPos blockPos, int initialRange, boolean initialAffectOwner) {
        this.blockPos = blockPos;
        this.currentRange = initialRange;
        this.affectOwner = initialAffectOwner;
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
        final int BUTTON_TEXT = 0xD2953D;

        final int BUTTON_BASE = 0xFF2E180D;
        final int BUTTON_HOVER = 0xFF884D27;
        final int ACCENT = 0xFFD47A3D;

    /* =======================
       LAYOUT SETUP
       ======================= */
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        // --- BORDER WRAPPER ---
        // Slightly slimmer width (220) fits two toggle buttons perfectly
        FlowLayout borderWrapper = Containers.verticalFlow(Sizing.fixed(220), Sizing.content());
        borderWrapper.surface(Surface.flat(BORDER_COLOR));
        borderWrapper.padding(Insets.of(1));

        // --- MAIN CARD ---
        FlowLayout mainCard = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        mainCard.padding(Insets.of(15))
                .surface(Surface.flat(MAIN_BACKGROUND))
                .horizontalAlignment(HorizontalAlignment.CENTER);

        // --- HEADER ---
        mainCard.child(
                Components.label(Text.literal("Fatigue Core"))
                        .shadow(true)
                        .color(Color.ofRgb(TITLE_TEXT))
                        .margins(Insets.bottom(15))
        );

        // --- CONTROLS ---
        // Range Button
        ButtonComponent rangeBtn = Components.button(
                Text.literal("Range: " + currentRange)
                        .styled(s -> s.withColor(TextColor.fromRgb(BUTTON_TEXT))),
                button -> {
                    if (this.currentRange < 8) this.currentRange = 8;
                    else if (this.currentRange < 16) this.currentRange = 16;
                    else this.currentRange = 4;

                    button.setMessage(Text.literal("Range: " + this.currentRange)
                            .styled(s -> s.withColor(TextColor.fromRgb(BUTTON_TEXT))));
                    sendUpdatePacket();
                }
        );
        rangeBtn.sizing(Sizing.fill(100), Sizing.fixed(20));
        rangeBtn.renderer(ButtonComponent.Renderer.flat(BUTTON_BASE, BUTTON_HOVER, ACCENT));
        rangeBtn.margins(Insets.bottom(8));
        mainCard.child(rangeBtn);

        // Affect Owner Button
        ButtonComponent ownerBtn = Components.button(
                Text.literal("Affect Owner: " + (affectOwner ? "YES" : "NO"))
                        .styled(s -> s.withColor(TextColor.fromRgb(BUTTON_TEXT))),
                button -> {
                    this.affectOwner = !this.affectOwner;
                    button.setMessage(Text.literal("Affect Owner: " + (this.affectOwner ? "YES" : "NO"))
                            .styled(s -> s.withColor(TextColor.fromRgb(BUTTON_TEXT))));
                    sendUpdatePacket();
                }
        );
        ownerBtn.sizing(Sizing.fill(100), Sizing.fixed(20));
        ownerBtn.renderer(ButtonComponent.Renderer.flat(BUTTON_BASE, BUTTON_HOVER, ACCENT));
        mainCard.child(ownerBtn);

        // Assembly
        borderWrapper.child(mainCard);
        rootComponent.child(borderWrapper);
    }

    private void sendUpdatePacket() {
        ModNetwork.CHANNEL.clientHandle().send(
                new FatigueCoreUpdatePacket(this.blockPos, this.currentRange, this.affectOwner)
        );
    }
}
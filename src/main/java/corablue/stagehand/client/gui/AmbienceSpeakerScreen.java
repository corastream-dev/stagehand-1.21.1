package corablue.stagehand.client.gui;

import corablue.stagehand.block.entity.AmbienceSpeakerBlockEntity;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import corablue.stagehand.network.ModNetwork;
import corablue.stagehand.network.AmbienceSpeakerUpdatePacket;

public class AmbienceSpeakerScreen extends BaseOwoScreen<FlowLayout> {

    private final BlockPos blockPos;
    private Identifier currentSound;
    private int currentRange;
    private boolean isPlaying;

    public AmbienceSpeakerScreen(BlockPos blockPos, Identifier currentSound, int initialRange, boolean isPlaying) {
        this.blockPos = blockPos;
        this.currentSound = currentSound;
        this.currentRange = initialRange;
        this.isPlaying = isPlaying;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

// Import statements assumed (io.wispforest.owo.ui.*, etc.)

    @Override
    protected void build(FlowLayout rootComponent) {

    /* =======================
       COLOR DEFINITIONS
       ======================= */

        // Surfaces
        final int BORDER_COLOR = 0xFF404040;
        final int MAIN_BACKGROUND = 0xD9101010;
        final int SELECTED_ENTRY = 0xFFD47A3D;
        final int HOVER_ENTRY = 0xFF2E180D;
        final int TRANSPARENT = 0x00000000;
        final int SCROLL_BACKGROUND = 0x40000000;

        // Text
        final int TITLE_TEXT = 0xFFFFFF;
        final int SUBTEXT = 0xAAAAAA;
        final int UNSELECTED_TEXT = 0xBBBBBB;
        final int BUTTON_TEXT = 0xD2953D;

        // Button Renderer Colors
        final int BUTTON_BASE = 0xFF2E180D;
        final int BUTTON_HOVER = 0xFF884D27;
        final int PLAY_ACCENT = 0xFF4CAF50;
        final int STOP_ACCENT = 0xFFEF5350;

    /* =======================
       LAYOUT SETUP
       ======================= */

        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        // --- BORDER WRAPPER ---
        FlowLayout borderWrapper = Containers.verticalFlow(Sizing.fixed(322), Sizing.content());
        borderWrapper.surface(Surface.flat(BORDER_COLOR));
        borderWrapper.padding(Insets.of(1));

        // --- MAIN CARD ---
        FlowLayout mainCard = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        mainCard.padding(Insets.of(15))
                .surface(Surface.flat(MAIN_BACKGROUND));

        // --- HEADER ---
        FlowLayout header = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        header.horizontalAlignment(HorizontalAlignment.CENTER).margins(Insets.bottom(15));

        header.child(
                Components.label(Text.translatable("block.stagehand.ambience_speaker"))
                        .shadow(true)
                        .color(Color.ofRgb(TITLE_TEXT))
                        .margins(Insets.bottom(2))
        );

        LabelComponent selectedLabel = Components.label(
                Text.literal(currentSound.getPath())
                        .styled(s -> s.withColor(SUBTEXT).withItalic(true))
        );
        selectedLabel.id("selected-sound-label");
        header.child(selectedLabel);
        mainCard.child(header);

        // --- SCROLLABLE SOUND LIST ---
        FlowLayout soundListContent = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        soundListContent.horizontalAlignment(HorizontalAlignment.CENTER);

        for (Identifier soundId : Registries.SOUND_EVENT.getIds()) {
            if (soundId.getNamespace().equals("stagehand") && soundId.getPath().contains("ambien")) {

                String translationKey = "sound." + soundId.getNamespace() + "." + soundId.getPath();
                boolean isSelected = this.currentSound.equals(soundId);

                FlowLayout entry = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(24));
                entry.padding(Insets.horizontal(8))
                        .verticalAlignment(VerticalAlignment.CENTER)
                        .cursorStyle(CursorStyle.HAND);

                entry.surface(isSelected
                        ? Surface.flat(SELECTED_ENTRY)
                        : Surface.outline(TRANSPARENT));

                LabelComponent nameLabel = Components.label(Text.translatable(translationKey));
                nameLabel.color(isSelected
                        ? Color.ofRgb(TITLE_TEXT)
                        : Color.ofRgb(UNSELECTED_TEXT));

                entry.child(nameLabel);

                entry.mouseEnter().subscribe(() -> {
                    if (!"selected".equals(entry.id())) {
                        entry.surface(Surface.flat(HOVER_ENTRY));
                        entry.margins(Insets.left(5));
                    }
                });

                entry.mouseLeave().subscribe(() -> {
                    if (!"selected".equals(entry.id())) {
                        entry.surface(Surface.outline(TRANSPARENT));
                        entry.margins(Insets.left(0));
                    }
                });

                entry.mouseDown().subscribe((mouseX, mouseY, button) -> {
                    this.currentSound = soundId;
                    this.isPlaying = true;

                    selectedLabel.text(Text.translatable(translationKey));

                    soundListContent.children().forEach(child -> {
                        if (child instanceof FlowLayout layout) {
                            layout.surface(Surface.outline(TRANSPARENT));
                            layout.id("unselected");
                            layout.margins(Insets.left(0));

                            if (layout.children().get(0) instanceof LabelComponent label) {
                                label.color(Color.ofRgb(UNSELECTED_TEXT));
                            }
                        }
                    });

                    entry.surface(Surface.flat(SELECTED_ENTRY));
                    entry.id("selected");
                    nameLabel.color(Color.ofRgb(TITLE_TEXT));

                    applyChangesLive();
                    return true;
                });

                if (isSelected) entry.id("selected");
                soundListContent.child(entry);
            }
        }

        ScrollContainer<FlowLayout> scrollContainer = Containers.verticalScroll(
                Sizing.fill(100), Sizing.fixed(140), soundListContent
        );

        scrollContainer.scrollbarThiccness(4);
        scrollContainer.surface(Surface.flat(SCROLL_BACKGROUND));
        scrollContainer.margins(Insets.bottom(15));
        scrollContainer.padding(Insets.of(2));
        mainCard.child(scrollContainer);

        // --- CONTROLS ---
        FlowLayout controls = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        controls.gap(10);

        FlowLayout rangeRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        rangeRow.verticalAlignment(VerticalAlignment.CENTER);

        rangeRow.child(Components.label(Text.translatable("ui.stagehand.ambience_speaker.range")).margins(Insets.right(10)));

        TextBoxComponent rangeField = Components.textBox(Sizing.fixed(50), String.valueOf(this.currentRange));
        rangeField.setTextPredicate(s -> s.matches("\\d*"));
        rangeField.setMaxLength(2);
        rangeField.onChanged().subscribe(s -> {
            if (!s.isEmpty()) {
                try {
                    this.currentRange = Math.max(1, Math.min(Integer.parseInt(s), 64));
                    applyChangesLive();
                } catch (NumberFormatException ignored) {}
            }
        });

        rangeRow.child(rangeField);
        controls.child(rangeRow);

        FlowLayout buttonRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonRow.gap(10);

        ButtonComponent playBtn = Components.button(
                Text.translatable("ui.stagehand.ambience_speaker.play")
                        .styled(style -> style.withColor(TextColor.fromRgb(BUTTON_TEXT))),
                btn -> {
                    this.isPlaying = true;
                    applyChangesLive();
                }
        );

        playBtn.sizing(Sizing.fill(50), Sizing.fixed(20));
        playBtn.renderer(ButtonComponent.Renderer.flat(BUTTON_BASE, BUTTON_HOVER, PLAY_ACCENT));

        ButtonComponent stopBtn = Components.button(
                Text.translatable("ui.stagehand.ambience_speaker.stop")
                        .styled(style -> style.withColor(TextColor.fromRgb(BUTTON_TEXT))),
                btn -> {
                    this.isPlaying = false;
                    applyChangesLive();
                }
        );

        stopBtn.sizing(Sizing.fill(50), Sizing.fixed(20));
        stopBtn.renderer(ButtonComponent.Renderer.flat(BUTTON_BASE, BUTTON_HOVER, STOP_ACCENT));

        buttonRow.child(playBtn).child(stopBtn);
        controls.child(buttonRow);

        mainCard.child(controls);

        borderWrapper.child(mainCard);
        rootComponent.child(borderWrapper);
    }

    private void applyChangesLive() {
        // 1. Send the packet to keep the server in the loop
        ModNetwork.CHANNEL.clientHandle().send(
                new AmbienceSpeakerUpdatePacket(this.blockPos, this.currentSound, this.currentRange, this.isPlaying)
        );

        // 2. Instantly update the client-side block entity so the audio reacts right now
        if (this.client != null && this.client.world != null) {
            if (this.client.world.getBlockEntity(this.blockPos) instanceof AmbienceSpeakerBlockEntity speaker) {
                speaker.updateSettings(this.currentSound, this.currentRange, this.isPlaying);
            }
        }
    }
}
package corablue.stagehand.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import corablue.stagehand.block.entity.ParticleEmitterBlockEntity;
import corablue.stagehand.network.ModNetwork;
import corablue.stagehand.network.ParticleEmitterUpdatePacket;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.SliderComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
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

public class ParticleEmitterScreen extends BaseOwoScreen<FlowLayout> {

    // --- STYLING CONSTANTS ---
    private static final int BORDER_COLOR = 0xFF404040;
    private static final int MAIN_BACKGROUND = 0xD9101010;
    private static final int SELECTED_ENTRY = 0xFFD47A3D;
    private static final int HOVER_ENTRY = 0xFF2E180D;
    private static final int SCROLL_BACKGROUND = 0x40000000;
    private static final int TITLE_TEXT = 0xFFFFFF;
    private static final int BUTTON_TEXT = 0xD2953D;
    private static final int BUTTON_BASE = 0xFF2E180D;
    private static final int BUTTON_HOVER = 0xFF884D27;
    private static final int ACCENT = 0xFFD47A3D;

    private final BlockPos blockPos;
    private final ParticleEmitterBlockEntity be;

    private Identifier particleType;
    private float c1R, c1G, c1B, c2R, c2G, c2B;
    private boolean useLifetimeColor;
    private float scale, gravity;
    private int lifetime;
    private double spawnRate;
    private float oX, oY, oZ, aX, aY, aZ;
    private float minVX, maxVX, minVY, maxVY, minVZ, maxVZ;
    private float orbX, orbY, orbZ;
    private boolean rotate, emissive;

    public ParticleEmitterScreen(BlockPos blockPos, ParticleEmitterBlockEntity be) {
        this.blockPos = blockPos;
        this.be = be;

        this.particleType = be.getParticleType();
        this.c1R = be.getC1R(); this.c1G = be.getC1G(); this.c1B = be.getC1B();
        this.c2R = be.getC2R(); this.c2G = be.getC2G(); this.c2B = be.getC2B();
        this.useLifetimeColor = be.getUseLifetimeColor();

        this.lifetime = be.getLifetime();
        this.scale = be.getScale(); this.gravity = be.getGravity(); this.spawnRate = be.getAmountPerTick();

        this.oX = be.getOffsetX(); this.oY = be.getOffsetY(); this.oZ = be.getOffsetZ();
        this.aX = be.getAreaX(); this.aY = be.getAreaY(); this.aZ = be.getAreaZ();
        this.minVX = be.getMinVelX(); this.maxVX = be.getMaxVelX();
        this.minVY = be.getMinVelY(); this.maxVY = be.getMaxVelY();
        this.minVZ = be.getMinVelZ(); this.maxVZ = be.getMaxVelZ();

        this.orbX = be.getOrbX(); this.orbY = be.getOrbY(); this.orbZ = be.getOrbZ();
        this.rotate = be.getRotate();
        this.emissive = be.getEmissive();
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout borderWrapper = Containers.verticalFlow(Sizing.fixed(360), Sizing.content());
        borderWrapper.surface(Surface.flat(BORDER_COLOR)).padding(Insets.of(1));

        FlowLayout mainCard = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        mainCard.padding(Insets.of(12)).surface(Surface.flat(MAIN_BACKGROUND)).horizontalAlignment(HorizontalAlignment.CENTER);

        mainCard.child(Components.label(Text.translatable("ui.stagehand.particle_emitter.title")).shadow(true).color(Color.ofRgb(TITLE_TEXT)).margins(Insets.bottom(10)));

        // --- PARTICLE SELECTOR GRID ---
        FlowLayout particleGrid = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        particleGrid.horizontalAlignment(HorizontalAlignment.CENTER).margins(Insets.bottom(10));
        FlowLayout currentRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        int count = 0;
        java.util.Map<Identifier, FlowLayout> particleBoxes = new java.util.HashMap<>();

        for (Identifier id : Registries.PARTICLE_TYPE.getIds()) {
            if (id.getNamespace().equals("stagehand")) {
                if (count > 0 && count % 8 == 0) { // Bumped to 8 per row to use the slightly wider screen
                    particleGrid.child(currentRow);
                    currentRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
                }

                Identifier textureId = Identifier.of(id.getNamespace(), "textures/particle/" + id.getPath() + ".png");
                FlowLayout imageBox = Containers.verticalFlow(Sizing.fixed(28), Sizing.fixed(28));
                imageBox.horizontalAlignment(HorizontalAlignment.CENTER).verticalAlignment(VerticalAlignment.CENTER).margins(Insets.of(2));

                imageBox.child(new BaseComponent() {
                    @Override
                    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        context.drawTexture(textureId, this.x, this.y, 16, 16, 0, 0, 16, 16, 16, 16);
                        RenderSystem.disableBlend();
                    }
                }.sizing(Sizing.fixed(16), Sizing.fixed(16)));

                boolean isSelected = id.equals(this.particleType);
                imageBox.surface(isSelected ? Surface.flat(SELECTED_ENTRY) : Surface.outline(BORDER_COLOR));
                imageBox.cursorStyle(CursorStyle.HAND);
                particleBoxes.put(id, imageBox);

                imageBox.mouseDown().subscribe((mouseX, mouseY, button) -> {
                    this.particleType = id;
                    applyChangesLive();
                    particleBoxes.forEach((pId, box) -> box.surface(pId.equals(this.particleType) ? Surface.flat(SELECTED_ENTRY) : Surface.outline(BORDER_COLOR)));
                    return true;
                });
                imageBox.mouseEnter().subscribe(() -> { if (!id.equals(this.particleType)) imageBox.surface(Surface.flat(HOVER_ENTRY)); });
                imageBox.mouseLeave().subscribe(() -> { if (!id.equals(this.particleType)) imageBox.surface(Surface.outline(BORDER_COLOR)); });

                currentRow.child(imageBox);
                count++;
            }
        }
        if (!currentRow.children().isEmpty()) particleGrid.child(currentRow);
        mainCard.child(particleGrid);

        mainCard.child(Containers.verticalFlow(Sizing.fill(100), Sizing.fixed(1)).surface(Surface.flat(BORDER_COLOR)).margins(Insets.vertical(5)));

        // --- SCROLLABLE PARAMETERS ---
        FlowLayout list = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        list.horizontalAlignment(HorizontalAlignment.CENTER);

        // -- COLOR SETTINGS --
        ButtonComponent colorModeBtn = Components.button(
                useLifetimeColor ? Text.translatable("ui.stagehand.particle_emitter.color_mode.lifetime")
                        : Text.translatable("ui.stagehand.particle_emitter.color_mode.random"),
                button -> {
                    useLifetimeColor = !useLifetimeColor;
                    button.setMessage(useLifetimeColor ? Text.translatable("ui.stagehand.particle_emitter.color_mode.lifetime")
                            : Text.translatable("ui.stagehand.particle_emitter.color_mode.random"));
                    applyChangesLive();
                }
        );
        colorModeBtn.sizing(Sizing.fixed(160), Sizing.fixed(20));
        colorModeBtn.renderer(ButtonComponent.Renderer.flat(BUTTON_BASE, BUTTON_HOVER, ACCENT));
        list.child(colorModeBtn.margins(Insets.bottom(8).top(4)));

// Letters like R, G, B, X, Y, Z are universally understood, so keeping them hardcoded is standard practice!
        addHeader(list, "R", "G", "B");
        setupColorRow(list, Text.translatable("ui.stagehand.particle_emitter.start").getString(), c1R, val -> c1R = val, c1G, val -> c1G = val, c1B, val -> c1B = val);
        setupColorRow(list, Text.translatable("ui.stagehand.particle_emitter.end").getString(), c2R, val -> c2R = val, c2G, val -> c2G = val, c2B, val -> c2B = val);

        list.child(Containers.verticalFlow(Sizing.fill(80), Sizing.fixed(1)).surface(Surface.flat(BORDER_COLOR)).margins(Insets.vertical(8)));

// -- BEHAVIOR & TIMING --
        addStandaloneSlider(list, Text.translatable("ui.stagehand.particle_emitter.scale").getString(), 0.05f, 1.0f, scale, val -> { scale = val; applyChangesLive(); });
        addStandaloneSlider(list, Text.translatable("ui.stagehand.particle_emitter.gravity").getString(), -1f, 1f, gravity, val -> { gravity = val; applyChangesLive(); });
        addStandaloneFloatSlider(list, Text.translatable("ui.stagehand.particle_emitter.amount").getString(), 0.1, 20.0, spawnRate, 2.0, val -> { spawnRate = val; applyChangesLive(); });
        addStandaloneIntSlider(list, Text.translatable("ui.stagehand.particle_emitter.lifetime").getString(), 1, 120, lifetime, val -> { lifetime = val; applyChangesLive(); });

// Toggles
        FlowLayout toggleRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        toggleRow.horizontalAlignment(HorizontalAlignment.CENTER).margins(Insets.vertical(6));

        CheckboxComponent rotateBox = Components.checkbox(Text.translatable("ui.stagehand.particle_emitter.rotates"));
        rotateBox.checked(rotate);
        rotateBox.onChanged(checked -> { rotate = checked; applyChangesLive(); });
        toggleRow.child(rotateBox.margins(Insets.right(20)));

        CheckboxComponent emissiveBox = Components.checkbox(Text.translatable("ui.stagehand.particle_emitter.emissive"));
        emissiveBox.checked(emissive);
        emissiveBox.onChanged(checked -> { emissive = checked; applyChangesLive(); });
        toggleRow.child(emissiveBox);

        list.child(toggleRow);

        list.child(Containers.verticalFlow(Sizing.fill(80), Sizing.fixed(1)).surface(Surface.flat(BORDER_COLOR)).margins(Insets.vertical(8)));

// -- VECTOR SETTINGS --
        addHeader(list, "X", "Y", "Z"); // Print XYZ header once
        setupCoordRow(list, Text.translatable("ui.stagehand.particle_emitter.offset").getString(), oX, val -> oX = val, oY, val -> oY = val, oZ, val -> oZ = val);
        setupCoordRow(list, Text.translatable("ui.stagehand.particle_emitter.spread").getString(), aX, val -> aX = val, aY, val -> aY = val, aZ, val -> aZ = val);
        setupCoordRow(list, Text.translatable("ui.stagehand.particle_emitter.min_vel").getString(), minVX, val -> minVX = val, minVY, val -> minVY = val, minVZ, val -> minVZ = val);
        setupCoordRow(list, Text.translatable("ui.stagehand.particle_emitter.max_vel").getString(), maxVX, val -> maxVX = val, maxVY, val -> maxVY = val, maxVZ, val -> maxVZ = val);
        setupCoordRow(list, Text.translatable("ui.stagehand.particle_emitter.spin").getString(), orbX, val -> orbX = val, orbY, val -> orbY = val, orbZ, val -> orbZ = val);

        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fixed(180), list);
        scroll.surface(Surface.flat(SCROLL_BACKGROUND)).padding(Insets.of(5)).margins(Insets.bottom(10));
        mainCard.child(scroll);

        ButtonComponent doneBtn = Components.button(Text.translatable("ui.stagehand.generic.done").styled(s -> s.withColor(TextColor.fromRgb(BUTTON_TEXT))), button -> this.close());
        doneBtn.sizing(Sizing.fixed(100), Sizing.fixed(20));
        doneBtn.renderer(ButtonComponent.Renderer.flat(BUTTON_BASE, BUTTON_HOVER, ACCENT));
        mainCard.child(doneBtn);

        borderWrapper.child(mainCard);
        rootComponent.child(borderWrapper);
    }

    // --- HELPER METHODS ---

    private void addHeader(FlowLayout list, String c1, String c2, String c3) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.horizontalAlignment(HorizontalAlignment.CENTER).verticalAlignment(VerticalAlignment.CENTER);

        // Blank spacer for the title alignment
        row.child(Containers.verticalFlow(Sizing.fixed(40), Sizing.content()).margins(Insets.right(5)));
        row.child(createHeaderLabel(c1));
        row.child(createHeaderLabel(c2));
        row.child(createHeaderLabel(c3));

        list.child(row.margins(Insets.bottom(2)));
    }

    private Component createHeaderLabel(String text) {
        return Containers.verticalFlow(Sizing.fixed(75), Sizing.content())
                .child(Components.label(Text.literal(text)).color(Color.ofRgb(0x888888)))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.horizontal(4));
    }

    private FlowLayout createGridRow(FlowLayout list, String title) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.horizontalAlignment(HorizontalAlignment.CENTER).verticalAlignment(VerticalAlignment.CENTER);

        // Title aligned left
        row.child(Components.label(Text.literal(title)).color(Color.ofRgb(0xAAAAAA))
                .sizing(Sizing.fixed(40), Sizing.content()).margins(Insets.right(5)));

        list.child(row.margins(Insets.vertical(2)));
        return row;
    }

    private void setupColorRow(FlowLayout list, String title, float r, java.util.function.Consumer<Float> or, float g, java.util.function.Consumer<Float> og, float b, java.util.function.Consumer<Float> ob) {
        FlowLayout row = createGridRow(list, title);
        addSlimSlider(row, 0, 1, r, val -> { or.accept(val); applyChangesLive(); });
        addSlimSlider(row, 0, 1, g, val -> { og.accept(val); applyChangesLive(); });
        addSlimSlider(row, 0, 1, b, val -> { ob.accept(val); applyChangesLive(); });
    }

    private void setupCoordRow(FlowLayout list, String title, float x, java.util.function.Consumer<Float> ox, float y, java.util.function.Consumer<Float> oy, float z, java.util.function.Consumer<Float> oz) {
        FlowLayout row = createGridRow(list, title);
        addSlimNumberField(row, x, val -> { ox.accept(val); applyChangesLive(); });
        addSlimNumberField(row, y, val -> { oy.accept(val); applyChangesLive(); });
        addSlimNumberField(row, z, val -> { oz.accept(val); applyChangesLive(); });
    }

    private void addSlimSlider(FlowLayout row, float min, float max, float current, java.util.function.Consumer<Float> onChange) {
        SliderComponent slider = Components.slider(Sizing.fixed(75));
        slider.sizing(Sizing.fixed(75), Sizing.fixed(12)); // Slimmer height
        slider.value((current - min) / (max - min));
        slider.onChanged().subscribe(v -> onChange.accept((float)(min + v * (max - min))));
        row.child(slider.margins(Insets.horizontal(4)));
    }

    private void addSlimNumberField(FlowLayout row, float current, java.util.function.Consumer<Float> onChange) {
        TextBoxComponent tb = Components.textBox(Sizing.fixed(75), String.format("%.2f", current));
        tb.sizing(Sizing.fixed(75), Sizing.fixed(16)); // Keep Textboxes compact
        tb.onChanged().subscribe(s -> { try { onChange.accept(Float.parseFloat(s)); } catch(Exception ignored){}});
        row.child(tb.margins(Insets.horizontal(4)));
    }

    // --- STANDALONE SLIDERS ---

    private void addStandaloneSlider(FlowLayout list, String name, float min, float max, float current, java.util.function.Consumer<Float> onChange) {
        FlowLayout row = createGridRow(list, name);
        SliderComponent slider = Components.slider(Sizing.fixed(160));
        slider.sizing(Sizing.fixed(160), Sizing.fixed(12));
        slider.value((current - min) / (max - min));
        slider.onChanged().subscribe(v -> onChange.accept((float)(min + v * (max - min))));
        row.child(slider);
    }

    private void addStandaloneFloatSlider(FlowLayout list, String name, double min, double max, double current, double exponent, java.util.function.DoubleConsumer onChange) {
        FlowLayout row = createGridRow(list, name);
        SliderComponent slider = Components.slider(Sizing.fixed(160));
        slider.sizing(Sizing.fixed(160), Sizing.fixed(12));
        double normalized = (current - min) / (max - min);
        slider.value(Math.pow(normalized, 1.0 / exponent));
        slider.onChanged().subscribe(v -> {
            double curved = Math.pow(v, exponent);
            onChange.accept(min + curved * (max - min));
        });
        row.child(slider);
    }

    private void addStandaloneIntSlider(FlowLayout list, String name, int min, int max, int current, java.util.function.Consumer<Integer> onChange) {
        FlowLayout row = createGridRow(list, name);
        SliderComponent slider = Components.slider(Sizing.fixed(160));
        slider.sizing(Sizing.fixed(160), Sizing.fixed(12));
        slider.value((double)(current - min) / (max - min));
        slider.onChanged().subscribe(v -> onChange.accept((int)Math.round(min + v * (max - min))));
        row.child(slider);
    }

    private void applyChangesLive() {
        this.be.updateSettings(
                particleType,
                c1R, c1G, c1B, c2R, c2G, c2B, useLifetimeColor,
                scale, gravity, spawnRate, lifetime,
                oX, oY, oZ, aX, aY, aZ,
                minVX, maxVX, minVY, maxVY, minVZ, maxVZ,
                orbX, orbY, orbZ, rotate, emissive
        );
        ModNetwork.CHANNEL.clientHandle().send(new ParticleEmitterUpdatePacket(
                this.blockPos, particleType,
                c1R, c1G, c1B, c2R, c2G, c2B, useLifetimeColor,
                scale, gravity, spawnRate, lifetime,
                oX, oY, oZ, aX, aY, aZ,
                minVX, maxVX, minVY, maxVY, minVZ, maxVZ,
                orbX, orbY, orbZ, rotate, emissive
        ));
    }
}
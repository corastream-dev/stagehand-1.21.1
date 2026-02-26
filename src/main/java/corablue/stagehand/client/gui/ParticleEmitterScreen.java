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

    private final BlockPos blockPos;
    private final ParticleEmitterBlockEntity be;

    private Identifier particleType;
    private float c1R, c1G, c1B, c2R, c2G, c2B;
    private boolean useLifetimeColor; // Toggle
    private float scale, gravity;
    private int lifetime;
    private double spawnRate;
    private float oX, oY, oZ;
    private float aX, aY, aZ;
    private float minVX, maxVX, minVY, maxVY, minVZ, maxVZ;

    // New Fields
    private float orbX, orbY, orbZ;
    private boolean rotate;

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
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {

        final int BORDER_COLOR = 0xFF404040;
        final int MAIN_BACKGROUND = 0xD9101010;
        final int SELECTED_ENTRY = 0xFFD47A3D;
        final int HOVER_ENTRY = 0xFF2E180D;
        final int SCROLL_BACKGROUND = 0x40000000;
        final int TITLE_TEXT = 0xFFFFFF;
        final int BUTTON_TEXT = 0xD2953D;
        final int BUTTON_BASE = 0xFF2E180D;
        final int BUTTON_HOVER = 0xFF884D27;
        final int ACCENT = 0xFFD47A3D;

        rootComponent.surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout borderWrapper = Containers.verticalFlow(Sizing.fixed(340), Sizing.content());
        borderWrapper.surface(Surface.flat(BORDER_COLOR)).padding(Insets.of(1));

        FlowLayout mainCard = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        mainCard.padding(Insets.of(12)).surface(Surface.flat(MAIN_BACKGROUND)).horizontalAlignment(HorizontalAlignment.CENTER);

        mainCard.child(Components.label(Text.literal("Particle Emitter Settings")).shadow(true).color(Color.ofRgb(TITLE_TEXT)).margins(Insets.bottom(10)));

        // --- PARTICLE SELECTOR GRID ---
        FlowLayout particleGrid = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        particleGrid.horizontalAlignment(HorizontalAlignment.CENTER).margins(Insets.bottom(10));
        FlowLayout currentRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        int count = 0;
        java.util.Map<Identifier, FlowLayout> particleBoxes = new java.util.HashMap<>();

        for (Identifier id : Registries.PARTICLE_TYPE.getIds()) {
            if (id.getNamespace().equals("stagehand")) {
                if (count > 0 && count % 7 == 0) {
                    particleGrid.child(currentRow);
                    currentRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
                }

                Identifier textureId = Identifier.of(id.getNamespace(), "textures/particle/" + id.getPath() + ".png");
                FlowLayout imageBox = Containers.verticalFlow(Sizing.fixed(28), Sizing.fixed(28));
                imageBox.horizontalAlignment(HorizontalAlignment.CENTER).verticalAlignment(VerticalAlignment.CENTER).margins(Insets.of(2));

                // === FIXED TRANSPARENCY RENDERING ===
                // Instead of Components.texture, we use a custom component that enables blending
                imageBox.child(new BaseComponent() {
                    @Override
                    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        context.drawTexture(textureId, this.x, this.y, 16, 16, 0, 0, 16, 16, 16, 16);
                        RenderSystem.disableBlend();
                    }
                }.sizing(Sizing.fixed(16), Sizing.fixed(16)));
                // ====================================

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

        // Color Mode Toggle
        ButtonComponent colorModeBtn = Components.button(
                Text.literal(useLifetimeColor ? "Color Mode: Lifetime" : "Color Mode: Random"),
                button -> {
                    useLifetimeColor = !useLifetimeColor;
                    button.setMessage(Text.literal(useLifetimeColor ? "Color Mode: Lifetime" : "Color Mode: Random"));
                    applyChangesLive();
                }
        );
        colorModeBtn.sizing(Sizing.fixed(160), Sizing.fixed(20));
        list.child(colorModeBtn.margins(Insets.bottom(5)));

        setupColorRow(list, "Start Color", c1R, val -> c1R = val, c1G, val -> c1G = val, c1B, val -> c1B = val);
        FlowLayout colorSpacer = createRow(list, " ");
        setupColorRow(list, "End Color", c2R, val -> c2R = val, c2G, val -> c2G = val, c2B, val -> c2B = val);



        FlowLayout behaviorRow = createRow(list, " ");
        addSlider(behaviorRow, "Scale", 0.05f, 4.0f, scale, val -> { scale = val; applyChangesLive(); });
        addSlider(behaviorRow, "Gravity", -1f, 1f, gravity, val -> { gravity = val; applyChangesLive(); });

        FlowLayout timingRow = createRow(list, "");
        addFloatSlider(timingRow, "Spawn/Tick", 0.0, 50.0, spawnRate, 2.0, val -> { spawnRate = val; applyChangesLive(); });
        addIntSlider(timingRow, "Lifetime", 1, 150, lifetime, val -> { lifetime = val; applyChangesLive(); });

        // Rotation Checkbox
        FlowLayout rotRow = createRow(list, "");
        CheckboxComponent rotateBox = Components.checkbox(Text.literal("Rotate & Spin"));
        rotateBox.checked(rotate);
        rotateBox.onChanged(checked -> { rotate = checked; applyChangesLive(); });
        rotRow.child(rotateBox);

        setupCoordRow(list, "Spawn Offset", oX, val -> oX = val, oY, val -> oY = val, oZ, val -> oZ = val);
        setupCoordRow(list, "Area Spread", aX, val -> aX = val, aY, val -> aY = val, aZ, val -> aZ = val);
        setupCoordRow(list, "Min Velocity", minVX, val -> minVX = val, minVY, val -> minVY = val, minVZ, val -> minVZ = val);
        setupCoordRow(list, "Max Velocity", maxVX, val -> maxVX = val, maxVY, val -> maxVY = val, maxVZ, val -> maxVZ = val);
        setupCoordRow(list, "Spin", orbX, val -> orbX = val, orbY, val -> orbY = val, orbZ, val -> orbZ = val);

        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fixed(160), list);
        scroll.surface(Surface.flat(SCROLL_BACKGROUND)).padding(Insets.of(5)).margins(Insets.bottom(10));
        mainCard.child(scroll);

        ButtonComponent doneBtn = Components.button(Text.literal("Done").styled(s -> s.withColor(TextColor.fromRgb(BUTTON_TEXT))), button -> this.close());
        doneBtn.sizing(Sizing.fixed(100), Sizing.fixed(20));
        doneBtn.renderer(ButtonComponent.Renderer.flat(BUTTON_BASE, BUTTON_HOVER, ACCENT));
        mainCard.child(doneBtn);

        borderWrapper.child(mainCard);
        rootComponent.child(borderWrapper);
    }

    // ... [Helpers: createRow, setupColorRow, setupCoordRow, addSlider, addFloatSlider, addIntSlider, addNumberField - Unchanged] ...
    // (Ensure you keep the helper methods from the original file here)

    private FlowLayout createRow(FlowLayout parentList, String title) {
        parentList.child(Components.label(Text.literal(title)).color(Color.ofRgb(0xAAAAAA)).margins(Insets.top(10).bottom(4)));
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.horizontalAlignment(HorizontalAlignment.CENTER).verticalAlignment(VerticalAlignment.CENTER);
        parentList.child(row);
        return row;
    }

    private void setupColorRow(FlowLayout list, String title, float r, java.util.function.Consumer<Float> or, float g, java.util.function.Consumer<Float> og, float b, java.util.function.Consumer<Float> ob) {
        FlowLayout row = createRow(list, title);
        addSlider(row, "R", 0, 1, r, val -> { or.accept(val); applyChangesLive(); });
        addSlider(row, "G", 0, 1, g, val -> { og.accept(val); applyChangesLive(); });
        addSlider(row, "B", 0, 1, b, val -> { ob.accept(val); applyChangesLive(); });
    }

    private void setupCoordRow(FlowLayout list, String title, float x, java.util.function.Consumer<Float> ox, float y, java.util.function.Consumer<Float> oy, float z, java.util.function.Consumer<Float> oz) {
        FlowLayout row = createRow(list, title);
        addNumberField(row, "X", x, val -> { ox.accept(val); applyChangesLive(); });
        addNumberField(row, "Y", y, val -> { oy.accept(val); applyChangesLive(); });
        addNumberField(row, "Z", z, val -> { oz.accept(val); applyChangesLive(); });
    }

    private void addSlider(FlowLayout row, String name, float min, float max, float current, java.util.function.Consumer<Float> onChange) {
        FlowLayout col = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content()).horizontalAlignment(HorizontalAlignment.CENTER).margins(Insets.horizontal(4));
        col.child(Components.label(Text.literal(name)).color(Color.ofRgb(0x888888)).margins(Insets.bottom(2)));
        SliderComponent slider = Components.slider(Sizing.fixed(80));
        slider.value((current - min) / (max - min));
        slider.onChanged().subscribe(v -> onChange.accept((float)(min + v * (max - min))));
        col.child(slider);
        row.child(col);
    }

    private void addFloatSlider(FlowLayout row, String name, double min, double max, double current, double exponent, java.util.function.DoubleConsumer onChange) {
        final double range = max - min;
        FlowLayout col = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content()).horizontalAlignment(HorizontalAlignment.CENTER).margins(Insets.horizontal(4));
        col.child(Components.label(Text.literal(name)).color(Color.ofRgb(0x888888)).margins(Insets.bottom(2)));
        SliderComponent slider = Components.slider(Sizing.fixed(130));
        double normalized = (current - min) / range;
        slider.value(Math.pow(normalized, 1.0 / exponent));
        slider.onChanged().subscribe(v -> {
            double curved = Math.pow(v, exponent);
            double value = min + curved * range;
            onChange.accept(value);
        });
        col.child(slider);
        row.child(col);
    }

    private void addIntSlider(FlowLayout row, String name, int min, int max, int current, java.util.function.Consumer<Integer> onChange) {
        FlowLayout col = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content()).horizontalAlignment(HorizontalAlignment.CENTER).margins(Insets.horizontal(4));
        col.child(Components.label(Text.literal(name)).color(Color.ofRgb(0x888888)).margins(Insets.bottom(2)));
        SliderComponent slider = Components.slider(Sizing.fixed(130));
        slider.value((double)(current - min) / (max - min));
        slider.onChanged().subscribe(v -> onChange.accept((int)Math.round(min + v * (max - min))));
        col.child(slider);
        row.child(col);
    }

    private void addNumberField(FlowLayout row, String name, float current, java.util.function.Consumer<Float> onChange) {
        FlowLayout col = (FlowLayout) Containers.verticalFlow(Sizing.content(), Sizing.content()).horizontalAlignment(HorizontalAlignment.CENTER).margins(Insets.horizontal(4));
        col.child(Components.label(Text.literal(name)).color(Color.ofRgb(0x888888)).margins(Insets.bottom(2)));
        TextBoxComponent tb = Components.textBox(Sizing.fixed(80), String.format("%.2f", current));
        tb.onChanged().subscribe(s -> { try { onChange.accept(Float.parseFloat(s)); } catch(Exception ignored){}});
        col.child(tb);
        row.child(col);
    }

    private void applyChangesLive() {
        this.be.updateSettings(
                particleType,
                c1R, c1G, c1B, c2R, c2G, c2B, useLifetimeColor,
                scale, gravity, spawnRate, lifetime,
                oX, oY, oZ, aX, aY, aZ,
                minVX, maxVX, minVY, maxVY, minVZ, maxVZ,
                orbX, orbY, orbZ, rotate
        );
        ModNetwork.CHANNEL.clientHandle().send(new ParticleEmitterUpdatePacket(
                this.blockPos, particleType,
                c1R, c1G, c1B, c2R, c2G, c2B, useLifetimeColor,
                scale, gravity, spawnRate, lifetime,
                oX, oY, oZ, aX, aY, aZ,
                minVX, maxVX, minVY, maxVY, minVZ, maxVZ,
                orbX, orbY, orbZ, rotate
        ));
    }
}
package corablue.stagehand.client.gui;

import corablue.stagehand.block.entity.LoreAnvilBlockEntity;
import corablue.stagehand.network.LoreAnvilUpdatePacket;
import corablue.stagehand.network.ModNetwork;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoreAnvilScreen extends HandledScreen<LoreAnvilBlockEntity.LoreAnvilScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("stagehand", "textures/gui/lore_desk.png");

    private TextFieldWidget nameField;
    private final List<TextFieldWidget> loreFields = new ArrayList<>();
    private ButtonWidget applyButton;
    private final List<ColorButton> colorButtons = new ArrayList<>();
    private ColorButton activeColorButton = null;
    private static final int PICKER_SIZE = 4; // 4x4 Grid
    private static final int COLOR_BOX_SIZE = 12; // Pixel size of each color box
    private ItemStack lastStack = ItemStack.EMPTY;
    private static final Identifier AUTHOR_TEX = Identifier.of("stagehand", "textures/gui/author.png");

    public LoreAnvilScreen(LoreAnvilBlockEntity.LoreAnvilScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 222;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
        this.titleX = 8;
        this.titleY = 6;
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        // --- 1. NAME FIELD ---
        // Background is at y+18, 16px high. Button is 14px high.
        // (16 - 14) / 2 = 1px offset. So button at y+19.
        this.nameField = new TextFieldWidget(textRenderer, x + 24, y + 21, 100, 12, Text.translatable("ui.stagehand.lore_anvil.name_placeholder"));
        setupField(this.nameField);
        this.addSelectableChild(this.nameField);

        // Aligned to the right of the name box
        ColorButton nameColorBtn = new ColorButton(x + 135, y + 19);
        this.addDrawableChild(nameColorBtn);
        this.colorButtons.add(nameColorBtn);

        // --- 2. LORE FIELDS ---
        this.loreFields.clear();
        for (int i = 0; i < 2; i++) {
            int rowY = y + 73 + (i * 18); // Matching the background sprite Y
            TextFieldWidget field = new TextFieldWidget(textRenderer, x + 24, rowY + 3, 100, 12, Text.translatable("ui.stagehand.lore_anvil.lore_placeholder", i));
            setupField(field);
            this.loreFields.add(field);
            this.addSelectableChild(field);

            // Center the 14px button in the 16px row (rowY + 1)
            ColorButton colorBtn = new ColorButton(x + 135, rowY + 1);
            this.addDrawableChild(colorBtn);
            this.colorButtons.add(colorBtn);
        }

        this.applyButton = ButtonWidget.builder(Text.empty(), button -> sendUpdate())
                .dimensions(x + 105, y + 44, 18, 18)
                .build();
        this.addDrawableChild(this.applyButton);

        checkItemUpdate(true);
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();
        checkItemUpdate(false);
    }

    private void checkItemUpdate(boolean force) {
        ItemStack current = handler.getSlot(0).getStack();
        if (force || !ItemStack.areEqual(current, lastStack)) {
            this.lastStack = current.copy();

            if (!current.isEmpty()) {
                this.nameField.setText(current.getName().getString());
                colorButtons.forEach(ColorButton::reset);

                if (current.contains(DataComponentTypes.LORE)) {
                    LoreComponent lore = current.get(DataComponentTypes.LORE);
                    List<Text> lines = lore != null ? lore.lines() : List.of();
                    for (int i = 0; i < loreFields.size(); i++) {
                        if (i < lines.size()) loreFields.get(i).setText(lines.get(i).getString());
                        else loreFields.get(i).setText("");
                    }
                } else {
                    loreFields.forEach(f -> f.setText(""));
                }
            }
        }
    }

    private void setupField(TextFieldWidget field) {
        field.setEditableColor(0xFFFFFF);
        field.setUneditableColor(0xA0A0A0);
        field.setDrawsBackground(false); // We draw the textured one manually below
        field.setMaxLength(50);
        field.setFocusUnlocked(true);
    }

    private void sendUpdate() {
        // Clean the text: only send the text entered, apply the color code prefix here
        String nameColor = colorButtons.get(0).getColorCode();
        String finalName = nameField.getText().isEmpty() ? "" : nameColor + nameField.getText();

        List<String> formattedLore = new ArrayList<>();
        for (int i = 0; i < loreFields.size(); i++) {
            String text = loreFields.get(i).getText();
            if (!text.isEmpty()) {
                String color = colorButtons.get(i + 1).getColorCode();
                formattedLore.add(color + text);
            }
        }

        if (this.client.player != null) {
            this.client.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        }

        ModNetwork.CHANNEL.clientHandle().send(new LoreAnvilUpdatePacket(handler.getEntity().getPos(), finalName, formattedLore));
    }

    // --- MOUSE HANDLING (Picker + Focus) ---
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 1. If Picker is Open, handle clicks there first
        if (activeColorButton != null) {
            if (handlePickerClick(mouseX, mouseY)) return true;
            // If we clicked outside the picker, close it
            activeColorButton = null;
        }

        // 2. Clear Focus
        this.nameField.setFocused(false);
        loreFields.forEach(f -> f.setFocused(false));

        // 3. Check Field Clicks
        boolean clickedField = false;
        if (this.nameField.isMouseOver(mouseX, mouseY)) {
            this.nameField.setFocused(true);
            clickedField = true;
        }
        for (TextFieldWidget field : loreFields) {
            if (field.isMouseOver(mouseX, mouseY)) {
                field.setFocused(true);
                clickedField = true;
            }
        }
        if (clickedField) return true;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handlePickerClick(double mouseX, double mouseY) {
        // Calculate Picker Position (Right next to the button)
        int startX = activeColorButton.getX() + 16;
        int startY = activeColorButton.getY();

        // Check 16 colors (Standard Minecraft colors are index 0-15 in Formatting)
        List<Formatting> colors = getColors();
        for (int i = 0; i < colors.size(); i++) {
            int row = i / PICKER_SIZE;
            int col = i % PICKER_SIZE;
            int boxX = startX + (col * COLOR_BOX_SIZE);
            int boxY = startY + (row * COLOR_BOX_SIZE);

            if (mouseX >= boxX && mouseX < boxX + COLOR_BOX_SIZE &&
                    mouseY >= boxY && mouseY < boxY + COLOR_BOX_SIZE) {
                // Clicked a color!
                activeColorButton.setColor(colors.get(i));
                activeColorButton = null; // Close picker
                return true;
            }
        }

        // Also check "Default/Reset" button (Bottom of picker)
        int resetY = startY + (4 * COLOR_BOX_SIZE) + 2;
        if (mouseX >= startX && mouseX < startX + (4 * COLOR_BOX_SIZE) &&
                mouseY >= resetY && mouseY < resetY + 12) {
            activeColorButton.reset();
            activeColorButton = null;
            return true;
        }

        return false;
    }

    // --- RENDERING ---
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // Render text fields
        this.nameField.render(context, mouseX, mouseY, delta);
        loreFields.forEach(f -> f.render(context, mouseX, mouseY, delta));

        // If button is disabled (no item), make it look "dimmed"
        boolean hasItem = !handler.getSlot(0).getStack().isEmpty();
        if (!hasItem) {
            // Draw at 50% opacity or just a dark version
            context.setShaderColor(0.5f, 0.5f, 0.5f, 1.0f);
        }

        context.drawTexture(AUTHOR_TEX, applyButton.getX() + 1, applyButton.getY() + 1, 0, 0, 16, 16, 16, 16);
        context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Reset color

        // Render Color Picker if open
        if (activeColorButton != null) renderPicker(context, mouseX, mouseY);

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void renderPicker(DrawContext context, int mouseX, int mouseY) {
        int startX = activeColorButton.getX() + 18; // Slight gap from button
        int startY = activeColorButton.getY();

        List<Formatting> colors = getColors();
        int rows = (int) Math.ceil(colors.size() / (double) PICKER_SIZE);

        int width = PICKER_SIZE * COLOR_BOX_SIZE;
        int height = (rows * COLOR_BOX_SIZE) + 14;

        // Draw Drop Shadow / Background
        context.fill(startX - 3, startY - 3, startX + width + 3, startY + height + 3, 0xFF000000);
        context.drawBorder(startX - 3, startY - 3, width + 6, height + 6, 0xFFFFFFFF);

        for (int i = 0; i < colors.size(); i++) {
            int row = i / PICKER_SIZE;
            int col = i % PICKER_SIZE;
            int boxX = startX + (col * COLOR_BOX_SIZE);
            int boxY = startY + (row * COLOR_BOX_SIZE);

            int colorHex = colors.get(i).getColorValue() | 0xFF000000;

            // Draw the color square
            context.fill(boxX + 1, boxY + 1, boxX + COLOR_BOX_SIZE - 1, boxY + COLOR_BOX_SIZE - 1, colorHex);

            // Hover effect
            if (mouseX >= boxX && mouseX < boxX + COLOR_BOX_SIZE && mouseY >= boxY && mouseY < boxY + COLOR_BOX_SIZE) {
                context.drawBorder(boxX, boxY, COLOR_BOX_SIZE, COLOR_BOX_SIZE, 0xFFFFFFFF);
            }
        }

        // Reset Button at bottom
        int resetY = startY + (rows * COLOR_BOX_SIZE) + 2;
        context.fill(startX, resetY, startX + width, resetY + 10, 0xFF404040);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("ui.stagehand.lore_anvil.none"), startX + width / 2, resetY + 1, 0xFFFFFF);
    }

    private List<Formatting> getColors() {
        return List.of(
                // Row 1: Bright/Warm
                Formatting.RED, Formatting.GOLD, Formatting.YELLOW, Formatting.LIGHT_PURPLE,
                // Row 2: Greens & Cyans
                Formatting.GREEN, Formatting.DARK_GREEN, Formatting.AQUA, Formatting.DARK_AQUA,
                // Row 3: Deep Tones
                Formatting.BLUE, Formatting.DARK_BLUE, Formatting.DARK_RED, Formatting.DARK_PURPLE,
                // Row 4: Neutrals
                Formatting.WHITE, Formatting.GRAY, Formatting.DARK_GRAY, Formatting.BLACK
        );
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        // 1. Draw your custom container background
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);

        // 3. THE 1.21.1 SPRITE WAY:
        // This pulls the professional textured box from the Minecraft sprite library
        Identifier TEXT_FIELD_SPRITE = Identifier.ofVanilla("container/anvil/text_field");

        // Name field background
        context.drawGuiTexture(TEXT_FIELD_SPRITE, x + 21, y + 18, 110, 16);

        // Lore field backgrounds
        context.drawGuiTexture(TEXT_FIELD_SPRITE, x + 21, y + 73, 110, 16);
        context.drawGuiTexture(TEXT_FIELD_SPRITE, x + 21, y + 91, 110, 16);

        // 4. Draw the Fake Slot for the item
        context.drawTexture(TEXTURE, x + 79, y + 44, 7, 139, 18, 18);
    }

    // --- KEYBOARD ---
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.player.closeHandledScreen();
            return true;
        }
        boolean anyFocused = this.nameField.isFocused() || loreFields.stream().anyMatch(TextFieldWidget::isFocused);
        if (anyFocused) {
            if (this.nameField.isFocused()) this.nameField.keyPressed(keyCode, scanCode, modifiers);
            loreFields.forEach(f -> { if (f.isFocused()) f.keyPressed(keyCode, scanCode, modifiers); });
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.nameField.isFocused()) return this.nameField.charTyped(chr, modifiers);
        for (TextFieldWidget f : loreFields) if (f.isFocused()) return f.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }

    // --- BUTTON CLASS (Simplified) ---
    class ColorButton extends ButtonWidget {
        private Formatting selectedColor = null; // Null = Default

        public ColorButton(int x, int y) {
            super(x, y, 14, 14, Text.empty(), button -> {
                // On Click: Toggle the Picker
                if (activeColorButton == (ColorButton)button) activeColorButton = null;
                else activeColorButton = (ColorButton)button;
            }, DEFAULT_NARRATION_SUPPLIER);
        }

        public void setColor(Formatting color) { this.selectedColor = color; }
        public void reset() { this.selectedColor = null; }

        public String getColorCode() {
            return (selectedColor == null) ? "" : selectedColor.toString();
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            context.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);

            if (selectedColor == null) {
                // Default Grey
                context.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0xFFA0A0A0);
            } else {
                // Selected Color
                int colorHex = selectedColor.getColorValue() | 0xFF000000;
                context.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, colorHex);
            }

            if (isHovered()) context.drawBorder(getX(), getY(), width, height, 0xFFFFFFFF);
        }
    }
}
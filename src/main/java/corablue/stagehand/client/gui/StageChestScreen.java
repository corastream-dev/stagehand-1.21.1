package corablue.stagehand.client.gui;

import corablue.stagehand.block.entity.StageChestBlockEntity.ChestMode;
import corablue.stagehand.network.StageChestUpdatePacket;
import corablue.stagehand.screen.StageChestScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class StageChestScreen extends HandledScreen<StageChestScreenHandler> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/generic_54.png");

    private StyledButton modeButton;
    private StyledButton saveTimeButton;

    private TextFieldWidget hoursField;
    private TextFieldWidget minutesField;
    private TextFieldWidget secondsField;

    public StageChestScreen(StageChestScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 114 + 3 * 18;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int panelX = this.x + 180;
        int panelY = this.y + 16;

        // Styled Mode Button (Orange Accent)
        this.modeButton = this.addDrawableChild(new StyledButton(panelX, panelY, 100, 20, getModeText(), 0xFFD47A3D, () -> {
            int nextMode = (this.handler.getMode() + 1) % ChestMode.values().length;
            sendUpdatePacket(nextMode, this.handler.getTimerTicks());
        }));

        // Math to load the current tick countdown into H:M:S
        int totalSeconds = this.handler.getTimerTicks() / 20;
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;

        // Hours input
        this.hoursField = new TextFieldWidget(this.textRenderer, panelX, panelY + 44, 30, 16, Text.literal("Hours"));
        this.hoursField.setText(String.valueOf(h));
        this.hoursField.setTextPredicate(str -> str.matches("\\d*"));
        this.hoursField.setMaxLength(4);
        this.addDrawableChild(this.hoursField);

        // Minutes input
        this.minutesField = new TextFieldWidget(this.textRenderer, panelX + 35, panelY + 44, 30, 16, Text.literal("Minutes"));
        this.minutesField.setText(String.valueOf(m));
        this.minutesField.setTextPredicate(str -> str.matches("\\d*"));
        this.minutesField.setMaxLength(4);
        this.addDrawableChild(this.minutesField);

        // Seconds input
        this.secondsField = new TextFieldWidget(this.textRenderer, panelX + 70, panelY + 44, 30, 16, Text.literal("Seconds"));
        this.secondsField.setText(String.valueOf(s));
        this.secondsField.setTextPredicate(str -> str.matches("\\d*"));
        this.secondsField.setMaxLength(4);
        this.addDrawableChild(this.secondsField);

        // Styled Save Button (Green Accent like the Stage Config save button)
        this.saveTimeButton = this.addDrawableChild(new StyledButton(panelX, panelY + 65, 100, 20, Text.translatable("ui.stagehand.stage_config.save"), 0xFF4CAF50, () -> {
            saveTimerTicks();
        }));

        updateButtonStates();
    }

    private Text getModeText() {
        ChestMode currentMode = ChestMode.values()[this.handler.getMode()];
        return Text.literal("Mode: " + currentMode.name());
    }

    private void updateButtonStates() {
        boolean isOwner = this.handler.isOwner();
        this.modeButton.visible = isOwner;

        boolean isTimer = ChestMode.values()[this.handler.getMode()] == ChestMode.TIMER;
        this.hoursField.visible = isOwner && isTimer;
        this.minutesField.visible = isOwner && isTimer;
        this.secondsField.visible = isOwner && isTimer;
        this.saveTimeButton.visible = isOwner && isTimer;

        if (isOwner) {
            this.modeButton.setMessage(getModeText());
        }
    }

    private void saveTimerTicks() {
        try {
            int h = hoursField.getText().isEmpty() ? 0 : Integer.parseInt(hoursField.getText());
            int m = minutesField.getText().isEmpty() ? 0 : Integer.parseInt(minutesField.getText());
            int s = secondsField.getText().isEmpty() ? 0 : Integer.parseInt(secondsField.getText());

            // Convert H:M:S back into standard game ticks (20 TPS)
            int totalTicks = (h * 3600 + m * 60 + s) * 20;
            sendUpdatePacket(this.handler.getMode(), totalTicks);
        } catch (NumberFormatException e) {
            // Failsafe catch for entirely malformed data, prevents client crashing
        }
    }

    private void sendUpdatePacket(int modeOrdinal, int timerTicks) {
        corablue.stagehand.network.ModNetwork.CHANNEL.clientHandle().send(new StageChestUpdatePacket(this.handler.pos, modeOrdinal, timerTicks));
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        updateButtonStates();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(TEXTURE, i, j, 0, 0, this.backgroundWidth, 3 * 18 + 17);
        context.drawTexture(TEXTURE, i, j + 3 * 18 + 17, 0, 126, this.backgroundWidth, 96);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        // Draw the labels hovering over the time fields
        if (this.handler.isOwner() && ChestMode.values()[this.handler.getMode()] == ChestMode.TIMER) {
            int panelX = this.x + 180;
            int panelY = this.y + 16;

            context.drawText(this.textRenderer, "Hr", panelX + 9, panelY + 34, 0xAAAAAA, false);
            context.drawText(this.textRenderer, "Min", panelX + 41, panelY + 34, 0xAAAAAA, false);
            context.drawText(this.textRenderer, "Sec", panelX + 76, panelY + 34, 0xAAAAAA, false);
        }
    }

    // --- Custom UI Elements --- //

    /**
     * A custom widget emulating the base owo-lib styling used across other menus.
     */
    private class StyledButton extends PressableWidget {
        private final int accentColor;
        private final Runnable onPress;

        public StyledButton(int x, int y, int width, int height, Text message, int accentColor, Runnable onPress) {
            super(x, y, width, height, message);
            this.accentColor = accentColor;
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            this.onPress.run();
        }

        @Override
        protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
            this.appendDefaultNarrations(builder);
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int baseColor = 0xFF2E180D;
            int hoverColor = 0xFF884D27;
            int textColor = 0xFFD2953D;

            boolean hovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight();
            int bgColor = hovered && this.active ? hoverColor : baseColor;

            // Render Outline
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.accentColor);
            // Render Inner Fill
            context.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.getWidth() - 1, this.getY() + this.getHeight() - 1, bgColor);

            // Center Text
            int textX = this.getX() + (this.getWidth() - textRenderer.getWidth(this.getMessage())) / 2;
            int textY = this.getY() + (this.getHeight() - 8) / 2;
            context.drawText(textRenderer, this.getMessage(), textX, textY, this.active ? textColor : 0xFFAAAAAA, true);
        }
    }
}
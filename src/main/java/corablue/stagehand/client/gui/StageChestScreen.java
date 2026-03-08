package corablue.stagehand.client.gui;

import corablue.stagehand.block.entity.StageChestBlockEntity.ChestMode;
import corablue.stagehand.network.StageChestUpdatePacket;
import corablue.stagehand.screen.StageChestScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class StageChestScreen extends HandledScreen<StageChestScreenHandler> {
    // We use the vanilla 54-slot chest texture, but we'll only draw the top 3 rows.
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/generic_54.png");

    private ButtonWidget modeButton;
    private ButtonWidget timeAddButton;
    private ButtonWidget timeSubButton;

    public StageChestScreen(StageChestScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 114 + 3 * 18; // Size for 3 rows + player inventory
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // Mode Cycle Button (Placed to the right of the UI)
        this.modeButton = this.addDrawableChild(ButtonWidget.builder(getModeText(), button -> {
            int nextMode = (this.handler.getMode() + 1) % ChestMode.values().length;
            sendUpdatePacket(nextMode, this.handler.getTimerTicks());
        }).dimensions(this.x + 180, this.y + 16, 100, 20).build());

        // Timer + Button
        this.timeAddButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("+1 Hr"), button -> {
            sendUpdatePacket(this.handler.getMode(), this.handler.getTimerTicks() + 1000); // 1 in-game hr = 1000 ticks
        }).dimensions(this.x + 180, this.y + 40, 48, 20).build());

        // Timer - Button
        this.timeSubButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("-1 Hr"), button -> {
            int newTime = Math.max(0, this.handler.getTimerTicks() - 1000);
            sendUpdatePacket(this.handler.getMode(), newTime);
        }).dimensions(this.x + 232, this.y + 40, 48, 20).build());

        updateButtonStates();
    }

    private Text getModeText() {
        ChestMode currentMode = ChestMode.values()[this.handler.getMode()];
        return Text.literal("Mode: " + currentMode.name());
    }

    private void updateButtonStates() {
        this.modeButton.setMessage(getModeText());

        // Only show timer buttons if the mode is TIMER
        boolean isTimer = ChestMode.values()[this.handler.getMode()] == ChestMode.TIMER;
        this.timeAddButton.visible = isTimer;
        this.timeSubButton.visible = isTimer;
    }

    private void sendUpdatePacket(int modeOrdinal, int timerTicks) {
        corablue.stagehand.network.ModNetwork.CHANNEL.clientHandle().send(new StageChestUpdatePacket(this.handler.pos, modeOrdinal, timerTicks));
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        updateButtonStates(); // Keep buttons updated if data syncs from server
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

        // Draw the timer value if in Timer mode
        if (ChestMode.values()[this.handler.getMode()] == ChestMode.TIMER) {
            int seconds = this.handler.getTimerTicks() / 20;
            context.drawText(this.textRenderer, "Cooldown: " + seconds + "s", this.x + 180, this.y + 65, 0xFFFFFF, true);
        }
    }
}
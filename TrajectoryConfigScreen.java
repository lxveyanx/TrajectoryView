package org.lxveyanx.trajectorymod.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class TrajectoryConfigScreen extends Screen {

    private final Screen parent;
    private final TrajectoryConfig config;

    private ColorSlider sliderR1, sliderG1, sliderB1; // Start
    private ColorSlider sliderR2, sliderG2, sliderB2; // End
    private ColorSlider sliderR3, sliderG3, sliderB3; // Marker

    public TrajectoryConfigScreen(Screen parent) {
        super(Text.of("Trajectory Settings"));
        this.parent = parent;
        this.config = TrajectoryConfig.get();
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Пусто
    }

    @Override
    protected void init() {
        int w = this.width;
        int h = this.height;
        int panelWidth = 340;
        int panelHeight = 270; // Увеличили высоту
        int panelX = w / 2 - panelWidth / 2;
        int panelY = h / 2 - panelHeight / 2;

        int leftX = panelX + 20;
        int rightX = panelX + 180;
        int startY = panelY + 40;

        // --- ЛЕВЫЕ КНОПКИ ---
        int btnW = 120;
        int btnH = 20;

        addDrawableChild(new ModernButton(leftX, startY, btnW, btnH, "Mod Enabled", () -> config.enabled, v -> config.enabled = v));
        addDrawableChild(new ModernButton(leftX, startY + 25, btnW, btnH, "Show Trident", () -> config.showTrident, v -> config.showTrident = v));
        addDrawableChild(new ModernButton(leftX, startY + 50, btnW, btnH, "Show Crossbow", () -> config.showCrossbow, v -> config.showCrossbow = v));
        addDrawableChild(new ModernButton(leftX, startY + 75, btnW, btnH, "Show Others", () -> config.showOthers, v -> config.showOthers = v));

        // --- ПРАВЫЕ СЛАЙДЕРЫ ---
        int sliderW = 130;

        // 1. Start Colors
        addDrawableChild(sliderR1 = new ColorSlider(rightX, startY, sliderW, 12, "Core R", config.colorStart >> 16 & 0xFF, 0xFFFF5555));
        addDrawableChild(sliderG1 = new ColorSlider(rightX, startY + 15, sliderW, 12, "Core G", config.colorStart >> 8 & 0xFF, 0xFF55FF55));
        addDrawableChild(sliderB1 = new ColorSlider(rightX, startY + 30, sliderW, 12, "Core B", config.colorStart & 0xFF, 0xFF5555FF));

        int y2 = startY + 55;
        // 2. End Colors
        addDrawableChild(sliderR2 = new ColorSlider(rightX, y2, sliderW, 12, "Neon R", config.colorEnd >> 16 & 0xFF, 0xFFFF5555));
        addDrawableChild(sliderG2 = new ColorSlider(rightX, y2 + 15, sliderW, 12, "Neon G", config.colorEnd >> 8 & 0xFF, 0xFF55FF55));
        addDrawableChild(sliderB2 = new ColorSlider(rightX, y2 + 30, sliderW, 12, "Neon B", config.colorEnd & 0xFF, 0xFF5555FF));

        int y3 = y2 + 55;
        // 3. Marker Colors (НОВОЕ)
        addDrawableChild(sliderR3 = new ColorSlider(rightX, y3, sliderW, 12, "Mark R", config.markerColorBlock >> 16 & 0xFF, 0xFFFF5555));
        addDrawableChild(sliderG3 = new ColorSlider(rightX, y3 + 15, sliderW, 12, "Mark G", config.markerColorBlock >> 8 & 0xFF, 0xFF55FF55));
        addDrawableChild(sliderB3 = new ColorSlider(rightX, y3 + 30, sliderW, 12, "Mark B", config.markerColorBlock & 0xFF, 0xFF5555FF));

        // Кнопка ВЫХОД
        addDrawableChild(new ModernButton(panelX + panelWidth/2 - 50, panelY + panelHeight - 30, 100, 20, "Save & Close", () -> true, v -> {
            saveColors();
            TrajectoryConfig.save();
            close();
        }) {
            @Override
            public void onClick(double mouseX, double mouseY) { this.setter.accept(true); }
            @Override
            protected int getColor(boolean isOn) { return 0x55FF55; }
        });
    }

    private void saveColors() {
        config.colorStart = ((sliderR1.getValueInt() & 0xFF) << 16) | ((sliderG1.getValueInt() & 0xFF) << 8) | (sliderB1.getValueInt() & 0xFF);
        config.colorEnd = ((sliderR2.getValueInt() & 0xFF) << 16) | ((sliderG2.getValueInt() & 0xFF) << 8) | (sliderB2.getValueInt() & 0xFF);
        config.markerColorBlock = ((sliderR3.getValueInt() & 0xFF) << 16) | ((sliderG3.getValueInt() & 0xFF) << 8) | (sliderB3.getValueInt() & 0xFF);
    }

    private int lightenColor(int color) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + 150);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 150);
        int b = Math.min(255, (color & 0xFF) + 150);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = width / 2;
        int centerY = height / 2;
        int panelWidth = 340;
        int panelHeight = 270;
        int x1 = centerX - panelWidth / 2;
        int y1 = centerY - panelHeight / 2;
        int x2 = centerX + panelWidth / 2;
        int y2 = centerY + panelHeight / 2;

        context.fillGradient(0, 0, width, height, 0x80000000, 0x80000000);
        context.fill(x1, y1, x2, y2, 0xEE151515);
        context.drawBorder(x1, y1, panelWidth, panelHeight, 0xFF444444);

        context.drawCenteredTextWithShadow(textRenderer, "TRAJECTORY CONFIG", centerX, y1 + 10, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Modules", x1 + 80, y1 + 30, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, "Visuals", x2 - 85, y1 + 30, 0xFFAAAAAA);

        super.render(context, mouseX, mouseY, delta);

        // --- ПРЕВЬЮ ---
        saveColors();
        int startC = config.colorStart;
        int endC = config.colorEnd;
        int startCore = lightenColor(startC);
        int endCore = lightenColor(endC);

        int pX = centerX;
        int pYTop = y1 + 50;
        int pYBot = y2 - 50;

        context.fillGradient(pX - 6, pYTop, pX + 6, pYBot, startC | 0xFF000000, endC | 0xFF000000);
        context.fillGradient(pX - 2, pYTop, pX + 2, pYBot, startCore | 0xFF000000, endCore | 0xFF000000);

        context.drawCenteredTextWithShadow(textRenderer, "Preview", pX, pYBot + 5, 0xFF666666);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private class ModernButton extends ClickableWidget {
        private final java.util.function.Supplier<Boolean> getter;
        protected final java.util.function.Consumer<Boolean> setter;
        private final String label;

        public ModernButton(int x, int y, int w, int h, String label, java.util.function.Supplier<Boolean> getter, java.util.function.Consumer<Boolean> setter) {
            super(x, y, w, h, Text.of(label));
            this.label = label;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public void onClick(double mouseX, double mouseY) { setter.accept(!getter.get()); }

        protected int getColor(boolean isOn) { return isOn ? 0xFF00AA00 : 0xFFAA0000; }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean isOn = getter.get();
            int bgColor = isHovered() ? 0xFF353535 : 0xFF202020;
            int borderColor = isHovered() ? 0xFF888888 : 0xFF333333;

            context.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
            context.drawBorder(getX(), getY(), width, height, borderColor);

            int indColor = getColor(isOn);
            context.fill(getX() + 2, getY() + 2, getX() + 5, getY() + height - 2, indColor);

            int textColor = isOn ? 0xFFFFFF : 0x999999;
            context.drawTextWithShadow(textRenderer, label, getX() + 10, getY() + (height - 8) / 2, textColor);

            String state = isOn ? "ON" : "OFF";
            context.drawTextWithShadow(textRenderer, state, getX() + width - textRenderer.getWidth(state) - 5, getY() + (height - 8) / 2, indColor);
        }
        @Override
        protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {}
    }

    private class ColorSlider extends ClickableWidget {
        private double value;
        private final String prefix;
        private final int barColor;

        public ColorSlider(int x, int y, int w, int h, String prefix, int initialValue, int barColor) {
            super(x, y, w, h, Text.empty());
            this.prefix = prefix;
            this.value = initialValue / 255.0;
            this.barColor = barColor;
            updateMessage();
        }

        private void updateMessage() {
            int val = (int) (value * 255);
            this.setMessage(Text.of(prefix + " " + val));
        }

        public int getValueInt() { return (int) (value * 255); }

        @Override
        public void onClick(double mouseX, double mouseY) { setValueFromMouse(mouseX); }

        @Override
        protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
            setValueFromMouse(mouseX);
            super.onDrag(mouseX, mouseY, deltaX, deltaY);
        }

        private void setValueFromMouse(double mouseX) {
            double relativeX = (mouseX - getX()) / (double) getWidth();
            this.value = MathHelper.clamp(relativeX, 0.0, 1.0);
            updateMessage();
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            context.fill(x, y, x + w, y + h, 0xFF151515);
            context.drawBorder(x, y, w, h, 0xFF444444);
            int fillW = (int) (value * (w - 2));
            context.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, barColor | 0xAA000000);
            context.drawTextWithShadow(textRenderer, prefix, x + 4, y + (h - 8) / 2, 0xCCCCCC);
            String valStr = String.valueOf(getValueInt());
            context.drawTextWithShadow(textRenderer, valStr, x + w - textRenderer.getWidth(valStr) - 4, y + (h - 8) / 2, 0xFFFFFF);
        }
        @Override
        protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {}
    }
}
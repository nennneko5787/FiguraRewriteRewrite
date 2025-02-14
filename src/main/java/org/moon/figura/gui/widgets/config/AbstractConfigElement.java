package org.moon.figura.gui.widgets.config;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.moon.figura.config.ConfigType;
import org.moon.figura.gui.widgets.AbstractContainerElement;
import org.moon.figura.gui.widgets.ParentedButton;
import org.moon.figura.gui.widgets.Button;
import org.moon.figura.gui.widgets.lists.ConfigList;
import org.moon.figura.utils.ui.UIHelper;

import java.util.Objects;

public abstract class AbstractConfigElement extends AbstractContainerElement {

    protected final ConfigType<?> config;
    protected final ConfigList parent;

    protected Button resetButton;

    protected Object initValue;

    public AbstractConfigElement(int width, ConfigType<?> config, ConfigList parent) {
        super(0, 0, width, 20);
        this.config = config;
        this.parent = parent;
        this.initValue = config.value;

        //reset button
        children.add(resetButton = new ParentedButton(0, 0, 60, 20, Component.translatable("controls.reset"), this, button -> config.resetTemp()));
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) return;

        //vars
        Font font = Minecraft.getInstance().font;
        int textY = getY() + getHeight() / 2 - font.lineHeight / 2;

        //hovered arrow
        setHovered(isMouseOver(mouseX, mouseY));
        if (isHovered()) font.draw(stack, HOVERED_ARROW, (int) (getX() + 8 - font.width(HOVERED_ARROW) / 2f), textY, 0xFFFFFF);

        //render name
        renderTitle(stack, font, textY);

        //render children
        super.render(stack, mouseX, mouseY, delta);
    }

    public void renderTitle(PoseStack stack, Font font, int y) {
        font.draw(stack, config.name, getX() + 16, y, (config.disabled ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE).getColor());
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        boolean over = this.parent.isInsideScissors(mouseX, mouseY) && super.isMouseOver(mouseX, mouseY);

        if (over && mouseX < this.getX() + this.getWidth() - 158)
            UIHelper.setTooltip(getTooltip());

        return over;
    }

    public MutableComponent getTooltip() {
        return config.tooltip.copy();
    }

    public boolean isDefault() {
        return this.config.isDefault();
    }

    public boolean isChanged() {
        return !Objects.equals(this.config.tempValue, this.initValue);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        resetButton.setX(x + getWidth() - 60);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        resetButton.setY(y);
    }
}

package icbm.classic.api;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/31/2018.
 */
public enum EnumTier implements StringRepresentable
{
    ONE(ChatFormatting.GREEN),
    TWO(ChatFormatting.YELLOW),
    THREE(ChatFormatting.GOLD),
    FOUR(ChatFormatting.RED),
    NONE(ChatFormatting.WHITE); //TODO we should move the colors to the translations directly

    private ChatFormatting tooltipColor;

    EnumTier(ChatFormatting tooltipColor)
    {
        this.tooltipColor = tooltipColor;
    }

    public ChatFormatting getTooltipColor()
    {
        return tooltipColor;
    }

    @Override
    public String toString()
    {
        return this.getSerializedName();
    }

    public String getLocalizedName()
    {
        return Component.translatable("tier.icbmclassic." + getSerializedName()).getString();
    }

    @Override
    public String getSerializedName()
    {
        return name().toLowerCase();
    }

    public static EnumTier get(int itemDamage)
    {
        if (itemDamage > 0 && itemDamage < values().length)
        {
            return values()[itemDamage];
        }
        return ONE;
    }
}

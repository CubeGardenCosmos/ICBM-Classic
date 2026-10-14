package icbm.classic.content.potion;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * 1.12: {@code CustomPotionEffect extends PotionEffect}, adding a {@code List<ItemStack> curativeItems} and
 * a constructor that could build an effect with a custom cure list (an <b>empty</b> list meant "not curable
 * by milk"). Vanilla built its curable items through {@code Potion#getCurativeItems()}.
 * <p>
 * 26.3 API check (decompiled sources of {@code net/minecraft/world/effect/MobEffect} and
 * {@code MobEffectInstance}): neither class exposes a curative item list, {@code MobEffect} only carries a
 * {@link MobEffectCategory}, colour, attribute modifiers and the tick/particle hooks. There is no
 * {@code getCurativeItems}/{@code isCurativeItem} equivalent, so the list is <b>dropped</b>: every ICBM
 * effect is a normal effect and this class is a plain {@link MobEffectInstance} subclass kept so the 1.12
 * call shape ({@code new CustomPotionEffect(holder, duration, amplifier)}) and class name survive the port.
 * The 1.12 comment/reference is preserved in {@link ContagiousPoison}.
 */
public class CustomPotionEffect extends MobEffectInstance
{
    public CustomPotionEffect(Holder<MobEffect> effect, int duration, int amplifier)
    {
        super(effect, duration, amplifier);
    }
}

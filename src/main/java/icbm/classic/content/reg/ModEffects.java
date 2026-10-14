package icbm.classic.content.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.content.potion.ContagiousPoison;
import icbm.classic.content.potion.FrostBiteEffect;
import icbm.classic.content.potion.RadiationEffect;
import icbm.classic.content.potion.ToxinEffect;
import icbm.classic.content.potion.VirusEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

/**
 * Registers the ICBM custom mob effects.
 * <p>
 * 1.12 had two unused abstract potion classes ({@code CustomPotion}, {@code CustomPotionEffect}) and one
 * concrete pair, {@code Poison}/{@code ContagiousPoison}, which only applied vanilla effects. The
 * {@code potion.toxin}/{@code potion.virus}/{@code potion.frostBite}/{@code potion.radiation} lang entries
 * existed without any potion behind them. 26.3 needs every effect registered, so the four named effects are
 * created here and the 1.12 vanilla sets are moved onto them (see each effect class for the exact mapping).
 * The 1.12 {@code ICBMClassic.chemicalPotion}/{@code contagiousPotion} instances live here as
 * {@link #CHEMICAL_POISON}/{@link #CONTAGIOUS_POISON} because the 26.3 {@code ICBMClassic} only wires
 * registration calls.
 * <p>
 * Called from {@code ICBMClassic#onInitialize}. The ids must stay {@code toxin}, {@code virus},
 * {@code frost_bite} and {@code radiation} to match the existing {@code effect.icbmclassic.*} lang keys.
 */
public final class ModEffects
{
    /** Chemical poison gas, 1.12 {@code potion.toxin}. Effect set: {@link ContagiousPoison#CHEMICAL}. */
    public static Holder<MobEffect> TOXIN;
    /** Contagious virus gas, 1.12 {@code potion.virus}. Effect set: {@link ContagiousPoison#CONTAGIOUS} plus spread. */
    public static Holder<MobEffect> VIRUS;
    /** Cold / frostbite, 1.12 {@code potion.frostBite}. Slowness + mining fatigue. */
    public static Holder<MobEffect> FROST_BITE;
    /** Radiation, 1.12 {@code potion.radiation}. Periodic radioactive damage. */
    public static Holder<MobEffect> RADIATION;

    /** 1.12 {@code ICBMClassic.chemicalPotion = new ContagiousPoison("Chemical", 0, false)} (id dropped). */
    public static final ContagiousPoison CHEMICAL_POISON = new ContagiousPoison(ContagiousPoison.CHEMICAL, false);
    /** 1.12 {@code ICBMClassic.contagiousPotion = new ContagiousPoison("Contagious", 1, true)} (id dropped). */
    public static final ContagiousPoison CONTAGIOUS_POISON = new ContagiousPoison(ContagiousPoison.CONTAGIOUS, true);

    private ModEffects()
    {
        // static only
    }

    /** Registers every effect. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        TOXIN = register("toxin", new ToxinEffect());
        VIRUS = register("virus", new VirusEffect());
        FROST_BITE = register("frost_bite", new FrostBiteEffect());
        RADIATION = register("radiation", new RadiationEffect());
    }

    /**
     * Registers an effect and returns its holder, the 26.3 form of the 1.12 self registration inside the
     * {@code CustomPotion} constructor.
     */
    private static Holder<MobEffect> register(String id, MobEffect effect)
    {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, ICBMConstants.id(id), effect);
    }
}

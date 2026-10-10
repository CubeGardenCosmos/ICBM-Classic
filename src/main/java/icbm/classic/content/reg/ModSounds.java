package icbm.classic.content.reg;

import net.minecraft.sounds.SoundEvent;

/**
 * Sound event registration. Replaces the 1.12 {@code ICBMSounds} enum and {@code sounds.json} wiring
 * (porting-guide.md §2.1).
 * <p>
 * Every field is filled by {@link #register()} from the id below; the matching entry (files + subtitle)
 * lives in {@code assets/icbmclassic/sounds.json}. Field names are the 1.12 enum constants, the ids are the
 * 1.12 {@code sounds.json} keys converted to lower case snake_case (registry-ids.md §7).
 * <p>
 * The 1.12 {@code play()} helpers were not ported: the 26.3 call sites pass a {@code SoundSource} per
 * playback, which is stored here as a comment (1.12 {@code SoundCategory.BLOCKS} unless noted) and applied by
 * the ported code at the play call.
 * <p>
 * 1.12 source: {@code legacy-1.12/src/main/java/icbm/classic/client/ICBMSounds.java}.
 */
public final class ModSounds
{
    /** 1.12 {@code ANTIMATTER("antimatter")}, SoundCategory.BLOCKS. */
    public static SoundEvent ANTIMATTER;
    /** 1.12 {@code BEAM_CHARGING("beamcharging")}, SoundCategory.BLOCKS. */
    public static SoundEvent BEAM_CHARGING;
    /** 1.12 {@code COLLAPSE("collapse")}, SoundCategory.BLOCKS, 8 files picked at random. */
    public static SoundEvent COLLAPSE;
    /** 1.12 {@code DEBILITATION("debilitation")}, SoundCategory.BLOCKS. */
    public static SoundEvent DEBILITATION;
    /** 1.12 {@code EMP("emp")}, SoundCategory.BLOCKS. */
    public static SoundEvent EMP;
    /** 1.12 {@code EXPLOSION("explosion")}, SoundCategory.BLOCKS. */
    public static SoundEvent EXPLOSION;
    /** 1.12 {@code EXPLOSION_FIRE("explosionfire")}, SoundCategory.BLOCKS. */
    public static SoundEvent EXPLOSION_FIRE;
    /** 1.12 {@code GAS_LEAK("gasleak")}, SoundCategory.BLOCKS. */
    public static SoundEvent GAS_LEAK;
    /** 1.12 {@code MACHINE_HUM("machinehum")}, SoundCategory.BLOCKS. */
    public static SoundEvent MACHINE_HUM;
    /** 1.12 {@code MISSILE_ENGINE("missileinair")}, SoundCategory.BLOCKS. */
    public static SoundEvent MISSILE_ENGINE;
    /** 1.12 {@code MISSILE_LAUNCH("missilelaunch")}, SoundCategory.BLOCKS, 2 files picked at random. */
    public static SoundEvent MISSILE_LAUNCH;
    /** 1.12 {@code POWER_DOWN("powerdown")}, SoundCategory.BLOCKS. */
    public static SoundEvent POWER_DOWN;
    /** 1.12 {@code REDMATTER("redmatter")}, SoundCategory.BLOCKS. */
    public static SoundEvent REDMATTER;
    /** 1.12 {@code SONICWAVE("sonicwave")}, SoundCategory.BLOCKS. */
    public static SoundEvent SONICWAVE;
    /** 1.12 {@code MEEP("meep", SoundCategory.NEUTRAL)}. */
    public static SoundEvent MEEP;
    /** 1.12 {@code RADIOACTIVE_TICK("radioactive_tick")}, SoundCategory.BLOCKS. */
    public static SoundEvent RADIOACTIVE_TICK;

    private ModSounds() {}

    /** Registers every sound event. Called from {@code ICBMClassic#onInitialize}. */
    public static void register()
    {
        ANTIMATTER = RegHelper.sound("antimatter");
        BEAM_CHARGING = RegHelper.sound("beam_charging");
        COLLAPSE = RegHelper.sound("collapse");
        DEBILITATION = RegHelper.sound("debilitation");
        EMP = RegHelper.sound("emp");
        EXPLOSION = RegHelper.sound("explosion");
        EXPLOSION_FIRE = RegHelper.sound("explosion_fire");
        GAS_LEAK = RegHelper.sound("gas_leak");
        MACHINE_HUM = RegHelper.sound("machine_hum");
        MISSILE_ENGINE = RegHelper.sound("missile_engine");
        MISSILE_LAUNCH = RegHelper.sound("missile_launch");
        POWER_DOWN = RegHelper.sound("power_down");
        REDMATTER = RegHelper.sound("redmatter");
        SONICWAVE = RegHelper.sound("sonic_wave");
        MEEP = RegHelper.sound("meep");
        RADIOACTIVE_TICK = RegHelper.sound("radioactive_tick");
    }
}

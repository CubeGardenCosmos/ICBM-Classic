package icbm.classic.content.blast.redmatter;

import icbm.classic.content.blast.redmatter.logic.RedmatterLogic;
import icbm.classic.content.reg.ModDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Damage source of the redmatter, kept as a thin subclass of {@link DamageSource} for the two behaviours the
 * 1.12 class added on top of the damage type.
 * <p>
 * 1.12 created its damage sources inline ({@code new DamageSource("icbm.redmatter")}) with
 * {@code setDamageBypassesArmor()} and {@code setDamageIsAbsolute()}. 26.3 resolves both from the data pack
 * entry {@code data/icbmclassic/damage_type/redmatter.json} plus the vanilla bypass tags (added in S3.02),
 * loaded through {@link ModDamageTypes}, so this class only has to carry the entity and the position. The two
 * 1.12 overrides map as follows:
 * <ul>
 *     <li>{@code getTrueSource()} → the causing entity slot of the 26.3 damage source, filled by the
 *     constructor. This is what keeps death messages, kill credit and mob anger pointing at the redmatter. It
 *     does not need an override.</li>
 *     <li>{@code getDamageLocation()} → {@link #getSourcePosition()} / {@link #sourcePositionRaw()}, which
 *     26.3 still consumes (knockback direction and damage indicator in
 *     {@code LivingEntity#dealDefaultKnockback}, the shield blocking angle and the damage event packet sent
 *     to the client). The public 26.3 constructors can carry a causing entity <b>or</b> a position, never
 *     both ({@code DamageSource(Holder, Vec3)} has no entity slot and
 *     {@code DamageSource(Holder, Entity, Entity)} has no position), so the position is answered by the
 *     overrides below. Deleting this class in favour of {@code ModDamageTypes#source(...)} would silently
 *     drop the damage direction.</li>
 * </ul>
 * The 1.12 class tolerated a null logic (both overrides answered null). The only call site passes the live
 * logic of the entity taking the damage, so a non null logic is required here.
 * <p>
 * Created by Dark(DarkGuardsman, Robin) on 5/23/2020.
 */
public class DamageSourceRedmatter extends DamageSource
{
    public final RedmatterLogic blastRedmatter;

    /**
     * 1.12: {@code DamageSourceRedmatter(RedmatterLogic blastRedmatter)}.
     *
     * @param blastRedmatter logic of the redmatter dealing the damage, hosts the entity that gets credited
     */
    public DamageSourceRedmatter(RedmatterLogic blastRedmatter)
    {
        // 1.12: super("icbm.redmatter") + setDamageBypassesArmor() + setDamageIsAbsolute(), see the class
        // javadoc for where those live in 26.3. The host is passed as the causing entity, the direct entity
        // is null as in 1.12
        super(ModDamageTypes.holder(levelOf(blastRedmatter), ModDamageTypes.REDMATTER), null, hostOf(blastRedmatter));
        this.blastRedmatter = blastRedmatter;
    }

    /**
     * 1.12 {@code getDamageLocation()}: position of the redmatter, read live so a moving redmatter reports
     * its current position like 1.12 did.
     */
    @Override
    @Nullable
    public Vec3 getSourcePosition()
    {
        return blastRedmatter != null && blastRedmatter.host != null
                ? blastRedmatter.host.position() //1.12: host.getPositionVector()
                : null;
    }

    /**
     * The damage event packet reads this getter to send the position to the client for the damage indicator,
     * see the class javadoc.
     */
    @Override
    @Nullable
    public Vec3 sourcePositionRaw()
    {
        return getSourcePosition();
    }

    private static Level levelOf(RedmatterLogic blastRedmatter)
    {
        final Entity host = hostOf(blastRedmatter);
        return host != null ? host.level() : null;
    }

    private static Entity hostOf(RedmatterLogic blastRedmatter)
    {
        return blastRedmatter != null ? blastRedmatter.host : null;
    }
}

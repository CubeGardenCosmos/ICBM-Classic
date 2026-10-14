package icbm.classic.content.potion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Reason a blast/block poisons a target, split from the actual set of vanilla effects so several gases can
 * share the "who is protected" check.
 * <p>
 * @deprecated remove at some point and apply potion effects directly (1.12 note kept).
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li>1.12 looked poisons up by name <b>and</b> by numeric id ({@code BiMap<String, Integer> poisonIDs},
 *     {@code getPoison(int)}, {@code getName(int)}, {@code getID(String)}). 26.3 has no numeric ids (ADR 0002
 *     dropped them for explosives too), nothing in the tree looked a poison up by id, so only the name map and
 *     {@link #getPoison(String)} survive. {@link #getName()} is kept for callers/logging.</li>
 *     <li>The 1.12 signatures took an emit {@code Pos} ({@code poisonEntity(Pos, EntityLivingBase, int)}).
 *     Its only consumer, {@code isEntityProtected}, had the anti-poison-armor lookup commented out and simply
 *     returned the creative-mode answer, so the unused parameter is dropped.</li>
 *     <li>{@code getAntiPoisonBlockCount(...)} is dropped: the 1.12 body was fully commented out and always
 *     returned {@code 0} (a 1.12 no-op, never called). Removing it loses no behaviour.</li>
 *     <li>The 1.12 player class creative check maps to {@link Player#isCreative()}.</li>
 * </ul>
 */
@Deprecated
public abstract class Poison
{
    private static final Map<String, Poison> poisons = new HashMap<>();

    protected String name;

    public static Poison getPoison(String name)
    {
        return poisons.get(name);
    }

    public Poison(String name)
    {
        this.name = name;
        poisons.put(name, this);
    }

    public String getName()
    {
        return this.name;
    }

    /**
     * Called to poison this specific entity with this specific type of poison.
     *
     * @param entity    to poison
     * @param amplifier level of the poison
     */
    public void poisonEntity(LivingEntity entity, int amplifier)
    {
        if (!isEntityProtected(entity, amplifier))
        {
            doPoisonEntity(entity, amplifier);
        }
    }

    public void poisonEntity(LivingEntity entity)
    {
        this.poisonEntity(entity, 0);
    }

    public boolean isEntityProtected(LivingEntity entity, int amplifier)
    {
        if (entity instanceof Player player && player.isCreative())
        {
            return true;
        }
        /* 1.12 had the anti-poison-armor walk commented out here (it collected worn armor pieces implementing
           the removed anti-poison armor interface); the port has no armor-poison system, gas protection is
           handled by the protective armor rating in the gas blasts, so this now only answers the creative case.

        return armorWorn.containsAll(this.armorRequired);
         */
        return false;
    }

    protected abstract void doPoisonEntity(LivingEntity entity, int amplifier);
}

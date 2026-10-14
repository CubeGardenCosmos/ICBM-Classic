package icbm.classic.content.blast.redmatter;

import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.data.ActionField;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.lib.actions.ActionBase;
import icbm.classic.lib.actions.status.ActionResponses;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;

/**
 * Action to spawn a redmatter entity
 * <p>
 * 26.3 port notes:
 * <ul>
 *     <li>The 1.12 constructor ({@code World, position, IActionSource, IActionData}) is kept verbatim, the
 *     ported {@link ActionBase} only exposes the same four values through its lombok generated
 *     {@code @RequiredArgsConstructor}.</li>
 *     <li>{@code ActionField<VALUE, TAG extends NBTBase>} lost its NBT tag type in 26.3, see
 *     {@link #setValue(ActionField, Object)} (same shape as {@code BlastBreach#setValue}).</li>
 *     <li>{@code Entity#setPosition} is {@code Entity#setPos} and {@code World#spawnEntity} is
 *     {@code Level#addFreshEntity}.</li>
 * </ul>
 */
public class ActionSpawnRedmatter extends ActionBase
{
    private Float size = null;

    public ActionSpawnRedmatter(Level world, Vec3 position, IActionSource source, IActionData actionData)
    {
        super(world, position, source, actionData);
    }

    /**
     * Reads the requested size and spawns the redmatter entity, 1.12 {@code doAction()}.
     *
     * @return completed when the entity entered the level, otherwise {@link ActionResponses#ENTITY_SPAWN_FAILED}
     */
    @Override
    public <VALUE> void setValue(ActionField<VALUE> key, VALUE value)
    {
        if (key == ActionFields.AREA_SIZE)
        {
            size = ActionFields.AREA_SIZE.cast(value);
        }
    }

    @NonNull
    @Override
    public IActionStatus doAction()
    {
        //Build entity
        final EntityRedmatter entityRedmatter = new EntityRedmatter(getWorld());
        entityRedmatter.setPos(getPosition().x, getPosition().y, getPosition().z); //1.12: setPosition(x, y, z)
        entityRedmatter.setBlastSize(ConfigBlast.redmatter.DEFAULT_SIZE);
        entityRedmatter.setBlastMaxSize(ConfigBlast.redmatter.MAX_SIZE);

        if (size != null)
        {
            entityRedmatter.setBlastSize(size);
        }

        //Attempt to spawn
        if (getWorld().addFreshEntity(entityRedmatter)) //1.12: world.spawnEntity(entity)
        {
            return ActionResponses.COMPLETED;
        }
        return ActionResponses.ENTITY_SPAWN_FAILED;
    }
}

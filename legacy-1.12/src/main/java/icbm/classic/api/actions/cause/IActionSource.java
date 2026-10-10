package icbm.classic.api.actions.cause;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Information about the source (starting point) of an action
 *
 * This is for tracking purposes and should never be exposed to the player. Main purpose of this data
 * should always be for mod interaction and admin tools. Player are not meant to see this data directly as it could
 * easily provide them with the location of attackers. Instead, players should have to work for figuring out
 * attacker distance and direction.
 *
 * That said ICBM team will not stop anyone from using the data. As there are valid interaction uses. Such as showing
 * missiles on an interactive map or exposing the player's own missile launch position to themselves.
 */
public interface IActionSource extends INBTSerializable<NBTTagCompound>
{
    /**
     * World source of the action
     *
     * @return world
     */
    World getWorld();

    /**
     * Source position of the action. May not align
     * with center point of the action or outcome of
     * the action.
     *
     * Example, blocks source will often be above the block. Such
     * as launchers using position above the block.
     *
     * @return position
     */
    Vec3d getPosition();


    /**
     * Get cause of the action.
     *
     * @return cause of the launch
     */
    IActionCause getCause();
}

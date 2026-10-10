package icbm.classic.api.explosion;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Version of the blast that can be restored from save
 * Created by Dark(DarkGuardsman, Robin) on 2/10/2019.
 *
 * 存档改走 ValueInput/ValueOutput，见 porting-guide §4
 *
 * @deprecated will be replaced with {@link icbm.classic.api.actions.IAction} which doesn't do save/load as
 * it is instant.
 */
public interface IBlastRestore extends IBlast
{
    void load(ValueInput in);

    void save(ValueOutput out);
}

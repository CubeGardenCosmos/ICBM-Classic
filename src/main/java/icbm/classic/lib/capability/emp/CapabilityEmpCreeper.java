package icbm.classic.lib.capability.emp;

import icbm.classic.api.actions.IAction;
import icbm.classic.api.caps.IEMPReceiver;
import icbm.classic.config.blast.ConfigBlast;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

/**
 * EMP receiver that super charges a creeper with lightning.
 * <p>
 * 1.12 wrapped this in a Forge capability ({@code ICapabilityProvider}) and created the lightning
 * with the removed {@code EntityLightningBolt} class. 26.3 has no capability wrapper: the receiver is
 * provided by the {@code emp_receiver} entity lookup (see {@link CapabilityEMP}) and the bolt is a
 * normal {@link LightningBolt} entity applied through {@link Creeper#thunderHit(ServerLevel, LightningBolt)}.
 *
 * Created by Dark(DarkGuardsman, Robin) on 3/12/2018.
 */
public class CapabilityEmpCreeper implements IEMPReceiver
{
    public final Creeper creeper;

    public CapabilityEmpCreeper(Creeper creeper)
    {
        this.creeper = creeper;
    }

    @Override
    public float applyEmpAction(Level level, double x, double y, double z, IAction emp_blast, float power, boolean doAction)
    {
        if (ConfigBlast.emp.ALLOW_LIGHTING_CREEPER && level instanceof ServerLevel serverLevel)
        {
            //Attack creeper with lighting
            final LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt != null)
            {
                bolt.setPos(x, y, z);
                // 1.12 `new EntityLightningBolt(world, x, y, z, true)` where true = effect only (no fire)
                bolt.setVisualOnly(true);
                creeper.thunderHit(serverLevel, bolt);
            }
        }
        return power;
    }
}

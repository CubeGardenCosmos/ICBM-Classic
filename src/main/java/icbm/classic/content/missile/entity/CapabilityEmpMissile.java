package icbm.classic.content.missile.entity;

import icbm.classic.api.actions.IAction;
import icbm.classic.api.caps.IEMPReceiver;
import icbm.classic.api.missiles.IMissile;
import icbm.classic.api.radio.IRadioTargetAssignable;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.content.missile.logic.flight.DeadFlightLogic;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 *
 * Created by Dark(DarkGuardsman, Robin) on 3/12/2018.
 */
public class CapabilityEmpMissile implements IEMPReceiver
{
    final IMissile missile;
    public CapabilityEmpMissile(IMissile missile)
    {
       this.missile = missile;
    }

    @Override
    public float applyEmpAction(Level world, double x, double y, double z, IAction emp_blast, float power, boolean doAction)
    {
        final Entity missileEntity = missile.getMissileEntity();
        if(ConfigBlast.emp.missiles.enabled && missileEntity != null && missileEntity.isAlive())
        {
            if (doAction)
            {
                if(world.getRandom().nextFloat() <= ConfigBlast.emp.missiles.missileKillChance) {
                    missileEntity.discard();

                    // Boom
                    if(world.getRandom().nextFloat() <= ConfigBlast.emp.missiles.engineFullBlowChance) {
                        // 1.12 world.createExplosion(entity, x, y, z, strength, false); the false (no fire) is
                        // ExplosionInteraction.NONE in 26.3
                        world.explode(missileEntity, x, y, z, ConfigBlast.emp.missiles.engineFullBlowStrength, Level.ExplosionInteraction.NONE);
                        // TODO drop fragments of missile that can still cause damage
                    }
                    //TODO else drop item or dead entity?
                }
                // Fry guidance systems
                else if(world.getRandom().nextFloat() <= ConfigBlast.emp.missiles.engineKillChance) {
                    missile.setFlightLogic(new DeadFlightLogic(0));
                }
                // Trigger firing circuits
                else if(world.getRandom().nextFloat() <= ConfigBlast.emp.missiles.missileTriggerChance) {
                    // TODO(S4.02): the explosive and actionable trigger branches belong to the content step
                    if(missileEntity instanceof IRadioTargetAssignable) {
                        // 1.12 checked EntitySurfaceToAirMissile (ported in S4.04); the SAM now implements
                        // IRadioTargetAssignable instead of being referenced by content type (S1.05/S4.04)
                        world.explode(missileEntity, x, y, z, ConfigBlast.emp.missiles.engineFullBlowStrength, Level.ExplosionInteraction.NONE);
                    }
                }
                // TODO chance for missile to fry trigger but still land
            }
        }
        return power;
    }
}

package icbm.classic.content.actions.emp;

import icbm.classic.api.actions.IActionData;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.data.ActionField;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.caps.IEMPReceiver;
import icbm.classic.api.events.EmpEvent;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.config.util.BlockReplacementData;
import icbm.classic.content.reg.ModSounds;
import icbm.classic.lib.actions.ActionBase;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.capability.emp.CapabilityEmpInventory;
import icbm.classic.lib.lookup.IcbmLookups;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runs an EMP over a cube around the action position. Applies block replacements, EMP receivers, EMP
 * receivers hidden in inventories and the EMP events.
 * <p>
 * 26.3 port notes (1.12 -> 26.3):
 * <ul>
 *     <li>{@code getWorld().isRemote} → {@code !Level#isClientSide()}; {@code world.rand} →
 *     {@code Level#getRandom()}.</li>
 *     <li>{@code ICBMSounds.EMP.play(world, x, y, z, 4.0F, pitch, true)} → {@code Level#playSound} with
 *     {@code ModSounds.EMP} and {@code SoundSource.BLOCKS}; the 1.12 helper only used its {@code distanceDelay}
 *     flag on the client, the server branch (the one running here) played a normal
 *     {@code world.playSound(null, ...)}.</li>
 *     <li>{@code BlockPos.MutableBlockPos#setPos} → {@code BlockPos.MutableBlockPos#set};
 *     {@code World#isBlockLoaded(BlockPos)} → {@code Level#isLoaded(BlockPos)};
 *     {@code World#getTileEntity(BlockPos)} → {@code Level#getBlockEntity(BlockPos)}.</li>
 *     <li>{@code EnumParticleTypes.SMOKE_LARGE} + {@code World#spawnParticle(type, x, y, z, 0, 0, 0)} (a single
 *     particle standing still) → {@code ServerLevel#sendParticles} with {@code ParticleTypes.LARGE_SMOKE} and a
 *     count of 0, matching the interpretation already used by the threaded blasts (S3.06).</li>
 *     <li>{@code tileEntity.hasCapability(CapabilityEMP.EMP, null)} + {@code getCapability} →
 *     {@code IcbmLookups.EMP_RECEIVER_BLOCK.find(level, pos, null)}; the same for entities through
 *     {@code EMP_RECEIVER_ENTITY}. Items are reached through {@code EMP_RECEIVER_ITEM} inside
 *     {@link CapabilityEmpInventory}.</li>
 *     <li>{@code tileEntity/entity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)} → the
 *     vanilla {@link Container} the ported 26.3 wrapper type checks (the 1.12 Forge item handler capability no
 *     longer exists).</li>
 *     <li>{@code MinecraftForge.EVENT_BUS.post(new EmpEvent...)} → the Fabric invokers on the ported
 *     {@link EmpEvent} ({@code BlockPre/BlockPost/EntityPre/EntityPost}); the 1.12 {@code post} returned true
 *     when canceled while the Fabric callbacks return false to cancel, so the pre checks are inverted.</li>
 *     <li>{@code World#getEntitiesWithinAABB(Entity.class, AxisAlignedBB)} →
 *     {@code Level#getEntitiesOfClass(Entity.class, AABB)}; {@code entity.posX/posY/posZ} →
 *     {@code Entity#getX/getY/getZ}.</li>
 *     <li>{@code EnergySystem}/{@code IEnergySystem} (the 1.12 Forge energy reflection helper) was replaced by
 *     Team Reborn Energy (ADR 0003); the external drain is S6.05 and only marked below.</li>
 * </ul>
 * 1.12 source: {@code legacy-1.12/src/main/java/icbm/classic/content/actions/emp/ActionEmpArea.java}.
 *
 * Created by Dark(DarkGuardsman, Robin) on 3/12/2018.
 */
@Getter
@Setter
public class ActionEmpArea extends ActionBase {

    //1.12 held a raw List<ActionField>, 26.3 typed the field so the wildcard is needed here
    static final List<ActionField<?>> SUPPORTED_FIELDS = new ArrayList<>(Collections.singleton(ActionFields.AREA_SIZE));

    @Accessors(chain = true)
    private int size = 1;

    public ActionEmpArea(Level world, Vec3 vec3d, IActionSource source, IActionData actionData) {
        super(world, vec3d, source, actionData);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <VALUE> VALUE getValue(ActionField<VALUE> key) {
        if (key == ActionFields.AREA_SIZE) {
            return (VALUE) ActionFields.AREA_SIZE.cast((float) size);
        }
        return null;
    }

    @Override
    public List<ActionField<?>> getFields() {
        return SUPPORTED_FIELDS;
    }

    @Override
    public boolean hasField(ActionField<?> key) {
        return key == ActionFields.AREA_SIZE;
    }

    @NonNull
    @Override
    public IActionStatus doAction() {
        if (!getWorld().isClientSide()) {
            this.empTiles();
            this.empEntities();

            //TODO VEProviderShockWave.spawnEffect(world(), position.x(), position.y(), position.z(), 0, 0, 0, 0, 0, 255, 1, 3);
            //TODO VEProviderShockWave.spawnEffect(world(), position.x(), position.y(), position.z(), 0, 0, 0, 0, 0, 255, 3, 3);
            //TODO VEProviderShockWave.spawnEffect(world(), position.x(), position.y(), position.z(), 0, 0, 0, 0, 0, 255, 5, 3);
            //1.12: ICBMSounds.EMP.play(getWorld(), x, y, z, 4.0F, pitch, true), the server branch of the helper
            //played through Level#playSound with SoundCategory.BLOCKS (no distance delay on the server)
            getWorld().playSound(null, getPosition().x, getPosition().y, getPosition().z, ModSounds.EMP, SoundSource.BLOCKS, 4.0F, (1.0F + (getWorld().getRandom().nextFloat() - getWorld().getRandom().nextFloat()) * 0.2F) * 0.7F);
        }
        return ActionResponses.COMPLETED;
    }

    protected void empTiles() {
        if (!ConfigBlast.emp.ALLOW_TILES) {
            return;
        }
        final BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos();
        //Loop through cube to effect blocks TODO replace with ray trace system
        for (int x = -this.size; x < this.size; x++) {
            for (int y = -this.size; y < this.size; y++) {
                for (int z = -this.size; z < this.size; z++) {
                    currentPos.set(this.getBlockPos().getX() + x, this.getBlockPos().getY() + y, this.getBlockPos().getZ() + z);

                    //Apply action on block if loaded
                    if (getWorld().isLoaded(currentPos)) {
                        this.empTile(currentPos);
                    }
                }
            }
        }
    }

    protected void empTile(BlockPos blockPos) {
        //Generate some effects
        if (blockPos.getY() == this.getBlockPos().getY() && getWorld() instanceof ServerLevel serverLevel) {
            //1.12: world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z, 0, 0, 0), a particle standing
            //still at the position (count 0 keeps the position exact and the speed 0)
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 0,
                1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        }

        //1.12 read the state for the event and again below for the replacement lookup
        final BlockState blockStateForEvent = getWorld().getBlockState(blockPos);
        float powerEntity = 1f;

        //Fire event to allow canceling action on entity
        //1.12: !MinecraftForge.EVENT_BUS.post(new EmpEvent.BlockPre(...)), post returned true when canceled
        if (EmpEvent.BlockPre.EVENT.invoker().onBlockPre(new EmpEvent.BlockPre(this, getWorld(), blockPos, blockStateForEvent))) {

            // TODO allow scaling replacement by emp energy key=value@nbt({"energy":@energy(min, max)}) with @energy being a replacement target
            // TODO allow gating replacement by emp energy @if(energy>30, replacement)
            final BlockState blockState = getWorld().getBlockState(blockPos);
            final BlockReplacementData replacement = EmpHandler.empBlockSwaps.getValue(blockState);
            if(replacement != null) {
                replacement.apply(getWorld(), blockPos);
                return;
            }

            // TODO Oct 15, 2024 - run action handler per block
            //                     add mod compatability for common mods using NBT editing and reflection

            final BlockEntity blockEntity = getWorld().getBlockEntity(blockPos);
            if (blockEntity != null) {
                boolean doInventory = true;
                //1.12: tileEntity.hasCapability(CapabilityEMP.EMP, null) + tileEntity.getCapability(...)
                final IEMPReceiver receiver = IcbmLookups.EMP_RECEIVER_BLOCK.find(getWorld(), blockPos, null);
                if (receiver != null) {
                    powerEntity = empEntity(blockEntity, powerEntity, receiver);
                    doInventory = receiver.shouldEmpSubObjects(getWorld(), blockEntity.getBlockPos().getX(), blockEntity.getBlockPos().getY(), blockEntity.getBlockPos().getZ());
                } else if (ConfigBlast.emp.DRAIN_ENERGY_TILES) {
                    // S6.05: drain external EnergyStorage
                    // 1.12 drained the tile's Forge energy storage here through lib/energy/system/EnergySystem
                    // when the storage allowed a direct set (otherwise the TODO below). ICBM machines drain
                    // through their own IEMPReceiver above and empty IcbmEnergyBuffer in S5. External mod
                    // storage is drained in S6.05 (ADR 0003 / energy-fe.md §5.7).
                    //TODO Spawn tick based effect to drain as much energy as possible over several ticks
                }

                //1.12: tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
                if (doInventory && blockEntity instanceof Container) {
                    powerEntity = empEntity(blockEntity, powerEntity, new CapabilityEmpInventory.TileInv(blockEntity));
                }
            }

        }

        //Fire post event to allow hooking EMP action
        EmpEvent.BlockPost.EVENT.invoker().onBlockPost(new EmpEvent.BlockPost(this, getWorld(), blockPos, blockStateForEvent));
    }

    protected void empEntities() {
        if (!ConfigBlast.emp.ALLOW_ENTITY) {
            return;
        }
        //Calculate bounds
        //1.12 read the lazily initialised ActionBase#pos field (Lombok getPos()), which empTiles() filled by
        //   now; 26.3 uses getBlockPos() which is the same lazily computed value
        final AABB bounds = new AABB(
            getBlockPos().getX() - this.size, getBlockPos().getY() - this.size, getBlockPos().getZ() - this.size,
            getBlockPos().getX() + this.size, getBlockPos().getY() + this.size, getBlockPos().getZ() + this.size);

        //Get entities in bounds
        //1.12: getWorld().getEntitiesWithinAABB(Entity.class, bounds)
        final List<Entity> entities = getWorld().getEntitiesOfClass(Entity.class, bounds);

        //Loop entities to apply effects
        for (Entity entity : entities) {
            float powerEntity = 1f;
            //Fire event to allow canceling action on entity
            //1.12: !MinecraftForge.EVENT_BUS.post(new EmpEvent.EntityPre(...)), post returned true when canceled
            if (EmpEvent.EntityPre.EVENT.invoker().onEntityPre(new EmpEvent.EntityPre(this, entity))) {
                boolean doInventory = true;
                //1.12: entity.hasCapability(CapabilityEMP.EMP, null) + entity.getCapability(...)
                final IEMPReceiver receiver = IcbmLookups.EMP_RECEIVER_ENTITY.find(entity, null);
                if (receiver != null) {
                    powerEntity = empEntity(entity, powerEntity, receiver);
                    doInventory = receiver.shouldEmpSubObjects(getWorld(), entity.getX(), entity.getY(), entity.getZ());
                } else if (ConfigBlast.emp.DRAIN_ENERGY_ENTITY) {
                    // S6.05: drain external EnergyStorage
                    //TODO Spawn tick based effect to drain as much energy as possible over several ticks
                }

                //1.12: entity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
                if (doInventory && entity instanceof Container) {
                    powerEntity = empEntity(entity, powerEntity, new CapabilityEmpInventory.EntityInv(entity));
                }

                //Fire post event to allow hooking EMP action
                EmpEvent.EntityPost.EVENT.invoker().onEntityPost(new EmpEvent.EntityPost(this, entity));
            }
        }
    }

    protected float empEntity(Entity entity, float powerEntity, IEMPReceiver receiver) {
        if (receiver != null) {
            powerEntity = receiver.applyEmpAction(getWorld(), entity.getX(), entity.getY(), entity.getZ(), this, powerEntity, true);
            //TODO spawn effects on entity if items were effected
            //TODO ICBMClassic.proxy.spawnShock(this.oldWorld(), this.position, new Pos(entity), 20);
        }
        return powerEntity;
    }

    protected float empEntity(BlockEntity entity, float powerEntity, IEMPReceiver receiver) {
        if (receiver != null) {
            powerEntity = receiver.applyEmpAction(getWorld(), entity.getBlockPos().getX(), entity.getBlockPos().getY(), entity.getBlockPos().getZ(), this, powerEntity, true);
            //TODO spawn effects on entity if items were effected
            //TODO ICBMClassic.proxy.spawnShock(this.oldWorld(), this.position, new Pos(entity), 20);
        }
        return powerEntity;
    }
}

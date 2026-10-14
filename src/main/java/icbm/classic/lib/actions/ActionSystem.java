package icbm.classic.lib.actions;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IPotentialAction;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.conditions.ICondition;
import icbm.classic.api.actions.data.ActionField;
import icbm.classic.api.actions.data.ActionFields;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.reg.events.*;
import icbm.classic.content.actions.causeby.CauseByTimer;
import icbm.classic.content.actions.emp.ActionDataEmpArea;
import icbm.classic.content.actions.entity.ActionSpawnEntity;
import icbm.classic.content.actions.conditionals.ConditionAnd;
import icbm.classic.content.actions.conditionals.ConditionOR;
import icbm.classic.content.actions.conditionals.ConditionTargetDistance;
import icbm.classic.content.actions.conditionals.ConditionalImpact;
import icbm.classic.content.missile.logic.source.cause.CausedByBlock;
import icbm.classic.content.missile.logic.source.cause.EntityCause;
import icbm.classic.content.missile.logic.source.cause.RedstoneCause;
import icbm.classic.lib.actions.conditionals.timer.TimerCondition;
import icbm.classic.lib.actions.conditionals.timer.TimerTickingStatus;
import icbm.classic.lib.actions.listners.ActionListenerHandler;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.actions.status.MissingFieldStatus;
import icbm.classic.lib.buildable.BuildableObjectRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class ActionSystem {
    public static final Identifier ACTION_ENTITY_SPAWN = ICBMConstants.id("entity.spawn");

    public static void setup()
    {
        ICBMClassicAPI.ACTION_LISTENER = new ActionListenerHandler();
        setupActionFields();
        setupConditionalRegistry();
        setupCauseRegistry();
        setupStatusRegistry();
        setupActionRegistry();
        setupActionPotentialRegistry();
        // TODO(S1.07/S1.10): 1.12 末尾调用 ActionProvider.register() 注册 Forge capability。
        //  26.3 无 capability 机制，改由 API Lookup / 数据附件挂接（S1.07/S1.10）后再接入。
    }

    private static void setupActionFields() {
        ActionFields.AREA_SIZE = ActionField.getOrCreate("area.size", Float.class, (out, key, v) -> out.putFloat(key, v), (in, key) -> in.getFloatOr(key, 0F));
        ActionFields.HOST_ENTITY = ActionField.getOrCreate("host.entity", Entity.class, null, null);
        ActionFields.HOST_POSITION = ActionField.getOrCreate("host.vec3d", Vec3.class, (out, key, v) -> out.store(key, Vec3.CODEC, v), (in, key) -> in.read(key, Vec3.CODEC).orElse(null));
        ActionFields.HOST_DIRECTION = ActionField.getOrCreate("host.direction", Direction.class, (out, key, v) -> out.store(key, Direction.CODEC, v), (in, key) -> in.read(key, Direction.CODEC).orElse(null));
        ActionFields.IMPACTED = ActionField.getOrCreate("impacted", Boolean.class, (out, key, v) -> out.putBoolean(key, v), (in, key) -> in.getBooleanOr(key, false));
        ActionFields.TARGET_POSITION = ActionField.getOrCreate("target.vec3d", Vec3.class, (out, key, v) -> out.store(key, Vec3.CODEC, v), (in, key) -> in.read(key, Vec3.CODEC).orElse(null));
        ActionFields.MOTION_VECTOR = ActionField.getOrCreate("motion.vec3d", Vec3.class, (out, key, v) -> out.store(key, Vec3.CODEC, v), (in, key) -> in.read(key, Vec3.CODEC).orElse(null));
        ActionFields.YAW = ActionField.getOrCreate("yaw", Float.class, (out, key, v) -> out.putFloat(key, v), (in, key) -> in.getFloatOr(key, 0F));
        ActionFields.PITCH = ActionField.getOrCreate("pitch", Float.class, (out, key, v) -> out.putFloat(key, v), (in, key) -> in.getFloatOr(key, 0F));
        ActionFields.ENTITY_REG_NAME = ActionField.getOrCreate("entity.regName", Identifier.class, (out, key, v) -> out.store(key, Identifier.CODEC, v), (in, key) -> in.read(key, Identifier.CODEC).orElse(null));
        ActionFields.ENTITY_DATA = ActionField.getOrCreate("entity.data", CompoundTag.class, (out, key, v) -> out.store(key, CompoundTag.CODEC, v), (in, key) -> in.read(key, CompoundTag.CODEC).orElse(null));
    }

    private static void setupConditionalRegistry() {
        final BuildableObjectRegistry<ICondition> registry = new BuildableObjectRegistry<>("CONDITIONALS", "conditionals");
        ICBMClassicAPI.CONDITION_REGISTRY = registry;

        registry.register(TimerCondition.REG_NAME, TimerCondition::new);
        registry.register(ConditionalImpact.REG_NAME, ConditionalImpact::new);
        registry.register(ConditionTargetDistance.REG_NAME, ConditionTargetDistance::new);
        registry.register(ConditionAnd.REG_NAME, ConditionAnd::new);
        registry.register(ConditionOR.REG_NAME, ConditionOR::new);

        //Fire registry event
        ConditionalRegistryEvent.EVENT.invoker().onRegisterConditions(registry);

        //Lock to prevent late registry
        registry.lock();
    }

    private static void setupStatusRegistry() {
        final BuildableObjectRegistry<IActionStatus> registry = new BuildableObjectRegistry<>("ACTION_STATUS", "action.status");
        ICBMClassicAPI.ACTION_STATUS_REGISTRY = registry;

        // Register defaults
        // TODO(S5): 注册 content/blocks/launcher/status/LauncherStatus
        ActionResponses.registerTypes();
        registry.register(TimerTickingStatus.REG_NAME, TimerTickingStatus::new);
        registry.register(MissingFieldStatus.REG_NAME, MissingFieldStatus::new);

        //Fire registry event
        ActionStatusRegistryEvent.EVENT.invoker().onRegisterActionStatuses(registry);

        //Lock to prevent late registry
        registry.lock();
    }

    private static void setupActionRegistry() {
        final BuildableObjectRegistry<icbm.classic.api.actions.IActionData> registry = new BuildableObjectRegistry<>("ACTION_DATA", "action.data");
        ICBMClassicAPI.ACTION_REGISTRY = registry;

        // Register defaults
        // 1.12: new ActionDataEmpArea().register()
        registry.register(ActionDataEmpArea.REG_NAME, ActionDataEmpArea::new);
        // 1.12: ICBMClassicAPI.ACTION_REGISTRY.register(ACTION_ENTITY_SPAWN, () -> new ActionDataGeneric(...))
        registry.register(ACTION_ENTITY_SPAWN,
                () -> new ActionDataGeneric(ACTION_ENTITY_SPAWN,
                        (w, x, y, z, s, d) -> new ActionSpawnEntity(w, new Vec3(x, y, z), s, d),
                        ActionSpawnEntity.FIELDS
                ));

        //Fire registry event
        ActionRegistryEvent.EVENT.invoker().onRegisterActions(registry);

        //Lock to prevent late registry
        registry.lock();
    }

    private static void setupCauseRegistry()
    {
        final BuildableObjectRegistry<IActionCause> registry = new BuildableObjectRegistry<>("ACTION_CAUSE", "action.cause");
        ICBMClassicAPI.ACTION_CAUSE_REGISTRY = registry;

        // Register defaults
        // TODO(S5): 注册 content/blocks/launcher/screen/BlockScreenCause
        registry.register(CauseByTimer.REG_NAME, CauseByTimer::new);
        // S4.01: missile causes (1.12 registered these in ActionSystem as well)
        registry.register(EntityCause.REG_NAME, EntityCause::new);
        registry.register(CausedByBlock.REG_NAME, CausedByBlock::new);
        registry.register(RedstoneCause.REG_NAME, RedstoneCause::new);

        //Fire registry event
        ActionCauseRegistryEvent.EVENT.invoker().onRegisterActionCauses(registry);

        //Lock to prevent late registry
        registry.lock();
    }

    private static void setupActionPotentialRegistry()
    {
        final BuildableObjectRegistry<IPotentialAction> registry = new BuildableObjectRegistry<>("ACTION_POTENTIAL", "action.potential");
        ICBMClassicAPI.ACTION_POTENTIAL_REGISTRY = registry;

        // Register defaults
        registry.register(PotentialAction.REG_NAME, PotentialAction::new);

        //Fire registry event
        ActionPotentialRegistryEvent.EVENT.invoker().onRegisterActionPotentials(registry);

        //Lock to prevent late registry
        registry.lock();
    }
}

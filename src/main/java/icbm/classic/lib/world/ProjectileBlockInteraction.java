package icbm.classic.lib.world;

import icbm.classic.ICBMClassic;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * For internal use or direct mod addons, use events for external interaction
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public final class ProjectileBlockInteraction {

    private static final HashMap<BlockState, IProjectileBlockInteraction> stateToInteraction = new HashMap<>();
    private static final HashMap<Block, IProjectileBlockInteraction> blockToInteraction = new HashMap<>();
    // 1.12 的 Material 在 26.3 已被删除（见移植指南 §2.1），改用方块标签表
    private static final Map<TagKey<Block>, IProjectileBlockInteraction> tagToInteraction = new HashMap<>();

    // 1.12 的 TODO“once on 1.19 use TAG system”由本次移植完成：Material 表已换成标签表

    public static void addBlockStateInteraction(BlockState state, IProjectileBlockInteraction function) {
        if (stateToInteraction.containsKey(state)) {
            ICBMClassic.logger().warn("interaction already exists for " + state + " replacing", new RuntimeException());
        }
        stateToInteraction.put(state, function);
    }

    public static void addBlockInteraction(Block block, IProjectileBlockInteraction function) {
        if (blockToInteraction.containsKey(block)) {
            ICBMClassic.logger().warn("interaction already exists for " + block + " replacing", new RuntimeException());
        }
        blockToInteraction.put(block, function);
    }

    public static void addTagInteraction(TagKey<Block> tag, IProjectileBlockInteraction function) {
        if (tagToInteraction.containsKey(tag)) {
            ICBMClassic.logger().warn("interaction already exists for " + tag + " replacing", new RuntimeException());
        }
        tagToInteraction.put(tag, function);
    }

    public static void addCollisionInteraction(Block block) {
        addBlockInteraction(block, (world, pos, hit, side, state, entity) -> IProjectileBlockInteraction.EnumHitReactions.CONTINUE_NO_IMPACT);
    }

    public static void breakBlockInteraction(Block block) {
        addBlockInteraction(block, (world, pos, hit, side, state, entity) -> {
            if (breakBlock(world, pos, state, entity)) {
                return IProjectileBlockInteraction.EnumHitReactions.CONTINUE_NO_IMPACT;
            }
            return IProjectileBlockInteraction.EnumHitReactions.CONTINUE;
        });
    }

    public static void breakTagInteraction(TagKey<Block> tag) {
        addTagInteraction(tag, (world, pos, hit, side, state, entity) -> {
            if (breakBlock(world, pos, state, entity)) {
                return IProjectileBlockInteraction.EnumHitReactions.CONTINUE_NO_IMPACT;
            }
            return IProjectileBlockInteraction.EnumHitReactions.CONTINUE;
        });
    }

    private static boolean breakBlock(Level world, BlockPos pos, BlockState state, Entity entity) {
        // 1.12 的 Forge 专用守卫 block.canEntityDestroy(state, world, pos, entity) 已删除，其默认实现恒为 true
        Block.dropResources(state, world, pos);
        world.removeBlock(pos, false);
        return true;
    }

    public static IProjectileBlockInteraction.EnumHitReactions handleSpecialInteraction(Level world, BlockPos pos, Vec3 hit, Direction side, BlockState state, Entity entity) {

        IProjectileBlockInteraction func = stateToInteraction.get(state);
        if (func != null) {
            final IProjectileBlockInteraction.EnumHitReactions result = func.apply(world, pos, hit, side, state, entity);
            if (result != null && result != IProjectileBlockInteraction.EnumHitReactions.PASS) {
                return result;
            }
        }
        func = blockToInteraction.get(state.getBlock());
        if (func != null) {
            final IProjectileBlockInteraction.EnumHitReactions result = func.apply(world, pos, hit, side, state, entity);
            if (result != null && result != IProjectileBlockInteraction.EnumHitReactions.PASS) {
                return result;
            }
        }
        // 1.12 materialToInteraction.get(state.getMaterial()) → 26.3 按标签逐个匹配 state.is(tag)
        for (Map.Entry<TagKey<Block>, IProjectileBlockInteraction> entry : tagToInteraction.entrySet()) {
            if (state.is(entry.getKey())) {
                final IProjectileBlockInteraction.EnumHitReactions result = entry.getValue().apply(world, pos, hit, side, state, entity);
                if (result != null && result != IProjectileBlockInteraction.EnumHitReactions.PASS) {
                    return result;
                }
            }
        }
        return IProjectileBlockInteraction.EnumHitReactions.CONTINUE;
    }

    public static void register() {

        // Portal handling
        addCollisionInteraction(Blocks.NETHER_PORTAL); // 1.12 Blocks.PORTAL → 26.3 Blocks.NETHER_PORTAL
        addCollisionInteraction(Blocks.END_PORTAL);
        addBlockInteraction(Blocks.END_GATEWAY, (world, pos, hit, side, state, entity) -> {
            // 1.12 取 TileEntityEndGateway 并调用 teleportEntity(entity)；26.3 无该 API，
            // EndGatewayBlock#entityInside 内部自己完成 setAsInsidePortal 与方块实体检查，这里直接委托方块状态回调
            state.entityInside(world, pos, entity, InsideBlockEffectApplier.NOOP, false);
            return IProjectileBlockInteraction.EnumHitReactions.TELEPORTED;
        });

        // Break as pass through handling
        breakTagInteraction(BlockTags.LEAVES); // 1.12 Material.LEAVES → 26.3 BlockTags.LEAVES
        breakTagInteraction(BlockTags.SNOW); // 1.12 Material.SNOW → 26.3 BlockTags.SNOW
        breakTagInteraction(BlockTags.IMPERMEABLE); // 1.12 Material.GLASS → 26.3 BlockTags.IMPERMEABLE
    }
}

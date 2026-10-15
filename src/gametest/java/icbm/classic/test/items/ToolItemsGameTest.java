package icbm.classic.test.items;

import icbm.classic.api.data.IBoundBox;
import icbm.classic.api.radio.IRadioMessage;
import icbm.classic.api.radio.IRadioReceiver;
import icbm.classic.api.radio.IRadioSender;
import icbm.classic.api.refs.ICBMExplosives;
import icbm.classic.content.entity.EntityExplosive;
import icbm.classic.content.items.ItemRadarGun;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.content.reg.ModItems;
import icbm.classic.lib.data.GpsData;
import icbm.classic.lib.radio.RadioRegistry;
import icbm.classic.lib.radio.messages.TriggerActionMessage;
import icbm.classic.lib.transform.vector.Pos;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** S5.08 regression coverage for GPS targeting, radio triggers, and defusing primed explosives. */
public class ToolItemsGameTest
{
    private static final class Receiver implements IRadioReceiver
    {
        private final Level level;
        private final BlockPos pos;
        private int triggerCount;

        private Receiver(Level level, BlockPos pos)
        {
            this.level = level;
            this.pos = pos;
        }

        @Override
        public void onMessage(IRadioSender sender, IRadioMessage message)
        {
            if (message instanceof TriggerActionMessage && Objects.equals("t1", message.getChannel()))
            {
                triggerCount++;
            }
        }

        @Override public BlockPos getBlockPos() { return pos; }
        @Override public Level getWorld() { return level; }
        @Override public IBoundBox<BlockPos> getRange() { return RadioRegistry.INFINITE; }
    }

    @GameTest
    public void radarGunStoresClickedCoordinate(GameTestHelper helper)
    {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos target = helper.absolutePos(new BlockPos(2, 1, 2));
        ItemStack stack = new ItemStack(ModItems.RADAR_GUN);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 hit = Vec3.atCenterOf(target);
        var result = ((ItemRadarGun) ModItems.RADAR_GUN).useOn(
                new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(hit, Direction.UP, target, false)));
        helper.assertTrue(result.consumesAction(), "radar gun block use should be consumed");
        GpsData gps = stack.get(ModDataComponents.GPS_DATA);
        helper.assertTrue(gps != null && gps.pos().equals(java.util.Optional.of(GlobalPos.of(helper.getLevel().dimension(), target))),
                "radar gun stores clicked block coordinate in gps_data");
        helper.succeed();
    }

    @GameTest
    public void remoteDetonatorTriggersSameChannel(GameTestHelper helper)
    {
        Level level = helper.getLevel();
        RadioRegistry.clear(level.dimension());
        Receiver receiver = new Receiver(level, helper.absolutePos(new BlockPos(1, 1, 1)));
        helper.assertTrue(RadioRegistry.add(receiver), "test receiver registers");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack stack = new ItemStack(ModItems.REMOTE_DETONATOR);
        stack.set(ModDataComponents.RADIO_CHANNEL, "t1");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND);
        helper.assertTrue(receiver.triggerCount == 1, "same-channel receiver receives one trigger action");
        RadioRegistry.clear(level.dimension());
        helper.succeed();
    }

    @GameTest
    public void defuserDropsExplosiveAndRemovesPrimedEntity(GameTestHelper helper)
    {
        Level level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var explosiveId = ICBMExplosives.CONDENSED.getRegistryKey();
        Block block = ModBlocks.EXPLOSIVE_BLOCKS.get(explosiveId);
        ItemStack blockStack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block)));
        EntityExplosive entity = new EntityExplosive(level, new Pos(pos.getX(), pos.getY(), pos.getZ()), Direction.UP,
                explosiveId, blockStack);
        level.addFreshEntity(entity);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack defuser = new ItemStack(ModItems.DEFUSER);
        player.setItemInHand(InteractionHand.MAIN_HAND, defuser);
        var result = AttackEntityCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND,
                entity, new net.minecraft.world.phys.EntityHitResult(entity));
        helper.assertTrue(result.consumesAction(), "attacking with the defuser consumes the entity interaction");
        helper.assertTrue(entity.isRemoved(), "primed explosive entity removed");
        helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, entity.getBoundingBox().inflate(1)).stream()
                .anyMatch(drop -> ItemStack.isSameItem(drop.getItem(), blockStack)),
                "defusing drops the explosive block stack, as the 1.12 entity capability did");
        helper.succeed();
    }
}

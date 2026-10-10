package icbm.classic.lib.energy;

import icbm.classic.config.ConfigMain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link IcbmEnergyBuffer} 的 JUnit 规则测试（energy-fe.md §4）。
 * 纯 Java，不引导 Minecraft 注册表；只直接读写静态 {@link ConfigMain#REQUIRES_POWER}。
 */
class IcbmEnergyBufferTest {

    @AfterEach
    void restoreRequiresPower() {
        ConfigMain.REQUIRES_POWER = true;
    }

    @Test
    @DisplayName("receive 受 maxReceive 与容量共同夹取，simulate 不写入")
    void receiveClampsToMaxReceiveAndCapacity() {
        final IcbmEnergyBuffer buffer = new IcbmEnergyBuffer(() -> 100L, () -> 30L);

        // simulate 只报可用量，不改缓冲
        assertEquals(30L, buffer.receive(50L, true), "simulate 应受 maxReceive 夹取");
        assertEquals(0L, buffer.getStored(), "simulate 不得写入");

        // 实际接收 30
        assertEquals(30L, buffer.receive(50L, false));
        assertEquals(30L, buffer.getStored());

        // 再接收：受 maxReceive=30 与剩余空间=70 共同限制，仍为 30
        assertEquals(30L, buffer.receive(100L, false), "实际量受 maxReceive 与剩余空间共同限制");
        assertEquals(60L, buffer.getStored());

        // 填满：最后一步由容量夹取
        assertEquals(30L, buffer.receive(50L, false));    // stored 90
        assertEquals(10L, buffer.receive(50L, false), "剩余空间 10 应夹住实际接收量");
        assertEquals(100L, buffer.getStored());
        assertEquals(0L, buffer.receive(50L, false), "满容量接收返回 0");
    }

    @Test
    @DisplayName("consume 不足返回 false 且不扣，足够则扣减")
    void consumeRequiresEnoughEnergy() {
        final IcbmEnergyBuffer buffer = new IcbmEnergyBuffer(() -> 100L, () -> 100L);
        buffer.receive(10L, false);
        assertEquals(10L, buffer.getStored());

        assertFalse(buffer.consume(11L), "能量不足必须失败");
        assertEquals(10L, buffer.getStored(), "失败不得扣除");

        assertTrue(buffer.consume(10L), "能量足够必须成功");
        assertEquals(0L, buffer.getStored());
    }

    @Test
    @DisplayName("REQUIRES_POWER=false 时视为满电、不消耗且不改内部值")
    void ignoresPowerReportsFullAndConsumesFree() {
        final IcbmEnergyBuffer buffer = new IcbmEnergyBuffer(() -> 100L, () -> 100L);
        buffer.receive(40L, false);
        assertEquals(40L, buffer.getStored());

        ConfigMain.REQUIRES_POWER = false;
        assertEquals(buffer.getCapacity(), buffer.getStored(), "忽略电力时应报告满电");
        assertTrue(buffer.consume(1_000_000L), "忽略电力时消耗总是成功");
        assertTrue(buffer.has(1_000_000L), "忽略电力时 has 总是 true");
        assertEquals(buffer.getCapacity(), buffer.getStored(), "忽略电力时报告值仍为容量");

        // 恢复需要电力后，内部 stored 必须仍是 40（consume 未改内部值）
        ConfigMain.REQUIRES_POWER = true;
        assertEquals(40L, buffer.getStored(), "忽略电力时不得改动内部 stored");
    }

    @Test
    @DisplayName("容量变小时 getStored 不超容量，setStored 夹到 [0, capacity]")
    void capacityShrinkClampsStored() {
        final long[] capacity = {100L};
        final IcbmEnergyBuffer buffer = new IcbmEnergyBuffer(() -> capacity[0], () -> 100L);
        buffer.receive(100L, false);
        assertEquals(100L, buffer.getStored());

        capacity[0] = 50L;
        assertEquals(50L, buffer.getStored(), "getStored 返回 min(stored, capacity)");
        assertTrue(buffer.getStored() <= 50L);

        buffer.setStored(999L);
        assertEquals(50L, buffer.getStored(), "setStored 夹到容量");

        buffer.setStored(-5L);
        assertEquals(0L, buffer.getStored(), "setStored 夹到 0");
    }

    @Test
    @DisplayName("onChange 在成功 receive/consume/setStored 各触发一次，simulate 不触发")
    void onChangeFiresOnlyOnMutation() {
        final AtomicInteger changes = new AtomicInteger();
        final IcbmEnergyBuffer buffer = new IcbmEnergyBuffer(() -> 100L, () -> 30L)
                .onChange(changes::incrementAndGet);

        buffer.receive(10L, true);
        assertEquals(0, changes.get(), "simulate 不得触发 onChange");

        buffer.receive(10L, false);
        assertEquals(1, changes.get(), "成功 receive 触发一次");

        buffer.consume(999L);
        assertEquals(1, changes.get(), "失败的 consume 不触发 onChange");

        buffer.consume(5L);
        assertEquals(2, changes.get(), "成功 consume 触发一次");

        buffer.setStored(7L);
        assertEquals(3, changes.get(), "setStored 触发一次");
    }

    @Test
    @DisplayName("consumeOrFree 忽略电力时成功且不扣，需要电力时走缓冲")
    void consumeOrFreeHonorsGlobalSwitch() {
        final IcbmEnergyBuffer buffer = new IcbmEnergyBuffer(() -> 100L, () -> 100L);
        buffer.receive(10L, false);

        assertFalse(EnergyRules.consumeOrFree(buffer, 11L), "需要电力且不足应失败");
        assertTrue(EnergyRules.consumeOrFree(buffer, 10L), "需要电力且足够应成功");
        assertEquals(0L, buffer.getStored());

        ConfigMain.REQUIRES_POWER = false;
        assertTrue(EnergyRules.consumeOrFree(buffer, 1_000_000L), "忽略电力时总是成功且不扣");
        assertEquals(buffer.getCapacity(), buffer.getStored(), "忽略电力时报告满电");

        // 内部 stored 必须仍是 0
        ConfigMain.REQUIRES_POWER = true;
        assertEquals(0L, buffer.getStored(), "忽略电力时不得改动内部 stored");
    }
}

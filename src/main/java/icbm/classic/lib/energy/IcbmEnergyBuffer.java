package icbm.classic.lib.energy;

import java.util.function.LongSupplier;

/** 机器内部能量缓冲。单位 FE。主线程使用，非线程安全。 */
public final class IcbmEnergyBuffer {
    private final LongSupplier capacity;     // 读配置，允许运行时变化
    private final LongSupplier maxReceive;   // 每次 receive 的上限（每 tick 由调用方保证只调一次或累计）
    private long stored;
    private Runnable onChange = () -> {};

    public IcbmEnergyBuffer(LongSupplier capacity, LongSupplier maxReceive) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
    }

    public IcbmEnergyBuffer onChange(Runnable r) {
        this.onChange = r;
        return this;
    }

    public long getStored() {
        return EnergyRules.ignoresPower() ? getCapacity() : Math.min(stored, getCapacity());
    }

    public long getCapacity() {
        return capacity.getAsLong();
    }

    /** 尝试接收，返回实际接收量；simulate=true 不修改，也不触发 {@link #onChange}。负数按 0。 */
    public long receive(long amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        final long space = getCapacity() - stored;
        final long accepted = Math.min(Math.min(amount, maxReceive.getAsLong()), space);
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            stored += accepted;
            onChange.run();
        }
        return accepted;
    }

    /** 有足够能量则扣除并返回 true；REQUIRES_POWER=false 时总返回 true 且不扣。 */
    public boolean consume(long amount) {
        if (EnergyRules.ignoresPower()) {
            return true;
        }
        if (stored >= amount) {
            stored -= amount;
            onChange.run();
            return true;
        }
        return false;
    }

    /** 只检查不扣除。 */
    public boolean has(long amount) {
        return EnergyRules.ignoresPower() || stored >= amount;
    }

    /** 直接设置（存档加载、EMP 清零用），夹到 [0, capacity]。 */
    public void setStored(long value) {
        stored = Math.min(Math.max(0L, value), getCapacity());
        onChange.run();
    }
}

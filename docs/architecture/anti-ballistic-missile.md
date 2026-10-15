# 反弹道导弹系统（ABM）

依据 [ADR 0004](../decisions/0004-anti-ballistic-missile-system.md)。实施阶段 S7（在 FE 之后、i18n 之前）。S4 先把 1.12 SAM **原样**移植（ID 已用 `anti_ballistic_missile`），S7 再按本文件改造。

## 1. 玩家视角的目标行为

1. 玩家放置雷达站，设定**探测范围**（detection range）与**保护半径**（trigger range），并设一个无线电频道。
2. 在同频道放置若干发射台（发射台基座 + 发射屏，或巡航发射器），装填**反弹道导弹**，接通电源。
3. 雷达 GUI 选择**防御模式**：
   - `OFF`：只探测、只输出红石，不触发任何防御。
   - `EMP_ONLY`：1.12 行为——每 `sam_delay` tick 广播来袭消息，范围内的 EMP 塔放电。
   - `ABM_ONLY`：只发射反导弹。
   - `ABM_THEN_EMP`（默认）：先发射反导弹；某目标距撞击 ≤ `emp_fallback_ticks` 且仍未被拦截时，才让 EMP 塔放电。
4. 敌方导弹进入探测范围后，雷达持续跟踪；若预测落点在保护半径内，雷达挑选**最合适的一座**发射台发射一枚 ABM（可配置每目标最多几枚）。
5. ABM 垂直助推升空，转入制导，追向目标；接近到近炸半径内，目标导弹被摧毁（默认不引爆战斗部），空中出现一个小型、不破坏方块的爆炸效果。
6. 拦截失败（燃料耗尽、目标丢失）的 ABM 在空中自毁，不会掉回己方基地。
7. 雷达 GUI 用颜色区分：绿=已探测、黄=威胁、红=已分配拦截弹、灰=已拦截（保留 2 秒）。

## 2. 组件与包

```text
icbm.classic.content.abm
├── math/
│   ├── InterceptMath.java          # 纯 Java：拦截点求解、线段最近距离（JUnit）
│   └── TrajectoryPredictor.java    # 纯 Java：带加速度的外推与落点预测（JUnit）
├── radar/
│   ├── RadarTrack.java             # 航迹
│   ├── TrackTable.java             # 雷达内的航迹表（按目标 UUID）
│   ├── TrackStatus.java            # DETECTED / THREAT / ENGAGED / INTERCEPTED / LOST
│   ├── RadarDefenseMode.java       # OFF / EMP_ONLY / ABM_ONLY / ABM_THEN_EMP
│   └── FireControl.java            # 交战决策与无线电两段式分配
├── radio/
│   ├── InterceptQueryMessage.java
│   ├── InterceptOffer.java
│   └── InterceptOrderMessage.java
├── EngagementRegistry.java         # 每世界：目标 UUID → 在飞 ABM UUID 集合（登记由 ABM 拥有，可跨目标迁移）
│                                   #           + 目标 UUID → 拦截时刻（保留 INTERCEPT_HISTORY_TICKS）
├── EntityAntiBallisticMissile.java # 由 EntitySurfaceToAirMissile 改名改造
├── AbmGuidance.java                # 助推→制导→自毁 状态机
├── ProximityFuse.java              # 近炸判定（调用 InterceptMath）
├── InterceptEvents.java            # Fabric Event：INTERCEPTED、ABM_LAUNCHED（供插件）
└── ConfigAbm.java                  # @IcbmConfig(file = "abm")
```

删除：`SAMTargetData`（扫描逻辑并入 `EntityAntiBallisticMissile` 的“目标丢失重新捕获”）、`ConfigSAMMissile`（改为 `ConfigAbm`）、`IncomingMissileMessage#onTriggerCallback` 中给 SAM 设目标的逻辑。

## 3. 配置 `config/icbmclassic/abm.json`（`ConfigAbm`）

| 键 | 默认 | 含义 |
| --- | --- | --- |
| `flight_speed` | 4.0 | ABM 制导段速度（方块/tick）。沿用 1.12 SAM `FLIGHT_SPEED` |
| `boost_ticks` | 10 | 助推段时长（垂直上升） |
| `boost_speed` | 1.5 | 助推段速度 |
| `max_turn_degrees` | 15.0 | 制导段每 tick 最大转向角 |
| `fuel_ticks` | 200 | 发射后最大飞行时间，耗尽自毁。沿用 SAM `FUEL` |
| `proximity_radius` | 2.5 | 近炸半径（方块） |
| `hit_chance` | 1.0 | 进入近炸半径后的摧毁概率 [0,1] |
| `target_warhead_on_kill` | `false` | true=被摧毁的目标触发自身战斗部；false=无害销毁 |
| `kill_effect_power` | 1.5 | 拦截视觉爆炸威力（`ExplosionInteraction.NONE`，不破坏方块） |
| `reacquire_range` | 30 | 目标丢失后重新捕获半径。沿用 SAM `TARGET_RANGE` |
| `reacquire_timeout_ticks` | 40 | 丢失目标后多久自毁 |
| `engagement_range` | 200 | 发射台到预测拦截点的最大距离，超出不报价 |
| `max_interceptors_per_target` | 1 | 每个目标同时在飞的 ABM 上限（跨所有雷达） |
| `fire_control_interval_ticks` | 10 | 雷达火控决策间隔 |
| `reengage_delay_ticks` | 40 | 同一目标两次下令的最小间隔 |
| `emp_fallback_ticks` | 40 | `ABM_THEN_EMP` 模式下 EMP 兜底阈值 |
| `use_missile_target_data` | `true` | 预测落点时优先使用导弹自带目标坐标 |
| `prediction_max_ticks` | 600 | 外推最大步数 |

## 4. 雷达航迹（`RadarTrack`、`TrackTable`）

```java
public final class RadarTrack {
    public final UUID targetId;
    public final int trackId;                 // 雷达内自增，GUI 显示用
    public Vec3 position;                     // 最近一次扫描位置
    public Vec3 velocity;                     // 方块/tick，取 entity.getDeltaMovement()
    public Vec3 acceleration = Vec3.ZERO;     // (velocity - lastVelocity) / scanInterval
    public @Nullable Vec3 predictedImpact;
    public int ticksToImpact = Integer.MAX_VALUE;
    public TrackStatus status = TrackStatus.DETECTED;
    public long lastSeenTick;
    public long nextEngageTick;               // 火控节流
    public long statusChangedTick;            // INTERCEPTED 显示 2 秒后移除
}
```

每次雷达扫描（`ConfigRadar.SCAN_TICKS`，主线程）：

1. 取半径 `min(detectionRange, ConfigRadar.MAX_RANGE)` 内的威胁实体（`isThreat`：实现 `IMissile`、有爆炸物、不是 ABM、`ticksInAir > 1`）。
2. 对每个实体：表中已有 → 更新 `position`、`acceleration`、`velocity`、`lastSeenTick`；没有 → 新建（`trackId = nextTrackId++`）。
3. 计算预测（§5）。`predictedImpact` 在保护半径内（水平距离 ≤ `triggerRange`）→ `THREAT`，否则 `DETECTED`。已是 `ENGAGED` 的保持 `ENGAGED`，除非 `EngagementRegistry` 中该目标已无在飞 ABM（→ 回到 `THREAT`）。
4. 本次扫描未见到的航迹：若 `InterceptEvents` 记录它被拦截 → `INTERCEPTED`；否则 → `LOST`，下一次扫描删除。`INTERCEPTED` 保留 40 tick 后删除。
5. 1.12 字段 `incomingThreats`（`List<IMissile>`）与 `detectedThreats` 改为从航迹表派生的只读视图，供红石输出与 `getRadarState()` 使用，保持旧行为：红石强度 = `min(15, THREAT+ENGAGED 数量)`。

航迹表**不存档**（重新加载后重新扫描即可），防御模式**存档**（NBT 键 `"defense_mode"`，存枚举名）。

## 5. 弹着点预测（`TrajectoryPredictor`，纯 Java）

使用自己的最小向量类型 `Vec3d(double x, y, z)` record（在 `math` 包内，避免依赖 Minecraft），调用方负责与 `net.minecraft.world.phys.Vec3` 互转。

```java
public record Prediction(Vec3d impact, int ticks) {}

/**
 * 逐 tick 外推：p += v; v += a。当 p.y <= floorY 时返回该点（线性插值到 floorY）与步数。
 * 超过 maxTicks 返回 null（视为“不会在可预测时间内落地”）。
 */
public static @Nullable Prediction predictImpact(Vec3d p, Vec3d v, Vec3d a, double floorY, int maxTicks)
```

- `floorY` = 雷达方块 Y（保护区以雷达高度为参考）。
- 若 `use_missile_target_data` 且导弹 `getTargetData()` 有效：`impact` = 目标坐标；`ticks` 仍用外推计算（以“下降到目标 Y”为终止条件）；外推失败时 `ticks` = 水平距离 / max(水平速度, 0.1)。
- JUnit 用例（S7.01）：①匀速直线下落；②初速水平 + 重力加速度（与解析解误差 ≤ 0.5 格）；③向上飞行、maxTicks 内不落地 → null；④已经低于 floorY → 0 tick 返回当前点。

## 6. 拦截数学（`InterceptMath`，纯 Java）

### 6.1 拦截点（前置追踪）

已知：拦截弹位置 `S`、速度大小 `s`（方块/tick），目标位置 `P`、速度 `V`、加速度 `A`。

```java
/** 返回拦截时间 t（tick）与瞄准点；无解返回 null。 */
public static @Nullable Intercept solve(Vec3d S, double s, Vec3d P, Vec3d V, Vec3d A, int maxTicks) {
    // 1) 忽略加速度，解 |D + V t| = s t，D = P - S
    Vec3d D = P.sub(S);
    double a = V.dot(V) - s * s;
    double b = 2 * D.dot(V);
    double c = D.dot(D);
    double t;
    if (Math.abs(a) < 1e-9) {                 // 速度相等的退化情形
        if (Math.abs(b) < 1e-9) return null;
        t = -c / b;
    } else {
        double disc = b * b - 4 * a * c;
        if (disc < 0) return null;
        double sq = Math.sqrt(disc);
        double t1 = (-b - sq) / (2 * a), t2 = (-b + sq) / (2 * a);
        t = smallestPositive(t1, t2);         // 都 <= 0 返回 -1
    }
    if (t <= 0 || t > maxTicks) return null;
    // 2) 用加速度修正，固定迭代 3 次（不要改成 while 循环）
    for (int i = 0; i < 3; i++) {
        Vec3d aim = P.add(V.scale(t)).add(A.scale(0.5 * t * t));
        t = aim.sub(S).length() / s;
        if (t > maxTicks) return null;
    }
    Vec3d aim = P.add(V.scale(t)).add(A.scale(0.5 * t * t));
    return new Intercept(aim, t);
}
```

JUnit：①静止目标 → 瞄准点 = 目标、t = 距离/s；②目标横向匀速、拦截弹更快 → 求得点满足 `|aim - S| ≈ s t`（误差 1e-6）；③目标比拦截弹快且远离 → null；④`a≈0` 退化分支；⑤t 超过 maxTicks → null。

### 6.2 一个 tick 内两条运动线段的最近距离（防穿越）

```java
/** 拦截弹 aPrev→aNow，目标 tPrev→tNow，假设同一 tick 内都匀速。返回最近距离。 */
public static double closestApproach(Vec3d aPrev, Vec3d aNow, Vec3d tPrev, Vec3d tNow) {
    Vec3d d0 = tPrev.sub(aPrev);                               // τ=0 时的相对位置
    Vec3d dv = tNow.sub(tPrev).sub(aNow.sub(aPrev));           // 相对位移
    double dd = dv.dot(dv);
    double tau = dd < 1e-12 ? 0 : clamp(-d0.dot(dv) / dd, 0, 1);
    return d0.add(dv.scale(tau)).length();
}
```

JUnit：①迎头穿越（两点在 tick 中点相遇）→ 0；②平行同速 → 初始距离；③擦肩（最近距离 2）→ 2；④最近点在 tick 外（τ<0）→ 取 τ=0 的距离。

## 7. 火控（`FireControl`，雷达方块实体持有）

每 `fire_control_interval_ticks`，且雷达有电（`consume` 成功的那一 tick）、频道非空、模式为 `ABM_ONLY` 或 `ABM_THEN_EMP` 时执行：

```text
threats = 航迹表中 status ∈ {THREAT} 的航迹，按 ticksToImpact 升序
for track in threats:
    if now < track.nextEngageTick: continue
    if not EngagementRegistry.canEngage(level, track.targetId): continue
    query = new InterceptQueryMessage(channel, track.targetId, track.position, track.velocity, track.acceleration)
    RadioRegistry.popMessage(level, radio, query)        // 同步：返回时报价已收集完
    if query.offers().isEmpty(): continue                // 无可用发射台，下一个目标
    best = 报价中 “launchPoint 到 InterceptMath.solve(...).aim 的距离” 最小者；solve 无解的报价丢弃
    order = new InterceptOrderMessage(channel, track.targetId, best.launcherPos())
    RadioRegistry.popMessage(level, radio, order)
    if order.launched():                                 // 发射台在 onMessage 中同步发射并回填
        EngagementRegistry.register(level, track.targetId, order.launchedAbmId())
        track.status = ENGAGED
    track.nextEngageTick = now + reengage_delay_ticks
```

`ABM_THEN_EMP` 与 `EMP_ONLY` 的 EMP 部分：对 `THREAT` 或 `ENGAGED` 且 `ticksToImpact <= emp_fallback_ticks`（`EMP_ONLY` 模式下不看 ticks）的航迹，按 1.12 方式每 `ConfigRadar.SAM_TICKS` 广播一次 `IncomingMissileMessage(channel, missile, true)`。EMP 塔行为不变。

### 7.1 发射台的处理（`RadioScreen`、`RadioCruise`、发射台基座能力）

- **删除**对 `IIncomingMissileMessage` 的“设定目标 + 开火”处理（ADR 0004 第 5 条）。仍处理手持遥控器发来的普通 `ITargetMessage`/`ITriggerActionMessage`。
- 收到 `InterceptQueryMessage`：满足以下**全部**条件才 `query.offer(...)`：
  1. 装填的是 `anti_ballistic_missile`；
  2. 没有正在进行的发射（`getFiringPackage() == null`）；
  3. 能量足够一次发射（`IcbmEnergyBuffer.has(ConfigLauncher.POWER_COST)`）；
  4. 发射点到目标当前位置距离 ≤ `engagement_range`。
  报价内容：`InterceptOffer(launcherPos, launchPoint=发射台顶端中心, abmSpeed=flight_speed, readyInTicks=0)`。
- 收到 `InterceptOrderMessage` 且 `order.launcherPos().equals(自己的主方块位置)`：立即发射（跳过 `isTargetTooClose`/`isTargetTooFar` 检查，跳过发射延迟），生成 ABM 实体，设置 `abm.setTargetId(order.targetId())`，回填 `order.markLaunched(abm.getUUID())`，触发 `InterceptEvents.ABM_LAUNCHED`。
- 多座发射台组成的发射台网络：报价/开火以**发射台基座**为单位，`launcherPos` 是基座位置。

## 8. ABM 实体（`EntityAntiBallisticMissile`）

### 8.1 状态机（`AbmGuidance`，只在服务端运行）

| 状态 | 进入条件 | 每 tick 行为 | 离开条件 |
| --- | --- | --- | --- |
| `BOOST` | 生成时 | 速度 = (0, `boost_speed`, 0) | 已过 `boost_ticks` → `GUIDE` |
| `GUIDE` | BOOST 结束 | 找目标实体（`ServerLevel#getEntity(UUID)`）；存在则 `InterceptMath.solve` 得瞄准点（无解时瞄准目标当前位置）；把当前速度方向向瞄准方向旋转，最多 `max_turn_degrees`；速度大小 = `flight_speed`；更新朝向 | 目标不存在/死亡 → `REACQUIRE`；近炸命中 → 销毁；`fuel_ticks` 用尽 → `SELF_DESTRUCT` |
| `REACQUIRE` | 目标丢失 | 进入时先释放旧目标的登记（`EngagementRegistry.unregister(level, abmId)`，登记按 ABM 归属，不依赖当时持有的目标 id）；保持直线；每 10 tick 在 `reacquire_range` 内找最近的威胁实体（`TileRadarStation.isThreat`），且该目标满足 `EngagementRegistry.canEngage(level, targetId)`（在飞 ABM 数 < `max_interceptors_per_target`） | 找到 → `abm.retarget(...)` 把登记迁到新目标并回 `GUIDE`；超过 `reacquire_timeout_ticks` → `SELF_DESTRUCT` |
| `SELF_DESTRUCT` | 燃料耗尽或超时 | 在当前位置 `level.explode(this, x, y, z, kill_effect_power, Level.ExplosionInteraction.NONE)`，`discard()` | — |

转向限制实现（`AbmGuidance.rotateTowards(Vec3 current, Vec3 desired, double maxRadians)`）：两向量归一化，夹角 θ = acos(clamp(dot, -1, 1))；θ ≤ max → 返回 desired；否则用球面线性插值 `slerp(current, desired, max/θ)`。θ≈π（反向）时，绕任一垂直轴旋转 max。JUnit 覆盖这三种情况。

### 8.2 近炸（`ProximityFuse`）

每 tick 在 `GUIDE` 状态，移动完成后：

```java
double d = InterceptMath.closestApproach(prevPos, currentPos, target.prevPosition(), target.position());
if (d <= ConfigAbm.PROXIMITY_RADIUS) {
    if (random.nextDouble() < ConfigAbm.HIT_CHANCE) kill(target);
    else selfDestruct();                     // 未命中也自毁（与 1.12 SAM 一致：触发即消失）
}
```

`target.prevPosition()` 用目标实体的 `xo/yo/zo`（上一 tick 位置，字段名以 javap 为准）。

### 8.3 摧毁目标（`kill(target)`）

1. `InterceptEvents.INTERCEPTED` 事件（参数：level、abm、target、点位），监听器返回 false 可取消（取消则当作未命中）。
2. `target_warhead_on_kill = true`：调用目标的爆炸物/动作触发（复用 1.12 `CapabilityEmpMissile` 中“Trigger firing circuits”分支的代码路径，抽成 `MissileKillHelper.detonate(missile, source)`）。否则：`target.discard()`。
3. 在拦截点 `level.explode(abm, x, y, z, kill_effect_power, Level.ExplosionInteraction.NONE)`。
4. 播放声音（复用 1.12 已有的导弹爆炸声）。
5. `EngagementRegistry.markIntercepted(level, targetId)` 记录拦截时刻；雷达下一次扫描把航迹标为 `INTERCEPTED`。该记录只在 `INTERCEPT_HISTORY_TICKS`（200 tick，> 航迹表保留 `INTERCEPTED` 的 40 tick）内有效，过期即失效并在下一次记录拦截时整体清理（避免历史无界增长）；窗口内可被多座雷达重复读取，读取不消费。
6. `abm.discard()`。

### 8.4 其他

- 生命值：沿用 `ConfigMissile.TIER_2_HEALTH`。
- 雷达不把 ABM 当威胁（`isThreat` 排除）。EMP 对 ABM 的效果保留 1.12 行为（`CapabilityEmpMissile` 对 SAM 的分支改为 ABM）。
- 存档：`abm_state`、`target_id`、`ticks_in_state`、`fuel_left`。重新加载后若目标不存在进入 `REACQUIRE`。
- 客户端：外观沿用 SAM 物品模型；尾焰颜色沿用 `FollowTargetLogic` 的 (1, 0.5, 0.5)。

## 9. 已知限制（必须写进发布说明）

- **区块外模拟飞行的导弹不可见**：1.12 的 `MissileTrackerHandler` 会在导弹离开已加载区块时把它转为数据模拟，接近目标时重新生成实体。雷达与 ABM 只能处理实体。若导弹在保护区上空才重新生成，可能来不及拦截 —— 实测见 §9.1。
- 不做敌我识别：同频道雷达会拦截任何预测落在保护区内的导弹，包括玩家自己误射的。
- 不拦截非 ICBM 导弹的投射物（箭、火球等）。

### 9.1 S7.08 实测：重新生成点的几何与可拦截性

测量方法：GameTest `icbmclassic/test/abm/SimulatedMissileGameTest`。导弹经 S4.03 生产路径
`MissileTrackerHandler.simulateMissile()` 进入模拟，再由 `MissileTrackerWorld#runSimulationTick()`
推进到 `spawnMissileOnDestination()` 重新生成；位置/速度直接取自重新生成的实体，到落地的 tick 由
`TrajectoryPredictor.predictImpact()` 以「重新生成位置 + 速度 + 零加速度、`floorY` = 雷达 y、
`prediction_max_ticks` = 600」计算（即雷达自己用的那套外推）。雷达/ABM 场景为：雷达（探测
`ConfigRadar.MAX_RANGE`、保护 20、`ABM_ONLY`、逐 tick 补电）+ 一座同频道、装 ABM 的巡航发射器；
「是否拦截成功」取 `EngagementRegistry.wasIntercepted()`，发射/命中 tick、高度与距离取自逐 tick 采样；
「ABM 每 tick 步长」也逐 tick 采样并被断言 ≤ `abm.flight_speed`（该断言即 §9.1 第 2 条缺陷的集成级守卫）。

重新生成点由 `spawnMissileOnDestination` 固定为 `(target.x, simulation_enter_height, target.z)`、速度
`(0, -simulation_enter_speed, 0)` —— **目标正上方**重新进入世界，水平距离恒为 0：

| 场景 | 重新生成点水平距离 | 重新生成高度（y） | 到落地 tick（`TrajectoryPredictor`） | 是否拦截成功（修复前） | 是否拦截成功（修复后） |
| --- | --- | --- | --- | --- | --- |
| 1.12 默认（`simulation_enter_height=450`、`simulation_enter_speed=1`） | 0.0 格 | 450（雷达 y=38，垂直距离 412 格） | 412 | 是（靠 ABM 单 tick 位移 273.8~335.9 格的越界线段恰好穿过目标 2.5 格内） | **是**（ABM 以 `flight_speed` 4 格/tick 正常接敌：发射 tick 157~166 → 结束 tick 189~193，结束点 y=128.9~142.5、距发射点 89.0~102.5 格，实测单 tick 最大位移恒为 4.0000000000 格） |
| `simulation_enter_height` ×2 = 900 | 0.0 格 | 900（垂直距离 862 格） | 862 > `prediction_max_ticks`(600) → 外推返回 null（雷达先按 `DETECTED` 跟踪，落至可外推后才转 `THREAT`） | 否（ABM 越界步长擦过目标 ~0.5 格，未命中；目标第 325~329 tick 落到雷达上） | **是**（发射 tick 278~287 → 结束 tick 305~309，结束点 y=107.0~125.0、距发射点 67.1~85.1 格，实测单 tick 最大位移恒为 4.0000000000 格） |

结论：

1. **重新生成高度不是限制**：即使 900 格重新生成高度，只要雷达探测得到，`engagement_range`(200) 就会在
   目标进入 200 格时下令发射（实测下令时目标距发射点 174.97 / 169.34 格），此后 ABM 以 4 格/tick 迎头，
   约 18~28 tick 完成交战，远早于目标落地（`fuel_ticks` 200 也远未用尽）。
   交战的「击毁归属」有两种，取决于同一 tick 内两个实体的 tick 顺序：ABM 近炸（写 `EngagementRegistry`）或
   来袭导弹自己的实体命中判定先打到 ABM（导弹空中爆炸、不写 registry）；两者都表示导弹在高空（y≈107~143）被拦下。
2. **曾经的「拦截结果由缺陷支配」已修复**（历史记录，见 §9.1 补充）：`Rotation.rotateTowards` 在
   「转向在 `max_turn_degrees` 内」的分支曾返回**未归一化**的 `desired`，而 `AbmGuidance.guide()` 直接把它
   `.scale(ConfigAbm.FLIGHT_SPEED)`，使 ABM 单 tick 位移 = `flight_speed × |aim − position|`（实测
   273.8~335.9 格）。当时的「默认行命中 / ×2 行未命中」只是「越界线段是否恰好穿过目标 2.5 格内」的偶然结果。
   修复方式：该分支改为返回归一化后的 `desired`（`Rotation.java`，函数契约即「返回方向」，θ>max 分支本就返回
   单位向量）；同时 `AbmMathTest` 增加「非单位 `desired` 输入 → 输出长度为 1」的回归用例，
   `SimulatedMissileGameTest` 增加「ABM 单 tick 位移 ≤ `flight_speed`」的集成断言。
3. **`simulation_enter_height`/`simulation_enter_speed` 的配置注释提示文案**：设计原意是让玩家知道
   「导弹会在目标正上方 `simulation_enter_height` 处重新出现」。文案尚未添加（改动会让已入库的
   `run/datagen/config/icbmclassic/missile.json` 快照失效），留给维护者与 S7.09 一起决定。
4. 雷达的 `prediction_max_ticks`(600) 对 900 格的重新生成高度不足以给出外推落点（返回 null）；
   若 `use_missile_target_data=true` 且导弹自带目标坐标，航迹仍会因「预测落点在保护半径内」成为 `THREAT`
   （落点取自目标坐标，`ticksToImpact` 退化为水平距离/水平速度），因此这一点不影响交战。

> §11 重测范围（S7.10 执行）：制导修复改变了 ABM 的飞行时间（不再瞬移），凡是依赖「发射后极短时间命中」的
> 场景都要重测，重点是 `单目标拦截`（80 格水平 + 60 格高，400 tick 预算是否仍够）、`不越权齐射`、
> `燃料耗尽自毁`（自毁时序）与 `跨雷达去重`；`落点在外不交战`/`EMP 兜底` 与制导无关，只需顺带确认。

详见 `docs/implementation/evidence/S7.08.md`。

## 10. 网络与 GUI

- 雷达渲染点（`RadarRenderData`）每个点增加 1 字节状态（`TrackStatus.ordinal()`）；客户端按 §1 第 7 条着色。
- 雷达 GUI 新增“防御模式”循环按钮（`fromClient()` codex `icbmclassic:radarstation/defense_mode`，服务端校验枚举范围）。
- 翻译键（加入 `en_us.json`）：`icbmclassic.radar.defense_mode`、`icbmclassic.radar.defense_mode.off|emp_only|abm_only|abm_then_emp`、`icbmclassic.radar.track.detected|threat|engaged|intercepted`、`icbmclassic.radio.intercept.launched`、`icbmclassic.radio.intercept.no_launcher`。

## 11. 测试矩阵（S7.10）

GameTest 结构模板（`.nbt`）放在 `src/gametest/resources/data/icbmclassic-test/structure/`。没有现成模板时用代码在测试开始时放置方块（推荐，弱 Agent 更不易出错）。

| 场景 | 布置 | 期望 |
| --- | --- | --- |
| 单目标拦截 | 雷达（频道 `t1`、保护 50、`ABM_ONLY`、`REQUIRES_POWER=false`）+ 1 座装 ABM 的巡航发射器（同频）；在雷达东 80 格、高 60 格生成一枚目标为雷达位置的 `condensed_missile` 实体，给定朝雷达的速度 | 400 tick 内目标实体消失；雷达位置方块未被破坏；`InterceptEvents.INTERCEPTED` 触发 1 次 |
| 不越权齐射 | 同上但 3 座发射台 | 只发射 1 枚 ABM（统计 `ABM_LAUNCHED` 次数 = 1，直到第一枚结束） |
| 落点在外不交战 | 目标落点在保护半径外 | 不发射 ABM |
| EMP 兜底 | `ABM_THEN_EMP`，发射台没装 ABM，有 EMP 塔 | 在 `ticksToImpact <= 40` 时 EMP 塔放电 |
| 燃料耗尽自毁 | 发射 ABM 后立刻删除目标、`reacquire_range=0` | ABM 在 `reacquire_timeout_ticks` 后自毁，不落地 |
| 跨雷达去重 | 2 部不同频道雷达，各配 1 座发射台 | 同一目标在飞 ABM ≤ 1 |

### 11.1 S7.10 实施结果（缩尺与覆盖对照）

**缩尺**（依据 `docs/implementation/evidence/S7.07.md` 的实测约束）：只有结构原点所在的区块列被 tick
实体，`@GameTest(padding)` 与 `ServerLevel#setChunkForced` 都不能扩大它，因此「雷达东 80 格」不可行。
实施时**高度 60 格原样保留**（区块列的 y 轴不受限），**水平 80 格缩到 4 格**，来袭速度取 1 格/tick；
发射点/展开点全部落在 `|local| ≲ 6` 内。缩尺只改变接敌几何，不改变 boost/制导/近炸任何生产逻辑。

**覆盖对照**（新增类 `icbm.classic.test.abm.AbmScenarioGameTest`，登记于 `src/gametest/resources/fabric.mod.json`）：

| 场景 | 自动化位置 |
| --- | --- |
| 单目标拦截 | 新增 `singleIncomingMissileIsInterceptedWithoutRadarDamage`（400 tick 预算、方块未破坏、INTERCEPTED=1）+ 既有 `LauncherInterceptGameTest#cruiseLauncherRespondsToRadarInterceptOrder`、`SimulatedMissileGameTest#defaultReentryIsIntercepted` |
| 不越权齐射 | 新增 `threeLaunchersCommitAtMostOneInterceptorPerTarget` |
| 落点在外不交战 | 新增 `impactOutsideProtectionRangeIsDetectedButNotEngaged`（有发射台在场，验证「不发射」）+ 既有 `RadarTrackGameTest#sidewaysMissileOutsideProtectionIsDetected`（航迹保持 DETECTED） |
| EMP 兜底 | 既有 `DefenseModeGameTest#abmThenEmpWaitsForFallbackThreshold`、`#abmOnlyDoesNotDischargeEmpTower`（未重写） |
| 燃料耗尽自毁 | 新增 `abmSelfDestructsWhenFuelRunsOut`（fuel_ticks 路径）+ `interceptorSelfDestructsWhenItsTargetIsLost`（发射后删除目标）+ 既有 `AbmGuidanceGameTest#noTargetReacquiresThenSelfDestructsWithoutBlockDamage` |
| 跨雷达去重 | 新增 `crossRadarEngagementKeepsOneInterceptorPerTarget` |

**确定性断言**：S7.08 §8 已证明「击毁归属」（ABM 近炸 vs 来袭导弹先撞上 ABM）取决于同一 tick 的实体
顺序，因此新增场景的来袭导弹夹具不跑继承的投射物 tick（不会反向射线命中 ABM），近炸成为唯一解算路径；
场景断言只覆盖「目标被解决 / 拦截弹数量不超过上限 / 拦截记录恰好一次」等确定性结果。
实测数据见 `docs/implementation/evidence/S7.10.md`。

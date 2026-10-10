# S7 反弹道导弹系统

## 阶段说明

把 S4.04 原样移植的 SAM 改造为完整的反弹道导弹（ABM）系统：雷达航迹与弹着点预测、火控分配、制导拦截、近炸摧毁、防御模式与 EMP 兜底。

**设计依据**：[anti-ballistic-missile.md](../architecture/anti-ballistic-missile.md)（下称“ABM 设计”）与 ADR 0004。**本阶段的类名、包名、配置键、消息流程以 ABM 设计为准，不要另想方案。**

**玩法变化（需写入 CHANGE_LOG，S9.02）**：发射屏/巡航发射器不再因雷达来袭消息而发射所装填的任意导弹；只有装填 ABM 的发射台在雷达指名时发射。

所有 Gradle 命令前：`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`。

---

### S7.01 拦截数学与轨迹预测（JUnit）

- 前置：S6.07
- 必读：ABM 设计 §5、§6、§8.1 转向限制
- 改动文件：新建 `src/main/java/icbm/classic/content/abm/math/{Vec3d,InterceptMath,TrajectoryPredictor,Intercept,Prediction,Rotation}.java`；`src/test/java/icbm/classic/content/abm/math/*Test.java`
- 目标：拦截所需的全部数学是纯 Java、可单测。

**操作**

1. `Vec3d`：`public record Vec3d(double x, double y, double z)`，方法 `add`、`sub`、`scale`、`dot`、`length`、`lengthSqr`、`normalize`（零向量返回零向量）、`cross`。**不得** import 任何 `net.minecraft`。
2. `InterceptMath.solve(...)`、`InterceptMath.closestApproach(...)`：照 ABM 设计 §6 骨架；`smallestPositive(a, b)` 返回两者中最小的正数，都不为正返回 -1。
3. `TrajectoryPredictor.predictImpact(...)`：照 §5。线性插值：上一步 `p0`（y > floorY）、这一步 `p1`（y ≤ floorY），`t = (p0.y - floorY) / (p0.y - p1.y)`，`impact = p0 + (p1 - p0) * t`。
4. `Rotation.rotateTowards(Vec3d current, Vec3d desired, double maxRadians)`：照 §8.1；slerp：`sin((1-f)θ)/sinθ * a + sin(fθ)/sinθ * b`；反向（θ > π - 1e-6）时选垂直轴：`axis = current × (0,1,0)`，若长度 < 1e-6 用 `current × (1,0,0)`，再按罗德里格斯公式旋转 `maxRadians`。
5. JUnit：ABM 设计 §5、§6.1、§6.2、§8.1 列出的全部用例（共 ≥ 16 个断言），浮点比较用 `assertEquals(expected, actual, 1e-6)`（预测类用 0.5）。

**验证**：`./gradlew test --tests 'icbm.classic.content.abm.math.*'`；`grep -rn "net.minecraft" src/main/java/icbm/classic/content/abm/math` 无输出。

**提交信息**：`feat(abm): 增加拦截数学与轨迹预测库`

---

### S7.02 雷达航迹表与弹着点预测

- 前置：S7.01
- 必读：ABM 设计 §4、§5
- 改动文件：新建 `content/abm/radar/{RadarTrack,TrackTable,TrackStatus}.java`；修改 `TileRadarStation`（`doScan` 改用航迹表；`incomingThreats`/`detectedThreats` 改为派生视图）；`ConfigAbm`（本步先建类，只含 `use_missile_target_data`、`prediction_max_ticks`，S7.09 补全）；GameTest `RadarTrackGameTest`
- 目标：雷达维护航迹、计算预测落点与撞击时间、区分 DETECTED/THREAT；1.12 的红石与状态行为不变。

**操作**

1. 按 ABM 设计 §4 第 1–5 条实现。`Vec3`↔`Vec3d` 转换工具放 `content/abm/radar/Vectors.java`。
2. `isMissileGoingToHit` 删除，判定改为“预测落点水平距离 ≤ `triggerRange`”。**保留** `isThreat` 静态方法签名（ABM 也调用）。
3. 1.12 中 `incomingThreats` 被 `IncomingMissileMessage` 使用（`incomingThreats.get(0)`）：改为“THREAT 航迹中 ticksToImpact 最小者对应的 `IMissile`”。
4. GameTest：
   - 朝雷达飞的导弹 → 航迹 THREAT，`predictedImpact` 与雷达水平距离 < 10。
   - 侧向飞过、落点在保护半径外 → DETECTED。
   - 导弹被移除后下一次扫描 → 航迹 LOST，再下一次被删除。
   - 红石输出强度 = THREAT 数（1）。

**提交信息**：`feat(radar): 雷达增加航迹表与弹着点预测`

---

### S7.03 ABM 实体与制导状态机

- 前置：S7.01
- 必读：ABM 设计 §8.1、§8.4、§3
- 改动文件：`EntitySurfaceToAirMissile` → `content/abm/EntityAntiBallisticMissile.java`（`git mv` 后修改，账本更新）；新建 `content/abm/AbmGuidance.java`；`ItemSurfaceToAirMissile` → `content/abm/ItemAntiBallisticMissile.java`；删除 `SAMTargetData`（逻辑并入 `REACQUIRE`）、`ConfigSAMMissile`（字段迁入 `ConfigAbm`）；更新所有引用（`ModEntities`、`ModItems`、发射台、`CapabilityEmpMissile`、`IncomingMissileMessage`）；GameTest `AbmGuidanceGameTest`
- 目标：ABM 按 BOOST→GUIDE→(REACQUIRE)→SELF_DESTRUCT 状态机飞行；本步**不**判定命中（S7.04）。

**操作**

1. 状态枚举 `AbmState { BOOST, GUIDE, REACQUIRE, SELF_DESTRUCT }`，存档键 `abm_state`。
2. `EntityAntiBallisticMissile` 不再使用 `IMissileFlightLogic` 驱动运动（覆写 tick：服务端由 `AbmGuidance.tick(this)` 设置速度并移动；仍调用 `EntityProjectile` 的移动/碰撞部分——若基类 tick 会再次改速度，则 ABM 跳过基类的飞行逻辑调用，具体看 S4.01 后的代码结构，在证据中说明）。
3. `setTargetId(UUID)`、`getTargetId()`；`IRadioTargetAssignable.assignTarget(Entity)` 保留，内部调用 `setTargetId`。
4. GUIDE 中调用 `InterceptMath.solve(abmPos, flightSpeed, targetPos, targetVel, targetAcc, fuelLeft)`；目标加速度 = 目标本 tick 速度 − 上 tick 速度（ABM 内保存上一 tick 目标速度）。
5. 每 tick：`fuelLeft--`；`setYRot/setXRot` 跟随速度方向（同 `FollowTargetLogic` 公式）；客户端尾焰颜色用 `DATA_SMOKE_COLOR`。
6. GameTest：
   - 无目标生成 ABM：`boost_ticks` 后进入 REACQUIRE；`reacquire_timeout_ticks` 后自毁（实体消失、发生 NONE 爆炸——断言地面方块未被破坏）。
   - 给定一个静止的目标实体（例如悬停的盔甲架，或速度为 0 的测试导弹）：ABM 在 GUIDE 中速度方向与“指向目标”夹角逐 tick 减小，且每 tick 转向 ≤ `max_turn_degrees`（记录每 tick 角度到日志）。

**提交信息**：`feat(abm): 反弹道导弹制导状态机`

---

### S7.04 近炸引信、摧毁效果、`InterceptEvents`

- 前置：S7.03
- 必读：ABM 设计 §8.2、§8.3
- 改动文件：新建 `content/abm/{ProximityFuse,InterceptEvents,MissileKillHelper}.java`；`EntityAntiBallisticMissile`；GameTest `AbmInterceptGameTest`
- 目标：ABM 接近目标即摧毁，事件可被插件监听/取消。

**操作**

1. `InterceptEvents`：

   ```java
   public final class InterceptEvents {
       @FunctionalInterface public interface Intercepted { boolean onIntercept(ServerLevel level, EntityAntiBallisticMissile abm, Entity target, Vec3 point); }
       @FunctionalInterface public interface AbmLaunched { void onLaunched(ServerLevel level, EntityAntiBallisticMissile abm, UUID targetId, BlockPos launcherPos); }
       public static final Event<Intercepted> INTERCEPTED = EventFactory.createArrayBacked(Intercepted.class,
           ls -> (lvl, abm, t, p) -> { for (Intercepted l : ls) if (!l.onIntercept(lvl, abm, t, p)) return false; return true; });
       public static final Event<AbmLaunched> ABM_LAUNCHED = EventFactory.createArrayBacked(AbmLaunched.class,
           ls -> (lvl, abm, id, pos) -> { for (AbmLaunched l : ls) l.onLaunched(lvl, abm, id, pos); });
   }
   ```

2. `MissileKillHelper.detonate(IMissile, IActionSource)`：从 1.12 `CapabilityEmpMissile` “Trigger firing circuits”分支抽出（S4.01 移植的代码），EMP 与 ABM 共用。
3. `ProximityFuse.check(abm, target)`：ABM 设计 §8.2；目标上一 tick 位置用 javap 核对 `Entity` 的 `xo/yo/zo` 字段或 `getPosition(0f)`/`oldPosition()` 一类方法。
4. 摧毁流程按 §8.3 第 1–6 步；`EngagementRegistry.markIntercepted` 在 S7.05 实现，本步先调用一个空的静态方法占位（S7.05 填充）。
5. GameTest：
   - 目标测试导弹匀速横穿，ABM 迎面拦截 → `INTERCEPTED` 触发 1 次，目标被移除，地面无破坏。
   - 注册一个返回 false 的监听器 → 目标不被移除，ABM 自毁。
   - `hit_chance = 0` → 目标不被移除，ABM 自毁。
   - `target_warhead_on_kill = true`（目标为 `condensed` 导弹，拦截点设在低空）→ 拦截点附近方块被破坏。
   - 高速穿越：目标速度 6 格/tick、ABM 4 格/tick 迎头 → 依靠线段最近距离仍判定命中。

**提交信息**：`feat(abm): 近炸引信与目标摧毁`

---

### S7.05 火控与拦截消息、`EngagementRegistry`

- 前置：S7.02、S7.04
- 必读：ABM 设计 §2 `radio/`、§7
- 改动文件：新建 `content/abm/radio/{InterceptQueryMessage,InterceptOffer,InterceptOrderMessage}.java`、`content/abm/EngagementRegistry.java`、`content/abm/radar/FireControl.java`；`TileRadarStation`（持有 `FireControl`，在有电 tick 中调用）；`ICBMClassic.java`（世界卸载清理 `EngagementRegistry`）；GameTest `FireControlGameTest`
- 目标：雷达两段式分配拦截任务；跨雷达去重。

**操作**

1. 消息类：

   ```java
   public final class InterceptQueryMessage implements IRadioMessage {
       private final String channel; private final UUID targetId; private final Vec3 targetPos, targetVel, targetAcc;
       private final List<InterceptOffer> offers = new ArrayList<>();
       public void offer(InterceptOffer o) { offers.add(o); }
       public List<InterceptOffer> offers() { return Collections.unmodifiableList(offers); }
       // getChannel() 等按 IRadioMessage 接口
   }
   public record InterceptOffer(BlockPos launcherPos, Vec3 launchPoint, double abmSpeed, int readyInTicks) {}
   public final class InterceptOrderMessage implements IRadioMessage {
       private final String channel; private final UUID targetId; private final BlockPos launcherPos;
       private @Nullable UUID launchedAbmId;
       public void markLaunched(UUID abmId) { if (launchedAbmId == null) launchedAbmId = abmId; }
       public boolean launched() { return launchedAbmId != null; }
       public @Nullable UUID launchedAbmId() { return launchedAbmId; }
   }
   ```

2. `EngagementRegistry`（每世界 `Map<UUID 目标, Set<UUID ABM>>` + `Set<UUID> intercepted`）：`register`、`activeCount(level, targetId)`（清理已不存在的 ABM：`level.getEntity(uuid) == null` 则移除）、`markIntercepted`、`wasIntercepted`、`clear(dimension)`。ABM 自毁/被移除时调用 `unregister`（在 `EntityAntiBallisticMissile#remove` 覆写中）。
3. `FireControl.tick(ServerLevel, TileRadarStation)`：严格按 ABM 设计 §7 伪代码。最佳报价：对每个报价 `InterceptMath.solve(launchPoint, abmSpeed, targetPos, targetVel, targetAcc, fuel_ticks)`，无解丢弃，取 `|aim - launchPoint|` 最小者；并列取 `launcherPos` 字典序最小（保证确定性）。
4. 本步发射台尚不响应（S7.06），GameTest 用测试无线电接收器模拟两座发射台：
   - 两个模拟发射台都报价，距离不同 → `InterceptOrderMessage.launcherPos` 是较近的那个。
   - 模拟发射台在 order 中 `markLaunched(随机UUID)` → 航迹变 ENGAGED，`activeCount = 1`；`reengage_delay_ticks` 内不再发 query。
   - 无报价 → 不发 order。

**提交信息**：`feat(abm): 雷达火控与拦截分配消息`

---

### S7.06 发射台响应拦截；删除来袭消息开火

- 前置：S7.05
- 必读：ABM 设计 §7.1；ADR 0004 第 5 条
- 改动文件：`RadioScreen`、`RadioCruise`、发射台基座能力（`LauncherCapability`/`CLauncherCapability`）；`IncomingMissileMessage`（删除 `onTriggerCallback` 中给 SAM 设目标的逻辑）；GameTest `LauncherInterceptGameTest`
- 目标：装 ABM 的发射台按 §7.1 报价与开火；任何发射台不再因 `IIncomingMissileMessage` 开火。

**操作**

1. 在两个 `Radio*` 的 `onMessage` 中：删除 `IIncomingMissileMessage` 进入“设目标+开火”路径的可能（注意 `IIncomingMissileMessage extends ITargetMessage, ITriggerActionMessage`——必须在处理 `ITargetMessage`/`ITriggerActionMessage` **之前**判断 `if (packet instanceof IIncomingMissileMessage) return;`）。
2. 新增 `InterceptQueryMessage`/`InterceptOrderMessage` 处理，条件与动作照 §7.1。发射 ABM：跳过最小/最大距离检查与发射延迟；ABM 初始位置 = 发射台导弹座位置（与普通导弹相同）；`abm.setTargetId(...)`；消耗导弹物品与 `POWER_COST` 能量；`order.markLaunched(abm.getUUID())`；`InterceptEvents.ABM_LAUNCHED.invoker().onLaunched(...)`。
3. 发射台网络中多个基座：以收到消息的接收器所在网络为单位，**每个基座**单独报价（`launcherPos` = 基座位置）。
4. GameTest：
   - 发射屏装 `nuclear_missile`、同频雷达检测到来袭导弹 → 200 tick 内**没有**导弹被发射（统计 `explosive_missile` 实体数不增加）。
   - 巡航发射器装 ABM、同频雷达 → 发射 1 枚 ABM 并最终拦截（端到端）。

**提交信息**：`feat(launcher): 发射台响应拦截命令并移除来袭消息自动开火`

---

### S7.07 防御模式、EMP 兜底、GUI 与渲染点

- 前置：S7.06
- 必读：ABM 设计 §1 第 3、7 条、§7 EMP 部分、§10
- 改动文件：新建 `content/abm/radar/RadarDefenseMode.java`；`TileRadarStation`（字段、存档键 `defense_mode`、codex `defense_mode` `fromClient()`、EMP 广播逻辑按模式）；`RadarRenderData`（每点加状态字节）；雷达菜单与屏幕（client）；`en_us.json`；GameTest `DefenseModeGameTest`
- 目标：GUI 可切换四种模式，行为符合设计；雷达屏显示颜色。

**操作**

1. 模式行为：`OFF` 不调用 `FireControl` 也不广播；`EMP_ONLY` 不调用 `FireControl`，按 1.12 方式广播；`ABM_ONLY` 只调用 `FireControl`；`ABM_THEN_EMP` 两者都调用，EMP 广播只针对 `ticksToImpact <= emp_fallback_ticks` 的航迹（广播消息中的导弹为该航迹目标）。默认 `ABM_THEN_EMP`。
2. codex 服务端校验：收到的 ordinal 在 `[0, values().length)` 内，否则丢弃。
3. 渲染点颜色：DETECTED 绿 `0x00FF00`、THREAT 黄 `0xFFFF00`、ENGAGED 红 `0xFF0000`、INTERCEPTED 灰 `0x808080`；GUI 中显示当前模式文本。
4. GameTest：
   - `EMP_ONLY`：发射台装 ABM 也不发射；EMP 塔放电。
   - `ABM_ONLY`：没有 ABM 时 EMP 塔也不放电。
   - `ABM_THEN_EMP`：无 ABM 可用时，EMP 只在 `ticksToImpact <= 40` 后放电（记录放电时的 ticksToImpact）。
   - `OFF`：都不触发，但红石仍输出。

**提交信息**：`feat(radar): 雷达防御模式与 EMP 兜底`

---

### S7.08 区块外模拟导弹限制测量

- 前置：S7.07
- 必读：ABM 设计 §9；S4.03 的追踪实现
- 改动文件：GameTest `SimulatedMissileGameTest`；`docs/architecture/anti-ballistic-missile.md` §9 补测量结果（同一提交）
- 目标：量化“模拟导弹重新生成点距目标多远”，判断 ABM 是否有足够时间拦截。

**操作**：构造一枚处于模拟状态、目标为雷达位置的导弹记录（S4.03 测试工具）→ 推进到重新生成 → 记录生成位置到目标的水平距离、高度、生成后到落地的 tick 数（用 `TrajectoryPredictor`）；同时在场景中放雷达 + ABM 发射台，记录是否拦截成功。分别测 1.12 默认配置与 `ConfigMissile` 中“重新生成提前量”相关配置（以实际配置名为准）调大一倍后的结果。把表格写入设计文档 §9 与证据。**本步不修改游戏逻辑**；若结论是默认配置下几乎无法拦截，在证据中标“需要维护者决定”，不 BLOCKED。

**提交信息**：`test(abm): 测量区块外模拟导弹的可拦截性`

---

### S7.09 配置、配方、资源、语言键

- 前置：S7.07
- 必读：ABM 设计 §3、§10
- 改动文件：`ConfigAbm`（补全 §3 全部键与注释）、`ConfigManager.CONFIG_CLASSES`；datagen 配方（沿用 1.12 SAM 配方，输出改为 `anti_ballistic_missile`；若 S2.08 已生成则只核对）；物品模型/纹理（沿用 SAM，文件名改为新 ID）；`en_us.json`（§10 键 + 物品/实体名 “Anti-Ballistic Missile”）；`scripts/check_lang.py` 通过
- 目标：ABM 相关资源齐全。

**提交信息**：`feat(abm): 补全反弹道导弹配置与资源`

---

### S7.10 ABM GameTest 矩阵

- 前置：S7.08、S7.09
- 必读：ABM 设计 §11
- 改动文件：`src/gametest/java/icbm/classic/test/abm/AbmScenarioGameTest.java`（若前面步骤已覆盖某场景，引用已有测试名，不重复）
- 目标：§11 六个场景全部自动化并通过。

**操作**：场景用代码放置方块（不用结构文件）。测试结构尺寸要容纳 80 格水平距离与 60 格高度：用 `@GameTest` 注解中的结构/尺寸参数或 Fabric 提供的空结构（javap 核对 `net.fabricmc.fabric.api.gametest.v1.GameTest` 字段；若无法得到足够大的空间，把距离缩小到 40/30 并在证据中说明）。每个场景 `maxTicks` 设足（例如 600）。

**验证**：`./gradlew build`（全部 GameTest）；`scripts/check.sh`。

**提交信息**：`test(abm): 增加反导场景测试矩阵`

---

### S7.11 人工门禁：实机拦截演练

- 前置：S7.10
- 准备者（Agent）：构建 jar + sha256；写清单；保持 `[H]`。
- 执行者（人）：客户端（建议同时用专用服务器）。

**检查清单**：

1. 雷达 GUI 切换四种模式，重进世界后保持。
2. 远处（> 300 格）发射核弹打雷达区域：雷达屏出现黄点→红点；1 枚 ABM 升空、拦截、灰点；无地面破坏。
3. 同时发射 3 枚导弹、配置 2 座 ABM 发射台：观察分配与补射（第二轮在 `reengage_delay_ticks` 后）。
4. 拿走 ABM，模式 `ABM_THEN_EMP`：EMP 塔在最后时刻放电。
5. 装填核弹的发射屏与来袭导弹同频：确认**不会**自动发射核弹。
6. 记录 TPS（`/tick query`，26.3 命令以实际为准）在 5 枚导弹 + 5 枚 ABM 同时飞行时的变化。

**提交信息**：`docs(impl): 记录 S7.11 反导实机演练`

# P1：升级到 Minecraft 26.3 + Fabric

状态：TODO。前置：P0。

交付物：`port/26.3-fabric` 分支上可构建的 7.0.0-dev 模组，内容与玩法与 6.5.1 等价（ADR 0002、0006 明确删除的部分除外）。

## 工具链与基线

- [x] (1) 建立工作分支、迁出旧代码到 `legacy-1.12/`。
- [x] (2) Gradle 9.8.1 + Loom 1.18.3 + Fabric 0.19.5/0.162.0+26.3 空骨架构建通过，专用服务端加载本模组。
- [x] (3) JUnit 5 与 Fabric GameTest 管线在 `./gradlew build` 中运行。
- [x] (4) `scripts/check.sh` 一键检查（构建、测试、禁用名称扫描、语言文件校验）。
- [x] (5) 移植账本：列出全部 1.12 Java 文件及目标状态。

## 基础库

- [x] (1) 向量/旋转/区域与 jlib 纯 Java 部分移植，JUnit 覆盖。（S1.01，证据：../implementation/evidence/S1.01.md）
- [x] (2) JSON 配置系统替代 Forge `@Config`，全部配置类迁移。（S1.02，证据：../implementation/evidence/S1.02.md）
- [x] (3) `SaveHandler`（ValueInput/ValueOutput）替代 `NbtSaveHandler`。（S1.03，证据：../implementation/evidence/S1.03.md）
- [x] (4) `PacketCodex` 传输层改为 Fabric 载荷，服务端校验。（S1.04，证据：../implementation/evidence/S1.04.md）
- [x] (5) 无线电系统按世界维度键分表。（S1.05，证据：../implementation/evidence/S1.05.md）
- [x] (6) 雷达注册表/地图。（S1.06，证据：../implementation/evidence/S1.06.md）
- [ ] (7) Capability → API Lookup / 数据附件 / DataComponent；`IcbmEnergyBuffer`。（部分：S1.07、S1.10；GPS 的 DataComponent 与电池/机器对外存储随 S2.04/S5.08/S6）
- [ ] (8) 动作系统（actions/conditions/causes/status）。（部分：S1.08：动作/条件/原因/状态/字段框架与 causeby、conditionals 已移植；content 动作注册、WorkTickingActionHandler、emp/** 与 entity/** 留 S3.02/S3.05/S3.07；证据：../implementation/evidence/S1.08.md）
- [ ] (9) 线程化爆炸：工作线程计算、主线程按预算应用。（部分：S1.09；`lib/thread` 的 LevelSnapshot 快照、WorkerThread/WorkerThreadManager 线程池、BlockEditQueue 主线程预算队列与 `ConfigThread.BLOCKS_PER_TICK` 已完成；ThreadExplosion/ThreadLargeExplosion/ThreadSmallExplosion 与 content/blast/threaded/** 按裁决留 S3.02/S3.06 在快照/队列架构上重写；证据：../implementation/evidence/S1.09.md）
- [x] (10) 公共 API 包签名迁移。（S1.10，证据：../implementation/evidence/S1.10.md）

## 注册与资源

- [ ] (1) 注册骨架与平铺化 ID（registry-ids.md）。
- [ ] (2) `ExplosiveRegistry` 与内容形态。
- [ ] (3) 简单方块（混凝土、玻璃、尖刺、按钮、压力板）。
- [ ] (4) 材料与简单物品。
- [ ] (5) 创造模式标签页。
- [ ] (6) datagen：方块状态、方块模型、物品模型定义、战利品表、标签。
- [ ] (7) 配方（datagen + `c:` 标签）。
- [ ] (8) 声音。
- [ ] (9) `en_us.json` 迁移与 `mergeLang`、`check_lang.py` 骨架。

## 爆炸

- [x] (1) 投射物基类、爆炸框架与伤害类型。（S3.01 + S3.02 已完成；证据：[S3.01.md](../implementation/evidence/S3.01.md)、[S3.02.md](../implementation/evidence/S3.02.md)）
- [x] (2) 爆炸相关实体（碎片、飞行方块、光束、烟雾、点燃炸药）。（S3.03 已完成；证据：[S3.03.md](../implementation/evidence/S3.03.md)）
- [x] (3) 常规爆炸（condensed、shrapnel、incendiary、anvil、repulsive、attractive、fragmentation、sonic、breaching、exothermic、endothermic、antigravitational、rot、mutation）。（S3.04 已完成；证据：[S3.04.md](../implementation/evidence/S3.04.md)）
- [x] (4) 气体爆炸、药水效果、防护装备（debilitation、chemical、contagious、colors、smoke）。（S3.05 已完成；证据：[S3.05.md](../implementation/evidence/S3.05.md)）
- [ ] (5) 线程化爆炸与红物质（nuclear、thermobaric、antimatter、redmatter）。
- [ ] (6) EMP 与末影爆炸。
- [ ] (7) 放射性方块。
- [ ] (8) 爆炸物方块与引信。
- [ ] (9) `/icbm` 命令。

## 导弹

- [ ] (1) 导弹实体与飞行逻辑。
- [ ] (2) 爆炸导弹、动作导弹、导弹物品。
- [ ] (3) 区块外导弹追踪模拟（SavedData）。
- [ ] (4) SAM 原样移植为 `anti_ballistic_missile`。
- [ ] (5) 手雷、炸弹矿车。
- [ ] (6) 集束导弹、子弹药、降落伞、气球、携带物品导弹、座椅、特殊配方。
- [ ] (7) 实体渲染器与粒子。

## 机器与工具

- [ ] (1) 方块实体基类、库存、GUI 组件。
- [ ] (2) 发射台网络方块与方块实体。
- [ ] (3) 发射台菜单与屏幕。
- [ ] (4) 巡航发射器。
- [ ] (5) 雷达站。
- [ ] (6) EMP 塔（多方块）。
- [ ] (7) 方块实体渲染器。
- [ ] (8) 工具物品（遥控器、激光引爆器、雷达枪、火箭筒、拆弹器、解毒剂）。
- [ ] (9) 发射器（发射/投掷）行为与红石。

## 验收

- [ ] (1) 全部 GameTest 通过；移植账本无 `TODO` 行。
- [ ] (2) [H] 客户端实机：创造模式取得全部物品、搭建发射台、发射导弹命中、雷达探测、EMP 塔放电、GUI 正常。

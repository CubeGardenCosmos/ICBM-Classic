# 系统概览（26.3 Fabric 目标架构）

## 1. 版本与工具链

见 [ADR 0001](../decisions/0001-target-minecraft-26.3-fabric.md)。摘要：Minecraft 26.3、Java 25、Gradle 9.8.1、Loom 1.18.3（`net.fabricmc.fabric-loom`）、Fabric Loader 0.19.5、Fabric API 0.162.0+26.3、Team Reborn Energy 5.0.0（S6 引入）、Lombok 1.18.48、JUnit 5.14.4。

## 2. 现状（1.12，6.5.1）

| 区域 | 包（`icbm.classic.` 之下） | 文件数 | 行数 | 说明 |
| --- | --- | --- | --- | --- |
| 公共 API | `api` | 117 | 5467 | 供插件使用的接口：爆炸物注册、导弹、发射器、无线电、动作系统 |
| 底层库 | `lib` | 171 | 16368 | 向量/旋转、NBT 保存、网络 Codex、无线电、雷达、能量、线程、动作 |
| 爆炸 | `content/blast` | 43 | 5994 | 各爆炸实现、线程化爆炸、红物质 |
| 方块/机器 | `content/blocks` | 71 | 7316 | 发射台网络、巡航发射器、雷达、EMP 塔、爆炸物方块、混凝土等 |
| 导弹 | `content/missile` | 41 | 4834 | 导弹实体、飞行逻辑、目标、SAM、区块外追踪模拟 |
| 其他内容 | `content/{cluster,cargo,items,actions,entity,gas,potion,radioactive}` | ~70 | ~9000 | 集束、降落伞/气球、工具物品、动作、实体、气体、药水 |
| 客户端 | `client` | 26 | 2362 | 渲染器、模型、粒子、声音 |
| 预制件 | `prefab` | 33 | 2742 | GUI 组件、库存、方块实体基类 |
| 配置/命令/联动/修复 | `config`、`command`、`mods`、`datafix` | 73 | 5288 | Forge 配置、命令、Mekanism/JEI、DataFixer |

## 3. 目标源码布局

Loom 拆分源集（`splitEnvironmentSourceSets()`），编译期强制客户端/服务端隔离：

```text
ICBM-Classic/
├── build.gradle / settings.gradle / gradle.properties / gradlew
├── legacy-1.12/                      # S0.02 移入的旧代码与旧构建文件，只读，S9.01 删除
├── src/
│   ├── main/java/icbm/classic/       # 双端代码（服务端逻辑、注册、网络载荷定义）
│   │   ├── ICBMClassic.java          # ModInitializer 入口
│   │   ├── ICBMConstants.java
│   │   ├── api/                      # 公共 API（保持包名，接口按 26.3 类型改签名）
│   │   ├── config/                   # @IcbmConfig 配置类 + ConfigManager
│   │   ├── content/                  # 方块、物品、实体、爆炸、导弹、机器
│   │   │   └── reg/                  # ModBlocks、ModItems、ModEntities、ModBlockEntities、ModMenus、
│   │   │                             # ModSounds、ModDataComponents、ModCreativeTabs、ExplosiveInit
│   │   ├── lib/                      # 底层库（energy 门面、network、radio、radar、saving、transform…）
│   │   ├── network/                  # Fabric 载荷类型与注册
│   │   └── command/                  # Brigadier 命令
│   ├── main/resources/
│   │   ├── fabric.mod.json
│   │   ├── icbmclassic.mixins.json
│   │   ├── assets/icbmclassic/       # 纹理、声音、手写模型（导弹 3D 模型等）、lang/en_us.json（手写部分）
│   │   └── data/icbmclassic/         # 手写配方、标签（大部分由 datagen 生成）
│   ├── main/generated/               # runDatagen 输出（提交进仓库）
│   ├── client/java/icbm/classic/client/   # ClientModInitializer、渲染器、Screen、客户端网络接收
│   ├── client/java/icbm/classic/datagen/  # DataGeneratorEntrypoint 与各 Provider（模型生成器只在客户端类路径）
│   ├── client/resources/icbmclassic.client.mixins.json
│   ├── gametest/java/icbm/classic/test/   # Fabric GameTest
│   ├── gametest/resources/fabric.mod.json # 测试模组 icbmclassic-test
│   ├── test/java/                    # JUnit 5 纯逻辑测试
│   └── i18n/ar_sa.source.json        # S8：未整形阿拉伯文源文件
├── scripts/                          # commit-msg 钩子、check.sh、check_lang.py、port_ledger.py
└── docs/
```

## 4. 入口（fabric.mod.json）

| 入口 | 类 | 源集 |
| --- | --- | --- |
| `main` | `icbm.classic.ICBMClassic` | main |
| `client` | `icbm.classic.client.ICBMClassicClient` | client |
| `fabric-datagen` | `icbm.classic.datagen.ICBMDataGenerator` | client |
| `fabric-gametest` | `icbm.classic.test.*GameTest`（测试模组） | gametest |

## 5. 初始化顺序（`ICBMClassic#onInitialize`，固定，不得调换）

1. `ConfigManager.loadAll()` —— 读取/生成 `config/icbmclassic/*.json`。注册需要配置值（例如是否启用某内容），所以最先执行。
2. `ICBMClassicAPI` 各 builder 注册表初始化（flight logic、target data、actions、conditions、causes、customizations 等）。
3. `ExplosiveInit.init()` —— 注册全部爆炸物数据（id、等级、工厂、引信时间），然后 `lock()`。**第三方插件必须在自己的 `onInitialize` 中、且在依赖声明 `"depends": {"icbmclassic": "*"}` 保证顺序的前提下注册；锁定后注册抛异常。**
4. `ModSounds` → `ModDataComponents` → `ModBlocks`（含平铺化爆炸物方块）→ `ModItems`（含方块物品与平铺化导弹/手雷/矿车/子弹药）→ `ModBlockEntities` → `ModEntities` → `ModMenus` → `ModCreativeTabs`。
5. `IcbmNetwork.registerPayloads()` —— 双向载荷类型注册（客户端接收器在 client 入口注册）。
6. 服务端事件：`ServerTickEvents.END_LEVEL_TICK`（无线电、雷达地图、导弹追踪、爆炸应用队列）、`ServerLevelEvents.UNLOAD`（清理每世界状态）、`ServerLifecycleEvents.SERVER_STOPPING`（停止爆炸线程）。
7. `CommandRegistrationCallback.EVENT` —— `/icbm` 命令。
8. （S6 起）`EnergyApi.register()`。
9. （S7 起）`EngagementRegistry` 事件挂接。

## 6. 线程模型

- 所有世界读写、实体更新、方块实体 tick、无线电分发、雷达扫描都在**服务端主线程**。
- 线程化爆炸（`lib/thread`、`content/blast/thread*`）：主线程在触发时拍摄所需区块的只读快照（`PalettedContainer` 复制或逐方块读入数组），工作线程计算受影响方块，结果入队；主线程在 `END_LEVEL_TICK` 每 tick 应用至多 `ConfigThread.BLOCKS_PER_TICK`（默认 2000）个修改。
- 客户端渲染只读取 RenderState，不在渲染中访问世界的可变状态（26.x 渲染架构要求：`extractRenderState` 复制 → `submit` 绘制）。

## 7. 每世界状态

| 状态 | 1.12 实现 | 26.3 实现 |
| --- | --- | --- |
| 无线电接收器表 | `RadioRegistry` 静态 `HashMap<dimId, RadioMap>` | `RadioRegistry` 静态 `Map<ResourceKey<Level>, RadioMap>`，世界卸载时清除；不持久化（接收器方块实体加载时重新登记） |
| 雷达地图 | `RadarRegistry` 静态按维度 | 同上，按 `ResourceKey<Level>` |
| 区块外导弹模拟 | `MissileTrackerHandler` 世界 NBT | `MissileTrackerData extends SavedData`，`SavedDataType` + Codec，存于 `ServerLevel#getDataStorage()` |
| 交战表（S7） | 无 | `EngagementRegistry`，内存，世界卸载清除 |

## 8. 不在本轮范围

- 1.12 存档迁移（ADR 0002）。
- REI/EMI/JEI、Jade 联动，Mekanism 联动（ADR 0006）。
- 游戏内配置界面。
- NeoForge 版本。
- 圣诞节怪物（1.12 中已注释掉注册，移植账本标记 DROPPED）。

# 0002：不迁移 1.12 存档，注册 ID 平铺化

- 状态：Accepted
- 日期：2026-10-10
- 决策参与者：项目维护者、规划 Agent
- 关联：ADR 0001

## 背景

1.12 使用 metadata（数值子类型）区分同一方块/物品的不同变体：爆炸物方块 `icbmclassic:explosives` 用方块实体存爆炸类型；导弹 `explosive_missile`、手雷 `grenade`、炸弹矿车 `bombcart` 用 `ItemStack` 的 damage 值存爆炸类型；`ingot`、`plate`、`circuit`、`wire`、`clump`、`concrete`、`radioactive` 用 metadata 区分材质。1.13 起 metadata 被移除；26.3 中物品附加数据应使用 DataComponent，方块变体应使用独立方块或方块状态属性。

1.12 世界无法被 26.3 直接打开后保留模组内容：原版 DataFixer 只升级原版数据，模组方块会丢失。为此编写迁移工具代价极高，且 1.12 服务器玩家通常不会把世界直接带到 26.3。

## 决定

**工程缺省**：

1. **不支持 1.12 存档迁移。** 不写 DataFixer，不保留 `icbm.classic.datafix` 包，不为旧 ID 写别名。发布说明必须写明“7.0.0 不兼容 6.x 世界”。
2. **平铺化**：每个变体一个注册 ID，命名全部小写 snake_case。完整表见 [registry-ids.md](../architecture/registry-ids.md)。
   - 爆炸物方块：`<explosive>_explosive`，例如 `icbmclassic:condensed_explosive`。
   - 爆炸导弹：`<explosive>_missile`；手雷：`<explosive>_grenade`；炸弹矿车：`<explosive>_bomb_cart`；子弹药：`<explosive>_bomblet`。
   - 只为在 1.12 对应内容注册表中启用的爆炸物生成该形态（启用列表抄自 `legacy-1.12` 的 `ExplosiveInit`，见 registry-ids.md）。
   - 材料：`steel_ingot`、`copper_ingot`、`steel_clump`、`steel_plate`、`iron_plate`、`basic_circuit`、`advanced_circuit`、`elite_circuit`、`copper_wire`、`gold_wire`。
   - 混凝土：`concrete`、`compact_concrete`、`reinforced_concrete`；放射性：`radioactive_dirt`、`radioactive_stone`。
3. 爆炸物与物品形态的对应关系存在代码注册表 `ExplosiveRegistry` 中，物品本身持有 `explosiveId` 字段（构造时传入），**不**用 DataComponent 存爆炸类型。可变的定制数据（如集束导弹子弹药数、爆炸规模覆盖）才用 DataComponent `icbmclassic:explosive_settings`。
4. 1.12 的爆炸物数值 ID（`newEx(0, "condensed", ...)` 中的 0）在 26.3 中不再出现在存档或网络中；只用 `Identifier`。

## 影响

- 物品与方块数量增多（约 100 个），全部由循环批量注册，模型/方块状态/战利品表/语言键全部由数据生成器（datagen）产生，禁止手写 100 份 JSON。
- 第三方插件通过 `ExplosiveRegistry` 注册的新爆炸物会自动获得平铺化方块与物品，前提是在 `onInitialize` 阶段、ICBM 注册冻结前注册（注册顺序见 overview.md §5）。
- 创造模式标签页按爆炸物等级排序展示。

## 备选方案

- **单个物品 + DataComponent 存爆炸类型 + 物品模型 `select`**：注册数少，但方块不能这样做（方块状态属性在注册时固定），且配方、标签、战利品表都要写组件谓词，弱 Agent 出错率高。拒绝。
- **编写 1.12 → 26.3 世界转换器**：成本高、收益低。拒绝；用户如有需求另立 ADR。

## 验证

未验证。S2.01（注册骨架）与 S2.06（数据生成）完成后，以 `/give` 全量 ID 的 GameTest 作为证据。

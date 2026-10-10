# S2 注册与资源

## 阶段说明

建立全部注册（方块、物品、方块实体类型占位、实体类型占位、菜单类型占位、声音、组件、创造标签），以及由数据生成器产出的资源。完成后：创造栏中能看到全部物品（机器方块此时可以是“无功能的普通方块”，S5 再接上方块实体与逻辑）。

**唯一 ID 来源**：[registry-ids.md](../architecture/registry-ids.md)。

**资源目录命名（26.x）**：纹理 `textures/block/`、`textures/item/`、`textures/entity/`、`textures/gui/`（1.12 为 `blocks`/`items`）；模型 `models/block/`、`models/item/`；物品模型定义 `items/`；数据包 `loot_table/`、`recipe/`、`tags/block/`、`tags/item/`、`damage_type/`（全部单数）。

所有 Gradle 命令前：`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`。

---

### S2.01 注册骨架

- 前置：S1.10
- 必读：porting-guide.md §2.2；overview.md §5；registry-ids.md 全文；`legacy-1.12/.../content/reg/**`
- 改动文件：新建 `src/main/java/icbm/classic/content/reg/{ModBlocks,ModItems,ModBlockEntities,ModEntities,ModMenus,ModSounds,ModDataComponents,ModCreativeTabs,ModAttachments,RegHelper}.java`（`ModAttachments` 若 S1.07 已建则只修改）；修改 `ICBMClassic.java`；账本（`BlockReg`→`MERGED → ModBlocks` 等）
- 目标：注册工具与各注册类骨架就位，按 overview §5 顺序调用。本步只注册**不依赖其他内容**的条目：DataComponent 类型。

**操作**

1. `RegHelper`：

   ```java
   public final class RegHelper {
       public static ResourceKey<Block> blockKey(String id) { return ResourceKey.create(Registries.BLOCK, ICBMConstants.id(id)); }
       public static ResourceKey<Item>  itemKey(String id)  { return ResourceKey.create(Registries.ITEM,  ICBMConstants.id(id)); }

       /** 注册方块（Properties 已自动 setId）。 */
       public static <B extends Block> B block(String id, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties props) {
           ResourceKey<Block> key = blockKey(id);
           return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
       }
       /** 注册方块对应的 BlockItem。 */
       public static BlockItem blockItem(Block block) {
           ResourceKey<Item> key = itemKey(BuiltInRegistries.BLOCK.getKey(block).getPath());
           return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
       }
       public static <I extends Item> I item(String id, Function<Item.Properties, I> factory, Item.Properties props) {
           ResourceKey<Item> key = itemKey(id);
           return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
       }
   }
   ```

   `useBlockDescriptionPrefix`、`Registry.register(Registry, ResourceKey, T)` 用 javap 核对。
2. 每个 `ModXxx` 类：`public static void register()`，字段 `public static final`（或 `public static` 在 `register()` 中赋值——方块与物品因依赖顺序，统一采用“在 `register()` 中赋值”的写法，字段非 final）。
3. `ModDataComponents.register()`：注册 registry-ids.md §6 中除电池外的组件。`ExplosiveSettings` 先实现为空 record（`Codec` 用 `MapCodec.unit` 或 `Codec.unit(new ExplosiveSettings())`），S3.07/S4.06 扩展字段时保持向后兼容即可（dev 期间无需兼容）。`GpsData(Optional<GlobalPos> pos)`：`RecordCodecBuilder` + `GlobalPos.CODEC.optionalFieldOf("pos")`；`StreamCodec` 用 `ByteBufCodecs.optional(GlobalPos.STREAM_CODEC)`。
4. `ICBMClassic.onInitialize` 按 overview §5 顺序调用：`ConfigManager.loadAll()`→（API 注册表初始化，S1.10 已有则保留）→`ExplosiveInit.init()`（S2.02 实现，现在先放空方法）→`ModSounds`→`ModDataComponents`→`ModBlocks`→`ModItems`→`ModBlockEntities`→`ModEntities`→`ModMenus`→`ModCreativeTabs`→`IcbmNetwork.registerPayloads()`→事件→命令。
5. GameTest `RegistrySmokeGameTest`：断言 `BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(ICBMConstants.id("gps_data"))`。

**提交信息**：`feat(reg): 建立 26.3 注册骨架与数据组件`

---

### S2.02 `ExplosiveRegistry` 与平铺化形态

- 前置：S2.01
- 必读：ADR 0002；registry-ids.md §1–§3；`legacy-1.12/.../content/reg/ExplosiveInit.java`、`lib/explosive/**`（8）、`api/reg/**`
- 改动文件：`lib/explosive/**`、`api/reg/**`（若 S1.10 未完成）、`content/reg/ExplosiveInit.java`、新建 `content/reg/ExplosiveForms.java`；GameTest；账本
- 目标：爆炸物数据注册完成并锁定；按形态规则得到“需要生成哪些方块/物品”的清单（本步只产出清单，不注册方块物品）。

**操作**

1. `ExplosiveRegistry`：去掉数值 ID（`forceID`、`getExplosivesIDs()` 返回 int 的方法删除或改为返回 `Identifier`）。`register(Identifier, EnumTier, IBlastFactory)` 保留。`lock()` 后注册抛 `IllegalStateException("ICBM explosive registry is locked")`。
2. `ExplosiveInit.init()`：逐条照抄 1.12 `newEx(...)` 调用，名称改为 registry-ids.md §1 的 ID（`redMatter`→`redmatter`）；删除 `hypersonic`、`missile`；保留 `rot`、`mutation`（等级 `NONE`）；引信时间 `setFuseSupplier`、`setFuseTickListener`、`setActivationListener`、`setInteractionListener` 全部照抄（lambda 中的 1.12 类型换新；回调里引用的内容类若尚未移植，保留调用但改为调用 `// TODO(S3.09)` 占位的静态方法，并在账本说明）。
3. 内容形态注册表（`EX_BLOCK_REGISTRY`、`EX_MISSILE_REGISTRY`、`EX_GRENADE_REGISTRY`、`EX_MINECART_REGISTRY`）保留；`enableContent` 规则照抄 `newEx`（等级≠NONE → 方块/导弹/矿车；等级=ONE → 手雷；子弹药跟随手雷）。
4. `ExplosiveForms`：

   ```java
   public enum ExplosiveForm {
       BLOCK("explosive"), MISSILE("missile"), GRENADE("grenade"), BOMB_CART("bomb_cart"), BOMBLET("bomblet");
       public final String suffix;
       public String idFor(Identifier explosiveId) { return explosiveId.getPath() + "_" + suffix; }
   }
   /** 返回某形态启用的爆炸物 ID，按等级、再按 1.12 数值 ID 顺序（colors、smoke 排在各自等级末尾）排序。 */
   public static List<Identifier> enabled(ExplosiveForm form)
   ```

   排序用一个写死的列表 `ORDER = List.of("condensed","shrapnel",...,"redmatter")`，与 registry-ids.md §1 表格顺序一致。
5. GameTest：`enabled(BLOCK).size()==23`、`enabled(GRENADE).size()==10`、`enabled(BOMBLET).size()==10`、`MISSILE` 包含 `nuclear`、`GRENADE` 不包含 `nuclear`；锁定后注册抛异常。

**提交信息**：`feat(explosive): 爆炸物注册表改为标识符并生成平铺化形态清单`

---

### S2.03 简单方块

- 前置：S2.01
- 必读：registry-ids.md §2；`legacy-1.12/.../content/blocks/{BlockConcrete,BlockReinforcedGlass,BlockSpikes,BlockGlassButton,BlockGlassPressurePlate}.java`
- 改动文件：上述方块的新类（每个变体是同一类的不同实例，或拆子类——按下述）；`ModBlocks`、`ModItems`；账本
- 目标：混凝土 ×3、加固玻璃、尖刺 ×3、玻璃按钮、玻璃压力板可放置、可挖掘（掉落在 S2.07）。

**操作**

1. 混凝土：一个 `Block` 类即可（原版 `Block`），三个实例，硬度/抗性分别照抄 1.12（`setHardness(10)` 与 `getExplosionResistance` 中按 meta 的分支值）。`.requiresCorrectToolForDrops()`。
2. 加固玻璃：继承 `TransparentBlock`（原版透明方块类名用 javap 确认，可能是 `TransparentBlock` 或 `HalfTransparentBlock`），`.noOcclusion()`，抗性照抄。
3. 尖刺：`BlockSpikes` 的三种类型→一个类 `BlockSpikes(SpikeType type, Properties)`，`SpikeType { NORMAL, POISON, FIRE }`；`entityInside` 中照抄 1.12 `onEntityCollision` 的伤害/中毒/着火逻辑（伤害源用 `level.damageSources().cactus()` 或 1.12 原逻辑对应的源，javap 核对）。碰撞箱与 1.12 一致。
4. 玻璃按钮/压力板：继承原版 `ButtonBlock` / `PressurePlateBlock`，构造参数（`BlockSetType`、按下时长）用 javap 核对；1.12 用的是石质还是木质行为，以旧代码为准。
5. 放射性方块（`radioactive_dirt`、`radioactive_stone`）**不在本步**（S3.08）。
6. 每个方块用 `RegHelper.blockItem` 注册物品。

**验证**：GameTest：逐个放置并断言存在；尖刺上放置一只僵尸，若干 tick 后其生命值下降（`helper.spawn(EntityType.ZOMBIE, pos)`）。

**提交信息**：`feat(blocks): 移植混凝土玻璃与尖刺等基础方块`

---

### S2.04 材料与简单物品

- 前置：S2.01
- 必读：registry-ids.md §3；`legacy-1.12/.../content/items/{ItemCrafting,ItemAntidote}.java`、`prefab/item/**`、`content/reg/ItemReg.java`
- 改动文件：`ModItems`、`ItemAntidote`、`prefab/item/**`（`ItemBase`/`ItemICBMBase` 改为普通 `Item` 子类或删除）；账本
- 目标：材料（钢锭等 10 个）、`sulfur_dust`、`saltpeter`、`saltpeter_ball`、`poison_powder`、`antidote`、`empty_bomblet`、`missile_module` 注册完成。

**操作**

1. `ItemCrafting` 删除（`MERGED → ModItems 普通 Item`）；10 个材料 ID 各注册一个 `Item`。
2. `ItemAntidote`：照抄 1.12 `onItemUseFinish` 逻辑 → 26.3 食物/可消耗组件（`Item.Properties#food(FoodProperties, Consumable)`，javap 已核实该方法存在），效果“清除 ICBM 毒气效果”的处理在 `finishUsingItem` 中实现（效果类型在 S3.05 注册；本步先清除原版 `POISON`、`WITHER` 与 1.12 代码中列出的原版效果，ICBM 自有效果加 `// TODO(S3.05)`）。
3. 1.12 `ItemReg` 中受 `ConfigItems.ENABLE_*` 控制的物品：保留开关语义——开关为 false 时**仍注册**（26.3 不能条件注册而不破坏数据包），但不加入创造标签、配方在 datagen 中加 `fabric:load_conditions`（若太复杂，配方照常生成，在证据中说明）。

**提交信息**：`feat(items): 移植材料与简单物品`

---

### S2.05 创造模式标签页

- 前置：S2.02、S2.03、S2.04
- 必读：`legacy-1.12/.../client/ICBMCreativeTab.java`（排序逻辑 `orderItem`）
- 改动文件：`ModCreativeTabs`；（同时在本步注册平铺化爆炸物的**物品**占位）`ModItems`、`ModBlocks`；新建 `content/blocks/explosive/ExplosiveBlock.java`（占位：普通方块）、`content/items/ExplosiveFormItem.java`（占位：普通物品，持有 `explosiveId`、`form` 字段）
- 目标：一个创造标签页 `icbmclassic:main`，按 1.12 顺序展示全部物品，包括 23+23+10+23+10 个平铺化条目。

**操作**

1. 平铺化注册（循环）：

   ```java
   for (Identifier ex : ExplosiveForms.enabled(ExplosiveForm.BLOCK)) {
       ExplosiveBlock b = RegHelper.block(ExplosiveForm.BLOCK.idFor(ex), p -> new ExplosiveBlock(ex, p), BlockBehaviour.Properties.of().strength(2f, 0f));
       EXPLOSIVE_BLOCKS.put(ex, b);
       RegHelper.blockItem(b);
   }
   // MISSILE / GRENADE / BOMB_CART / BOMBLET 同理注册 ExplosiveFormItem，存入 Map<Identifier, Item>
   ```

   S3.09、S4.02、S4.05、S4.06 会把占位类替换为真实类（保持构造签名 `(Identifier explosiveId, Properties)`）。
2. `ModCreativeTabs`：`FabricCreativeModeTab.builder()`（javap 核对 `net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab` 的 builder 方法；或原版 `CreativeModeTab.builder(...)`）`.title(Component.translatable("itemGroup.icbmclassic.main"))`、`.icon(() -> new ItemStack(<nuclear_missile 物品>))`、`.displayItems((params, output) -> ...)`。
3. 顺序照抄 1.12 `ICBMCreativeTab`：先机器方块，再工具，再导弹（按 ExplosiveForms 顺序），再手雷、矿车、炸药方块、材料。机器方块此时尚未注册（S5），在 `displayItems` 中**按 ID 查注册表**，不存在则跳过——这样 S5 注册后自动出现。

**验证**：GameTest：`BuiltInRegistries.ITEM` 中 `icbmclassic` 命名空间物品数 ≥ 89+材料等（打印实际数量写入证据）；`/give @p icbmclassic:nuclear_missile` 由 GameTest 调用 `helper.getLevel().getServer().getCommands()` 执行不报错（或直接 `new ItemStack(item)` 断言非空）。

**提交信息**：`feat(reg): 注册平铺化爆炸物条目与创造模式标签页`

---

### S2.06 datagen：方块状态、模型、物品模型定义

- 前置：S2.05
- 必读：references.md §2“数据生成”；`legacy-1.12/src/main/resources/assets/icbmclassic/{blockstates,models,textures}/**`
- 改动文件：`build.gradle`（`fabricApi { configureDataGeneration { client = true } }`）；`fabric.mod.json`（`fabric-datagen` 入口）；新建 `src/client/java/icbm/classic/datagen/{ICBMDataGenerator,ICBMModelProvider}.java`；复制纹理到 `src/main/resources/assets/icbmclassic/textures/{block,item,entity,gui,models}/`；手写复杂模型到 `src/main/resources/assets/icbmclassic/models/`；生成物 `src/main/generated/**`
- 目标：所有已注册方块和物品在客户端有模型（不再是紫黑方块）。

**操作**

1. 纹理迁移：`legacy-1.12/.../textures/blocks/*` → `textures/block/`，`textures/items/*` → `textures/item/`，其余目录同名复制（`.mcmeta` 一并复制）。文件名若含大写或与新 ID 不匹配，**改名为新 ID**，并在证据中列出“旧文件名 → 新文件名”对照表。用脚本做（`.dev/tmp/` 下的一次性 Python 脚本，不提交）。
2. 简单方块（混凝土、玻璃、放射性方块等）：`ICBMModelProvider.generateBlockStateModels` 中 `createTrivialCube`；按钮/压力板用原版生成器对应方法（javap 核对 `BlockModelGenerators` 中 button/pressure plate 方法名）。
3. 爆炸物方块（23 个）：1.12 的 `blockstates/explosives.json` 中每个爆炸物有 top/side/bottom 纹理。生成 `cube_bottom_top` 风格模型，纹理路径 `icbmclassic:block/explosives/<ex>_top|_side|_bottom`（以复制后的实际文件名为准）。
4. 机器方块（发射台、雷达、EMP 塔等）：1.12 JSON 模型复制到 `models/block/`，修改其中纹理引用 `icbmclassic:blocks/...` → `icbmclassic:block/...`；方块状态 JSON 中的属性在 S5 定义前，先生成“单一变体”指向主模型（S5 修改）。
5. 物品：普通物品 `generateFlatItem`；平铺化导弹/手雷/矿车/子弹药按 1.12 `models/item/missiles/<ex>.json` 等复制的纹理生成 flat 物品模型；3D 导弹模型（若 1.12 物品模型是 elements 模型）复制为手写模型，物品模型定义指向它。
6. 26.x 的物品模型定义文件（`items/<id>.json`）由 `ItemModelGenerators` 自动产出；手写模型的物品需要手写 `items/<id>.json`：`{"model": {"type": "minecraft:model", "model": "icbmclassic:item/<id>"}}`。
7. 运行 `./gradlew runDatagen`，提交 `src/main/generated/`。

**验证**：`./gradlew runDatagen && ./gradlew build`；脚本检查：对每个已注册物品 ID，存在 `src/main/(generated|resources)/assets/icbmclassic/items/<id>.json`（用一个 GameTest 遍历注册表并用 `ClassLoader.getResource` 检查文件存在——GameTest 运行在服务端，但资源仍在 jar 中可读）。客户端外观为人工门禁 S5.12。

**提交信息**：`feat(assets): 迁移纹理并生成方块状态与物品模型`

---

### S2.07 datagen：战利品表与标签

- 前置：S2.06
- 改动文件：新建 `src/client/java/icbm/classic/datagen/{ICBMBlockLootProvider,ICBMBlockTagProvider,ICBMItemTagProvider}.java`；生成物
- 目标：每个方块被挖掘后掉落自身（与 1.12 `getDrops` 一致的特殊情况照抄）；挖掘工具标签；`c:` 公约标签。

**操作**

1. 掉落：默认 `dropSelf`。1.12 中有特殊掉落（例如加固玻璃是否掉落、放射性方块掉落什么）：`grep -rn "getDrops\|getItemDropped\|quantityDropped" legacy-1.12/src/main/java/icbm/classic/content` 逐个照抄。
2. 方块标签：全部金属/石质方块 → `minecraft:mineable/pickaxe`；混凝土、机器 → 另加 `minecraft:needs_iron_tool`（1.12 `setHarvestLevel("pickaxe", 2)` 等，以旧代码为准；没写 harvest level 的不加 needs 标签）。
3. 物品标签：registry-ids.md §3 列出的 `c:` 标签。

**提交信息**：`feat(data): 生成方块战利品表与挖掘和公约标签`

---

### S2.08 配方

- 前置：S2.07
- 必读：porting-guide.md §9；`legacy-1.12/src/main/resources/assets/icbmclassic/recipes/**`（23 个 JSON 及子目录）、`legacy-1.12/.../RecipeGeneration.java`、`lib/recipe/**`
- 改动文件：新建 `src/client/java/icbm/classic/datagen/ICBMRecipeProvider.java`；（若有特殊配方）`content/recipe/**`；生成物
- 目标：1.12 全部配方在 26.3 存在，输入改用标签。

**操作**

1. 列出 1.12 全部配方：JSON 文件 + `RecipeGeneration.java` 中代码注册的配方。在证据中写成表：`配方名 | 类型（有序/无序/熔炼/特殊）| 输出 | 状态`。
2. 有序/无序/熔炼：`ICBMRecipeProvider` 中用原版构建器（`ShapedRecipeBuilder`/`ShapelessRecipeBuilder`/`SimpleCookingRecipeBuilder`，javap 核对 `FabricRecipeProvider` 的 `getRecipeProvider` 写法——26.x 的 datagen 配方 API 与 1.21.1 不同，以 Fabric 文档 data-generation/recipes 页面为准）。OreDictionary 名（`ingotSteel` 等）→ `c:` 标签；原版物品直接引用。
3. 爆炸物/导弹/手雷/矿车的平铺化配方：1.12 配方中输出 `explosive_missile` + `"data": N` → 输出对应 `<ex>_missile`。
4. 特殊配方（`_factories.json` 声明的类型、`RecipeHeldItemMissile`、`RecipeHeldItemMissileDecraft`、集束导弹配方）：**本步只列出，不实现**，在证据表中标“→ S4.06”。
5. 1.12 中受配置开关控制的配方：见 S2.04 第 3 条。

**验证**：`./gradlew runDatagen && ./gradlew build`；GameTest：通过 `level.getServer().getRecipeManager()`（javap 核对 26.x 的配方访问方式）断言至少一个代表性配方存在（例如 `icbmclassic:condensed_explosive`）。

**提交信息**：`feat(data): 迁移配方并改用公约标签`

---

### S2.09 声音

- 前置：S2.01
- 必读：`legacy-1.12/src/main/resources/assets/icbmclassic/sounds.json`、`legacy-1.12/.../client/ICBMSounds.java`
- 改动文件：`ModSounds`；`src/main/resources/assets/icbmclassic/sounds.json`；`sounds/**` 复制；registry-ids.md §7 补充清单（同一提交）
- 目标：全部声音事件注册，`.ogg` 可被引用。

**操作**

1. 复制 `sounds/**`。
2. 把 `ICBMSounds` 枚举改为 `ModSounds` 中的静态 `SoundEvent` 字段：`SoundEvent.createVariableRangeEvent(id)` + `Registry.register(BuiltInRegistries.SOUND_EVENT, id, ev)`。ID 转小写 snake_case；`sounds.json` 键同步修改。
3. `sounds.json` 中每个条目加 `"subtitle": "subtitles.icbmclassic.<name>"`，并把这些键加入 `en_us.json`（英文字幕自拟，简短描述）。

**提交信息**：`feat(sounds): 注册声音事件与字幕`

---

### S2.10 `en_us` 迁移、`mergeLang`、`check_lang.py` 骨架

- 前置：S2.06
- 必读：i18n.md §2、§4、§8；`legacy-1.12/src/main/resources/assets/icbmclassic/lang/en_US.lang`
- 改动文件：新建 `scripts/migrate_lang.py`、`scripts/check_lang.py`、`src/client/java/icbm/classic/datagen/ICBMLanguageProvider.java`、`src/main/resources/assets/icbmclassic/lang/en_us.json`；修改 `build.gradle`（`mergeLang`）
- 目标：英文名称全部存在；旧英文文本迁移到新键；校验脚本可运行。

**操作**

1. `ICBMLanguageProvider`（datagen）：为全部方块/物品/实体生成英文名。平铺化条目：`"<Explosive English> <Form English>"`，爆炸物英文名取 1.12 `en_US.lang` 中 `tile.icbmclassic:explosives.<ex>.name` 一类键的值去掉形态后缀（以旧文件实际键为准，在证据中列出取值规则）；形态英文：Explosive / Missile / Grenade / Bomb Cart / Bomblet。创造标签 `itemGroup.icbmclassic.main` = `ICBM-Classic`。
2. `scripts/migrate_lang.py`：读取 `.lang`（忽略 `#` 注释与空行，按第一个 `=` 分割），按**映射规则**输出新键：
   - 物品/方块名称键（`tile.*.name`、`item.*.name`）→ 不迁移（由 datagen 生成）。
   - `command.icbmclassic:icbm.X` → `icbmclassic.command.X`
   - `gui.icbmclassic:X` → `icbmclassic.gui.X`
   - `config.icbmclassic:*` → 丢弃
   - 其他前缀：输出到 `unmapped` 列表（脚本打印），由执行者在脚本的 `MANUAL` 字典中逐个指定新键，直到 `unmapped` 为空。
   - 值中的 `§x` 颜色码删除；`%n$s` 保留。
   - 用法：`python3 -I scripts/migrate_lang.py legacy-1.12/.../en_US.lang > .dev/tmp/en_us.migrated.json`，然后人工（即执行 Agent）并入 `en_us.json`。
   - **代码中引用的旧键**（`grep -rhoE '"[a-z]+\.icbmclassic[:.][A-Za-z0-9_.]+"' src/`）同步改为新键——只改已移植的代码；脚本输出“旧键→新键”映射 JSON 存入 `scripts/lang_key_map.json`（提交），后续移植步骤查此表替换。
3. `mergeLang`（`build.gradle`）：读取 `src/main/generated/assets/icbmclassic/lang/en_us.json` 与 `src/main/resources/assets/icbmclassic/lang/en_us.json`，合并（同键不同值 → `throw new GradleException("lang key conflict: " + key)`），写到 `build/generated/lang/assets/icbmclassic/lang/en_us.json`；`processResources` 排除两个源中的 `en_us.json`，并 `from("build/generated/lang")`，`dependsOn mergeLang`。用 `groovy.json.JsonSlurper`/`JsonOutput`。
4. `check_lang.py`：实现 i18n.md §8 的第 1、2、3、5、6、7 项；第 4 项默认只警告，`--strict` 时报错；`--dump-en`、`--fix-order`、`--report` 选项。第 7 项正则：`Component\.translatable\(\s*"([^"]+)"`。
5. 把 `check_lang.py` 接入 `scripts/check.sh`（S0.05 已预留）。

**验证**：`./gradlew runDatagen build`；`python3 -I scripts/check_lang.py; echo $?` → 0；`unzip -p build/libs/*-dev.jar assets/icbmclassic/lang/en_us.json | python3 -I -c 'import json,sys;print(len(json.load(sys.stdin)))'` 打印键数。

**提交信息**：`feat(i18n): 迁移英文语言文件并增加合并与校验工具`

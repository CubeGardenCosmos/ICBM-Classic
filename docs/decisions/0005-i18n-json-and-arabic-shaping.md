# 0005：国际化——JSON 语言文件、必需语言与阿拉伯文预整形

- 状态：Accepted
- 日期：2026-10-10
- 决策参与者：项目维护者、规划 Agent
- 关联需求：[需求记录](../meetings/2026-10-10-upgrade-requirements.md) 第 1 条；详细规则见 [i18n.md](../architecture/i18n.md)

## 背景

1.12 使用 `.lang`（`key=value`）文件，键名大小写混用（如 `config.icbmclassic:main.title`、`tile.icbmclassic:explosives.condensed.name`）。1.13 起 Minecraft 只读取 `assets/<ns>/lang/<小写语言码>.json`。平铺化（ADR 0002）后物品/方块的翻译键也随 ID 改变。

26.3 客户端在 `FormattedBidiReorder` 中调用 ICU `Bidi` 做从右到左重排，但**没有调用 `ArabicShaping`**（2026-10-10 `javap -c` 核实），因此阿拉伯字母在游戏中很可能以“孤立形式”显示、不连写。

## 决定

**用户要求**：支持中文、法文、德文、日文、阿拉伯文；在所有其他升级之后执行。

**工程缺省**：

1. 必需语言（每个键都必须翻译）：`en_us`（源）、`zh_cn`、`fr_fr`、`de_de`、`ja_jp`、`ar_sa`。
2. 旧语言 `es_es`、`ko_kr`、`pt_br`、`ru_ru`：机械转换并迁移到新键后保留，缺失键回退英文，不保证完整（待用户确认，见需求记录“未决事项”）。
3. 翻译键使用原版风格：`block.icbmclassic.<id>`、`item.icbmclassic.<id>`、`entity.icbmclassic.<id>`、`itemGroup.icbmclassic.main`；其他键统一前缀 `icbmclassic.`，全部小写 + 点号 + 下划线（详见 i18n.md §2）。
4. 方块/物品/实体名称键由数据生成器写入 `en_us.json`；其余键手写在 `src/main/resources/assets/icbmclassic/lang/en_us.json` 中维护（datagen 输出与手写文件的合并规则见 i18n.md §4）。
5. 阿拉伯文：维护人员编辑**未整形源文件** `src/i18n/ar_sa.source.json`（标准阿拉伯字母），构建任务 `shapeArabic` 用 ICU4J（与 26.3 同版本 `78.3`）的 `ArabicShaping` 转为呈现形式（U+FB50–U+FDFF、U+FE70–U+FEFF）后写入 `assets/icbmclassic/lang/ar_sa.json`。是否在游戏中显示正确属于**人工门禁**；若人工验证发现原版已能正确连写，则新建 ADR 取消预整形。
6. 新增校验脚本 `scripts/check_lang.py`：检查所有语言文件 JSON 合法、键集合 ⊆ `en_us`、必需语言键集合 = `en_us`、格式化占位符（`%s`、`%1$s`、`%d`）数量与位置一致。CI（`scripts/check.sh`）调用它。
7. 术语表（i18n.md §6）是翻译的强制约束。

## 影响

- 所有 `Component.literal("英文")` 必须改为 `Component.translatable(key)`（S8.02）。
- `ar_sa.json` 是生成文件，禁止手改；修改阿拉伯文只改源文件。
- 中文旧翻译（322 条）可作为参考，但要迁移到新键并按术语表复核。

## 备选方案

- **运行时 Mixin 给 `FormattedBidiReorder` 加 `ArabicShaping`**：影响全局文本渲染，与其他模组冲突风险高。拒绝（可作为后续范围单独评估）。
- **不处理阿拉伯文连写**：显示效果差。拒绝。

## 验证

- `ArabicShaping` 未被 26.3 调用：已核实（`javap -c`）。
- 预整形后的实际显示效果：未验证（人工门禁 S8.12）。

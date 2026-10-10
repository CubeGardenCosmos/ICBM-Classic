# S8 国际化

## 阶段说明

最后执行的功能阶段：所有面向玩家的文字使用翻译键，`en_us` 完整，必需语言（zh_cn、fr_fr、de_de、ja_jp、ar_sa）100% 覆盖，阿拉伯文预整形。

**设计依据**：[i18n.md](../architecture/i18n.md)、ADR 0005。术语表（i18n.md §6）是强制约束。

**翻译步骤（S8.05–S8.09）的通用规则**：

1. 输入：`python3 -I scripts/check_lang.py --dump-en > .dev/tmp/en_full.json`（合并后的英文全集）。
2. 已有旧翻译（zh_cn、de_de 有 1.12 文件）：先用 `scripts/migrate_lang.py` + `scripts/lang_key_map.json` 把旧键迁移到新键，作为初稿；旧译文与术语表冲突时以术语表为准。
3. 逐键翻译。**必须**：保留占位符集合（可用 `%1$s` 调整语序）；不加 `§`；不翻译专有名词 “ICBM”、“FE”、“EMP”（各语言术语表另有规定的除外）；物品名称用名词短语，不加句号；消息句子按该语言习惯加标点。
4. 平铺化物品名（23 导弹、23 炸药、23 矿车、10 手雷、10 子弹药）：按“爆炸物译名 + 形态译名”在该语言中的自然语序组合（中文“凝聚导弹”、日文“凝縮ミサイル”、法文“Missile condensé”、德文“Kondensrakete”/“Kondensierte Rakete”——以 S8.04 术语表为准，同一语言内必须一致）。
5. 输出文件按键排序：`python3 -I scripts/check_lang.py --fix-order`。
6. 自检：`python3 -I scripts/check_lang.py --report`，该语言缺失 0、多余 0、占位符不一致 0。
7. 证据中附：本语言的“抽样 30 条”（随机，`random.seed(本步骤编号)`）英文-译文对照表，供 S8.12 审校者快速检查。
8. 机器（Agent）翻译完成后状态为“待母语审校”，不得在任何文档写“已支持某语言”。

所有 Gradle 命令前：`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`。

---

### S8.01 翻译键审计

- 前置：S7.10
- 必读：i18n.md §2、§7
- 改动文件：新建 `scripts/audit_i18n.py`；证据（审计报告）
- 目标：得到“代码中所有面向玩家的文字”清单。

**操作**：`audit_i18n.py` 扫描 `src/main/java`、`src/client/java`：

1. `Component.literal("...")` 中含字母的字面量（排除仅数字/符号/格式化坐标的调用）。
2. `Component.translatable("...")` 的键是否存在于合并后的 `en_us`。
3. 字符串拼接构造的键（`"icbmclassic." + x`）：列出位置，人工确认运行时键集合（例如爆炸物名 `icbmclassic.explosive.<id>`），把展开后的全部键写进报告并检查存在。
4. 1.12 遗留的旧格式键（含 `:`，如 `gui.icbmclassic:radar.name`）。
5. `LanguageUtility`、`I18n.translateToLocal` 残留。

报告格式：`文件:行 | 类别 | 内容 | 建议键`。只审计，不修改代码。

**提交信息**：`chore(i18n): 增加翻译键审计脚本与报告`

---

### S8.02 硬编码文本清理

- 前置：S8.01
- 改动文件：审计报告中列出的源文件；`en_us.json`；`scripts/lang_key_map.json`
- 目标：审计报告第 1、3、4、5 类清零。

**操作**：逐条改为 `Component.translatable(新键, 参数)`；新键按 i18n.md §2 命名并加入 `en_us.json`（英文沿用原字面量）；旧格式键改名并更新映射表。日志与异常信息保持英文、不改。每改一个文件跑一次 `./gradlew compileJava compileClientJava`。

**验证**：重新运行 `audit_i18n.py`，第 1、3、4、5 类为 0；`./gradlew build`。

**提交信息**：`refactor(i18n): 面向玩家的文本全部改用翻译键`

---

### S8.03 `en_us` 补全与严格校验

- 前置：S8.02
- 改动文件：`en_us.json`；`scripts/check_lang.py`（加入 `--strict` 到 `scripts/check.sh` 的调用仅在 S8.11 后；本步只保证英文自身严格无误）
- 目标：英文全集完整、措辞一致。

**操作**：`check_lang.py` 第 2、5、6、7 项全过；检查英文措辞：同一概念统一用术语表英文列（如统一 “Radar Station” 而非 “Radar”）；修正 1.12 英文中的拼写错误（列入证据）。

**提交信息**：`docs(i18n): 补全并统一英文语言文件`

---

### S8.04 术语表补全

- 前置：S8.03
- 改动文件：`docs/architecture/i18n.md` §6
- 目标：把 25 个爆炸物名称、5 个形态名、所有机器 GUI 中反复出现的名词（见 en_us 中出现 ≥ 3 次的名词）补入术语表 5 种语言。

**操作**：中文优先取 1.12 `zh_cn.lang` 已有译名（`legacy-1.12/src/main/resources/assets/icbmclassic/lang/zh_cn.lang`），德文优先取 `de_DE.lang`；法文、日文、阿拉伯文新译。爆炸物名尽量用该语言中对应的军事/化学术语（例如 thermobaric = 温压 / thermobarique / thermobarisch / サーモバリック / حراري ضغطي）。

**提交信息**：`docs(i18n): 补全多语言术语表`

---

### S8.05 zh_cn

- 前置：S8.04
- 改动文件：`src/main/resources/assets/icbmclassic/lang/zh_cn.json`
- 目标：简体中文 100%。

**附加规则**：中文标点用全角（，。：；！？），但占位符与英文数字之间不加空格；“FE”“EMP”保留英文；物品名不加引号。

**提交信息**：`feat(i18n): 简体中文翻译`

---

### S8.06 fr_fr

- 前置：S8.04
- 改动文件：`lang/fr_fr.json`
- **附加规则**：冒号、问号、感叹号前加不换行空格（U+00A0）；形容词后置（« Missile nucléaire »）；用 vous 语气。

**提交信息**：`feat(i18n): 法语翻译`

---

### S8.07 de_de

- 前置：S8.04
- 改动文件：`lang/de_de.json`
- **附加规则**：名词首字母大写；复合词连写；用 Sie 语气；注意长复合词在 GUI 按钮中可能溢出——超过 20 个字符的按钮文本在证据中列出，供 S8.12 检查。

**提交信息**：`feat(i18n): 德语翻译`

---

### S8.08 ja_jp

- 前置：S8.04
- 改动文件：`lang/ja_jp.json`
- **附加规则**：外来语用片假名；句末用「。」；不在词间加空格；占位符前后不加空格。

**提交信息**：`feat(i18n): 日语翻译`

---

### S8.09 ar_sa 源文件与 `shapeArabic`

- 前置：S8.04
- 必读：i18n.md §5；compatibility/arabic-rendering.md
- 改动文件：新建 `src/i18n/ar_sa.source.json`、`src/i18n/README.md`；`build.gradle`（`shapeArabic` 任务，`buildscript` classpath `com.ibm.icu:icu4j:78.3`）；生成 `src/main/generated/assets/icbmclassic/lang/ar_sa.json`；`scripts/check_lang.py`（ar_sa 同时检查源文件）
- 目标：阿拉伯文翻译完成并预整形。

**操作**

1. 翻译写入源文件（逻辑顺序，标准阿拉伯字母 U+0600–U+06FF）。**附加规则**：数字用西方阿拉伯数字（0–9），与原版 ar_sa 一致；占位符保持 ASCII。
2. `shapeArabic` 任务：读源 JSON → 对每个值 `new ArabicShaping(ArabicShaping.LETTERS_SHAPE | ArabicShaping.LENGTH_GROW_SHRINK).shape(value)` → 写生成文件（UTF-8，键排序，2 空格缩进）→ `processResources` 依赖它（生成目录本就在资源中，注意不要与 `runDatagen` 的输出冲突：`runDatagen` 若清空 `src/main/generated`，则在 `runDatagen` 之后自动执行 `shapeArabic`——`tasks.named('runDatagen') { finalizedBy 'shapeArabic' }`）。
3. JUnit（在 `src/test`，依赖 `testImplementation "com.ibm.icu:icu4j:78.3"`）：对“صاروخ”整形后不含 U+0600–U+06FF 范围的基本字母（全部变为呈现形式）；含 `%s` 的字符串整形后 `%s` 仍在。
4. `check_lang.py`：`ar_sa.source.json` 与生成的 `ar_sa.json` 键集合相同；生成文件中不应出现基本阿拉伯字母（说明忘了整形）。

**提交信息**：`feat(i18n): 阿拉伯语翻译与字形预整形`

---

### S8.10 旧语言迁移

- 前置：S8.03
- 改动文件：`lang/es_es.json`、`ko_kr.json`、`pt_br.json`、`ru_ru.json`
- 目标：1.12 旧翻译迁移到新键并保留（不补译）。

**操作**：`migrate_lang.py` + `lang_key_map.json`；平铺化物品名：若旧文件有该爆炸物与形态的旧名称键，按旧值组合；没有的键直接缺省（游戏回退英文）。`check_lang.py` 对这些语言只做非严格检查（多余键仍是错误）。

**提交信息**：`feat(i18n): 迁移旧语言文件到新键`

---

### S8.11 严格校验通过

- 前置：S8.05、S8.06、S8.07、S8.08、S8.09、S8.10
- 改动文件：`scripts/check.sh`（`check_lang.py --strict`）
- 目标：必需语言 100%、全部检查通过。

**验证**：`scripts/check.sh` 退出码 0；`check_lang.py --report` 输出贴入证据。

**提交信息**：`ci(i18n): 启用语言文件严格校验`

---

### S8.12 人工门禁：母语审校与阿拉伯文截图

- 前置：S8.11
- 准备者（Agent）：为每种必需语言导出审校表（`.dev/tmp/review_<lang>.csv`：键、英文、译文、备注列；不提交），把文件位置与抽样表写入证据；保持 `[H]`。
- 执行者（人）：每种语言一名母语审校者；一名测试者用客户端截图。

**检查清单**：

1. 每种语言：审校者在 CSV 中标注需修改的条目；修改由后续普通步骤提交（新建步骤卡 `S8.12-fix-<lang>`，由维护者添加到总目录）。
2. 阿拉伯文：客户端切换 العربية，截图：创造标签页悬停 5 个物品名、雷达 GUI、发射屏 GUI、一条含占位符的聊天消息（如 `/icbm blast` 结果）；对照原版物品名是否连写（判断原版是否已整形，见 compatibility/arabic-rendering.md）。结果填入该兼容性文件。
3. 德文/法文：长文本是否溢出按钮或 GUI 边界。
4. 日文/中文：字体显示正常，无豆腐块。

**提交信息**：`docs(i18n): 记录母语审校与阿拉伯文渲染结果`

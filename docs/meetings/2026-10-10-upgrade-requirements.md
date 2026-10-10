# 2026-10-10 升级需求记录

- 记录日期：2026-10-10
- 参与者：项目维护者（用户）、规划 Agent
- 性质：需求原文与规划时的调研结论。当前规则以 `docs/architecture/` 与 `docs/decisions/` 为准。

## 用户原始需求

> 搜索互联网，为本项目升级制定一份详细的文档。包括以下更新：
> 1、i18n 支持中文、法文、德文、日文、阿拉伯文（最后执行）
> 2、升级到 26.3 并使用 fabric 开发（首要）
> 3、电力使用 FE
> 4、增加反导导弹系统：即雷达检测到导弹不再仅可使用 EMP 塔，还能发射反弹道导弹直接摧毁目标（第二最后执行）
> 这是一个长期目标。参考 ../Minecraft-Server-Launcher 的文档结构进行文档规划。注意，你的文档将指挥多个能力远低于你的 Agent 工作，因此在技术细节上务必详细。

## 由需求推出的执行顺序

1. 26.3 + Fabric 移植（首要）
2. FE 电力（用户未指定位置；移植完成后、反导之前）
3. 反弹道导弹（倒数第二）
4. i18n（最后）

## 规划时核实的事实（2026-10-10）

| 事实 | 来源 / 核实方式 |
| --- | --- |
| 当前代码：Forge 1.12.2-14.23.5.2860，Java 8，版本 6.5.1，约 714 个 Java 文件、6.1 万行 | `build.gradle`、`gradle.properties`、`find src -name '*.java'` |
| 1.12 已有 FE 能量（`CapabilityEnergy.ENERGY`、`EnergySystemFE`） | `src/main/java/icbm/classic/lib/energy/` |
| 1.12 已有 SAM 地对空导弹原型：雷达在无线电频道广播 `IncomingMissileMessage`，装填 SAM 的发射台全部开火，SAM 追踪后对目标造成伤害 | `content/missile/entity/anti/`、`TileRadarStation#update`、`IncomingMissileMessage`、`RadioCruise` |
| 1.12 已有语言：en_US、de_DE、zh_cn、es_ES、ko_KR、pt_BR、ru_RU（`.lang` 格式） | `src/main/resources/assets/icbmclassic/lang/` |
| 26.1 起 Minecraft 不再混淆；Loom 插件 ID 为 `net.fabricmc.fabric-loom`，不再 remap；Yarn 不再官方支持 | Fabric 26.1 公告 |
| 26.3 Java 主版本 25 | Mojang 26.3 版本 JSON `javaVersion.majorVersion=25` |
| Fabric Loader 最新 0.19.5；Fabric API 最新 `0.162.0+26.3`；Loom 最新 1.18.3 | meta.fabricmc.net、maven.fabricmc.net |
| 同机项目 `../Minecraft-Server-Launcher/game-mods` 已用 Gradle 9.8.1 + Loom 1.18.3 + Loader 0.19.5 + Fabric API 0.162.0+26.3 构建成功并跑通 GameTest | 该仓库 `docs/implementation/evidence/S3.01.md`、`S3.04.md` |
| Team Reborn Energy 5.x 用于 MC 26.1 及以后；Tech Reborn 6.2.0（26.3）使用 `energy_version=5.0.0` | TechReborn/Energy README；TechReborn `26.3` 分支 `gradle.properties` |
| 26.3 资源中存在 `ar_sa`、`de_de`、`fr_fr`、`ja_jp`、`zh_cn` 原版语言 | 26.3 资源索引 `34.json` |
| 26.3 的 `FormattedBidiReorder` 只调用 ICU `Bidi`，**没有调用 `ArabicShaping`** → 阿拉伯字母可能不连写 | `javap -c net.minecraft.client.resources.language.FormattedBidiReorder` |
| 26.3 客户端自带 `com.ibm.icu:icu4j:78.3`、`gson:2.14.0` | 26.3 版本 JSON libraries |
| 26.3 存档 API：`ValueInput`/`ValueOutput`；GUI 绘制：`GuiGraphicsExtractor` 与 `extract*` 方法；方块实体渲染：`createRenderState`/`extractRenderState`/`submit` | 对 `minecraft-merged-deobf-26.3.jar` 执行 `javap` |

## 规划结论（已写入 ADR）

- 平台与工具链：ADR 0001
- 不迁移 1.12 存档、平铺化注册：ADR 0002
- FE 经 Team Reborn Energy 互通：ADR 0003
- 反导系统设计：ADR 0004
- 国际化格式与阿拉伯文处理：ADR 0005
- 配置、测试、Lombok 等工程缺省：ADR 0006

## 未决事项（需要用户确认，未确认前按工程缺省执行）

| 事项 | 工程缺省 | 何时需要确认 |
| --- | --- | --- |
| 旧语言 es_es、ko_kr、pt_br、ru_ru 是否继续保留 | 机械转换为 JSON 保留，不保证完整，不做新翻译 | S8 开始前 |
| JEI/REI/EMI 配方查看器联动 | 后续范围 | 发布后 |
| 是否同时发布 NeoForge 版本 | 不发布；架构上能源门面预留接口 | 发布后 |
| 7.0.0 是否公开发布到 Modrinth/CurseForge | 只准备构件，不上传 | S9 |

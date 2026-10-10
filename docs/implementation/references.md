# 外部链接与固定版本

给执行 Agent 使用。执行 Agent **不能网页搜索**，只能用 `curl -fsSL` 访问这里列出的 URL。版本号于 2026-10-10 从官方源查询；除非步骤卡明确要求，**不要升级或替换**。

## 0. 自行核对版本与哈希

```bash
# Maven（Fabric）：列出版本
curl -fsSL https://maven.fabricmc.net/<group/path>/<artifact>/maven-metadata.xml | grep -o '<version>[^<]*' | tail -5
# Maven Central：sha1
curl -fsSL https://repo1.maven.org/maven2/<group/path>/<artifact>/<version>/<artifact>-<version>.jar.sha1
# Fabric Loader 列表（26.3）
curl -fsSL https://meta.fabricmc.net/v2/versions/loader/26.3 | python3 -I -c 'import json,sys;print([x["loader"]["version"] for x in json.load(sys.stdin)][:5])'
# Modrinth：某项目 26.3 Fabric 版本
curl -fsSL 'https://api.modrinth.com/v2/project/<slug>/version?game_versions=%5B%2226.3%22%5D&loaders=%5B%22fabric%22%5D' | python3 -I -m json.tool | head -60
```

GitHub API 未认证限速 60 次/小时；遇到限速就等待，不换来源。

## 1. 工具链与依赖

| 项目 | 版本 | 坐标 / URL | 校验 |
| --- | --- | --- | --- |
| Gradle | 9.8.1 | https://services.gradle.org/distributions/gradle-9.8.1-bin.zip | `distributionSha256Sum=dce76f55f8e251a3a1f130eb120f30b3d271de2b76c9b0729d316b5a1b6dc01f`（与 https://services.gradle.org/distributions/gradle-9.8.1-bin.zip.sha256 一致；同机项目 wrapper 已使用） |
| Fabric Loom | 1.18.3 | Gradle 插件 `net.fabricmc.fabric-loom`，仓库 `https://maven.fabricmc.net/` | — |
| Minecraft | 26.3 | 版本 JSON：https://piston-meta.mojang.com/v1/packages/702fe59163c6ee6578607daa85811d9bc9c7cc40/26.3.json | `javaVersion.majorVersion=25` |
| Fabric Loader | 0.19.5 | `net.fabricmc:fabric-loader:0.19.5` | — |
| Fabric API | 0.162.0+26.3 | `net.fabricmc.fabric-api:fabric-api:0.162.0+26.3` | Modrinth 文件 sha1 `273cd2dcbd92d1559edcc91c9f23fee47f6ff93f`（同机项目记录） |
| Team Reborn Energy | 5.0.0 | `teamreborn:energy:5.0.0`（maven.fabricmc.net）；jar：https://maven.fabricmc.net/teamreborn/energy/5.0.0/energy-5.0.0.jar | sha256 `889afc438d3e4add5cfdac76517da7987a2c495e4731690a56f2c5dee775db59`（2026-10-10 计算） |
| Lombok | 1.18.48 | `org.projectlombok:lombok:1.18.48`（Maven Central 最新 release） | — |
| JUnit BOM | 5.14.4 | `org.junit:junit-bom:5.14.4` | — |
| ICU4J（仅构建脚本，S8.09） | 78.3 | `com.ibm.icu:icu4j:78.3`（与 26.3 客户端内置版本一致） | — |
| Tech Reborn（仅人工门禁 S6.08） | 6.2.0 | Modrinth 项目 `techreborn`，筛选 26.3 / fabric | 门禁时记录 sha512 |

## 2. Fabric 文档（页面版本标注可能为 26.2，用法以 javap 为准）

| 主题 | URL |
| --- | --- |
| 文档首页 | https://docs.fabricmc.net/develop/ |
| 移植到新版本 | https://docs.fabricmc.net/develop/porting/ |
| Mojang 名称迁移 | https://docs.fabricmc.net/develop/porting/mappings/ |
| Loom 中的 Fabric API 配置（configureTests / configureDataGeneration） | https://docs.fabricmc.net/develop/loom/fabric-api |
| 项目结构（拆分源集） | https://docs.fabricmc.net/develop/getting-started/project-structure |
| fabric.mod.json | https://docs.fabricmc.net/develop/loader/fabric-mod-json |
| 第一个方块 | https://docs.fabricmc.net/develop/blocks/first-block |
| 方块状态 | https://docs.fabricmc.net/develop/blocks/blockstates |
| 方块实体 | https://docs.fabricmc.net/develop/blocks/block-entities |
| 方块实体渲染器 | https://docs.fabricmc.net/develop/blocks/block-entity-renderer |
| 容器菜单 | https://docs.fabricmc.net/develop/blocks/container-menus |
| 方块容器 | https://docs.fabricmc.net/develop/blocks/block-containers |
| 第一个物品 | https://docs.fabricmc.net/develop/items/first-item |
| 物品模型 | https://docs.fabricmc.net/develop/items/item-models |
| 数据组件 | https://docs.fabricmc.net/develop/items/custom-data-components |
| 创造标签 | https://docs.fabricmc.net/develop/items/custom-creative-tabs |
| 第一个实体 | https://docs.fabricmc.net/develop/entities/first-entity |
| 投射物 | https://docs.fabricmc.net/develop/entities/projectiles |
| 伤害类型 | https://docs.fabricmc.net/develop/entities/damage-types |
| 效果 | https://docs.fabricmc.net/develop/entities/effects |
| 网络 | https://docs.fabricmc.net/develop/networking |
| 数据附件 | https://docs.fabricmc.net/develop/serialization/data-attachments |
| SavedData | https://docs.fabricmc.net/develop/serialization/saved-data |
| Codec | https://docs.fabricmc.net/develop/serialization/codecs |
| 自定义屏幕 / 控件 | https://docs.fabricmc.net/develop/rendering/gui/custom-screens 、 https://docs.fabricmc.net/develop/rendering/gui/custom-widgets |
| GUI 绘制 | https://docs.fabricmc.net/develop/rendering/gui-graphics |
| 粒子 | https://docs.fabricmc.net/develop/rendering/particles/creating-particles |
| 声音 | https://docs.fabricmc.net/develop/sounds/custom |
| 命令 | https://docs.fabricmc.net/develop/commands/basics 、 https://docs.fabricmc.net/develop/commands/arguments |
| 数据生成：设置 | https://docs.fabricmc.net/develop/data-generation/setup |
| 数据生成：方块模型 / 物品模型 | https://docs.fabricmc.net/develop/data-generation/block-models 、 https://docs.fabricmc.net/develop/data-generation/item-models |
| 数据生成：战利品表 / 标签 / 配方 / 翻译 | https://docs.fabricmc.net/develop/data-generation/loot-tables 、 …/tags 、 …/recipes 、 …/translations |
| 自动化测试（GameTest） | https://docs.fabricmc.net/develop/automatic-testing |
| Mixin Accessor | https://docs.fabricmc.net/develop/mixins/accessors |
| Class Tweaker | https://docs.fabricmc.net/develop/class-tweakers/ |
| 文本与翻译 | https://docs.fabricmc.net/develop/text-and-translations |
| Fabric API Javadoc（26.3） | https://maven.fabricmc.net/docs/fabric-api-0.162.0+26.3/index.html |

## 3. 版本公告

| 公告 | URL | 要点 |
| --- | --- | --- |
| Fabric for 26.1 | https://fabricmc.net/2026/03/14/261.html | 去混淆；插件 ID `net.fabricmc.fabric-loom`；`modImplementation`→`implementation`；`remapJar`→`jar`；Yarn 不再支持；`fabric` mod id 改依赖 `fabric-api`；`ItemStack` 不能在世界加载前创建；`ColorProviderRegistry`→`BlockColorRegistry` |
| Fabric for 26.2 | https://fabricmc.net/2026/06/15/262.html | Vulkan 可选后端，禁止直接 OpenGL；`BlockIds`/`ItemIds` 分离；`setScreen` 移到 `Gui` |
| Fabric for 26.3 | https://fabricmc.net/2026/09/15/263.html | GLFW→SDL（文本控件焦点通知）；方块 Codec 移除；多个注册表改为 DataComponent/数据驱动 |

## 4. 能源

| 主题 | URL |
| --- | --- |
| Team Reborn Energy README（版本对照、示例、push 约定） | https://raw.githubusercontent.com/TechReborn/Energy/master/README.md |
| Tech Reborn 26.3 依赖版本 | https://raw.githubusercontent.com/TechReborn/TechReborn/26.3/gradle.properties |

## 5. 同机参考（只读）

| 内容 | 位置 |
| --- | --- |
| 同版本 Fabric Gradle 配置 | `/home/klp/Minecraft-Server-Launcher/game-mods/build.gradle`、`settings.gradle`、`gradle.properties`、`gradle/wrapper/` |
| GameTest 配置样例 | `/home/klp/Minecraft-Server-Launcher/game-mods/fabric-server-mod/build.gradle`（`fabricApi { configureTests { … } }`） |
| GameTest 方法名核对结论 | `/home/klp/Minecraft-Server-Launcher/docs/implementation/evidence/S3.01.md`（`makeMockServerPlayerInLevel()`、`assertTrue(boolean, String)`、`succeed()`、`fail(String)` 在 26.3 存在） |

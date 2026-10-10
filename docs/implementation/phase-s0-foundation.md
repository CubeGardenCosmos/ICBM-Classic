# S0 基线与工具链

## 阶段说明

目标：在新分支上得到一个**空的、能构建、能跑测试、能被专用服务端加载**的 Fabric 26.3 模组工程，旧代码保留在 `legacy-1.12/` 供参考。

**现状（`prod/1.12` @ `e862b4f15`）**：Forge 1.12.2 构建（ForgeGradle 6、Java 8 toolchain、Gradle wrapper 8.8）；源码 `src/main/java`（660 个 Java 文件）、`src/test/java`（54 个），合计 714 个；`libs/` 有两个 1.12 专用 jar；`.github/workflows/gradle.yml` 用 JDK 1.8。

所有 Gradle 命令前：`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk`。

---

### S0.01 建立工作分支与基线记录

- 前置：P0 文档集已由维护者提交到 `prod/1.12`（`git log -1 --format=%s` 应为文档提交；若 `docs/` 仍是未跟踪状态 → BLOCKED，原因：工作区不干净）
- 必读：AGENTS.md；本手册 §7
- 改动文件：新建 `docs/implementation/evidence/S0.01.md`
- 目标：创建 `port/26.3-fabric` 分支并记录起点。

**操作**

1. `git status --short` 必须为空。
2. `git switch -c port/26.3-fabric`
3. 记录：`git log -1 --format='%H %s'`、`git branch --show-current`、`ls`、`find src -name '*.java' | wc -l`（期望 main+test 合计 714）、`/usr/lib/jvm/java-25-openjdk/bin/java -version`。
4. **不要**尝试构建 1.12 工程（需要 Java 8 与已停止维护的 Forge 仓库，不是本项目目标）。在证据中写明“未构建 1.12 基线，原因：工具链不在本机，且不在升级范围”。

**验证**：`git branch --show-current` 输出 `port/26.3-fabric`。

**完成判据**：分支存在；证据文件包含第 3 条全部输出。

**提交信息**：`docs(impl): 记录移植分支基线`

---

### S0.02 旧代码移入 `legacy-1.12/`

- 前置：S0.01
- 必读：overview.md §3
- 改动文件：`git mv` 下列路径；修改 `.gitattributes`（若需要）；新建 `legacy-1.12/README.md`
- 目标：旧代码以原相对结构保存在 `legacy-1.12/`，根目录腾空以便新建 Fabric 工程。

**操作**

1. 创建目录并移动（保留 git 历史，必须用 `git mv`）：

   ```bash
   mkdir -p legacy-1.12
   git mv src legacy-1.12/src
   git mv build.gradle legacy-1.12/build.gradle
   git mv settings.gradle legacy-1.12/settings.gradle
   git mv gradle.properties legacy-1.12/gradle.properties
   git mv libs legacy-1.12/libs
   git mv gradle legacy-1.12/gradle
   git mv gradlew legacy-1.12/gradlew
   git mv gradlew.bat legacy-1.12/gradlew.bat
   ```

   **不移动**：`README.md`、`LICENSE.md`、`CHANGE_LOG.md`、`notes/`、`scripts/`、`docs/`、`.github/`、`.editorconfig`、`.gitattributes`、`.gitignore`、`AGENTS.md`、`CLAUDE.md`。

2. 新建 `legacy-1.12/README.md`：

   ```markdown
   # legacy-1.12（只读）

   ICBM-Classic 6.5.1（Forge 1.12.2）的源码与构建文件，自 `prod/1.12` 提交 e862b4f15 移入。
   仅供 26.3 移植参考：不编译、不修改。移植完成后由 S9.01 删除。
   ```

3. `git status --short` 应只显示重命名（`R`）与新增的 README。

**验证**

```bash
test -d legacy-1.12/src/main/java/icbm/classic && echo OK-legacy
test ! -e src && echo OK-root-clean
find legacy-1.12/src -name '*.java' | wc -l    # 期望 714（main 660 + test 54）
```

**完成判据**：三条输出均符合期望。

**失败处理**：`git mv` 报错“not under version control” → 该文件是未跟踪文件，BLOCKED 询问。

**提交信息**：`chore(legacy): 将 1.12 源码移入 legacy-1.12 目录`

---

### S0.03 Fabric 26.3 Gradle 工程骨架

- 前置：S0.02
- 必读：ADR 0001、ADR 0006；overview.md §3–§5；references.md §1、§5；`/home/klp/Minecraft-Server-Launcher/game-mods/build.gradle`
- 改动文件：新建 `settings.gradle`、`build.gradle`、`gradle.properties`、`gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar`、`gradle/wrapper/gradle-wrapper.properties`、`src/main/resources/fabric.mod.json`、`src/main/resources/icbmclassic.mixins.json`、`src/client/resources/icbmclassic.client.mixins.json`、`src/main/java/icbm/classic/ICBMClassic.java`、`src/main/java/icbm/classic/ICBMConstants.java`、`src/client/java/icbm/classic/client/ICBMClassicClient.java`、`src/main/resources/assets/icbmclassic/icon.png`、`run/server/eula.txt`（不提交，见下）
- 目标：`./gradlew build` 成功；专用服务端启动日志显示加载了 `icbmclassic`。

**操作**

1. Wrapper：从同机项目复制（同为 9.8.1，已带 sha256）：

   ```bash
   mkdir -p gradle/wrapper
   cp /home/klp/Minecraft-Server-Launcher/game-mods/gradle/wrapper/gradle-wrapper.jar gradle/wrapper/
   cp /home/klp/Minecraft-Server-Launcher/game-mods/gradle/wrapper/gradle-wrapper.properties gradle/wrapper/
   cp /home/klp/Minecraft-Server-Launcher/game-mods/gradlew /home/klp/Minecraft-Server-Launcher/game-mods/gradlew.bat .
   chmod +x gradlew
   grep distributionSha256Sum gradle/wrapper/gradle-wrapper.properties
   curl -fsSL https://services.gradle.org/distributions/gradle-9.8.1-bin.zip.sha256; echo
   ```

   两个 sha256 必须相同，否则 BLOCKED。

2. `settings.gradle`：

   ```groovy
   pluginManagement {
       repositories {
           maven { url "https://maven.fabricmc.net/" }
           gradlePluginPortal()
       }
   }
   rootProject.name = "ICBM-Classic"
   ```

3. `gradle.properties`：

   ```properties
   org.gradle.jvmargs=-Xmx3G
   org.gradle.parallel=false

   minecraft_version=26.3
   loader_version=0.19.5
   fabric_version=0.162.0+26.3
   java_version=25

   mod_version=7.0.0-dev
   maven_group=com.builtbroken.icbmclassic
   archives_base_name=ICBM-classic-26.3
   lombok_version=1.18.48
   junit_version=5.14.4
   ```

4. `build.gradle`（骨架，照抄后补 `// TODO(agent)` 无）：

   ```groovy
   plugins {
       id 'java'
       id 'net.fabricmc.fabric-loom' version '1.18.3'
   }

   version = project.mod_version
   group = project.maven_group
   base { archivesName = project.archives_base_name }

   java {
       toolchain { languageVersion = JavaLanguageVersion.of(project.java_version as int) }
       withSourcesJar()
   }

   loom {
       splitEnvironmentSourceSets()
       mods {
           icbmclassic {
               sourceSet sourceSets.main
               sourceSet sourceSets.client
           }
       }
   }

   repositories {
       mavenCentral()
       maven { url "https://maven.fabricmc.net/" }
   }

   dependencies {
       minecraft "com.mojang:minecraft:${project.minecraft_version}"
       implementation "net.fabricmc:fabric-loader:${project.loader_version}"
       implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"

       compileOnly "org.projectlombok:lombok:${project.lombok_version}"
       annotationProcessor "org.projectlombok:lombok:${project.lombok_version}"
       clientCompileOnly "org.projectlombok:lombok:${project.lombok_version}"
       clientAnnotationProcessor "org.projectlombok:lombok:${project.lombok_version}"
   }

   processResources {
       inputs.property "version", project.version
       filesMatching("fabric.mod.json") { expand "version": project.version }
   }

   tasks.withType(JavaCompile).configureEach {
       options.encoding = "UTF-8"
       options.release = project.java_version as int
   }

   jar {
       from("LICENSE.md") { rename { "${it}_${project.base.archivesName.get()}" } }
   }

   tasks.register('installLocalGitHook', Copy) {
       from new File(rootProject.rootDir, 'scripts/commit-msg')
       into { new File(rootProject.rootDir, '.git/hooks') }
       filePermissions { unix(0775) }
   }
   tasks.named('build') { dependsOn 'installLocalGitHook' }
   ```

   若 `clientCompileOnly`/`clientAnnotationProcessor` 配置名不存在（Loom 拆分源集的配置名以 `./gradlew dependencies` 输出为准），改为实际名称并在证据中记录。

5. `src/main/resources/fabric.mod.json`：

   ```json
   {
     "schemaVersion": 1,
     "id": "icbmclassic",
     "version": "${version}",
     "name": "ICBM-Classic",
     "description": "Intercontinental ballistic missiles, explosives, launchers, radar and missile defense.",
     "authors": ["DarkCow (Robin Seifert)", "Calclavia"],
     "contributors": ["bl4ckscor3", "dmod", "GHXX", "M1W3st", "tgame14", "kmecpp", "LetterN", "Graugger", "Vexatos"],
     "contact": {
       "homepage": "https://modrinth.com/mod/icbm",
       "sources": "https://github.com/BuiltBrokenModding/ICBM-Classic",
       "issues": "https://github.com/BuiltBrokenModding/ICBM-Classic/issues"
     },
     "license": "SEE LICENSE.md",
     "icon": "assets/icbmclassic/icon.png",
     "environment": "*",
     "entrypoints": {
       "main": ["icbm.classic.ICBMClassic"],
       "client": ["icbm.classic.client.ICBMClassicClient"]
     },
     "mixins": [
       "icbmclassic.mixins.json",
       { "config": "icbmclassic.client.mixins.json", "environment": "client" }
     ],
     "depends": {
       "fabricloader": ">=0.19.5",
       "minecraft": "~26.3",
       "java": ">=25",
       "fabric-api": "*"
     }
   }
   ```

   `contact` 中的 URL 来自旧 `README.md`/`mcmod.info`；GitHub 地址用 `git remote get-url origin` 的结果替换（去掉 `.git`），若无 remote 则删除 `sources`/`issues` 两项。

6. 两个 mixins JSON（空 mixin 列表）：

   ```json
   {
     "required": true,
     "minVersion": "0.8",
     "package": "icbm.classic.mixin",
     "compatibilityLevel": "JAVA_25",
     "mixins": [],
     "injectors": { "defaultRequire": 1 }
   }
   ```

   客户端文件 `package` 为 `icbm.classic.client.mixin`，键 `"client": []` 代替 `"mixins"`。若构建/启动报 `JAVA_25` 不识别，改 `JAVA_21` 并记录（同机项目经验）。

7. `ICBMConstants.java`：从 `legacy-1.12/.../ICBMConstants.java` 复制常量（`DOMAIN`、`PREFIX`、`TEXTURE_DIRECTORY`、`GUI_DIRECTORY`、`TICKS_*` 等全部），删除 Forge 引用，新增：

   ```java
   public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(DOMAIN, path); }
   ```

8. `ICBMClassic.java`：

   ```java
   package icbm.classic;

   import net.fabricmc.api.ModInitializer;
   import org.slf4j.Logger;
   import org.slf4j.LoggerFactory;

   public final class ICBMClassic implements ModInitializer {
       public static final Logger LOGGER = LoggerFactory.getLogger("ICBM-Classic");

       /** 兼容 1.12 调用点 ICBMClassic.logger() */
       public static Logger logger() { return LOGGER; }

       @Override
       public void onInitialize() {
           LOGGER.info("ICBM-Classic initializing (26.3 port)");
           // 初始化顺序见 docs/architecture/overview.md §5，后续步骤按顺序在此追加调用
       }
   }
   ```

   `ICBMClassicClient implements ClientModInitializer`，`onInitializeClient()` 只打日志。

9. `icon.png`：用 `legacy-1.12/src/main/resources/assets/icbmclassic/textures/` 中任意 16×16 或更大的导弹物品纹理复制（例如 `textures/items/` 下的导弹图标），在证据中写明来源文件。
10. 服务端 EULA（测试用，不提交）：`mkdir -p run/server && echo 'eula=true' > run/server/eula.txt`。确认 `build.gradle` 的 Loom 运行目录：若 `runServer` 默认目录不是 `run`，按实际目录放置。

**验证**

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk
./gradlew --version | head -5                 # Gradle 9.8.1
./gradlew build 2>&1 | tail -20               # BUILD SUCCESSFUL
ls build/libs/                                # ICBM-classic-26.3-7.0.0-dev.jar 与 -sources.jar
unzip -p build/libs/ICBM-classic-26.3-7.0.0-dev.jar fabric.mod.json | grep '"version"'   # "7.0.0-dev"
timeout 240 ./gradlew runServer --args="--nogui" 2>&1 | tee .dev/tmp/s003-server.log | tail -5
grep -E "ICBM-Classic initializing|Done \(" .dev/tmp/s003-server.log
```

Lombok 证明：在 `ICBMConstants` 临时加一个 `@lombok.Getter private static int lombokProbe = 1;` → 构建成功后删掉，再构建一次。证据记录两次结果。（若 `@Getter` 用于静态字段不生成，改用一个临时类上的 `@Getter` 实例字段验证。）

**完成判据**：构建成功；jar 中版本已替换；服务端日志同时出现初始化日志与 `Done (`；Lombok 验证通过；服务端进程已结束。

**失败处理**：

- Loom 插件解析失败 → 检查 `settings.gradle` 仓库；仍失败 BLOCKED。
- Lombok 在 Java 25 失败（`java.lang.IllegalAccessError`/`NoSuchFieldError` 等）→ BLOCKED（ADR 0006 第 3 条，需要人决定）。
- `runServer` 卡住超过 240 秒 → 记录日志后 BLOCKED。

**提交信息**：`build: 建立 Fabric 26.3 工程骨架`（提交文件不含 `run/`、`.dev/`；若 `.gitignore` 未忽略它们，在本步补充 `/run/`、`/.dev/`）

---

### S0.04 JUnit 与 GameTest 管线

- 前置：S0.03
- 必读：references.md §2“自动化测试”；`/home/klp/Minecraft-Server-Launcher/game-mods/fabric-server-mod/build.gradle` 中 `fabricApi { configureTests {...} }`；同机证据 S3.01
- 改动文件：修改 `build.gradle`；新建 `src/gametest/resources/fabric.mod.json`、`src/gametest/java/icbm/classic/test/SmokeGameTest.java`、`src/test/java/icbm/classic/UnitSmokeTest.java`
- 目标：`./gradlew build` 运行 JUnit 与服务端 GameTest，任一失败则构建失败。

**操作**

1. `build.gradle` 追加：

   ```groovy
   fabricApi {
       configureTests {
           createSourceSet = true
           modId = "icbmclassic-test"
           enableGameTests = true
           enableClientGameTests = false
           eula = true
       }
   }

   dependencies {
       testImplementation platform("org.junit:junit-bom:${project.junit_version}")
       testImplementation "org.junit.jupiter:junit-jupiter"
       testRuntimeOnly "org.junit.platform:junit-platform-launcher"
       gametestCompileOnly "org.projectlombok:lombok:${project.lombok_version}"
       gametestAnnotationProcessor "org.projectlombok:lombok:${project.lombok_version}"
   }

   test { useJUnitPlatform() }
   ```

   `gametest*` 配置名以 `./gradlew dependencies` 实际输出为准。确认 `./gradlew tasks --all | grep -i gametest` 中存在 `runGameTest`，且 `build` 依赖它（同机项目即如此）；若 `build` 不依赖它，追加 `tasks.named('check') { dependsOn 'runGameTest' }`。

2. `src/gametest/resources/fabric.mod.json`：

   ```json
   {
     "schemaVersion": 1,
     "id": "icbmclassic-test",
     "version": "0.0.0",
     "environment": "*",
     "entrypoints": {
       "fabric-gametest": ["icbm.classic.test.SmokeGameTest"]
     },
     "depends": { "icbmclassic": "*" }
   }
   ```

3. `SmokeGameTest.java`：

   ```java
   package icbm.classic.test;

   import net.fabricmc.fabric.api.gametest.v1.GameTest;
   import net.minecraft.core.BlockPos;
   import net.minecraft.gametest.framework.GameTestHelper;
   import net.minecraft.world.level.block.Blocks;

   public class SmokeGameTest {
       @GameTest
       public void stoneCanBePlaced(GameTestHelper helper) {
           BlockPos pos = new BlockPos(0, 1, 0);
           helper.setBlock(pos, Blocks.STONE);
           helper.assertBlockPresent(Blocks.STONE, pos);
           helper.succeed();
       }
   }
   ```

   `setBlock`/`assertBlockPresent` 签名用 javap 核对 `net.minecraft.gametest.framework.GameTestHelper`，不同则按 javap 修改并记录。以后每新增一个 GameTest 类，都要加到测试模组 `fabric-gametest` 入口数组中。

4. `UnitSmokeTest.java`：一个 `@Test` 断言 `ICBMConstants.id("x").toString()` 等于 `"icbmclassic:x"`。**注意**：JUnit 测试运行时没有引导 Minecraft 注册表；`Identifier` 是纯数据类，可用。若报类加载错误，改为断言 `ICBMConstants.DOMAIN.equals("icbmclassic")` 并记录。

**验证**

```bash
./gradlew build 2>&1 | tail -30
ls build/test-results/test/ | head
```

然后**故意失败**：把 `helper.succeed()` 前加 `helper.fail("probe")`（签名按 javap）→ `./gradlew build` 必须失败 → 改回 → 再次成功。JUnit 同理把断言改错一次。

**完成判据**：成功、故意失败两种情况都符合期望，并记录在证据中。

**失败处理**：`configureTests` 不存在 → 原样记录错误，BLOCKED。

**提交信息**：`test: 建立 JUnit 与 GameTest 测试管线`

---

### S0.05 `scripts/check.sh` 与 `.gitignore`

- 前置：S0.04
- 必读：porting-guide.md §1 第 4 条
- 改动文件：新建 `scripts/check.sh`、`scripts/forbidden_names.txt`；修改 `.gitignore`；修改 `.github/workflows/gradle.yml`
- 目标：一条命令完成全部本地检查；CI 定义改为 JDK 25（只改文件，不 push）。

**操作**

1. `scripts/forbidden_names.txt`（每行一个正则；只扫描 `src/`，不扫描 `legacy-1.12/`）：

   ```text
   net\.minecraftforge
   net\.minecraft\.util\.math\.Vec3d
   \bEnumFacing\b
   \bNBTTagCompound\b
   \bTileEntity\b
   \bEntityPlayer\b
   \bIBlockState\b
   \bResourceLocation\b
   @SideOnly
   import org\.lwjgl\.opengl
   GlStateManager
   ```

2. `scripts/check.sh`：

   ```bash
   #!/usr/bin/env bash
   set -euo pipefail
   cd "$(dirname "$0")/.."
   export JAVA_HOME=/usr/lib/jvm/java-25-openjdk

   echo "== forbidden 1.12 names in src/"
   fail=0
   while IFS= read -r pattern; do
     [ -z "$pattern" ] && continue
     if grep -rEn --include='*.java' "$pattern" src/ ; then
       echo "!! forbidden pattern: $pattern"; fail=1
     fi
   done < scripts/forbidden_names.txt

   echo "== client classes in main source set"
   if grep -rEn --include='*.java' '^import (net\.minecraft\.client|com\.mojang\.blaze3d|net\.fabricmc\.fabric\.api\.client)' src/main/java ; then
     echo "!! client import in src/main"; fail=1
   fi

   if [ -f scripts/check_lang.py ]; then
     echo "== lang files"
     python3 -I scripts/check_lang.py || fail=1
   fi

   echo "== gradle build (compile + JUnit + GameTest)"
   ./gradlew build --console=plain || fail=1

   exit $fail
   ```

   `chmod +x scripts/check.sh`。

3. `.gitignore` 追加（若缺）：`/run/`、`/.dev/`、`/build/`、`.gradle/`、`/src/main/generated/.cache/`、`/out/`、`*.log`。**不要**忽略 `src/main/generated/`（datagen 输出要提交）。
4. `.github/workflows/gradle.yml`：`build` 作业改为 `actions/checkout@v4`、`actions/setup-java@v4`（`distribution: temurin`，`java-version: '25'`）、运行 `./gradlew build`；删除 `sonarCloudTrigger` 作业（新构建没有 `sonarqube` 任务），并在证据中注明“Sonar 作业删除，若需要由维护者另行配置”。

**验证**：`scripts/check.sh; echo exit=$?` → `exit=0`。再临时在 `ICBMClassic.java` 加一行注释 `// EnumFacing` → `exit=1` → 删掉。

**完成判据**：两次结果符合期望。

**提交信息**：`build: 增加本地检查脚本并更新 CI 到 JDK 25`

---

### S0.06 移植账本

- 前置：S0.02
- 必读：porting-guide.md §3
- 改动文件：新建 `scripts/port_ledger.py`、`docs/implementation/port-ledger.md`
- 目标：列出 `legacy-1.12/src/main/java` 下所有 `.java` 文件，每行一个状态，作为移植进度的唯一记录。

**操作**

1. `scripts/port_ledger.py`（只用标准库）：
   - `generate`：扫描 `legacy-1.12/src/main/java/**/*.java`，按路径排序，输出 Markdown 表 `| 状态 | 旧文件（相对 legacy-1.12/src/main/java） | 目标 / 说明 | 步骤 |` 到 `docs/implementation/port-ledger.md`。初始状态全部 `TODO`；“步骤”列按 porting-guide.md §3 的包前缀映射预填（例如 `icbm/classic/lib/transform/` → `S1.01`；`icbm/classic/mods/` → 状态直接 `DROPPED`、说明“ADR 0006”；`icbm/classic/datafix/` → `DROPPED`、“ADR 0002”；`icbm/classic/CommonProxy.java` → `DROPPED`）。前缀映射写成脚本中的有序列表（最长前缀优先），内容完全照抄 §3 表。
   - `stats`：打印各状态数量与各步骤剩余 `TODO` 数。
   - `check`：若存在 `TODO` 行，列出并退出码 1（S5.11 使用）。
   - **已存在账本时 `generate` 拒绝覆盖**（防止抹掉进度），需要 `--force`。
2. 运行 `python3 -I scripts/port_ledger.py generate`，再 `stats`。
3. 账本状态取值：`TODO`、`PORTED`、`MERGED → <新文件>`、`DROPPED（原因）`。`DROPPED` 必须有原因。

**验证**：`python3 -I scripts/port_ledger.py stats` 显示总数 660（`legacy-1.12/src/main/java`）；`DROPPED` 数量等于 `mods/`、`datafix/`、`CommonProxy.java` 文件数之和。

**完成判据**：账本生成；统计正确；`check` 退出码 1（因为还有 TODO）。

**提交信息**：`docs(impl): 生成 1.12 源码移植账本`

# 0006：工程缺省——配置、测试、Lombok、联动模组

- 状态：Accepted
- 日期：2026-10-10
- 决策参与者：规划 Agent（工程缺省，可由用户推翻）
- 关联：ADR 0001

## 背景

1.12 代码依赖 Forge 的 `@Config` 注解配置、MinecraftJUnit 测试库、Lombok、Mekanism/JEI 联动。移植时每一项都需要明确替代方案，否则执行 Agent 会各自选择不同的库。

## 决定（全部为工程缺省）

1. **配置系统**：自研轻量 JSON 配置（Gson，随 Minecraft 自带 2.14.0），保留 1.12 的“配置类 + `public static` 字段”写法以减少调用点改动。注解 `@IcbmConfig(file=...)`、`@ConfigEntry(name=..., comment=...)`、`@ConfigRange(min=..., max=...)`。文件位置 `config/icbmclassic/<file>.json`。读写规则见 [porting-guide.md §6](../architecture/porting-guide.md#6-配置系统)。不引入 Cloth Config、MidnightLib 等第三方库。游戏内配置界面属于后续范围。
2. **测试**：
   - 纯逻辑（数学、编解码、配置解析、数据结构）用 JUnit 5（`junit-bom 5.14.4`，与同机项目一致）。
   - 需要世界、方块、实体的逻辑用 Fabric GameTest（`fabricApi.configureTests`），在 `./gradlew build` 中自动运行。
   - 1.12 测试（`src/test`，54 个文件）随旧代码移入 `legacy-1.12/`；只移植仍有意义的断言，不移植 MinecraftJUnit/Mockito 世界模拟。
   - Mockito 不再使用（GameTest 提供真实世界）。
3. **Lombok**：保留，版本 `1.18.48`（`compileOnly` + `annotationProcessor`）。S0.03 必须证明它在 Java 25 下工作；若失败，BLOCKED，由人决定是否改为手写 getter/setter。
4. **联动模组**：Mekanism、JEI、IC2 代码全部删除。REI/EMI/JEI 配方查看器、Jade/WTHIT 信息显示属于后续范围。
5. **线程化爆炸**：保留工作线程计算的设计，但工作线程**只读**区块快照、产出“待修改方块列表”；方块修改在服务端 tick 中按每 tick 预算（默认 2000 个方块）应用。不在工作线程调用 `Level#setBlock`。
6. **网络**：1.12 自研的 `PacketCodex` 构建器 API 保留（减少上层改动），底层改为一个 Fabric 自定义载荷（详见 porting-guide.md §5）。
7. **Access Transformer**：作废。需要私有成员时依次考虑：公开 API → Mixin `@Accessor`/`@Invoker` → Class Tweaker。
8. **Git 钩子**：保留 `installLocalGitHook` 任务（安装 `scripts/commit-msg`）。

## 影响

- 配置文件格式从 Forge `.cfg` 变为 JSON，旧配置不迁移（与 ADR 0002 一致）。
- 测试覆盖范围在移植初期会下降；每个移植步骤卡都规定了最少要加的测试。

## 备选方案

- Cloth Config：增加运行时依赖，且 API 跟随版本变化。拒绝。
- 去掉 Lombok：要改动数百处注解，增加移植工作量。暂不采用。

## 验证

S0.03（Lombok + 构建）、S0.04（GameTest + JUnit）、S1.02（配置系统 JUnit）产生证据。

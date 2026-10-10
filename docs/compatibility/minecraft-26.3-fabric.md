# 兼容性：Minecraft 26.3 / Fabric 工具链

- 状态：Not tested（本项目）
- 调查日期：2026-10-10
- 测试日期：Not tested
- 目标：Minecraft 26.3、Java 25、Fabric Loader 0.19.5、Fabric API 0.162.0+26.3、Loom 1.18.3、Gradle 9.8.1

## 上游事实

- Fabric 26.3 公告：https://fabricmc.net/2026/09/15/263.html （推荐 Loom 1.17 / Gradle 9.6.0 为发文时版本；本项目用更新的 1.18.3 / 9.8.1，与同机项目一致）。
- Fabric 26.1 公告（去混淆、插件 ID、移除 remap）：https://fabricmc.net/2026/03/14/261.html
- Fabric 26.2 公告（Vulkan 后端、ID 分离、setScreen 移动）：https://fabricmc.net/2026/06/15/262.html
- Mojang 26.3 版本 JSON：`javaVersion.majorVersion = 25`。
- 同机项目 `../Minecraft-Server-Launcher` 以相同版本组合构建通过、服务端 GameTest 通过（该项目证据 S3.01）。**这是旁证，不是本项目证据。**

## 本项目测试

| 场景 | 结果 | 证据 |
| --- | --- | --- |
| `./gradlew build`（空骨架 + Lombok） | Not tested | S0.03 |
| 服务端 GameTest 管线 | Not tested | S0.04 |
| 专用服务端启动加载本模组 | Not tested | S0.03 |
| 客户端启动、进入单人世界 | Not tested | S5.12（人工） |
| 全部方块/物品 `/give` 与放置 | Not tested | S2 GameTest、S5.12 |

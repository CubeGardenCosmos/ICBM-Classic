# 兼容性：能源互通（Team Reborn Energy）

- 状态：Not tested（本项目）
- 调查日期：2026-10-10
- 依赖：`teamreborn:energy:5.0.0`，sha256 `889afc438d3e4add5cfdac76517da7987a2c495e4731690a56f2c5dee775db59`，来源 https://maven.fabricmc.net/teamreborn/energy/5.0.0/energy-5.0.0.jar

## 上游事实

- TechReborn/Energy README：“Version 5.x should be used for Minecraft 26.1 or later”。
- Tech Reborn `26.3` 分支 `gradle.properties`：`mod_version=6.2.0`、`minecraft_version=26.3`、`energy_version=5.0.0`、`fapi_version=0.160.5+26.3`。
- Modrinth：Tech Reborn 6.2.0 for 26.3 Fabric 发布于 2026-09-15。
- Energy 5.0.0 `fabric.mod.json` 依赖：`java>=25`、`minecraft>=26.1-`、`fabric-transfer-api-v1>=5.1.0`。

## 本项目测试

| 场景 | 结果 | 证据 |
| --- | --- | --- |
| GameTest：测试方块推送能量到雷达/发射台/EMP 塔 | Not tested | S6.07 |
| GameTest：电池放电 | Not tested | S6.04 |
| 实机：Tech Reborn 6.2.0 发电机 + 线缆 → ICBM 机器 | Not tested | S6.08（人工） |
| 实机：Tech Reborn 充电器为 ICBM 电池充电 | Not tested | S6.08（人工） |

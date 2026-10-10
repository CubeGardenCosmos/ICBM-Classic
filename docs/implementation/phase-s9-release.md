# S9 收尾

## 阶段说明

清理移植辅助物，更新版本与发布文档，产出构件。**本阶段不发布、不上传、不 push。** 是否公开发布由维护者在 S9.04 决定。

---

### S9.01 删除 `legacy-1.12/`

- 前置：S8.11（以及 S5.12、S6.08、S7.11 已由人完成或维护者明确豁免——在证据中写明各门禁状态）
- 改动文件：删除 `legacy-1.12/`；`scripts/port_ledger.py`、`docs/implementation/port-ledger.md` 保留（历史记录）；`scripts/forbidden_names.txt` 不变
- 目标：仓库中不再包含 1.12 源码（历史仍在 git 中）。

**操作**：`git rm -r legacy-1.12`；`grep -rn "legacy-1.12" --include='*.md' --include='*.gradle' --include='*.py' --include='*.sh' .` 列出引用：文档中的历史引用保留，脚本中的路径引用改为说明“已删除，见 git 历史 `prod/1.12`”或删除对应功能（`port_ledger.py generate` 加判断：目录不存在时提示并退出）。`./gradlew build` 通过。

**提交信息**：`chore(legacy): 移除 1.12 旧源码目录`

---

### S9.02 版本号、CHANGE_LOG、README

- 前置：S9.01
- 必读：`CHANGE_LOG.md` 现有格式；`notes/5.x.x-release.md`（发布说明写法参考）
- 改动文件：`gradle.properties`（`mod_version=7.0.0`）；`CHANGE_LOG.md`；`README.md`
- 目标：发布文档反映 26.3 版本的全部变化。

**CHANGE_LOG 7.0.0 必须包含**：

1. 平台：Minecraft 26.3 + Fabric（Loader ≥ 0.19.5，需要 Fabric API）；Java 25。
2. **不兼容 6.x 世界与配置**（ADR 0002、0006）；所有 ID 改为平铺化（附 registry-ids.md 链接或摘要表）。
3. 电力：FE，经 Team Reborn Energy 与其他 Fabric 模组互通。
4. 新增：反弹道导弹（替代 SAM）、雷达防御模式、EMP 兜底。
5. 玩法变化：发射台不再因雷达来袭消息自动发射所装导弹。
6. 已知限制：区块外模拟飞行的导弹不可拦截（附 S7.08 测量结论）；无游戏内配置界面；不再支持 Mekanism/JEI 联动。
7. 语言：新增 fr_fr、ja_jp、ar_sa；更新 zh_cn、de_de；es_es、ko_kr、pt_br、ru_ru 不完整。

README：Install 一节改为 Fabric 安装说明；下载链接保留；新增“开发”一节指向 `docs/README.md`。

**提交信息**：`chore(release): 版本 7.0.0 与发布说明`

---

### S9.03 构件与校验和

- 前置：S9.02
- 改动文件：`build.gradle`（如需 `apiJar` 任务：照 1.12 `apiJar` 只打包 `icbm/classic/api/**`）；证据
- 目标：产出主 jar、sources jar、api jar，记录 SHA-256。

**操作**：`./gradlew clean build`；`sha256sum build/libs/*.jar` 写入证据；`unzip -p build/libs/ICBM-classic-26.3-7.0.0.jar fabric.mod.json` 检查版本与依赖；确认 jar 内含 `META-INF/jars/energy-5.0.0.jar`、全部语言文件、`LICENSE.md_*`。

**提交信息**：`build(release): 产出 7.0.0 构件`

---

### S9.04 人工门禁：总验收

- 前置：全部
- 执行者（人）：维护者。

**检查清单**：

1. TODO P1–P5 全部条目已勾选或有维护者书面豁免。
2. 所有 `[H]` 门禁证据齐全。
3. 用 S9.03 构件在干净的 26.3 Fabric 客户端 + 专用服务器各跑一遍 S5.12 清单的第 1、4、6、7 项与 S7.11 第 2 项。
4. 决定：是否把 `port/26.3-fabric` 合并到新的主分支（例如 `prod/26.3`）、是否在 Modrinth/CurseForge 发布。决定写入新 ADR。

**提交信息**：`docs(release): 记录 7.0.0 总验收`

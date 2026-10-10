# 架构决策记录（ADR）

本目录记录会改变目标平台、存档语义、注册 ID、能源接口、玩法规则或资源格式的决定。需求原文见[需求记录](../meetings/2026-10-10-upgrade-requirements.md)。

命名 `NNNN-kebab-case-title.md`；状态为 `Proposed`、`Accepted`、`Rejected`、`Superseded`。调整已接受的决定时**新建** ADR 并在旧 ADR 顶部加一行“被 NNNN 取代”，不删除历史。使用 [模板](template.md)。

每条决定都要标注来源：**用户要求** / **讨论结论** / **工程缺省**。工程缺省可以由用户推翻，推翻时新建 ADR。

## 记录列表

- [0001 目标平台：Minecraft 26.3 + Fabric](0001-target-minecraft-26.3-fabric.md)（Accepted）
- [0002 不迁移 1.12 存档，注册 ID 平铺化](0002-no-legacy-world-migration-and-flattening.md)（Accepted）
- [0003 FE 电力经 Team Reborn Energy 互通](0003-fe-energy-via-team-reborn-energy.md)（Accepted）
- [0004 反弹道导弹系统：雷达火控 + 制导拦截](0004-anti-ballistic-missile-system.md)（Accepted）
- [0005 国际化：JSON 语言文件、必需语言与阿拉伯文预整形](0005-i18n-json-and-arabic-shaping.md)（Accepted）
- [0006 工程缺省：配置、测试、Lombok、联动模组](0006-engineering-defaults.md)（Accepted）

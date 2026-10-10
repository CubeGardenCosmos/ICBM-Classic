# 兼容性证据

架构文档描述“预期支持”；本目录保存“实际验证”。上游声明、版本元数据、同机其他项目的结果都**不等于**本项目验收通过。

## 记录

- [Minecraft 26.3 / Fabric 工具链](minecraft-26.3-fabric.md)
- [能源互通（Team Reborn Energy / Tech Reborn）](energy-interop.md)
- [阿拉伯文渲染](arabic-rendering.md)
- [记录模板](template.md)

## 状态

- `Not tested`：有上游声明或候选版本，未在本项目环境运行。
- `Partial`：仅通过明确列出的部分场景。
- `Passed`：列出的精确版本/系统下 MUST 场景全部通过。
- `Failed`：可复现失败，附日志。
- `Not supported`：产品范围排除，写明理由与重新评估条件。

状态只适用于记录中列出的版本与环境。构件要记文件名、版本、SHA-256、来源 URL。

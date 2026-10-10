# 兼容性：阿拉伯文渲染（ar_sa）

- 状态：Not tested
- 调查日期：2026-10-10

## 上游事实（javap 核实）

- `net.minecraft.client.resources.language.FormattedBidiReorder`（26.3）引用 `com.ibm.icu.text.Bidi`、`com.ibm.icu.text.BidiRun`，不引用 `ArabicShaping`。
- 26.3 客户端库包含 `com.ibm.icu:icu4j:78.3`。
- 26.3 原版资源包含 `minecraft/lang/ar_sa.json`。

推论（待验证）：原版只做 Bidi 重排、不做连写整形，因此本项目采用预整形（ADR 0005）。

## 本项目测试

| 场景 | 结果 | 证据 |
| --- | --- | --- |
| 物品名称（创造栏悬停）连写与方向 | Not tested | S8.12（人工，截图） |
| 雷达 GUI 文本 | Not tested | S8.12 |
| 含占位符的消息（如“已发射，目标 %s”） | Not tested | S8.12 |
| 原版文本（对照组：原版 ar_sa 物品名是否连写） | Not tested | S8.12 |

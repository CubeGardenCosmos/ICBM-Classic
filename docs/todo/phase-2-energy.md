# P2：FE 电力

状态：TODO。前置：P1。设计：[energy-fe.md](../architecture/energy-fe.md)、ADR 0003。

## 实现

- [ ] (1) 引入 `teamreborn:energy:5.0.0`（Jar-in-Jar），建立 `EnergyApi`。
- [ ] (2) `ExposedEnergyStorage`（事务安全）。
- [ ] (3) 所有机器注册 `EnergyStorage.SIDED`；发射台网络平均分配。
- [ ] (4) 电池 `SimpleEnergyItem`；机器电池槽放电。
- [ ] (5) EMP 抽空外部能量存储。
- [ ] (6) GUI 能量条、FE 格式化、提示文本。

## 验收

- [ ] (1) GameTest：事务回滚、网络分配、电池放电、外部推送、EMP 抽空。
- [ ] (2) [H] 与 Tech Reborn 6.2.0 实机互通。

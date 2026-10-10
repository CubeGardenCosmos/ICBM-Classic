# P4：国际化

状态：TODO。前置：P3。设计：[i18n.md](../architecture/i18n.md)、ADR 0005。

## 实现

- [ ] (1) 翻译键审计与硬编码文本清理。
- [ ] (2) `en_us` 补全与 `check_lang.py` 完整校验（`--strict`）。
- [ ] (3) 术语表补全（爆炸物名称）。
- [ ] (4) zh_cn。
- [ ] (5) fr_fr。
- [ ] (6) de_de。
- [ ] (7) ja_jp。
- [ ] (8) ar_sa 源文件与 `shapeArabic` 生成。
- [ ] (9) 旧语言迁移（es_es、ko_kr、pt_br、ru_ru）。

## 验收

- [ ] (1) `check_lang.py --strict` 通过。
- [ ] (2) [H] 母语审校（每种必需语言一名审校者）与阿拉伯文实机截图。

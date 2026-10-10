#!/usr/bin/env python3
"""移植账本：生成、统计、检查（S0.06）。

账本是 1.12 → 26.3 移植进度的唯一记录，覆盖 legacy-1.12/src/main/java 下每个 .java 文件。

用法：
  python3 -I scripts/port_ledger.py generate [--force]
  python3 -I scripts/port_ledger.py stats
  python3 -I scripts/port_ledger.py check

状态取值：
  TODO                未开始
  PORTED              已移植到同名路径（客户端类在 src/client）
  MERGED → <新文件>    合并进新文件（例如 ModBlocks）
  DROPPED（原因）       不再移植，必须写原因

「步骤」列由 docs/architecture/porting-guide.md §3 的包前缀映射预填；§3 未列出的行留 `—`，
由实际执行该文件移植的步骤补写（S5.11 要求账本中不再有 TODO）。

只用标准库；脚本一律以 `python3 -I` 运行。
"""

import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LEGACY_JAVA = ROOT / "legacy-1.12" / "src" / "main" / "java"
LEDGER = ROOT / "docs" / "implementation" / "port-ledger.md"
REL_PREFIX = "legacy-1.12/src/main/java"

STATUS_TODO = "TODO"
STATUS_PORTED = "PORTED"

# porting-guide.md §3 的包级映射，最长前缀优先。
# 每项：(相对 legacy-1.12/src/main/java 的前缀, 步骤, 说明, 初始状态)
PREFIX_MAP = [
    # 底层库与纯 Java
    ("com/builtbroken/jlib/", "S1.01", "jlib 纯 Java，ByteBuf 相关改 FriendlyByteBuf", STATUS_TODO),
    ("icbm/classic/lib/transform/", "S1.01", "Vec3d→Vec3、EnumFacing→Direction", STATUS_TODO),
    ("icbm/classic/config/", "S1.02", "改写为 @IcbmConfig JSON 配置", STATUS_TODO),
    ("icbm/classic/lib/saving/", "S1.03", "NbtSaveHandler → SaveHandler（ValueInput/ValueOutput）", STATUS_TODO),
    ("icbm/classic/lib/network/", "S1.04", "PacketCodex 保留 API，netty/ 与 PacketManager 删除", STATUS_TODO),
    ("icbm/classic/lib/radio/", "S1.05", "RadioRegistry 按 ResourceKey<Level> 分表", STATUS_TODO),
    ("icbm/classic/api/radio/", "S1.05", "同上", STATUS_TODO),
    ("icbm/classic/lib/radar/", "S1.06", "RadarRegistry 按 ResourceKey<Level> 分表", STATUS_TODO),
    ("icbm/classic/lib/capability/", "S1.07", "按 §2.6 改为 API Lookup / 附件 / 组件", STATUS_TODO),
    ("icbm/classic/api/caps/", "S1.07", "同上", STATUS_TODO),
    ("icbm/classic/lib/energy/", "S1.07、S6", "S1.07 建内部 IcbmEnergyBuffer，S6 完成门面", STATUS_TODO),
    ("icbm/classic/api/energy/", "S1.07、S6", "同上", STATUS_TODO),
    ("icbm/classic/api/actions/", "S1.08", "动作系统", STATUS_TODO),
    ("icbm/classic/lib/actions/", "S1.08", "动作系统", STATUS_TODO),
    ("icbm/classic/content/actions/", "S1.08", "动作系统", STATUS_TODO),
    ("icbm/classic/lib/thread/", "S1.09", "工作线程只读快照 + 主线程按预算应用", STATUS_TODO),
    ("icbm/classic/content/blast/thread", "S1.09", "同上（content/blast/thread*/）", STATUS_TODO),
    ("icbm/classic/api/", "S1.10", "其余公共 API 换 26.3 类型", STATUS_TODO),
    # 注册与资源
    ("icbm/classic/content/reg/", "S2", "Mod* 注册类；BlockReg→ModBlocks 等改名", STATUS_TODO),
    ("icbm/classic/lib/explosive/", "S2.02", "ExplosiveRegistry + 平铺化内容生成", STATUS_TODO),
    ("icbm/classic/api/reg/", "S2.02", "同上", STATUS_TODO),
    ("icbm/classic/content/radioactive/", "S2.03、S3.08", "平铺化方块", STATUS_TODO),
    ("icbm/classic/content/blocks/BlockConcrete.java", "S2.03、S3.08", "平铺化方块", STATUS_TODO),
    ("icbm/classic/content/blocks/BlockReinforcedGlass.java", "S2.03、S3.08", "平铺化方块", STATUS_TODO),
    ("icbm/classic/content/blocks/BlockSpikes.java", "S2.03、S3.08", "平铺化方块", STATUS_TODO),
    ("icbm/classic/content/blocks/BlockGlassButton.java", "S2.03、S3.08", "平铺化方块", STATUS_TODO),
    ("icbm/classic/content/blocks/BlockGlassPressurePlate.java", "S2.03、S3.08", "平铺化方块", STATUS_TODO),
    ("icbm/classic/content/items/ItemCrafting", "S2.04", "平铺化材料物品", STATUS_TODO),
    ("icbm/classic/content/items/ItemBase", "S2.04", "平铺化材料物品", STATUS_TODO),
    ("icbm/classic/RecipeGeneration.java", "S2.08", "改为 datagen ICBMRecipeProvider", STATUS_TODO),
    # 爆炸
    ("icbm/classic/content/blast/", "S3.02–S3.07", "各爆炸实现", STATUS_TODO),
    ("icbm/classic/content/gas/", "S3.05", "气体爆炸", STATUS_TODO),
    ("icbm/classic/content/potion/", "S3.05", "药水效果", STATUS_TODO),
    ("icbm/classic/content/blocks/explosive/", "S3.09", "每个爆炸物一个 ExplosiveBlock + 共享 BlockEntity", STATUS_TODO),
    ("icbm/classic/command/", "S3.10", "Brigadier 命令", STATUS_TODO),
    ("icbm/classic/lib/projectile/", "S3.01、S3.03", "投射物基类与爆炸相关实体", STATUS_TODO),
    ("icbm/classic/content/entity/", "S3.01、S3.03", "爆炸相关实体（碎片、飞行方块等）", STATUS_TODO),
    # 导弹
    ("icbm/classic/content/missile/", "S4", "导弹实体与飞行逻辑", STATUS_TODO),
    ("icbm/classic/content/cluster/", "S4", "集束导弹与子弹药", STATUS_TODO),
    ("icbm/classic/content/cargo/", "S4", "货物投放（降落伞/气球）", STATUS_TODO),
    # 机器与工具
    ("icbm/classic/prefab/", "S5.01", "方块实体基类、库存、GUI 组件", STATUS_TODO),
    ("icbm/classic/content/blocks/launcher/", "S5", "发射台网络与巡航发射器", STATUS_TODO),
    ("icbm/classic/content/blocks/radarstation/", "S5", "雷达站", STATUS_TODO),
    ("icbm/classic/content/blocks/emptower/", "S5", "EMP 塔", STATUS_TODO),
    # 客户端（目标：src/client/java/icbm/classic/client/）
    ("icbm/classic/client/", "S2.06、S4.07、S5.07", "客户端类移入 client 源集", STATUS_TODO),
    # 不移植
    ("icbm/classic/mods/", "—", "联动模组代码删除（ADR 0006）", "DROPPED（ADR 0006）"),
    ("icbm/classic/datafix/", "—", "不迁移旧存档（ADR 0002）", "DROPPED（ADR 0002）"),
    ("icbm/classic/CommonProxy.java", "—", "Fabric 无 proxy，逻辑并入入口类", "DROPPED（无 proxy）"),
]

TABLE_HEADER = [
    "| 状态 | 旧文件（相对 " + REL_PREFIX + "） | 目标 / 说明 | 步骤 |",
    "| --- | --- | --- | --- |",
]


def map_file(rel: str):
    """返回 (步骤, 说明, 初始状态)。未匹配 §3 映射时步骤为 `—`。"""
    for prefix, step, note, status in PREFIX_MAP:
        if rel.startswith(prefix):
            return step, note, status
    return "—", "", STATUS_TODO


def legacy_files():
    if not LEGACY_JAVA.is_dir():
        sys.exit(f"error: 找不到 {LEGACY_JAVA}（S0.02 未完成？）")
    files = sorted(p.relative_to(LEGACY_JAVA).as_posix() for p in LEGACY_JAVA.rglob("*.java"))
    if not files:
        sys.exit(f"error: {LEGACY_JAVA} 下没有 .java 文件")
    return files


def cmd_generate(argv):
    force = "--force" in argv
    if LEDGER.exists() and not force:
        sys.exit(f"error: {LEDGER.relative_to(ROOT)} 已存在，拒绝覆盖（会抹掉进度）。需要 --force。")
    files = legacy_files()
    lines = [
        "# 1.12 → 26.3 移植账本",
        "",
        f"由 `python3 -I scripts/port_ledger.py generate` 生成；覆盖 `{REL_PREFIX}` 下全部 {len(files)} 个 .java 文件。",
        "状态：`TODO`、`PORTED`、`MERGED → <新文件>`、`DROPPED（原因）`；`DROPPED` 必须有原因。",
        "「步骤」列按 [porting-guide.md §3](../architecture/porting-guide.md#3-包级移植映射) 预填；`—` 表示 §3 未列出，",
        "由执行该文件移植的步骤补写。改动本文件即改动进度，`check` 要求所有行不再是 `TODO`。",
        "",
        *TABLE_HEADER,
    ]
    for rel in files:
        step, note, status = map_file(rel)
        lines.append(f"| {status} | {rel} | {note} | {step} |")
    lines.append("")
    LEDGER.parent.mkdir(parents=True, exist_ok=True)
    LEDGER.write_text("\n".join(lines), encoding="utf-8")
    print(f"generated {LEDGER.relative_to(ROOT)}: {len(files)} rows")


def parse_ledger():
    """返回 [(状态, 文件, 说明, 步骤)]，跳过表头与分隔行。"""
    if not LEDGER.is_file():
        sys.exit(f"error: {LEDGER.relative_to(ROOT)} 不存在，先运行 generate")
    rows = []
    for line in LEDGER.read_text(encoding="utf-8").splitlines():
        if not line.startswith("|"):
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) != 4 or cells[0] == "状态" or set(cells[0]) <= {"-", ":"}:
            continue
        rows.append(tuple(cells))
    if not rows:
        sys.exit(f"error: {LEDGER.relative_to(ROOT)} 中没有解析到账本行")
    return rows


def cmd_stats():
    rows = parse_ledger()
    statuses = Counter(status for status, *_ in rows)
    print(f"total: {len(rows)}")
    for status, count in sorted(statuses.items()):
        print(f"  {status}: {count}")
    todo_by_step = defaultdict(int)
    for status, _, _, step in rows:
        if status == STATUS_TODO:
            todo_by_step[step] += 1
    print("TODO by step:")
    for step, count in sorted(todo_by_step.items()):
        print(f"  {step}: {count}")
    return 0


def cmd_check():
    rows = parse_ledger()
    todos = [rel for status, rel, _, _ in rows if status == STATUS_TODO]
    for rel in todos:
        print(f"TODO {rel}")
    print(f"TODO rows: {len(todos)} / {len(rows)}")
    return 1 if todos else 0


def main(argv):
    if len(argv) < 2:
        sys.exit(__doc__.strip())
    cmd, rest = argv[1], argv[2:]
    if cmd == "generate":
        cmd_generate(rest)
        return 0
    if cmd == "stats":
        return cmd_stats()
    if cmd == "check":
        return cmd_check()
    sys.exit(f"error: 未知命令 {cmd!r}（可用：generate / stats / check）")


if __name__ == "__main__":
    sys.exit(main(sys.argv))

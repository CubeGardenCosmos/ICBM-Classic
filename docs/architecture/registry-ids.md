# 注册 ID 对照表（平铺化）

依据 [ADR 0002](../decisions/0002-no-legacy-world-migration-and-flattening.md)。命名空间一律 `icbmclassic`。所有 ID 小写 snake_case。“1.12 ID/元数据”一栏仅用于对照旧代码。

**规则**：本表是唯一事实源。代码中的 ID 字符串必须与本表一致；新增/改名 ID 先改本表（同一提交）。

## 1. 爆炸物（`ExplosiveRegistry`，不是 Minecraft 注册表）

“形态”列决定会生成哪些方块/物品：B=爆炸物方块，M=导弹，G=手雷，C=炸弹矿车，D=子弹药（bomblet）。规则与 1.12 `ExplosiveInit#newEx` 一致：等级≠NONE → B、M、C；等级=ONE → 另加 G、D。

| 爆炸物 ID | 1.12 名/数值 | 等级 | 形态 |
| --- | --- | --- | --- |
| `condensed` | condensed / 0 | 1 | B M G C D |
| `shrapnel` | shrapnel / 1 | 1 | B M G C D |
| `incendiary` | incendiary / 2 | 1 | B M G C D |
| `debilitation` | debilitation / 3 | 1 | B M G C D |
| `chemical` | chemical / 4 | 1 | B M G C D |
| `anvil` | anvil / 5 | 1 | B M G C D |
| `repulsive` | repulsive / 6 | 1 | B M G C D |
| `attractive` | attractive / 7 | 1 | B M G C D |
| `colors` | colors / -1 | 1 | B M G C D |
| `smoke` | smoke / -1 | 1 | B M G C D |
| `fragmentation` | fragmentation / 8 | 2 | B M C |
| `contagious` | contagious / 9 | 2 | B M C |
| `sonic` | sonic / 10 | 2 | B M C |
| `breaching` | breaching / 11 | 2 | B M C |
| `thermobaric` | thermobaric / 13 | 2 | B M C |
| `nuclear` | nuclear / 15 | 3 | B M C |
| `emp` | emp / 16 | 3 | B M C |
| `exothermic` | exothermic / 17 | 3 | B M C |
| `endothermic` | endothermic / 18 | 3 | B M C |
| `antigravitational` | antigravitational / 19 | 3 | B M C |
| `ender` | ender / 20 | 3 | B M C |
| `antimatter` | antimatter / 22 | 4 | B M C |
| `redmatter` | redMatter / 23 | 4 | B M C |
| `rot` | rot / 25 | 无 | （只有爆炸，无物品） |
| `mutation` | mutation / 26 | 无 | （只有爆炸，无物品） |

已删除：`hypersonic`（21，1.12 中已是占位）、`missile`（24，导弹模块占位；改为普通物品 `missile_module`，见 §3）。

## 2. 方块

| 26.3 方块 ID | 1.12 ID / meta | 方块实体 | 物品 |
| --- | --- | --- | --- |
| `<ex>_explosive`（23 个，见 §1 形态 B） | `explosives` / 爆炸物 meta | `explosive`（共享一个类型） | 同名 BlockItem |
| `concrete` | `concrete` / 0 | — | 同名 |
| `compact_concrete` | `concrete` / 1 | — | 同名 |
| `reinforced_concrete` | `concrete` / 2 | — | 同名 |
| `reinforced_glass` | `reinforcedGlass` | — | 同名 |
| `glass_button` | `glassButton` | — | 同名 |
| `glass_pressure_plate` | `glassPressurePlate` | — | 同名 |
| `spikes` | `spikes` / 0 | — | 同名 |
| `poison_spikes` | `spikes` / 1 | — | 同名 |
| `fire_spikes` | `spikes` / 2 | — | 同名 |
| `radioactive_dirt` | `radioactive` / 0 | — | 同名 |
| `radioactive_stone` | `radioactive` / 1 | — | 同名 |
| `launcher_base` | `launcherbase` | `launcher_base` | 同名 |
| `launcher_frame` | `launcherframe` | `launcher_frame` | 同名 |
| `launcher_connector` | `launcher_connector` | `launcher_connector` | 同名 |
| `launcher_screen` | `launcherscreen` | `launcher_screen` | 同名 |
| `cruise_launcher` | `cruiseLauncher` | `cruise_launcher` | 同名 |
| `radar_station` | `radarstation` | `radar_station` | 同名 |
| `emp_tower` | `emptower` | `emp_tower`；假方块部分 `emp_tower_fake` | 同名（`ItemBlockEmpTower` 逻辑保留） |

1.12 的 `multiblock`（`TileMulti`）方块实体若仅被 EMP 塔使用，则合并进 `emp_tower_fake`；移植时用 `grep -rn TileMulti legacy-1.12/src` 确认，结果写入证据。

## 3. 物品（非方块）

| 26.3 物品 ID | 1.12 ID / meta | 备注 |
| --- | --- | --- |
| `<ex>_missile`（23 个） | `explosive_missile` / 爆炸物 meta | 导弹 |
| `missile_module` | `explosive_missile` / 24 | 空导弹壳，合成材料 |
| `<ex>_grenade`（10 个） | `grenade` / meta | |
| `<ex>_bomb_cart`（23 个） | `bombcart` / meta | |
| `<ex>_bomblet`（10 个） | `explosive_bomblet` / meta | 集束导弹子弹药 |
| `empty_bomblet` | `empty_bomblet` | |
| `cluster_missile` | `cluster_missile` | 子弹药配置存 DataComponent `explosive_settings` |
| `held_item_missile` | `held_item_missile` | 携带物品的导弹 |
| `anti_ballistic_missile` | `surface_to_air_missile` | ADR 0004；S4 起即使用新 ID |
| `parachute` | `parachute` | |
| `balloon` | `balloon` | |
| `antidote` | `antidote` | |
| `defuser` | `defuser` | |
| `radar_gun` | `radarGun` | |
| `remote_detonator` | `remoteDetonator` | |
| `laser_detonator` | `laserDetonator` | |
| `rocket_launcher` | `rocketLauncher` | `ItemRocketLauncher(false)` |
| `ballistic_launcher` | `ballisticLauncher` | `ItemRocketLauncher(true)` |
| `battery` | `battery` | S6：`EnergyStorage.ITEM` |
| `poison_powder` | `poisonPowder` | |
| `sulfur_dust` | `sulfurDust` | 加入 `c:dusts/sulfur` |
| `saltpeter` | `saltpeter` | 加入 `c:dusts/saltpeter` |
| `saltpeter_ball` | `saltpeter_ball` | |
| `steel_ingot` | `ingot` / steel | `c:ingots/steel` |
| `copper_ingot` | `ingot` / copper | 1.12 遗留；加入 `c:ingots/copper`。是否删除改用原版铜锭：**工程缺省保留** |
| `steel_clump` | `clump` / steel | |
| `steel_plate` | `plate` / steel | `c:plates/steel` |
| `iron_plate` | `plate` / iron | `c:plates/iron` |
| `basic_circuit` | `circuit` / basic | |
| `advanced_circuit` | `circuit` / advanced | |
| `elite_circuit` | `circuit` / elite | |
| `copper_wire` | `wire` / copper | |
| `gold_wire` | `wire` / gold | |

1.12 中存在但上表没有的物品：在移植账本中标记并 BLOCKED 询问，不要自行命名。

## 4. 实体类型

| 26.3 ID | 1.12 ID | 类 | 1.12 追踪距离（方块）/ 更新间隔 | 26.3 `clientTrackingRange`（区块）/ `updateInterval` |
| --- | --- | --- | --- | --- |
| `flying_block` | `block.gravity` | `EntityFlyingBlock` | 128 / 1 | 8 / 1 |
| `fragment` | `block.fragment` | `EntityFragments` | 40 / 1 | 3 / 1 |
| `primed_explosive` | `block.explosive` | `EntityExplosive` | 50 / 5 | 4 / 5 |
| `explosive_missile` | `explosive_missile` | `EntityExplosiveMissile` | 500 / 1 | 32 / 1 |
| `generic_missile` | `generic_missile` | `EntityMissileActionable` | 500 / 1 | 32 / 1 |
| `anti_ballistic_missile` | `surface_to_air_missile` | S4：`EntitySurfaceToAirMissile`；S7 改名 `EntityAntiBallisticMissile` | 500 / 1 | 32 / 1 |
| `held_item_missile` | `held_item_missile` | `EntityHeldItemMissile` | 500 / 1 | 32 / 1 |
| `explosion_holder` | `holder.explosion` | `EntityExplosion` | 100 / 5 | 7 / 5 |
| `light_beam` | `beam.light` | `EntityLightBeam` | 80 / 5 | 5 / 5 |
| `grenade` | `item.grenade` | `EntityGrenade` | 50 / 5 | 4 / 5 |
| `bomb_cart` | `cart.bomb` | `EntityBombCart` | 50 / 2 | 4 / 2 |
| `missile_seat` | `holder.seat` | `EntityPlayerSeat` | 50 / 2 | 4 / 2 |
| `redmatter` | `redmatter` | `EntityRedmatter` | 500 / 1 | 32 / 1 |
| `smoke` | `smoke` | `EntitySmoke` | 100 / 15 | 7 / 15 |
| `bomblet` | `bomb_droplet` | `EntityBombDroplet` | 500 / 1 | 32 / 1 |
| `parachute` | `parachute` | `EntityParachute` | 500 / 1 | 32 / 1 |
| `balloon` | `balloon` | `EntityBalloon` | 500 / 1 | 32 / 1 |

DROPPED：1.12 中注释掉的圣诞节怪物（`skeleton.xmas.*`、`zombie.xmas.*`）。

## 5. 菜单类型（`BuiltInRegistries.MENU`）

`launcher_base`、`launcher_screen`、`cruise_launcher`、`radar_station`、`emp_tower`。全部为 `ExtendedMenuType<?, BlockPos>`。

## 6. DataComponent（`BuiltInRegistries.DATA_COMPONENT_TYPE`）

| ID | 类型 | 用途 |
| --- | --- | --- |
| `explosive_settings` | `ExplosiveSettings`（record，Codec） | 集束导弹子弹药数、爆炸规模覆盖等定制；1.12 `IExplosiveCustomization` 存储 |
| `gps_data` | `GpsData(Optional<GlobalPos>)` | 遥控器/激光引爆器/雷达枪记录的坐标 |
| `radio_channel` | `String` | 遥控器无线电频道 |
| （不注册）| — | 电池存量**不**自建组件，直接使用 Team Reborn Energy 提供的 `EnergyStorage.ENERGY_COMPONENT`（`DataComponentType<Long>`，S6；见 energy-fe.md §5） |
| `held_item` | `ItemStackTemplate` 或 `ItemStack`（以 26.3 javap 为准） | 携带物品导弹装载物 |

## 7. 其他

- 声音事件：照搬 `legacy-1.12/src/main/resources/assets/icbmclassic/sounds.json` 的键名，转为小写 snake_case（`.` 保留）。S2.09 定稿的 16 项（括号内为 1.12 旧键，未写则同名）：
  `antimatter`、`beam_charging`(beamcharging)、`collapse`、`debilitation`、`emp`、`explosion`、`explosion_fire`(explosionfire)、
  `gas_leak`(gasleak)、`machine_hum`(machinehum)、`missile_engine`(missileinair)、`missile_launch`(missilelaunch)、
  `power_down`(powerdown)、`redmatter`、`sonic_wave`(sonicwave)、`meep`、`radioactive_tick`。
  字幕键固定为 `subtitles.icbmclassic.<id>`；`.ogg` 文件名沿用 1.12（如 `missileinair_short.ogg`），不随事件 ID 改名。
  1.12 的 `SoundCategory`（除 `meep` 为 NEUTRAL 外均为 BLOCKS）在 26.3 由播放点传入 `SoundSource`，不写进 `sounds.json`。
- 伤害类型（数据包 `damage_type`）：`missile_impact`、`radioactive_block`、`chemical_gas`、`contagious_gas`、`shrapnel`、`antimatter`、`redmatter`。来源：`grep -rn 'DamageSource("' legacy-1.12/src`；以 grep 结果为准，在 S3.02 证据中列出最终清单。
- 方块实体类型：见 §2 第三列。
- 粒子类型：S4.07 根据 `client/fx` 确定后补入本表。
- API Lookup ID：`missile`、`missile_launcher`、`missile_holder`、`emp_receiver`。
- 数据附件：`space_chicken`。

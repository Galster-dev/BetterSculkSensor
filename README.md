# BetterSculkSensor
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/bettersculksensor) <br>
The sensor we all wanted. Inspired by [Purplers](https://www.youtube.com/c/Purplers) ([video](https://youtu.be/LpKZS_8IZsw))
[![Requires Fabric API](https://i.imgur.com/Ol1Tcf8.png)](https://www.curseforge.com/minecraft/mc-mods/fabric-api)

## Features
### Video
| #   | Idea                                       | Done? | Note                                                                                                                                                                                                                |
|-----|--------------------------------------------|-------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1   | Make 2 versions of the sculk sensor        | ✔️    | The vanilla sculk sensor is left untouched                                                                                                                                                                          |
| 2   | Shorten the pulse length to 2gt            | ✔️    | Sensor now has additional cooldown (not powered) of 6 gt. In total you can get 1 pulse every 8gt                                                                                                                    |
| 3   | Remove unnecessary sounds from detection   | ❓️    | Sculk sensor (both vanilla and modded) get triggered by new mechanic called "game events" and not sounds. Because of that removing bloat is problematic (and actually not that needed) so this feature is not done. |
| 4   | Wipe out sculk frequencies feature 🔫      | ✔️    | Comparator does nothing special now                                                                                                                                                                                 |
| 5   | Make sculk sensor "tunable" like noteblock | ✔️    | You can calibrate BetterSculkSensor by right-clicking it with specific items (full list of item2event can be found [here](#Item2Event list))                                                                        |
| 6   | Make sculk sensor strong power blocks      | ✔️    | -                                                                                                                                                                                                                   |
| 7   | Allow pushing sculk sensor with pistons    | ❓️    | This mod does support pushable sculk sensors, but requires [Carpet Mod](https://github.com/gnembon/fabric-carpet/) to make it work. The command is `/carpet setDefault movableBlockEntities true`                   |

### Additional
| #   | Feature                                                         | Note                                                                                                           |
|-----|-----------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| 1   | Fixed multiple bugs related to vanilla sculk sensor             | I can't remember all of them but it's mostly about copying a block with NBT (Ctrl+middle click, WorldEdit etc) |
| 2   | Use block ticking to fix some (input?) bug about redstone delay | Needs more research                                                                                            |

## TODO
- [ ] Use tags instead of hardcoded items to calibrate the sensor
- [ ] ? Make the sensor steal the item on calibrating in survival
- [ ] Research redstone bug to find more clean solution

# Item2Event list
| Game event             | Item to calibrate       |
|------------------------|-------------------------|
| none                   | empty hand              |
| `step`                 | any* boots              |
| `flap`                 | phantom membrane        |
| `swim`                 | conduit                 |
| `elytra_glide`         | elytra                  |
| `hit_ground`           | any* falling block      |
| `teleport`             | ender pearl             |
| `splash`               | splash potion           |
| `entity_shake`         | any* carpet             |
| `block_change`         | observer                |
| `note_block_play`      | noteblock               |
| `projectile_shoot`     | bow                     |
| `drink`                | glass bottle            |
| `prime_fuse`           | flint and steel         |
| `projectile_land`      | arrow                   |
| `eat`                  | apple                   |
| `entity_interact`      | saddle                  |
| `entity_damage`        | any* sword              |
| `equip`                | any* chestplate         |
| `shear`                | shears                  |
| `entity_roar`          | dragon egg (questions?) |
| `block_close`          | any* trapdoor           |
| `block_deactivate`     | redstone lamp           |
| `block_detach`         | string                  |
| `dispense_fail`        | dispenser               |
| `block_open`           | any* door               |
| `block_activate`       | any* pressure plate     |
| `block_attach`         | tripwire hook           |
| `entity_place`         | egg                     |
| `block_place`          | stone                   |
| `fluid_place`          | any* filled bucket      |
| `entity_die`           | bone                    |
| `block_destroy`        | any* pickaxe            |
| `fluid_pickup`         | empty bucket            |
| `item_interact_finish` | item frame              |
| `container_close`      | barrel                  |
| `piston_contract`      | sticky pison            |
| `piston_extend`        | piston                  |
| `container_open`       | chest                   |
| `item_interact_start`  | glowing item frame      |
| `explode`              | tnt                     |
| `lightning_strike`     | lightning rod           |
| `instrument_play`      | goat horn               |
* "any" is only about vanilla items until we use tags (see [TODO](#TODO))

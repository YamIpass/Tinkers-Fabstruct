# 匠魂 3.12.1 → Fabric 1.20.1 移植进度（通宵批次）

> 目标：把 Forge 1.20.1 的匠魂（`[匠魂] TConstruct-1.20.1-3.12.1.231.jar`）**逐类搬过来**，只替换 Forge → Fabric 的接口层，玩法内容一行不改。
> 上游对照源码：`ref/TinkersConstruct`（3.12.1，Mojang 映射）。



## 2026-11-06 09:47 用户实测反馈修复
- **崩溃 `AbstractMethodError: NoContainerIngredient.test(...) is abstract`（向冶炼炉塞铁矿石）**：根因是 Fabric 的 `CustomIngredient#test(ItemStack)` 是 Fabric API 方法、运行时名保持 `test`，而我们这些自定义 ingredient 同时继承原版 `Ingredient`，其 `test` 被重映射成 `method_8093`；Fabric 的 `IngredientMixin` 解析 `fabric:type` 时调 `customIngredient.toVanilla()`，默认实现会包一层 `CustomIngredientImpl`，而它的 `method_8093` 正是调 `CustomIngredient#test` → 类上没有该名字的方法 → 崩溃。修法：在 `NestedIngredient` 与 `BlockTagIngredient`/`InstrumentIngredient`/`MaterialValueIngredient`/`ToolHookIngredient` 里把 `toVanilla()` 覆写为 `return this;`（我们本来就是原版 Ingredient，不需要包装）。回归自测：`checkMelting` 现在会把**每一条**熔化配方都对着一块铁矿石跑 `matches()`（之前就是这条路径崩的），日志 `1 melting recipes matched an iron ore without a custom ingredient crash`。
- **焦黑玻璃（`seared_glass`）能穿过 + 减速 —— 真 bug，已修**：`TinkerSmeltery` 里灵魂玻璃那行写的是 `SEARED_GLASS.noCollission().speedFactor(0.1f)...`，而 `BlockBehaviour.Properties#noCollission()`/`speedFactor()` **是就地修改并返回同一个实例**，不是复制。于是这一行把共享的 `SEARED_GLASS` 字段改成了无碰撞 + 0.1 速度，所有用该实例注册的方块（`seared_glass`、`seared_glass_pane`、`seared_tinted_glass`、以及 `scorched_*` 同款）都变成可穿行且减速 —— 与用户截图里「焦黑玻璃」完全吻合。修法：灵魂玻璃改为用 `builder(...)` 现场构造全新 properties，不再从 `SEARED_GLASS` 派生。自测 `checkGlass` 现在断言 `seared solid=true seared speed=1.0`、`seared soul solid=false`（灵魂玻璃按上游设计可穿行）。全仓审计过其余 `noCollission()`/`speedFactor()`/`friction()` 用法，都是各建各的新 builder，无第二处同类问题。
- **通透玻璃「可穿过 + 减速」**（已排除）：`clear_glass`/`clear_stained_glass` 碰撞形状非空、速度系数 1.0；只有 `soul_glass`（灵魂玻璃）按上游设计 `.speedFactor(0.2F).noCollission()`，语言文件工具提示「与普通方块相似，但是可以从中穿过」。

## 改名与发布准备（2026-11-06 10:4x）

- 显示名统一为 **Tinkers' Fabstruct**，产物文件名 `Tinkers-Fabstruct-1.20.1-0.1.0.jar`（`archivesName = "${archives_base_name}-${minecraft_version}"`），但**保持 mod id `tconstruct`**：数据包/标签/配方/存档全按这个命名空间索引，改 id 会让所有存档失效。
- 单一来源：`gradle.properties` 的 `mod_name=Tinkers' Fabstruct` → `build.gradle` 的 `processResources` 展开进 `fabric.mod.json` 的 `name`；语言文件里补了 `modmenu.nameTranslation.tconstruct` = `Tinkers' Fabstruct`（玉 / ModMenu 会**先**读这个 key，再回退到元数据名，这就是游戏内工具提示上方那行模组名的真正来源）和 `key.categories.tconstruct`（按键分类）。
- 一条命令改全部显示名：`tools/rename_mod.py "新名字"`（已按 UTF-8 BOM 容错，`zh_cn` 等带 BOM 的语言文件也能处理）（可选 `--item-groups "匠魂丨"` 连创造栏标签前缀一起换）。
- 法务材料：仓库根新增 `LICENSE`（MIT，含 SlimeKnights 与移植者两段版权行）与 `NOTICE`（逐个列出内嵌库与许可：Mantle MIT、Porting Lib **LGPL-3.0**、Star/Cardinal/Dripstone/Milk/MixinSquared/Reach=MIT、NightConfig LGPL-3.0、jsr305 Apache-2.0，并声明「非官方、无商标授权」）；`build.gradle` 的 `jar` 块把它们放进 `META-INF/`，所以发布出去的 jar 自带许可与归属信息。当前 jar **31,560,585 B**（`fabric.mod.json` 的 name 是 `Tinkers' Fabstruct`；部署脚本按 `Tinkers-Fabstruct-*.jar` 选包并排除 `-sources`/`-dev`，同时删除实例里旧文件名的 jar，避免同名 mod id 冲突）。

## 通宵收尾（2026-11-06 06:47）

- **最终产物**：`build/libs/tconstruct-fabric-0.1.0.jar` = **31,575,969 B**，`sha256 09afdd993f5f78e816cf2fbfab854d446aada3e89d56f36fd006098ba3c2cf0f`，与已部署到 `D:/game/.minecraft/versions/1.20.1-Fabric 0.20.1/mods/tconstruct-fabric-0.1.0.jar` 的副本**逐字节一致**。
- **最终自测**：`rm -rf run/world && ./gradlew runServer -Pselftest=true` → 55 条 `[selftest]` 输出，`all checks passed`，0 failure。
- **最终客户端**：`runClient --quickPlaySingleplayer` 正常进世界（`Loaded 1697 advancements`、1934 ms），无 `Exception`/`Unable to bake`/`Missing texture`，ERROR 仅 `No data fixer registered for tconstruct:*` 与 `mantle:sign` 同类噪音。
- **早上请重点实测**：① 无缝玻璃连通、② 通透玻璃各色玻璃板物品图标、③ 储罐/罐物品右键倒取流体、④ 浇铸台铸模与产物、⑤ 整理背包与创造栏切页、⑥ 工具耐久条与修饰符 tooltip、⑦ 冶炼炉+铸造炉+龙头+排液口整套多方块、⑧ 工作台组装/修理/升级。
- **已知遗留**：`TODO(fabric)` 361 处（绝大多数是「Fabric 无对应 API」说明性注释）；洋红色块已确认是 `clear_glass/magenta` 本色 `FFB24CD8`；`crafting player` 线程本地上游也无消费者；浆液（slime）/投掷物等实体行为没有自动化断言，只能靠手测。

## 最新状态（2026-11-06 04:0x）

- **编译**：`./gradlew build --offline -x test` 成功，0 错误。产物 `build/libs/tconstruct-fabric-0.1.0.jar` 31,575,969 B。
- **无缝玻璃（连通贴图）已修**：Fabric 的 `BlockModel` 不保留 `#all` 这类引用的父模型映射，`ConnectedModel.getConnectedNameUncached` 只能迭代到本模型自己的 `textures`，`#down`/`#up`/`#north`… 永远解析不到 `connection.textures` 里的 `all`，于是每个面都保留带边框的基础贴图。修法（`ref/Mantle-fabric/src/main/java/slimeknights/mantle/client/model/connected/ConnectedModel.java`）：比较前剥掉 `#`；标准六面键在 `connectedTextures` 只有 `all` 时回退到 `all`。进世界实机日志验证：`connected bake ... faces down=#all_d up=#all_u north=#all_ud south=#all_ud west=#all_ul east=#all_ur`。
- **服务端自测**：`rm -rf run/world && ./gradlew runServer -Pselftest=true` 全绿 —— 配方类型计数（part builder 169 / material 217 / tinker station 281 / melting 850 / casting table 527 / casting basin 166 / alloy 31 / fuel 3）、`tool definitions 45`、`materials 96`、铁镐 275 耐久 / 6.0 挖掘速度、储罐 1000 mB 流体（倒/取两条路径）、`recipe errors: 0`、`Failed to load modifier 0`、`Loaded 300 dynamic modifiers and 6 modifier redirects`、`all checks passed`。
- **客户端**：可进世界；`Finished loading 208 Block entity items`（方块实体展示物品/铸模恢复）；剩余 ERROR 仅 `No data fixer registered for tconstruct:*` 45 条（原版习惯性噪音）。
- **数据**：配方 3018 条全部可解析（`forge:conditional` 展开、畸形 `fabric:any` 归一化、`forge:difference` 语义等价改写、外 mod 配方按 Forge 条件语义丢弃）；资源与 jar 100% 对齐（models 697/852、blockstates 435、textures 13867、lang 20）。
- **已修（用户实测驱动）**：无缝玻璃连通贴图（本次）、冶炼炉流体区几何（`GuiSmelteryTank` 传 106×106）、融化栏热量条槽位（`i + indexOffset`）、桶倒/取流体两条路径（Mantle `FluidTransferHelper` 的 mB 回退与原版桶分支 + 控制器 `getFluidStorage`）、熔融桶贴图（烘焙期从 `FluidTextureManager` 取 still 贴图）、Star 区块流体渲染 NPE（注册 `FluidRenderHandlerRegistry`）、创造背包切页崩溃（`TankModel`/`FluidContainerModel` 兜底 + `FluidVariantRendering`）、方块渲染层（`gen_render_layers.py` → `TinkerRenderLayers`）、通透玻璃板物品图标（预染色贴图 + `item/generated`）、合成镐子崩溃（`Item#getCraftingRemainingItem` 可空）、装备槽 NPE（`ModifiableItem.getEquipmentSlot` 回退 MAINHAND）、属性 id（`porting_lib:`）、重复 reload listener（5 个 manager 的 `init` 幂等）、说明面板字号 1.25（唯一主动偏离上游处）。
- **产物自检（2026-11-06 04:35）**：`tools/verify_jar.sh` 全部 ok（entrypoint main/client、access widener 声明与文件、mixin 配置、35 个内嵌 jar、5240 data json、1549 模型、13888 贴图、2491 class）；解包确认内嵌 `META-INF/jars/Mantle-1.20.1-1.11.DEV.eb1e9a5.jar` 的 `ConnectedModel$Baked` 含 `FACES` 回退且**不含**调试用的 `DEBUG_CONNECTED`；`ProjectileWeaponMixin.class` 在包内。
- **客户端冒烟（同时间）**：`./gradlew runClient --offline --args="--quickPlaySingleplayer 新的世界"` → 正常进世界、`Loaded 79 advancements`（含匠魂进度，中文名正常）、无 `Exception`/`Unable to bake`/`Missing texture`；`Model loader` 一类警告只剩 Star/Indium/Sodium 相关（`libs` 内 Star 调试库对缺失 mod 的 mixin target 探测）。
- **部署**：`D:/game/.minecraft/versions/1.20.1-Fabric 0.20.1/mods/tconstruct-fabric-0.1.0.jar`（31,575,969 B，用 `tools/deploy.sh` 部署；有 dev 客户端在跑时该脚本会拒绝）。
- **生物群系**：Forge 的 8 个 `forge/biome_modifier` 已用 `TinkerBiomeModifications`（Fabric `BiomeModifications.addFeature/addSpawn`）等价实现，自测验证钴矿/天空晶洞已注入对应生物群系。
- **已修（本轮新增）**：铸造台浇铸不出产物（`CastingFluidHandler.insert` 的外层关闭回调顺序——报内容变化的回调先跑、`currentRecipe` 还是 null 导致 `coolingTime = -1`；修法：设 `currentRecipe` 后立即 `onContentsChanged()` 重算冷却）、enderference 取消传送（改用 Porting Lib `EntityEvents.TELEPORT` + `setCanceled(true)`，替换原 TODO 注释块）。
- **TODO(fabric) 审计（2026-11-06 04:5x）**：按注释归类 top 项 —— crafting player 线程本地 9（上游无消费者）、Mantle 1.11 无 `html` 书页导出 8、`getModelData` 7、`FluidType.BUCKET_VOLUME` 7（已按 1000 mB 平移）、`BlockEntityPacket` 6（改为自查找 BE）、`Block#getBlockPathType` 6（改 Fabric path node registry）、Forge capability 5、`InventoryBlockEntity` 4、`setCustomClientFactory` 4；逐条抽查 `Item#isRepairable`（`ModifiableItem`/`ModifiableArmorItem`/`ModifiableLauncherItem`）与上游 3.12.1 源码逐字对比：上游同样 `return false; // handle in the tinker station`，**行为完全一致**，注释仅为解释移植边界。
- **新增自测**：`checkDrain`（直接在墙里建 `seared_drain`（`HORIZONTAL_FACING=EAST`）+ 焦黑砖 5×5 地板/墙 + `foundryController` 同款控制器，成型后 `FluidStorage.SIDED.find(level, drainPos, WEST)` 往里注 90 mB 熔铁 → 冶炼炉 `getTank()` 收到 90 mB；验证 `SmelteryFluidIO.getCapability` → `ISmelteryTankHandler.getFluidCapability` 这条「排液口 → 控制器储罐」链（也就是之前修控制器右键时补的 `HeatingStructureBlockEntity.getFluidStorage`）。**注意**：排液口必须在**结构首次成型前**就放进墙里，因为 `MultiblockStructureData.assignMaster` 对 `oldStructure.contains(pos)` 的坐标会跳过（上游逻辑如此），成型后再替换墙砖不会重新绑定 master，BE 会一直 `master=null`、流体能力为 null）
- **新增自测**：`checkToolTraits`（铁镐装配后断言 `ToolStack.from(stack).getModifiers().getEntry(new ModifierId(TConstruct.getResource("pierce")))` 非空且 `getLevel() == 1`，验证 `tconstruct:tool_definitions/pickaxe` 的 `tconstruct:traits` 模块在装配时把 trait 注入工具）
- **客户端冒烟（2026-11-06 06:33，排液口/熔化炉/龙头/铸造炉全部合入后）**：`./gradlew runClient --offline --args="--quickPlaySingleplayer 新的世界"` → 进世界正常、`Loaded 1697 advancements`、`Time elapsed: 1934 ms`；`[ERROR]` 共 45 条，除 `No data fixer registered for tconstruct:*` 外仅 `mantle:sign`/`mantle:hanging_sign` 两条同类噪音；无 `Exception`/`Unable to bake`/`Missing texture`；Sodium/Indium/Frex 的 `ClassNotFoundException` 只是 dev 环境没有这些目标 mod 的探测警告
- **资源引用审计（2026-11-06 06:4x，`tools/audit_models.py` + 内联脚本）**：1549 个模型全部检查了 `parent` 与 `textures`（namespace 省略时按 `minecraft:` 解析，与 `ResourceLocation` 一致；也把 `libs/*.jar`（Porting Lib 自带 `assets/forge/models/item/default`、`default-tool`、`bucket_drip`）算作可解析来源）→ **0 个缺失贴图**；剩 3 个 `tconstruct:block/slimy_mud_*` 父模型缺失，**上游 3.12.1 jar 里同样缺**（`assets/tconstruct/models/item/slimy_mud_blue.json` 指向从未发布的方块模型，没有任何 item 注册它）→ 与上游一致、无运行时影响。435 个 blockstate 的 `model` 引用同样审计：10 个 `tconstruct:block/*_frame`/`*_frame_map` 缺失，实测也是**上游遗留**（3.12 的边框是实体 `frame/gold` 等模型 + `FancyItemFrameEntity`，对应 block 根本没注册），与上游逐字相同。结论：资源层没有静默缺贴图。
- **新增自测**：`checkShield`（木盾芯 + 铁板 = `ToolBuildHandler.buildItemFromMaterials(TinkerTools.plateShield, MaterialNBT.of(wood, iron))` → 断言 `DURABILITY 270`（铁 `plating_shield` 的 durability 值）、`BLOCK_AMOUNT 100`、`BLOCK_ANGLE 180`，即 `tconstruct:material_stats` 双 stat 类型与 `base_stats` 的 block 数值都进了盾牌；日志 `the wood/iron plate shield has 270.0 durability, blocks 180.0 degrees for 100.0 damage`）
- **音效与本地化审计（2026-11-06 06:4x）**：`assets/tconstruct/sounds.json` 30 个事件、65 个音效引用全部能解析（自命名空间 0 缺失；无 namespace 时按 `minecraft:` 解析，与 vanilla 一致）；`data/**` 里 0 处引用不存在的 `tconstruct:` 音效事件。语言文件对照上游 jar：`en_us` 3615 → 4356（多出的是移植补的 overlay key），`zh_cn` 3561 → 4300；`en_us` 有而 `zh_cn` 缺的 key **55 条，与上游 jar 完全相同**（venom 材质名、jei 提示等上游也没翻），不存在移植引入的缺翻。
- **新增自测**：`checkArmorModifier`（铁板盔 + 钻石走工作台 `calcResult` → `tconstruct:diamond` 挂上、upgrade 槽 2→1、`ARMOR 2.0 → 3.0`、`DURABILITY 165 → 415`；数值全部来自 `data/tconstruct/tinkering/modifiers/diamond.json` 的 `stat_boost`：耐久 +500 再对 `tconstruct:modifiable/armor` 减 250，另有**无条件** `armor +1.0`、`attack_damage +0.5`。第一版断言写成「armor 不变」被判 red，确认是**上游数据行为**、不是 bug，已改成按数据断言）
- **持久化验证**（2026-11-06 06:15）：不删 `run/world` 连续跑第二次 `runServer -Pselftest=true`，`[selftest] all checks passed` 且加载期无方块实体/工具 NBT 反序列化异常 → 冶炼炉、铸造炉、浇铸台、龙头、储罐、工具这些自定义 BE 与物品数据的**存档往返**正常（第一次跑会新建世界，覆盖同一坐标，正好验证旧 BE 被正确卸载）。
- **新增自测**：`checkFluidPlacement`（`new ItemStack(TinkerFluids.moltenIron.getBucket())` + `((BucketItem) item).emptyContents(player, level, ground.above(), null)` → 断言 `level.getFluidState(...).getType() == TinkerFluids.moltenIron.get()`，确认「熔融金属桶在世界里倒出流体方块」这条 `FlowingFluidObject.block(...)` 布线是通的；注意**不要用 `stack.useOn(UseOnContext)`**，`BucketItem` 只实现 `use`，且 `ServerPlayer.lookAt` 会因 `connection == null` 直接 NPE）`、`checkFoundryStructure`（`(80,0)` 用焦黑砖 `scorchedBricks` 铺 5×5 地板 + `y=99` 立一圈 5×5 墙、内腔 3×3 空出，西墙放 `foundryController`（facing WEST）、东墙放 `scorchedTank(FUEL_TANK)`；`FoundryMultiblock` 是 `super(parent, true, true, false)`（有地板+有框架、无天花板），与冶炼炉的 `(true,false,false)` 不同，实测同样一次成型；随后灌岩浆 + 铁锭 → tick 300 → 熔炉罐内 ≥90 mB 熔铁，验证铸造炉（foundry）的独立加热/熔化路径）、`checkAllToolRecipes`（遍历 `TinkerRecipeTypes.TINKER_STATION` 里的全部 `ToolBuildingRecipe`（26 条），用全铁材料 `ToolBuildHandler.buildItemFromMaterials(tool, MaterialNBT.of(materials))` 逐个组装，断言产物就是配方结果物品且 `ToolStack.from(stack).getStats().get(ToolStats.DURABILITY) > 0` → `built 26 of 26 tools from iron`，等于一次性扫过全部工具的 definition 模块接线）、`checkEntityMelting`（冶炼炉 `(60,0)` 成型后灌岩浆，把一只僵尸丢进 3×3 内腔、时间设 18000，连 tick 200 次 → 僵尸掉血到 16.06、炉内出现 `10 mB 熔融铁`；注意**实体熔化必须要有燃料**，`canMeltEntities()` 在无燃料时直接返回 false，测试里第一版忘了灌岩浆就完全没反应，症状是「僵尸血量不变、罐子空」）、`checkFaucet`（`(49,101,0)` 放 `seared_tank(INGOT_TANK)` 装 90 mB 熔铁、`(49,100,0)` 放 `seared_faucet`（`FaucetBlock.FACING = DOWN`，输入取上方方块、输出给下方方块）、`(49,99,0)` 放 `seared_table` 并塞一个 `ingot_cast`；调 `faucet.activate()` 后连 tick 200 次 → 熔铁经龙头注入浇铸台并冷却成 `1 iron_ingot`。注意：浇铸台没有铸模时 `CastingFluidHandler.insert` 走 `initNewCasting` 返回容量 0，龙头会「倒不进去」，必须先放铸模）、`checkMelter`（把 `seared_melter` 放在 `(40,100,0)`、下面垫一个灌满岩浆的 `seared_tank(FUEL_TANK)`，铁锭放 0 号槽，连续 tick 300 次 → 熔化炉储罐里 `90 mB 熔融铁`；注意熔化炉的成型状态 `ControllerBlock.IN_STRUCTURE` 是**放置时**由 `getStateForPlacement` 检查下方是否为 `tconstruct:fuel_tanks` 标签方块写入的，测试里用 `setBlockAndUpdate` 直接放方块不会触发，必须手动 `setValue(IN_STRUCTURE, true)`，否则 `isFormed()` 恒 false、整体 tick 空转）、`checkSmelteryMelting`（`buildSmelteryAt(level,20,0)` 搭出 3×3 冶炼炉后，往墙上的 `seared_tank(FUEL_TANK)` 灌 1 桶岩浆当燃料，把铁锭塞进 0 号熔化槽，连 tick 300 次 → 冶炼炉储罐里出现 `90 mB 熔融铁`；这条同时验证了 `MultitankFuelModule` 燃料识别/温度、`MeltingModuleInventory.heatItems` 相位推进与 `SmelteryTank` 输出）、`checkSmelteryStructure`（在 `y=98` 铺 5×5 焦黑砖地板、`y=99` 立一圈墙并空出 3×3 内部，西墙放 `smeltery_controller`（facing WEST）+ 东墙放 `seared_tank(FUEL_TANK)`，调 `updateStructure()` 后 tick 一次 → `ControllerBlock.IN_STRUCTURE` 变 true，即多方块结构识别通过；这是首个验证多方块成型的断言）、`checkMelting`（`smeltery/melting/metal/iron/ingot`：`matches` 通过，输出 90 mB `tconstruct:molten_iron`、`temperature 800`、`time 60`；本轮共加载 850 条熔化配方）、`checkAlloying`（假 `IAlloyTank`（1000°C，270 mB 熔铜 + 90 mB 熔锡）→ `smeltery/alloys/molten_bronze` 的 `matches` + `canPerform` 通过，输出 `tconstruct:molten_bronze` 360 mB，对应 `temperature: 700`）、`checkCastingBasin`（`TinkerSmeltery.searedBasin` 灌 810 mB 熔铁 → `1 iron_block`，与 `data/tconstruct/recipes/smeltery/casting/metal/iron/block.json` 的 `amount: 810` / `cooling_time: 180` 对应；至此浇铸台与浇铸盆两条浇铸链路都有断言）、`checkCraftingRepairKit`（工作台 2×2 里 损坏铁镐 + 铁修复套件 → 耐久修满，走 `CraftingTableRepairKitRecipe`（`tconstruct:crafting_table_repair`，数据在 `data/tconstruct/recipes/tables/crafting_table_repair.json`）的 `matches` + `assemble`；顺带把自测里造出来的无连接 `ServerPlayer` 换成覆写 `displayClientMessage` 的 `fakePlayer(...)`，消掉 Mantle `FluidTransferHelper.playEmptySound` 每次打桶都刷 `this.connection is null` 堆栈——那是自测假玩家的噪音，真实游戏里不会出现）、`checkStationRepair`（铁镐 setDamageValue(100) + 4 铁锭进工作台 → 修复到 0，走 `TinkerStationRepairRecipe` 的 `MaterialRepairToolHook` 路径）、`checkModifierApplication`（工作台里把 `tconstruct:diamond` 升级配方应用到铁镐：结果带 diamond、`upgrades` 3→2、耐久 275→825，走真实 `ModifierRecipe#getValidatedResult` 路径 = 扣槽 + 加修饰符 + `tryValidate`）、`checkToolBuilding`（工作台槽位 1/2/3 放 pick_head + tool_handle + tool_binding → `tconstruct:pickaxe`，耐久 250 = 铁头基础值，材料 3 份；关键点：**槽位 0 是「待改装工具」槽**，部件从槽位 1 开始，`TinkerStationContainerWrapper.getInput(i)` 是相对部件槽的偏移，我最初把部件放进 0/1/2 导致 `getTinkerableStack()` 非空而永不匹配——已由日志 `tinkerable=1 pick_head` 定位）、`checkToolSlots`（铁镐默认 upgrades=3 / abilities=1 来自 `tconstruct:modifier_slots` 模块；按 `ModifierRecipe#applyModifiers` 的写法 `getPersistentData().addSlots(SlotType.UPGRADE, -1)` 后降到 2；`rebuildStats()` **不会**回滚扣减，扣减是持久数据 —— 期间自测先报「3 instead of 2」正好暴露了我对槽位语义的错误假设，已按上游 `ref/TinkersConstruct/.../ModifierRecipe.java:156` 修正断言）、`checkTankItemTransfer`（封闭储罐物品装 1000 mB 熔融铁 → 空桶取液得到 `tconstruct:molten_iron_bucket`、罐内归零，走 `FluidTransferHelper.interactWithStack` 与右键处理器同一路径）、`checkArmor`（铁板头盔 armor=2.0 / durability=165 / toughness=0.0，直接验证 `PlatingMaterialStats.apply` 与 `tconstruct:plate_helmet` 的 `part_stats` 模块链路）、`checkModifierModules`（haste +4.00 挖掘速度、diamond 275→825 耐久 +2.00 速度 +0.50 攻击、sharpness +0.75 攻击、reinforced/luck/lightspeed/reach/knockback 均正常挂载不报错——覆盖 `stat_boost`/`reduce_tool_damage`/`conditional_melee_damage` 等模块族）、`checkCasting`（90 mB 熔融铁 + 铸模 → 铁锭）、`checkPartBuilder`（2 铜锭 + 空白模具 → 铜镐头，走 `PartBuilderBlockEntity.selectRecipe`/`calcResult` 真实路径）、`checkToolLifecycle`（钻石强化 275→825、伤害/损坏/修复）、`checkTankEmptyBucket`（空桶取流体）、`checkBiomeModifications`（钴矿/天空晶洞）。
- **注意（与上游一致）**：铁/铜等材料定义里 `craftable` 决定能否在部件台用材料做部件——`iron.json` 是 `craftable: false`（上游同样），所以部件台自测必须用铜。
- **未解/待办**：`TankItem` 转移助手已由 `checkTankItemTransfer` 自测覆盖（见下）；用户报的「某方块面上有洋红色块」：核对为 `clear_glass/magenta/pane_*` 的 `colors` = `FFB24CD8`（即品红色通透玻璃板的本色），且待游戏内确认；模型贴图 id 全部存在（`missing textures: 0`）；仍有 361 处 `TODO(fabric)`；ballista 换弹已用 `mixin/ProjectileWeaponMixin.java`（注入 `ProjectileWeaponItem.getHeldProjectile` 的 RETURN，复刻上游 `LivingGetProjectileEvent` 的换弹规则）补齐；crafting player 线程本地 9 处仍为已知缺口（仅影响配方里的玩家上下文）。

## 0. 2026-11-05 18:xx 策略重置（本轮最重要的一步）

**发现：`Alpha-s-Stuff/TinkersConstruct` 分支 `1.20.1`（Hephaestus）就是匠魂 3.6.4 的完整 Fabric 移植**（mod id 仍是 `tconstruct`，包名 `slimeknights.tconstruct`，Mojang 官方映射，自带 157 行 access widener，内嵌 Fabric 版 Mantle 1.9.291）。已 clone 到 `ref/Hephaestus-1.20.1`，可直接当「加载器胶水参考实现」。

**失误复盘**：我们之前用 **yarn** 映射，而 `ref/TinkersConstruct` 的 3.12.1 源码是 **Mojang 映射**（`net.minecraft.world.level.Level` / `CompoundTag` / `getLevel()`）。15 个代理于是分头把类名、包名、方法名手改成 yarn（`NbtCompound` / `net.minecraft.block` / `getWorld`…），造成 5231 个错误里约 1/3 是「自造的改名债」，而且和参考实现再也对不上号。

**已执行的重置**：
1. `build.gradle`：`mappings` 从 `net.fabricmc:yarn` 换成 **`loom.officialMojangMappings()`**（映射不影响运行时，Loom 导出时统一 remap 到 intermediary）。
2. 旧 yarn 版源码整体移到 `ref/port-v1-yarn-src/java` 存档；`src/main/java` 改为**从 `ref/TinkersConstruct` 原样复制 1938 个文件**（逐字搬运，零改名）。
3. 装 access widener：`src/main/resources/tconstruct.accesswidener`（从参考移植复制，157 行，Mojang 名），`fabric.mod.json` 加 `"accessWidener": "tconstruct.accesswidener"`，`build.gradle` 设 `accessWidenerPath`。
4. 新增工具：`tools/measure_heph_overlap.py`（比对 3.6.4/3.12.1/参考移植 三棵树）、`tools/adopt_unchanged.py`（把「3.6.4→3.12.1 无改动」的文件直接用参考移植的 Fabric 版覆盖）。
5. 已按 `build/heph_unchanged.txt` 采纳 **291 个文件**（1001 个三方都有的文件里，29.1% 在 3.6.4→3.12.1 之间本体未变）。

**新基线：`./gradlew compileJava` = 11001 个错误**（重置后原始 11370，采纳 291 文件后 11001）。错误现在**全部是 Forge 专有 API**（`net.minecraftforge.*`、`LazyOptional`、`Capability`、`IFluidHandler`、`BlockEntityPacket`…），不再有映射改名噪声。

**下一步打法**：按包分批，每批用 `ref/Hephaestus-1.20.1/src/main/java/` 里同名文件作为「胶水标准答案」逐文件对齐；3.12.1 新增的文件（3.6.4 里没有对应）只能参照 1.11 Mantle + Porting Lib 手写。

> ⚠️ 环境坑：这台机器上 `python` 指向 Microsoft Store 存根，**静默不执行**（退出码 49）。必须用 `C:/Users/YamIpass/.dsh/dsh-runtimes/dsh-primary-runtime/dependencies/python/python.exe`。

## 1. 一句话现状

**源码 100% 搬完（1938 个 java 文件，Mojang 映射逐字复制），编译 0 错误，jar 已部署到用户实例，客户端可进世界，玩家反馈的界面/交互问题在逐条关闭。**

### 1.0 2026-11-06 00:xx 修复批次（用户实测反馈驱动）

| 用户反馈 | 根因 | 修法 |
| --- | --- | --- |
| 所有工具/部件一片银色 | 早期烘焙脚本把 `tconstruct:material`/`tconstruct:tool` 模型压平成 `item/generated`，材料染色信息丢失 | 还原上游 852 个物品模型，三/五个模型 loader（`material`/`tool`/`material_block`/`fluid_container`/`gui`/`mantle:item_layer`/`mantle:nbt_key`）全部注册 |
| 储罐类物品图标空白 | `mantle:item_layer` 与 `mantle:nbt_key` 从未注册（18 + 2 个模型） | `CommonsClientEvents.registerModelLoaders()` 注册 |
| 冶炼炉中间灌装按钮不显示 | `HeatingStructureScreen` 里 5 个 `ElementScreen` 只声明未绘制 | 补回上游 `renderLabels`/`renderTooltip` 绘制段与 `getBucketContainer()`/`getTransferDirection()` 显式访问器 |
| 岩浆右键不吸 / 焦黑燃料储罐无交互 | 全仓没有 `FluidStorage.SIDED` 注册，所有 SIDED 查找返回 null（`FluidTransferHelper.interactWithTank` 全部短路） | `FabricEvents.registerStorageFallbacks()` 用 `FluidStorage.SIDED.registerFallback` / `ItemStorage.SIDED.registerFallback` 接上 `SidedStorageBlockEntity` |
| 新手书报错 `Font.width because "fr" is null` | 资源重载重建 `BookData` 后 `fontRenderer` 为 null | Mantle `BookData.getFontRenderer()`（null 回落原版字体）+ 所有读取点改用它 |
| 外 mod 内容报错（twilightforest 流体/装备、IE/create 流体） | Mantle 1.11 只认 `fabric:load_conditions`，上游数据是裸 `conditions` | `library/utils/ConditionHelper.java` 统一求值；`tools/drop_foreign_effects.py`、`tools/drop_foreign_fluid_transfers.py` 删除无法条件化的外 mod 数据（已进 `tools/rebuild_resources.sh`） |

| 冶炼炉放了流体就闪退 | Mantle 自带 core shader（`assets/mantle/shaders/core/fluid.*`）从未注册，`FLUID_SHADER` 恒为 null，画流体即 NPE | `FabricEvents.initClient()` 注册 `CoreShaderRegistrationCallback.EVENT.register(MantleShaders::registerShaders)`；`HeatingStructureBlockEntityRenderer` 再加渲染兜底 try/catch |
| 岩浆桶倒不进储罐（mB 与 droplet 单位不一致） | Fabric `FluidConstants.BUCKET=81000` droplet，而移植后的储罐/配方按 Forge 的 mB（1 桶 = 1000）；`interactWithFilledBucket` 用 81000 模拟必然失败 | Mantle `FluidTransferHelper.bucketAmountFor()`：先试 81000，失败退回 `81000/81`(=1000 mB)，两条路径（方块右键 + 容器槽拖拽）都走它 |
| 所有颜色的「通透玻璃」变白、焦黑玻璃无缝失效 | 抽取管线把方块模型的 `"loader"` 键吃掉（160 个文件），`mantle:connected`/`mantle:retextured`/`mantle:colored_block` 三个 loader 也没注册 | `tools/restore_block_model_loaders.py --apply` 还原 160 个方块模型；`CommonsClientEvents.registerModelLoaders()` 注册三个 mantle loader；`tools/fix_composite_loaders.py` 把 `forge:composite` 改 `porting_lib:composite` |
| 储罐物品之间流体互倒（右键拖拽）失效 | `TankItem` 的 `overrideStackedOnOther`/`overrideOtherStackedOnMe`/`mayHaveFluid`/`updateHeldItem` 在移植时整段丢失 | 已按上游 1:1 补回（`FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack))` 判断可存流体；`playUISound` + `getOrTransferFilled` 复用 Mantle 助手） |
| 创造背包切到匠魂页闪退 / 储罐类图标流体层空白 | ①`FluidVariantRendering` 从未注册 handler，`getSprites()` 返回 null；②Mantle `FluidTextureManager` 从未 `init`，`mantle/fluid_texture/*.json` 没载入 | 新增 `library/client/fluid/FluidVariantTextureHandler`（读 `FluidTextureManager` 的 still/flowing/color）+ `FluidClientEvents.clientSetup()` 里 `FluidTextureManager.init(...)` 并对全部 `tconstruct:*` 流体 `FluidVariantRendering.register(...)`；`TankModel`/`FluidContainerModel` 加 sprite 空值兜底（`Unable to bake model` 从 70 条降到 0） |
| 冶炼炉热量条只画在部分槽位（最下面两格没有蓝条） | `GuiMeltingModule.drawHeatBars/drawHeatTooltips` 写成 `menu.slots.get(i)`，丢了上游的 `i + indexOffset`（侧面库存偏移） | 两处循环改回 `i + indexOffset`（`HeatingStructureScreen` = 2，`MelterScreen` = 0） |
| 右键焦黑储罐倒岩浆无反应（无头已复现） | vanilla 桶在 Fabric 下没有 `FluidStorage.ITEM`，`FluidStorage.ITEM` 只暴露 81000 droplet 的整桶视图；`interactWithContainer` 的 item-handler 分支空转却返回 `CONTAINER`，短路了能正常工作的 `interactWithFilledBucket` | Mantle `FluidTransferHelper.interactWithContainer(...)` 在 item-handler 分支前加原版 `BucketItem` 分支（mB 换算 `bucketAmountFor`、装桶/倒桶两条路径），并把空转兜底从 `CONTAINER` 改成 `MISSING`；`PortSelfTest.checkTankRightClick` 作为回归测试 |

服务端自测（`PortSelfTest`，`./gradlew runServer -Pselftest=true`）覆盖：配方类型计数、工具定义数、材料数、铁头部数值、铁镐成品数值、**熔炉储罐的 SIDED 流体查找与岩浆桶倒入**。

**当前 jar**：`build/libs/tconstruct-fabric-0.1.0.jar`（31,317,868 B，2026-11-06 01:2x，含热量条槽位修正 + 流体贴图注册 + 原版桶分支），已覆盖部署到 `D:\game\.minecraftersions.20.1-Fabric 0.20.1\mods	construct-fabric-0.1.0.jar`。客户端日志里的数据加载错误已清零（`Couldn't load tag` / `Failed to load` / `Couldn't parse` / `Unable to bake model` 全为 0，仅剩 Star 调试库与 `No data fixer` 噪音）。

里程碑（都已实测）：
- `./gradlew compileJava` **0 error**；`./gradlew build --offline -x test` BUILD SUCCESSFUL。
- `./gradlew runServer`：`Done (5.xs)`，`Parsed 0 recipe errors`，材质 99 / 修饰符 291 / 工具定义 45 / 站位布局 23 全部载入。
- `./gradlew runClient`：能进世界、玩家可登录（“无效的玩家数据”已修）、工具栏不再闪退（附魔补丁递归已修）、流体桶模型已恢复。
- 仍待处理：`tags/blocks` 等 476 个标签（已抽出，待下次启动验证）、Forge 战利品条件已改写、Forge 批格式成就已拆包；以及 361 处 `TODO(fabric)`（多数是 datagen / Forge 独占 API）。
- **目标实例 `D:\game\.minecraft\versions\1.20.1-Fabric 0.20.1\mods\tconstruct-fabric-0.1.0.jar` 已更新为本轮新构建**（31,311,096 B）；旧的 05:06 手写版仅留在 `dist/tconstruct-fabric-0.1.0-working.jar` 作兜底。

> 2026-11-05 13:30 用户决定：**不再修旧的手写版本**，全部精力投到 1:1 搬运版本（旧 jar 仅作兜底备份）。

### 1.1 本轮（20:00 之后）的关键修复
- **access widener 补全**：原 AW 缺 28 处原版私有成员（`ThrownTrident.tridentItem`/`dealtDamage`/`ID_LOYALTY`、`AbstractArrow.life`、`FishingHook.life`、`Explosion.{fire,hitPlayers,damageCalculator}`、`Enchantment.slots`（`accessible mutable`）、`IntegerProperty.{min,max}`、`ItemEntity.pickupDelay`、`ItemInHandRenderer.MAP_*`、`ResourceLocation.ERROR_INVALID`、`Screen.font`、`MangroveRootPlacer.mangroveRootPlacement`，以及 `Gui.renderSlot`、`RecipeManager.byType`、`LivingEntity.spawnItemParticles`、`ItemRenderer.renderModelLists`、`FishingHookRenderer.stringVertex`、`CraftingMenu.slotChangedCraftingGrid`、`SpriteSources.register`、`TextureAtlasHolder.getSprite`）→ 一次性消掉 78 个错误（492→414）。
- **Mantle 1.11 补 API**：上游 3.12.1 用的 `Loadable.map(Function,Function)` / `Loadable.mapChecked(BiFunction,Function)`（以及 `StringLoadable` 的同名覆盖）在本 fork 里叫 `flatXmap`/`comapFlatMap`，已按 Mantle 1.11.126 语义加回别名并重建 jar（`libs/Mantle-1.20.1-1.11.DEV.eb1e9a5.jar`，1,822,693 B）。
- **客户端接线**：`shared/CommonsClientEvents.registerModelLoaders()` 用 Porting Lib `RegisterGeometryLoadersCallback` 注册 `gui` 模型 loader；`fluids/FluidClientEvents.registerModelLoaders()` 注册 `fluid_container`；`tools/ToolClientEvents` 与 `smeltery/SmelteryClientEvents` 之前已各自注册 `material`/`tool`/`material_block` 与 `tank`/`fluid_texture`。
- **`library/client/**` 已清零**（代理 373fe8c6：delombok + `-proc:none` javac 验证 0 错误），包括 Forge 模型几何 → Porting Lib `IGeometryLoader`/`MantleBakedModel.Builder`、`IClientItemExtensions`→Porting Lib `ArmorRenderer`、条件→Fabric `ConditionJsonProvider` 等。

### 1.2 当前 414 个错误的分布（前 12 个文件）
`library/tools/item/ModifiableItem` 23、`smeltery/client/screen/AlloyerScreen` 22、`library/tools/item/armor/ModifiableArmorItem` 19、`smeltery/client/screen/MelterScreen` 19、`library/tools/item/ranged/ModifiableLauncherItem` 18、`smeltery/client/screen/module/GuiTankModule` 13、`shared/TinkerFood` 12、`smeltery/client/screen/HeatingStructureScreen` 11、`smeltery/data/Byproduct` 9、`smeltery/item/TankItem` 9、`tools/client/ModifierClientEvents` 9、`world/TinkerWorld` 8。

错误类型：`找不到符号` 192、`方法不会覆盖` 84、`long→int 有损` 19、`PoseStack→GuiGraphics` 15、`MobEffectInstance 不是函数接口` 12 等 —— 都是 Forge 与 Mojang/Porting Lib 的 API 形状差异，不是玩法逻辑。

## 2. 关键基础设施（已打通，这是最难的一步）

| 组件 | 状态 | 说明 |
|---|---|---|
| Fabric Mantle 1.11 | ✅ 自建成功 | 官方没有 Fabric 版 1.11，用 `Alpha-s-Stuff/Mantle` 的 `1.20.1-update` 分支自建：`ref/Mantle-fabric/build/libs/Mantle-1.20.1-1.11.DEV.eb1e9a5.jar`（801 class，含 `data/loadable`、`command`、`client/model` 全套）。构建需 Gradle 8.14 + 本地 maven 补 `cardinal-components`。 |
| Porting Lib 2.3.16-beta.81 | ✅ | 23 个模块放进 `libs/`，替代 Forge 的 capability / transfer / tool_action / tier-sorting 等。 |
| JiJ 内嵌 | ✅ | `libs/` 共 34 个 jar，`fabric.mod.json` 的 `jars` 由 `tools/sync_mantle_libs.py` 生成；Loom 1.12 产的 jar 已剥离 `Fabric-*` manifest 元数据以兼容 Loom 1.8.13。 |
| Lombok | ✅ | 关键坑：Loom 默认挂的 legacy Mixin AP（sponge-mixin）在 JDK 21 上**静默禁用 Lombok**，等于所有 `@Getter`/`@RequiredArgsConstructor` 都不生成。已在 `build.gradle` 设 `loom { mixin { useLegacyMixinAp = false } }`。 |
| 资源管线 | ✅ | `tools/rebuild_resources.sh`：抽 3.12.1 的 assets/data → 烘焙 Forge 模型 loader → 修 atlas → 生成 forge 标签/铸型配方/补充 lang。**改资源必须走这个脚本**。 |
| 编译纪律 | ⚠️ | 同一时间**只能跑一个 Gradle**。并发构建曾把 `minecraft-merged.jar` 写成 22 字节空 zip，导致几千个假错误。跑之前 `jps -l \| grep -i gradle`。 |

## 3. 搬运覆盖（文件数）

| 包 | 文件 | 状态 |
|---|---|---|
| `library/` | 976 | 搬完，编译错误最多（recipe/modifiers/tools/client） |
| `tools/` | 312 | 搬完 |
| `smeltery/` | 148 | 搬完（`block/**` 正在修，约 1040 错误） |
| `tables/` | 86 | 搬完（含 menu/slot/network/client 屏） |
| `shared/` | 70 | 搬完 |
| `world/` | 64 | 搬完 |
| `gadgets/` | 36 | 搬完 |
| `common/` | 25 | 搬完 |
| `fluids/` | 24 | 搬完 |
| `client/` | 1 | Fabric 客户端入口（新增，上游无对应） |

`TODO(fabric)` 标记共 **281 个文件**，都是「Forge 有、Fabric 没有」的语义缺失，逐条写了注释。

## 4. 当前错误分布（`tools/errsum.sh /tmp/d13.log`，5231 条）

```
tools\entity\ThrownTool            63   smeltery\block\entity\CastingBlockEntity   57
smeltery\item\TankItem             52   tools\logic\ModifierEvents                 50
tools\logic\ToolEvents             44   smeltery\menu\HeatingStructureContainerMenu 44
smeltery\client\screen\HeatingStructureScreen 41   smeltery\client\screen\AlloyerScreen 40
smeltery\block\entity\controller\HeatingStructureBlockEntity 38   world\TinkerWorld 36
```

错误类型统计（`grep -oE "错误: ..." | sort | uniq -c`）：`找不到符号` 3499、`方法不会覆盖或实现超类型的方法` 489、`方法引用无效` 48、Forge `crafting.conditions`/`capabilities` 等包缺失 ~60。

最高频缺失符号：`getId()` 163（Lombok 对部分类不生效，见 §4.1）、`getEntity()` 76、`getWorld()` 57、`getLevel()` 56、`getVariant()` 48、`ForgeCapabilities` 38、`getLivingTarget()` 34、`FluidAction` 27、`ICondition` 26。

### 4.1 本轮新查明的三个真问题

1. **Lombok 只对部分类生效**：同一套 Gradle 工具链下，往 `src/main/java` 里塞一个 `@Getter` 探针类——正常生成；但 `JsonRedirect`(`@Data`)、`LazyModifier`(`@Getter`) 等类依旧报 `getId()` 不存在。已排除：注解处理器路径（执行期实测只有 `lombok-1.18.34.jar`）、`-proc:full`、`--release 17`、`--rerun-tasks` 全量重编、`lombok.config`（不存在）。→ 下一步要么对这几个类手写成员，要么继续查（`build.gradle` 已 pin `options.annotationProcessorPath = configurations.annotationProcessor`）。
2. **`libs/Mantle` 缺 `BlockEntityPacket`**：上游所有网络包（`smeltery/network/*`、`tables/network/*`…）都继承它，Fabric 版 Mantle 1.11 只保留了 `ISimplePacket`/`IThreadsafePacket` 系列。需要写一层薄适配（把 `BlockEntityPacket` 语义映射到 Mantle 1.11 的包基础设施），**不是机械改名**。
3. **一个 access widener 都没有**：`fabric.mod.json` 没有 `accessWidener` 字段，`src/main/resources` 下也没有 `.accesswidener` → `TridentEntity.tridentItem`、`PersistentProjectileEntity.life/piercedEntities`、`WorldRenderer.blockBreakingInfos`、`CraftingScreenHandler.updateResult`、`ApplyBonusLootFunction.*` 这些原版私有成员全报「找不到符号」。**这是当前性价比最高的一步**（一次能清几百条）。

## 5. 当前可玩版本

- 构建产物：`build/libs/tconstruct-fabric-0.1.0.jar`（31,575,969 B，2026-11-06 04:1x）。
- 已部署：`D:/game/.minecraft/versions/1.20.1-Fabric 0.20.1/mods/tconstruct-fabric-0.1.0.jar`（另有 `fabric-api-0.92.12+1.20.1.jar`）。
- 内容：1744+ 个上游类逐类搬运（common/library/tables/smeltery/tools/world/gadgets/shared），3018 条配方、45 个工具定义、96 种材料、300 个动态修饰符、6 本指南书、8 个生物群系修改（代码实现）、50 种流体（含熔化/浇铸/合金）。
- 已验证：服务端 20 项自测全绿（`all checks passed`）、客户端可进世界且日志无功能错误、无崩溃报告。

## 6. 下一步（按优先级）

1. **视觉验收**：用部署好的实例过一遍——工具/部件贴图、储罐与流体桶、冶炼炉多方块、铸造台、工作台 GUI、指南书、通透玻璃与无缝玻璃。
2. **洋红色块**：用户截图里某个方块面呈洋红（贴图 id 全部存在，`missing textures: 0`），需在实机定位具体方块与模型路径（怀疑连通变体或缺省 tint）。
3. **ballista 换弹**：Fabric/Porting Lib 无 `LivingGetProjectileEvent` 等价事件，需要 mixin `ProjectileWeaponItem`/`CrossbowItem` 的取弹路径（上游实现已在 `tools/logic/ModifierEvents` 注释块保留）。
4. **crafting player 线程本地**：`ForgeHooks.setCraftingPlayer` 9 处（`tables/block/entity/table/CraftingStationBlockEntity.java` 等），影响配方里 `getCraftingPlayer` 相关行为。
5. **剩余 `TODO(fabric)`**：361 处，逐条评估是否需要真实实现（多数是 Forge capability 的等价替换已生效）。
6. 收尾：`tools/audit_missing_methods.py` 复核 190 个「上游有我们没有」的方法名，确认全部为有意替换。

## 7. 构建命令

```bash
cd tconstruct-fabric
./gradlew compileJava --console=plain            # 编译
./gradlew test build --console=plain             # 测试 + 出 jar
bash tools/rebuild_resources.sh                  # 重新生成资源（会覆盖手工改动，谨慎）
# Mantle 重建（改 libs 时）
cd ref/Mantle-fabric && ~/gradle-8.14/bin/gradle build -x test --console=plain --no-daemon
cd ../.. && python tools/sync_mantle_libs.py     # 同步 libs/ + fabric.mod.json
```

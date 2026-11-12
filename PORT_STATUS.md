## 2026-11-08 15:05 JEI 标签页标题本地化

JEI 的「流体标签 / 物品标签」页标题走翻译键 `tag.<registryPath>.<tagNamespace>.<tagPath>`（Fabric 实现见 `mezz/jei/fabric/platform/RenderHelper.getTagTranslationKey`，`sanitizePath` 会把 `/` 换成 `.`），取不到就回退成 `#<namespace>:<path>`。上游 3.12.1 一个这样的键都没有，所以所有标签页标题都是原始 id（用户 2026-11-08 截图反馈）。

- 用 `.port_tmp/gen_tag_lang.py` 扫描 `src/main/resources/data/*/tags/**`，按 `items→item`、`blocks→block`、`fluids→fluid`、`entity_types→entity_type` 等映射生成键，共 **885 个标签**（item 475 / fluid 131 / block 190 / entity_type 44 / enchantment 21 / damage_type 21 / 其余 3）。
- `en_us.json` 与 `zh_cn.json` 各新增 **864 条**（英文是路径单词首字母大写用 ` · ` 连接；中文走术语表，如 `tooltips/metal` → 「工具提示 · 金属」、`patterns` → 「模具」）。
- 标题下方的灰色小字仍是 JEI 自己画的原始 id，无法用语言文件改。
- 产物 **31,745,483 B**，已部署到 `D:/game/.minecraft/versions/1.20.1-Fabric 0.20.1/mods/Tinkers-Fabstruct-1.20.1-0.2.0.jar`。

## 2026-11-08 03:20 JEI 插件（第一阶段：工作站配方）

移植版此前完全没有 JEI 插件（`plugin/` 目录是空的），所以 JEI 只能看到原版合成表里的匠魂物品，看不到匠魂自己的工作站配方。本轮把上游 `plugin/jei` 搬进来并只保留不依赖 Forge 流体 API 的分类：

- 新分类：工具装配（tool building）、工具改造（tool modification）、强化（modifiers）、部件制造台（part builder）、模具（molding）、强化工作台（modifier worktable）。
- 新增 `shaped/shapeless` 材料配方扩展、`ToolTinkeringExtension`、两个配方转移处理器（TinkerStationTransferInfo / ToolInventoryTransferInfo）。
- JEI 作为 `modCompileOnly` 依赖，不进 jar（构建产物里没有任何 jei jar），玩家自备 JEI。
- 首次进世界日志：`[jei] station recipes: tool building=26, tool modification=206, modifiers=270, part builder=204, molding=58, worktable=25`，无 `Unknown ingredient` / 无分类注册异常。
- 顺带修掉两处 jei 相关噪音：Mantle jar 的 `jei_mod_plugin` 入口指向它自己构建里被排除的类（JEI 报 invalid entrypoint），已从内嵌 jar 的 `fabric.mod.json` 去掉；`slimeknights.mantle.plugin.jei.MantleJEIConstants` 在我们源码里补了一个最小实现（`getCraftingIndex`）。

流体类分类（熔炼/浇铸/合金/实体熔炼/断头/燃料）暂未注册：Fabric 版 JEI 用 `FabricTypes.FLUID_STACK` + `JeiFluidIngredient(Fluid, long, CompoundTag)` 表示流体，上游插件是针对 Forge `FluidStack` 写的，需要改写后才能搬。

## 2026-11-08 01:20 控制器燃料槽取液（droplet / mB 的最后一处）

- 现象：空桶左键点控制器右侧燃料槽取不出岩浆；矿物/流体槽正常。
- 根因：`FluidTransferHelper.fillStack` 的空桶分支写死 `long amount = FluidConstants.BUCKET / 81;`（1000）后 `teHandler.extract(...)`，而燃料模块暴露的是 droplet 存储（81000/桶），抽 1000 droplet 只有 12 mB，装桶失败。
- 修法：
  - `ref/Mantle-fabric` 的 `fillStack` 改为**双单位尝试**：先试 `FluidConstants.BUCKET`（81000，droplet 罐），再试 `/81`（1000，毫桶罐），精确命中才算成功。
  - `MultitankFuelModule.getLastFluid()` 返回毫桶（`FluidStorages.stack(view)`），`fillStack` 的 `fluid.getAmount()` 判断才有意义；显示链路仍走自己在 `GuiFuelModule` 的换算。
  - `playUISound` 增加 `serverPlayer.connection != null` 守卫，避免无连接的假玩家把交互中断（`NullPointerException: Cannot invoke "ServerGamePacketListenerImpl.send"`）。
  - 关闭上一版为定位问题加的聊天栏探针（`[tank] droplet view ... raw ...`）。
- 新增自测 `checkFuelSlotDrain`（`PortSelfTest.java`）：造冶炼炉 → 燃料罐灌 1 桶岩浆 → 构造 `HeatingStructureContainerMenu` → 手持空桶 → `clickMenuButton(player, 1)` → 断言得到岩浆桶；日志 `[selftest] the fuel gauge drained one lava bucket from the smeltery`、`all checks passed`。
- 产物：`dist/Tinkers-Fabstruct-1.20.1-0.2.0.jar`，31,599,887 B，SHA1 `4B50CA9BB2F0D14BAA0A89FF688303B094311C53`。

## 2026-11-07 14:50 流体单位：把所有「桶 ×81」的入口一并改掉

- 上一轮只修了储罐的右键，`CastingBlockEntity`、`CastingTankBlockEntity`、`FluidCannonBlockEntity`、
  `ProxyTankBlockEntity` 仍然把**毫桶**罐子直接交给 Mantle 的容器交互，于是倒一桶被写成 81000 mB（81 桶）。
- 本轮把这四处统一改成 `MillibucketStorage.of(tank)`（droplet 视图）后再交给
  `FluidTransferHelper.interactWithContainer / interactWithFilledBucket`；至此全部容器交互入口只剩
  `library/fluid/FluidStorages` 一个门面，内部逻辑依旧用 `FluidStorages.raw(...)` 拿 mB 罐子。
- 验证：`[selftest] droplet view reported 81000 droplets for 1000 mB`、`an empty bucket drained the seared fuel tank,
  tank now holds 0 mB`、`all checks passed`。
- 产物 `Tinkers-Fabstruct-1.20.1-0.2.0.jar` = 31,593,688 B，已部署到实例 mods。

## 2026-11-07 14:20 流体数值单位：倒桶 / 抽桶按 mB 正确计数

- 症状：玩家倒 1 桶岩浆，罐里变成 81 桶（900 mB 显示成 `72B 900mB`）。
- 根因：内嵌 Mantle 的 `FluidTransferHelper` 里「空桶 / 满桶」两条分支都按 `FluidConstants.BUCKET / 81`（1000）去
  调 `extract`，而 Fabric 的存储（含我们的 droplet 视图）以 droplet 计数，等于只抽了 12 mB。
- 修法：`interactWithStack` 与 `interactWithContainer` 的桶分支都改成双尝试——先按 `FluidConstants.BUCKET`（81000
  droplet）抽，失败再退回 1000（mB 存储）。Mantle 侧改动通过 `gradle-8.14` 本地构建 + `repack_mantle.py` 重新打包。
- `library/fluid/FluidStorages.java` 简化为「`interactWithTankRaw` 只走 `FluidStorage.SIDED`（droplet 视图）+
  `interactWithContainer`」，内部读取仍走 `raw(...)` 拿 mB 罐子。
- 验证：`[selftest] an empty bucket drained the seared fuel tank, tank now holds 0 mB` + `all checks passed`；
  产物 `Tinkers-Fabstruct-1.20.1-0.2.0.jar` = 31,593,402 B，已部署到 `D:/game/.minecraft/versions/1.20.1-Fabric 0.20.1/mods/`。

## 2026-11-07 13:25 流体 tooltip 单位修正（Mantle）

- 现象：控制器 GUI 里流体提示显示 `7B` + `2900 droplet`（实际应为 72900 mB = 72 B + 900 mB），而玉显示正确。
- 根因：`Mantle` 的 `FluidTooltipHandler` 单位表是 droplet 尺度的常量（`BUCKET = 10000`、`MILLIBUCKET = 81`、默认列表 `(BUCKET, DROPLET)`），而 tconstruct 的所有调用点传的是 **mB** 数量，于是 72900 被算成 7 B、余数按 droplet 显示。
- 修法：在 `ref/Mantle-fabric/src/main/java/slimeknights/mantle/fluid/tooltip/FluidTooltipHandler.java` 把 `BUCKET` 改回 `1000`、`MILLIBUCKET` 改回 `1`、`DEFAULT_LIST` 改为 `List.of(BUCKET)`（余数由 appendBuckets 里的 MILLIBUCKET 补齐，输出 `72 B` + `900 mB`）。
- 构建链：`~/gradle-8.14/bin/gradle.bat build -x test --offline`（`ref/Mantle-fabric`）→ 新 jar 的 `META-INF/MANIFEST.MF` 里 `Fabric-Loom-Version` 是 1.12.7，被 Loom 1.8.13 拒绝（`Mod was built with a newer version of Loom`）→ 用 `.port_tmp/repack_mantle.py` 重打包成 `Mantle-1.20.1-1.11.DEV.eb1e9a5.jar` 并把该字段改成 1.8.13 → 覆盖 `tconstruct-fabric/libs/`。
- 验证：`runServer -Pselftest=true` → `all checks passed`；产物 `Tinkers-Fabstruct-1.20.1-0.2.0.jar` = 31,593,333 B，已部署到 `D:\game\.minecraft\versions\1.20.1-Fabric 0.20.1\mods\`。

## 2026-11-07 02:35 玉（Jade）流体单位：按 Fabric 约定对外暴露 droplet

- 目标：对外（玉、其他模组、传输 API 消费者）一律用 Fabric 的 droplet，内部继续用 mB，两者在方块实体边界转换。
- 新增接口 `library/fluid/MillibucketStorageBlockEntity extends SidedStorageBlockEntity`：
  - `getMillibucketStorage(Direction)` 返回本体罐子（mB），供本模组逻辑使用；
  - `getFluidStorage(Direction)` 变成 default 方法，返回 `MillibucketStorage.of(getMillibucketStorage(side))`（droplet 视图）。
- 九个方块实体改为实现该接口并把原方法改名为 `getMillibucketStorage`：`CastingBlockEntity`、`CastingTankBlockEntity`、`ChannelBlockEntity`、`ProxyTankBlockEntity`、`SmelteryInputFluidIO`（`SmelteryInputOutputBlockEntity` 内部类）、`TankBlockEntity`、`AlloyerBlockEntity`、`HeatingStructureBlockEntity`、`MelterBlockEntity`。
- 内部的 24 处 `FluidStorage.SIDED.find(...)` 改为新的 `library/fluid/FluidStorages.raw(level, pos, side)`：先取 `MillibucketStorageBlockEntity#getMillibucketStorage`，否则退回普通查找（`FuelModule`、`MultitankFuelModule`、`SolidFuelModule`、`MixerAlloyTank`、`TankInteractionModule`、`PortSelfTest`、`PortShowcase`）。
- 方块右键交互（`SearedTankBlock`、`SearedDrainBlock`、`TinyMultiblockControllerBlock`）改走 `FluidStorages.interactWithTankRaw(...)` = `FluidTransferHelper.interactWithContainer(world, pos, rawStorage, player, hand)`，否则 Mantle 的 `interactWithTank` 会从 droplet 视图取罐子、把 81000 当 81000 mB。
- 新增自测 `checkDropletView`：给焦黑燃料储罐灌 1000 mB，再走 `FluidStorage.SIDED` 读回，断言读到 81000 droplet；日志 `[selftest] droplet view reported 81000 droplets for 1000 mB`。
- 回归：`runServer -Pselftest=true` → `all checks passed`（含铸造台 90 mB→铁锭、浇铸盆 810 mB→铁块、排液口入罐、龙头浇筑、空桶抽燃料、燃料识别、合金、熔化）。
- 产物：`Tinkers-Fabstruct-1.20.1-0.2.0.jar` = 31,593,213 B，已部署到 `D:/game/.minecraft/versions/1.20.1-Fabric 0.20.1/mods/`。

## 2026-11-07 02:20 玉（Jade）流体单位问题：控制器修复，其余待显示层

- 现象：玉的流体行把毫桶数当 droplet 解释（1 mB = 81 droplet），显示成 `133 1/8 mB`；焦黑/焦褐燃料储罐、储罐、控制器都受影响。
- 根因：`snownee/jade/util/FluidTextHelper` 按 droplet 计算并带分数格式化；匠魂内部全部是 mB。
- 已修：`HeatingStructureBlockEntity#getFluidStorage` 返回 `MillibucketStorage.of(tank)`（读 ×81、写 ÷81），冶炼炉/铸造炉控制器显示恢复正常。
- 试过但回退：把换算放到所有方块共用的 `FluidStorage.SIDED` 兜底（`FabricEvents#registerStorageFallbacks`）→ 铸造台/浇铸盆/排液口/龙头/燃料模块也走这条入口，单位被当 droplet，自测报 `inserted 81`、`the drain accepted 81 mB but the smeltery tank holds 1 mB`，故回退到只包控制器。
- 结论：正确做法是给玉写显示插件（compileOnly 依赖 Jade API + `@WailaPlugin` 入口），按 mB 渲染数字，而不是改存储单位；下一版处理。
- 当前产物：`Tinkers-Fabstruct-1.20.1-0.2.0.jar` = 31,590,538 B，服务端自测 `all checks passed`。
## 2026-11-07 01:26 鐏甸瓊鐜荤拑瑕嗙洊灞?+ 鐗堟湰 0.2.0

- 鐗堟湰锛歚gradle.properties` 鐨?`mod_version=0.2.0`锛屼骇鐗?`Tinkers-Fabstruct-1.20.1-0.2.0.jar`锛?1,585,398 B锛夛紝宸查儴缃插埌 `D:/game/.minecraft/versions/1.20.1-Fabric 0.20.1/mods/`銆?
- 瑕嗙洊灞備笁鏉¤矾绾匡細鈶?鍙敤鍘熺増 `renderTex` + 娣峰悎锛屼粛鏄潎鍖€鐏般€佺湅涓嶅嚭璐村浘锛涒憽 mixin 鍐呰嚜缁?quad锛屽悓甯ц GUI 鎵规鍗犵敤鐢讳笉鍑猴紱鈶?鏈€缁堟柟妗?`shared/client/SoulGlassOverlay` 娉ㄥ唽 `HudRenderCallback`锛氬厛 `GuiGraphics#fill` 閾?20% 榛戯紝鍐?`blit` 鐏甸瓊鐜荤拑璐村浘閾烘弧灞忓箷锛岀珯杩涙柟鍧楀嵆鍙湅鍒板崐閫忔槑鐜荤拑璐村浘鍙犲湪涓栫晫涓娿€?
- `mixin/ScreenEffectRendererMixin` 淇濈暀锛屽彇娑?`tconstruct` 鏂瑰潡鐨?vanilla 瑕嗙洊灞傦紝閬垮厤鍙犱袱灞傘€?
- 瑁呯疆淇锛歚CaptureScenario` 瀵?`11_soul_glass` 闀滃ご鍘熷厛闅愯棌 GUI锛宍HudRenderCallback` 鍥犳鏁存涓嶆墽琛岋紱鏀逛负杩涢暅澶村墠 `minecraft.options.hideGui = false`銆?
# 鍖犻瓊 3.12.1 鈫?Fabric 1.20.1 绉绘杩涘害锛堥€氬鎵规锛?

> 鐩爣锛氭妸 Forge 1.20.1 鐨勫尃榄傦紙`[鍖犻瓊] TConstruct-1.20.1-3.12.1.231.jar`锛?*閫愮被鎼繃鏉?*锛屽彧鏇挎崲 Forge 鈫?Fabric 鐨勬帴鍙ｅ眰锛岀帺娉曞唴瀹逛竴琛屼笉鏀广€?
> 涓婃父瀵圭収婧愮爜锛歚ref/TinkersConstruct`锛?.12.1锛孧ojang 鏄犲皠锛夈€?



## 2026-11-06 23:58 鐏甸瓊鐜荤拑銆岀珯杩涘幓榛戝睆銆嶄慨澶?

- 鏍瑰洜锛氱珯鍦?isViewBlocking 鏂瑰潡閲屾椂锛寁anilla ScreenEffectRenderer.renderScreenEffect 浼氱敤鏂瑰潡妯″瀷閲岀殑**绮掑瓙鍥炬爣**锛圔lockModelShaper.getParticleIcon锛夌敾涓€寮犻摵婊″睆骞曠殑 quad锛岄《鐐归鑹插浐瀹?(0.1, 0.1, 0.1, 1.0) 涓?*涓嶅紑鍚贩鍚?* 鈥斺€?浜庢槸鏁村睆琚笉閫忔槑鐨勬繁鑹茬汗鐞嗙硦姝伙紝鐏甸瓊鐜荤拑鐪嬭捣鏉ュ氨鏄粦灞忋€?
- 淇硶锛氭柊澧炲鎴风 mixin src/main/java/slimeknights/tconstruct/mixin/ScreenEffectRendererMixin.java锛屽湪 ScreenEffectRenderer.renderTex 鐨?HEAD/RETURN 涓婏紝鍙湁褰撹鐩栧眰鐢ㄧ殑 sprite 灞炰簬 	construct 鍛藉悕绌洪棿鏃舵墠 RenderSystem.enableBlend() / disableBlend()锛涘叾瀹冩柟鍧楋紙鐭冲ご銆佹矙瀛愮瓑 vanilla 鏂瑰潡锛夌殑瑕嗙洊灞備繚鎸佸師鏍枫€?
- 楠岃瘉锛?Pcapture=true 鐨?11_soul_glass 闀滃ご锛堢浉鏈哄湪 3脳3脳3 鐏甸瓊鐜荤拑鎴块棿鍐呴儴锛変粠銆屽潎鍖€涓嶉€忔槑鐏般€嶅彉鎴愬崐閫忔槑銆侀€忓嚭涓栫晫涓庡喍鐐肩倝锛涙棩蹇楃‘璁?overlay sprite=tconstruct:block/soul_glass alpha-blended锛涙湇鍔＄鑷祴 58 鏉″叏缁裤€?
- 浜х墿锛歍inkers-Fabstruct-1.20.1-0.1.0.jar = 31,581,981 B锛宻ha256 鍓嶇紑 4E0F22D9C20613EB锛屽凡閮ㄧ讲鍒?D:/game/.minecraft/versions/1.20.1-Fabric 0.20.1/mods/銆?

## 2026-11-06 09:47 鐢ㄦ埛瀹炴祴鍙嶉淇
- **宕╂簝 `AbstractMethodError: NoContainerIngredient.test(...) is abstract`锛堝悜鍐剁偧鐐夊閾佺熆鐭筹級**锛氭牴鍥犳槸 Fabric 鐨?`CustomIngredient#test(ItemStack)` 鏄?Fabric API 鏂规硶銆佽繍琛屾椂鍚嶄繚鎸?`test`锛岃€屾垜浠繖浜涜嚜瀹氫箟 ingredient 鍚屾椂缁ф壙鍘熺増 `Ingredient`锛屽叾 `test` 琚噸鏄犲皠鎴?`method_8093`锛汧abric 鐨?`IngredientMixin` 瑙ｆ瀽 `fabric:type` 鏃惰皟 `customIngredient.toVanilla()`锛岄粯璁ゅ疄鐜颁細鍖呬竴灞?`CustomIngredientImpl`锛岃€屽畠鐨?`method_8093` 姝ｆ槸璋?`CustomIngredient#test` 鈫?绫讳笂娌℃湁璇ュ悕瀛楃殑鏂规硶 鈫?宕╂簝銆備慨娉曪細鍦?`NestedIngredient` 涓?`BlockTagIngredient`/`InstrumentIngredient`/`MaterialValueIngredient`/`ToolHookIngredient` 閲屾妸 `toVanilla()` 瑕嗗啓涓?`return this;`锛堟垜浠湰鏉ュ氨鏄師鐗?Ingredient锛屼笉闇€瑕佸寘瑁咃級銆傚洖褰掕嚜娴嬶細`checkMelting` 鐜板湪浼氭妸**姣忎竴鏉?*鐔斿寲閰嶆柟閮藉鐫€涓€鍧楅搧鐭跨煶璺?`matches()`锛堜箣鍓嶅氨鏄繖鏉¤矾寰勫穿鐨勶級锛屾棩蹇?`1 melting recipes matched an iron ore without a custom ingredient crash`銆?
- **鐒﹂粦鐜荤拑锛坄seared_glass`锛夎兘绌胯繃 + 鍑忛€?鈥斺€?鐪?bug锛屽凡淇?*锛歚TinkerSmeltery` 閲岀伒榄傜幓鐠冮偅琛屽啓鐨勬槸 `SEARED_GLASS.noCollission().speedFactor(0.1f)...`锛岃€?`BlockBehaviour.Properties#noCollission()`/`speedFactor()` **鏄氨鍦颁慨鏀瑰苟杩斿洖鍚屼竴涓疄渚?*锛屼笉鏄鍒躲€備簬鏄繖涓€琛屾妸鍏变韩鐨?`SEARED_GLASS` 瀛楁鏀规垚浜嗘棤纰版挒 + 0.1 閫熷害锛屾墍鏈夌敤璇ュ疄渚嬫敞鍐岀殑鏂瑰潡锛坄seared_glass`銆乣seared_glass_pane`銆乣seared_tinted_glass`銆佷互鍙?`scorched_*` 鍚屾锛夐兘鍙樻垚鍙┛琛屼笖鍑忛€?鈥斺€?涓庣敤鎴锋埅鍥鹃噷銆岀劍榛戠幓鐠冦€嶅畬鍏ㄥ惢鍚堛€備慨娉曪細鐏甸瓊鐜荤拑鏀逛负鐢?`builder(...)` 鐜板満鏋勯€犲叏鏂?properties锛屼笉鍐嶄粠 `SEARED_GLASS` 娲剧敓銆傝嚜娴?`checkGlass` 鐜板湪鏂█ `seared solid=true seared speed=1.0`銆乣seared soul solid=false`锛堢伒榄傜幓鐠冩寜涓婃父璁捐鍙┛琛岋級銆傚叏浠撳璁¤繃鍏朵綑 `noCollission()`/`speedFactor()`/`friction()` 鐢ㄦ硶锛岄兘鏄悇寤哄悇鐨勬柊 builder锛屾棤绗簩澶勫悓绫婚棶棰樸€?
- **閫氶€忕幓鐠冦€屽彲绌胯繃 + 鍑忛€熴€?*锛堝凡鎺掗櫎锛夛細`clear_glass`/`clear_stained_glass` 纰版挒褰㈢姸闈炵┖銆侀€熷害绯绘暟 1.0锛涘彧鏈?`soul_glass`锛堢伒榄傜幓鐠冿級鎸変笂娓歌璁?`.speedFactor(0.2F).noCollission()`锛岃瑷€鏂囦欢宸ュ叿鎻愮ず銆屼笌鏅€氭柟鍧楃浉浼硷紝浣嗘槸鍙互浠庝腑绌胯繃銆嶃€?

## 鏀瑰悕涓庡彂甯冨噯澶囷紙2026-11-06 10:4x锛?

- 鏄剧ず鍚嶇粺涓€涓?**Tinkers' Fabstruct**锛屼骇鐗╂枃浠跺悕 `Tinkers-Fabstruct-1.20.1-0.1.0.jar`锛坄archivesName = "${archives_base_name}-${minecraft_version}"`锛夛紝浣?*淇濇寔 mod id `tconstruct`**锛氭暟鎹寘/鏍囩/閰嶆柟/瀛樻。鍏ㄦ寜杩欎釜鍛藉悕绌洪棿绱㈠紩锛屾敼 id 浼氳鎵€鏈夊瓨妗ｅけ鏁堛€?
- 鍗曚竴鏉ユ簮锛歚gradle.properties` 鐨?`mod_name=Tinkers' Fabstruct` 鈫?`build.gradle` 鐨?`processResources` 灞曞紑杩?`fabric.mod.json` 鐨?`name`锛涜瑷€鏂囦欢閲岃ˉ浜?`modmenu.nameTranslation.tconstruct` = `Tinkers' Fabstruct`锛堢帀 / ModMenu 浼?*鍏?*璇昏繖涓?key锛屽啀鍥為€€鍒板厓鏁版嵁鍚嶏紝杩欏氨鏄父鎴忓唴宸ュ叿鎻愮ず涓婃柟閭ｈ妯＄粍鍚嶇殑鐪熸鏉ユ簮锛夊拰 `key.categories.tconstruct`锛堟寜閿垎绫伙級銆?
- 涓€鏉″懡浠ゆ敼鍏ㄩ儴鏄剧ず鍚嶏細`tools/rename_mod.py "鏂板悕瀛?`锛堝凡鎸?UTF-8 BOM 瀹归敊锛宍zh_cn` 绛夊甫 BOM 鐨勮瑷€鏂囦欢涔熻兘澶勭悊锛夛紙鍙€?`--item-groups "鍖犻瓊涓?` 杩炲垱閫犳爮鏍囩鍓嶇紑涓€璧锋崲锛夈€?
- 娉曞姟鏉愭枡锛氫粨搴撴牴鏂板 `LICENSE`锛圡IT锛屽惈 SlimeKnights 涓庣Щ妞嶈€呬袱娈电増鏉冭锛変笌 `NOTICE`锛堥€愪釜鍒楀嚭鍐呭祵搴撲笌璁稿彲锛歁antle MIT銆丳orting Lib **LGPL-3.0**銆丼tar/Cardinal/Dripstone/Milk/MixinSquared/Reach=MIT銆丯ightConfig LGPL-3.0銆乯sr305 Apache-2.0锛屽苟澹版槑銆岄潪瀹樻柟銆佹棤鍟嗘爣鎺堟潈銆嶏級锛沗build.gradle` 鐨?`jar` 鍧楁妸瀹冧滑鏀捐繘 `META-INF/`锛屾墍浠ュ彂甯冨嚭鍘荤殑 jar 鑷甫璁稿彲涓庡綊灞炰俊鎭€傚綋鍓?jar **31,560,585 B**锛坄fabric.mod.json` 鐨?name 鏄?`Tinkers' Fabstruct`锛涢儴缃茶剼鏈寜 `Tinkers-Fabstruct-*.jar` 閫夊寘骞舵帓闄?`-sources`/`-dev`锛屽悓鏃跺垹闄ゅ疄渚嬮噷鏃ф枃浠跺悕鐨?jar锛岄伩鍏嶅悓鍚?mod id 鍐茬獊锛夈€?

## 閫氬鏀跺熬锛?026-11-06 06:47锛?

- **鏈€缁堜骇鐗?*锛歚build/libs/tconstruct-fabric-0.1.0.jar` = **31,575,969 B**锛宍sha256 09afdd993f5f78e816cf2fbfab854d446aada3e89d56f36fd006098ba3c2cf0f`锛屼笌宸查儴缃插埌 `D:/game/.minecraft/versions/1.20.1-Fabric 0.20.1/mods/tconstruct-fabric-0.1.0.jar` 鐨勫壇鏈?*閫愬瓧鑺備竴鑷?*銆?
- **鏈€缁堣嚜娴?*锛歚rm -rf run/world && ./gradlew runServer -Pselftest=true` 鈫?55 鏉?`[selftest]` 杈撳嚭锛宍all checks passed`锛? failure銆?
- **鏈€缁堝鎴风**锛歚runClient --quickPlaySingleplayer` 姝ｅ父杩涗笘鐣岋紙`Loaded 1697 advancements`銆?934 ms锛夛紝鏃?`Exception`/`Unable to bake`/`Missing texture`锛孍RROR 浠?`No data fixer registered for tconstruct:*` 涓?`mantle:sign` 鍚岀被鍣煶銆?
- **鏃╀笂璇烽噸鐐瑰疄娴?*锛氣憼 鏃犵紳鐜荤拑杩為€氥€佲憽 閫氶€忕幓鐠冨悇鑹茬幓鐠冩澘鐗╁搧鍥炬爣銆佲憿 鍌ㄧ綈/缃愮墿鍝佸彸閿€掑彇娴佷綋銆佲懀 娴囬摳鍙伴摳妯′笌浜х墿銆佲懁 鏁寸悊鑳屽寘涓庡垱閫犳爮鍒囬〉銆佲懃 宸ュ叿鑰愪箙鏉′笌淇グ绗?tooltip銆佲懄 鍐剁偧鐐?閾搁€犵倝+榫欏ご+鎺掓恫鍙ｆ暣濂楀鏂瑰潡銆佲懅 宸ヤ綔鍙扮粍瑁?淇悊/鍗囩骇銆?
- **宸茬煡閬楃暀**锛歚TODO(fabric)` 361 澶勶紙缁濆ぇ澶氭暟鏄€孎abric 鏃犲搴?API銆嶈鏄庢€ф敞閲婏級锛涙磱绾㈣壊鍧楀凡纭鏄?`clear_glass/magenta` 鏈壊 `FFB24CD8`锛沗crafting player` 绾跨▼鏈湴涓婃父涔熸棤娑堣垂鑰咃紱娴嗘恫锛坰lime锛?鎶曟幏鐗╃瓑瀹炰綋琛屼负娌℃湁鑷姩鍖栨柇瑷€锛屽彧鑳介潬鎵嬫祴銆?

## 鏈€鏂扮姸鎬侊紙2026-11-06 04:0x锛?

- **缂栬瘧**锛歚./gradlew build --offline -x test` 鎴愬姛锛? 閿欒銆備骇鐗?`build/libs/tconstruct-fabric-0.1.0.jar` 31,575,969 B銆?
- **鏃犵紳鐜荤拑锛堣繛閫氳创鍥撅級宸蹭慨**锛欶abric 鐨?`BlockModel` 涓嶄繚鐣?`#all` 杩欑被寮曠敤鐨勭埗妯″瀷鏄犲皠锛宍ConnectedModel.getConnectedNameUncached` 鍙兘杩唬鍒版湰妯″瀷鑷繁鐨?`textures`锛宍#down`/`#up`/`#north`鈥?姘歌繙瑙ｆ瀽涓嶅埌 `connection.textures` 閲岀殑 `all`锛屼簬鏄瘡涓潰閮戒繚鐣欏甫杈规鐨勫熀纭€璐村浘銆備慨娉曪紙`ref/Mantle-fabric/src/main/java/slimeknights/mantle/client/model/connected/ConnectedModel.java`锛夛細姣旇緝鍓嶅墺鎺?`#`锛涙爣鍑嗗叚闈㈤敭鍦?`connectedTextures` 鍙湁 `all` 鏃跺洖閫€鍒?`all`銆傝繘涓栫晫瀹炴満鏃ュ織楠岃瘉锛歚connected bake ... faces down=#all_d up=#all_u north=#all_ud south=#all_ud west=#all_ul east=#all_ur`銆?
- **鏈嶅姟绔嚜娴?*锛歚rm -rf run/world && ./gradlew runServer -Pselftest=true` 鍏ㄧ豢 鈥斺€?閰嶆柟绫诲瀷璁℃暟锛坧art builder 169 / material 217 / tinker station 281 / melting 850 / casting table 527 / casting basin 166 / alloy 31 / fuel 3锛夈€乣tool definitions 45`銆乣materials 96`銆侀搧闀?275 鑰愪箙 / 6.0 鎸栨帢閫熷害銆佸偍缃?1000 mB 娴佷綋锛堝€?鍙栦袱鏉¤矾寰勶級銆乣recipe errors: 0`銆乣Failed to load modifier 0`銆乣Loaded 300 dynamic modifiers and 6 modifier redirects`銆乣all checks passed`銆?
- **瀹㈡埛绔?*锛氬彲杩涗笘鐣岋紱`Finished loading 208 Block entity items`锛堟柟鍧楀疄浣撳睍绀虹墿鍝?閾告ā鎭㈠锛夛紱鍓╀綑 ERROR 浠?`No data fixer registered for tconstruct:*` 45 鏉★紙鍘熺増涔犳儻鎬у櫔闊筹級銆?
- **鏁版嵁**锛氶厤鏂?3018 鏉″叏閮ㄥ彲瑙ｆ瀽锛坄forge:conditional` 灞曞紑銆佺暩褰?`fabric:any` 褰掍竴鍖栥€乣forge:difference` 璇箟绛変环鏀瑰啓銆佸 mod 閰嶆柟鎸?Forge 鏉′欢璇箟涓㈠純锛夛紱璧勬簮涓?jar 100% 瀵归綈锛坢odels 697/852銆乥lockstates 435銆乼extures 13867銆乴ang 20锛夈€?
- **宸蹭慨锛堢敤鎴峰疄娴嬮┍鍔級**锛氭棤缂濈幓鐠冭繛閫氳创鍥撅紙鏈锛夈€佸喍鐐肩倝娴佷綋鍖哄嚑浣曪紙`GuiSmelteryTank` 浼?106脳106锛夈€佽瀺鍖栨爮鐑噺鏉℃Ы浣嶏紙`i + indexOffset`锛夈€佹《鍊?鍙栨祦浣撲袱鏉¤矾寰勶紙Mantle `FluidTransferHelper` 鐨?mB 鍥為€€涓庡師鐗堟《鍒嗘敮 + 鎺у埗鍣?`getFluidStorage`锛夈€佺啍铻嶆《璐村浘锛堢儤鐒欐湡浠?`FluidTextureManager` 鍙?still 璐村浘锛夈€丼tar 鍖哄潡娴佷綋娓叉煋 NPE锛堟敞鍐?`FluidRenderHandlerRegistry`锛夈€佸垱閫犺儗鍖呭垏椤靛穿婧冿紙`TankModel`/`FluidContainerModel` 鍏滃簳 + `FluidVariantRendering`锛夈€佹柟鍧楁覆鏌撳眰锛坄gen_render_layers.py` 鈫?`TinkerRenderLayers`锛夈€侀€氶€忕幓鐠冩澘鐗╁搧鍥炬爣锛堥鏌撹壊璐村浘 + `item/generated`锛夈€佸悎鎴愰晲瀛愬穿婧冿紙`Item#getCraftingRemainingItem` 鍙┖锛夈€佽澶囨Ы NPE锛坄ModifiableItem.getEquipmentSlot` 鍥為€€ MAINHAND锛夈€佸睘鎬?id锛坄porting_lib:`锛夈€侀噸澶?reload listener锛? 涓?manager 鐨?`init` 骞傜瓑锛夈€佽鏄庨潰鏉垮瓧鍙?1.25锛堝敮涓€涓诲姩鍋忕涓婃父澶勶級銆?
- **浜х墿鑷锛?026-11-06 04:35锛?*锛歚tools/verify_jar.sh` 鍏ㄩ儴 ok锛坋ntrypoint main/client銆乤ccess widener 澹版槑涓庢枃浠躲€乵ixin 閰嶇疆銆?5 涓唴宓?jar銆?240 data json銆?549 妯″瀷銆?3888 璐村浘銆?491 class锛夛紱瑙ｅ寘纭鍐呭祵 `META-INF/jars/Mantle-1.20.1-1.11.DEV.eb1e9a5.jar` 鐨?`ConnectedModel$Baked` 鍚?`FACES` 鍥為€€涓?*涓嶅惈**璋冭瘯鐢ㄧ殑 `DEBUG_CONNECTED`锛沗ProjectileWeaponMixin.class` 鍦ㄥ寘鍐呫€?
- **瀹㈡埛绔啋鐑燂紙鍚屾椂闂达級**锛歚./gradlew runClient --offline --args="--quickPlaySingleplayer 鏂扮殑涓栫晫"` 鈫?姝ｅ父杩涗笘鐣屻€乣Loaded 79 advancements`锛堝惈鍖犻瓊杩涘害锛屼腑鏂囧悕姝ｅ父锛夈€佹棤 `Exception`/`Unable to bake`/`Missing texture`锛沗Model loader` 涓€绫昏鍛婂彧鍓?Star/Indium/Sodium 鐩稿叧锛坄libs` 鍐?Star 璋冭瘯搴撳缂哄け mod 鐨?mixin target 鎺㈡祴锛夈€?
- **閮ㄧ讲**锛歚D:/game/.minecraft/versions/1.20.1-Fabric 0.20.1/mods/tconstruct-fabric-0.1.0.jar`锛?1,575,969 B锛岀敤 `tools/deploy.sh` 閮ㄧ讲锛涙湁 dev 瀹㈡埛绔湪璺戞椂璇ヨ剼鏈細鎷掔粷锛夈€?
- **鐢熺墿缇ょ郴**锛欶orge 鐨?8 涓?`forge/biome_modifier` 宸茬敤 `TinkerBiomeModifications`锛團abric `BiomeModifications.addFeature/addSpawn`锛夌瓑浠峰疄鐜帮紝鑷祴楠岃瘉閽寸熆/澶╃┖鏅舵礊宸叉敞鍏ュ搴旂敓鐗╃兢绯汇€?
- **宸蹭慨锛堟湰杞柊澧烇級**锛氶摳閫犲彴娴囬摳涓嶅嚭浜х墿锛坄CastingFluidHandler.insert` 鐨勫灞傚叧闂洖璋冮『搴忊€斺€旀姤鍐呭鍙樺寲鐨勫洖璋冨厛璺戙€乣currentRecipe` 杩樻槸 null 瀵艰嚧 `coolingTime = -1`锛涗慨娉曪細璁?`currentRecipe` 鍚庣珛鍗?`onContentsChanged()` 閲嶇畻鍐峰嵈锛夈€乪nderference 鍙栨秷浼犻€侊紙鏀圭敤 Porting Lib `EntityEvents.TELEPORT` + `setCanceled(true)`锛屾浛鎹㈠師 TODO 娉ㄩ噴鍧楋級銆?
- **TODO(fabric) 瀹¤锛?026-11-06 04:5x锛?*锛氭寜娉ㄩ噴褰掔被 top 椤?鈥斺€?crafting player 绾跨▼鏈湴 9锛堜笂娓告棤娑堣垂鑰咃級銆丮antle 1.11 鏃?`html` 涔﹂〉瀵煎嚭 8銆乣getModelData` 7銆乣FluidType.BUCKET_VOLUME` 7锛堝凡鎸?1000 mB 骞崇Щ锛夈€乣BlockEntityPacket` 6锛堟敼涓鸿嚜鏌ユ壘 BE锛夈€乣Block#getBlockPathType` 6锛堟敼 Fabric path node registry锛夈€丗orge capability 5銆乣InventoryBlockEntity` 4銆乣setCustomClientFactory` 4锛涢€愭潯鎶芥煡 `Item#isRepairable`锛坄ModifiableItem`/`ModifiableArmorItem`/`ModifiableLauncherItem`锛変笌涓婃父 3.12.1 婧愮爜閫愬瓧瀵规瘮锛氫笂娓稿悓鏍?`return false; // handle in the tinker station`锛?*琛屼负瀹屽叏涓€鑷?*锛屾敞閲婁粎涓鸿В閲婄Щ妞嶈竟鐣屻€?
- **鏂板鑷祴**锛歚checkDrain`锛堢洿鎺ュ湪澧欓噷寤?`seared_drain`锛坄HORIZONTAL_FACING=EAST`锛? 鐒﹂粦鐮?5脳5 鍦版澘/澧?+ `foundryController` 鍚屾鎺у埗鍣紝鎴愬瀷鍚?`FluidStorage.SIDED.find(level, drainPos, WEST)` 寰€閲屾敞 90 mB 鐔旈搧 鈫?鍐剁偧鐐?`getTank()` 鏀跺埌 90 mB锛涢獙璇?`SmelteryFluidIO.getCapability` 鈫?`ISmelteryTankHandler.getFluidCapability` 杩欐潯銆屾帓娑插彛 鈫?鎺у埗鍣ㄥ偍缃愩€嶉摼锛堜篃灏辨槸涔嬪墠淇帶鍒跺櫒鍙抽敭鏃惰ˉ鐨?`HeatingStructureBlockEntity.getFluidStorage`锛夈€?*娉ㄦ剰**锛氭帓娑插彛蹇呴』鍦?*缁撴瀯棣栨鎴愬瀷鍓?*灏辨斁杩涘閲岋紝鍥犱负 `MultiblockStructureData.assignMaster` 瀵?`oldStructure.contains(pos)` 鐨勫潗鏍囦細璺宠繃锛堜笂娓搁€昏緫濡傛锛夛紝鎴愬瀷鍚庡啀鏇挎崲澧欑爾涓嶄細閲嶆柊缁戝畾 master锛孊E 浼氫竴鐩?`master=null`銆佹祦浣撹兘鍔涗负 null锛?
- **鏂板鑷祴**锛歚checkToolTraits`锛堥搧闀愯閰嶅悗鏂█ `ToolStack.from(stack).getModifiers().getEntry(new ModifierId(TConstruct.getResource("pierce")))` 闈炵┖涓?`getLevel() == 1`锛岄獙璇?`tconstruct:tool_definitions/pickaxe` 鐨?`tconstruct:traits` 妯″潡鍦ㄨ閰嶆椂鎶?trait 娉ㄥ叆宸ュ叿锛?
- **瀹㈡埛绔啋鐑燂紙2026-11-06 06:33锛屾帓娑插彛/鐔斿寲鐐?榫欏ご/閾搁€犵倝鍏ㄩ儴鍚堝叆鍚庯級**锛歚./gradlew runClient --offline --args="--quickPlaySingleplayer 鏂扮殑涓栫晫"` 鈫?杩涗笘鐣屾甯搞€乣Loaded 1697 advancements`銆乣Time elapsed: 1934 ms`锛沗[ERROR]` 鍏?45 鏉★紝闄?`No data fixer registered for tconstruct:*` 澶栦粎 `mantle:sign`/`mantle:hanging_sign` 涓ゆ潯鍚岀被鍣煶锛涙棤 `Exception`/`Unable to bake`/`Missing texture`锛汼odium/Indium/Frex 鐨?`ClassNotFoundException` 鍙槸 dev 鐜娌℃湁杩欎簺鐩爣 mod 鐨勬帰娴嬭鍛?
- **璧勬簮寮曠敤瀹¤锛?026-11-06 06:4x锛宍tools/audit_models.py` + 鍐呰仈鑴氭湰锛?*锛?549 涓ā鍨嬪叏閮ㄦ鏌ヤ簡 `parent` 涓?`textures`锛坣amespace 鐪佺暐鏃舵寜 `minecraft:` 瑙ｆ瀽锛屼笌 `ResourceLocation` 涓€鑷达紱涔熸妸 `libs/*.jar`锛圥orting Lib 鑷甫 `assets/forge/models/item/default`銆乣default-tool`銆乣bucket_drip`锛夌畻浣滃彲瑙ｆ瀽鏉ユ簮锛夆啋 **0 涓己澶辫创鍥?*锛涘墿 3 涓?`tconstruct:block/slimy_mud_*` 鐖舵ā鍨嬬己澶憋紝**涓婃父 3.12.1 jar 閲屽悓鏍风己**锛坄assets/tconstruct/models/item/slimy_mud_blue.json` 鎸囧悜浠庢湭鍙戝竷鐨勬柟鍧楁ā鍨嬶紝娌℃湁浠讳綍 item 娉ㄥ唽瀹冿級鈫?涓庝笂娓镐竴鑷淬€佹棤杩愯鏃跺奖鍝嶃€?35 涓?blockstate 鐨?`model` 寮曠敤鍚屾牱瀹¤锛?0 涓?`tconstruct:block/*_frame`/`*_frame_map` 缂哄け锛屽疄娴嬩篃鏄?*涓婃父閬楃暀**锛?.12 鐨勮竟妗嗘槸瀹炰綋 `frame/gold` 绛夋ā鍨?+ `FancyItemFrameEntity`锛屽搴?block 鏍规湰娌℃敞鍐岋級锛屼笌涓婃父閫愬瓧鐩稿悓銆傜粨璁猴細璧勬簮灞傛病鏈夐潤榛樼己璐村浘銆?
- **鏂板鑷祴**锛歚checkShield`锛堟湪鐩捐姱 + 閾佹澘 = `ToolBuildHandler.buildItemFromMaterials(TinkerTools.plateShield, MaterialNBT.of(wood, iron))` 鈫?鏂█ `DURABILITY 270`锛堥搧 `plating_shield` 鐨?durability 鍊硷級銆乣BLOCK_AMOUNT 100`銆乣BLOCK_ANGLE 180`锛屽嵆 `tconstruct:material_stats` 鍙?stat 绫诲瀷涓?`base_stats` 鐨?block 鏁板€奸兘杩涗簡鐩剧墝锛涙棩蹇?`the wood/iron plate shield has 270.0 durability, blocks 180.0 degrees for 100.0 damage`锛?
- **闊虫晥涓庢湰鍦板寲瀹¤锛?026-11-06 06:4x锛?*锛歚assets/tconstruct/sounds.json` 30 涓簨浠躲€?5 涓煶鏁堝紩鐢ㄥ叏閮ㄨ兘瑙ｆ瀽锛堣嚜鍛藉悕绌洪棿 0 缂哄け锛涙棤 namespace 鏃舵寜 `minecraft:` 瑙ｆ瀽锛屼笌 vanilla 涓€鑷达級锛沗data/**` 閲?0 澶勫紩鐢ㄤ笉瀛樺湪鐨?`tconstruct:` 闊虫晥浜嬩欢銆傝瑷€鏂囦欢瀵圭収涓婃父 jar锛歚en_us` 3615 鈫?4356锛堝鍑虹殑鏄Щ妞嶈ˉ鐨?overlay key锛夛紝`zh_cn` 3561 鈫?4300锛沗en_us` 鏈夎€?`zh_cn` 缂虹殑 key **55 鏉★紝涓庝笂娓?jar 瀹屽叏鐩稿悓**锛坴enom 鏉愯川鍚嶃€乯ei 鎻愮ず绛変笂娓镐篃娌＄炕锛夛紝涓嶅瓨鍦ㄧЩ妞嶅紩鍏ョ殑缂虹炕銆?
- **鏂板鑷祴**锛歚checkArmorModifier`锛堥搧鏉跨洈 + 閽荤煶璧板伐浣滃彴 `calcResult` 鈫?`tconstruct:diamond` 鎸備笂銆乽pgrade 妲?2鈫?銆乣ARMOR 2.0 鈫?3.0`銆乣DURABILITY 165 鈫?415`锛涙暟鍊煎叏閮ㄦ潵鑷?`data/tconstruct/tinkering/modifiers/diamond.json` 鐨?`stat_boost`锛氳€愪箙 +500 鍐嶅 `tconstruct:modifiable/armor` 鍑?250锛屽彟鏈?*鏃犳潯浠?* `armor +1.0`銆乣attack_damage +0.5`銆傜涓€鐗堟柇瑷€鍐欐垚銆宎rmor 涓嶅彉銆嶈鍒?red锛岀‘璁ゆ槸**涓婃父鏁版嵁琛屼负**銆佷笉鏄?bug锛屽凡鏀规垚鎸夋暟鎹柇瑷€锛?
- **鎸佷箙鍖栭獙璇?*锛?026-11-06 06:15锛夛細涓嶅垹 `run/world` 杩炵画璺戠浜屾 `runServer -Pselftest=true`锛宍[selftest] all checks passed` 涓斿姞杞芥湡鏃犳柟鍧楀疄浣?宸ュ叿 NBT 鍙嶅簭鍒楀寲寮傚父 鈫?鍐剁偧鐐夈€侀摳閫犵倝銆佹祰閾稿彴銆侀緳澶淬€佸偍缃愩€佸伐鍏疯繖浜涜嚜瀹氫箟 BE 涓庣墿鍝佹暟鎹殑**瀛樻。寰€杩?*姝ｅ父锛堢涓€娆¤窇浼氭柊寤轰笘鐣岋紝瑕嗙洊鍚屼竴鍧愭爣锛屾濂介獙璇佹棫 BE 琚纭嵏杞斤級銆?
- **鏂板鑷祴**锛歚checkFluidPlacement`锛坄new ItemStack(TinkerFluids.moltenIron.getBucket())` + `((BucketItem) item).emptyContents(player, level, ground.above(), null)` 鈫?鏂█ `level.getFluidState(...).getType() == TinkerFluids.moltenIron.get()`锛岀‘璁ゃ€岀啍铻嶉噾灞炴《鍦ㄤ笘鐣岄噷鍊掑嚭娴佷綋鏂瑰潡銆嶈繖鏉?`FlowingFluidObject.block(...)` 甯冪嚎鏄€氱殑锛涙敞鎰?*涓嶈鐢?`stack.useOn(UseOnContext)`**锛宍BucketItem` 鍙疄鐜?`use`锛屼笖 `ServerPlayer.lookAt` 浼氬洜 `connection == null` 鐩存帴 NPE锛塦銆乣checkFoundryStructure`锛坄(80,0)` 鐢ㄧ劍榛戠爾 `scorchedBricks` 閾?5脳5 鍦版澘 + `y=99` 绔嬩竴鍦?5脳5 澧欍€佸唴鑵?3脳3 绌哄嚭锛岃タ澧欐斁 `foundryController`锛坒acing WEST锛夈€佷笢澧欐斁 `scorchedTank(FUEL_TANK)`锛沗FoundryMultiblock` 鏄?`super(parent, true, true, false)`锛堟湁鍦版澘+鏈夋鏋躲€佹棤澶╄姳鏉匡級锛屼笌鍐剁偧鐐夌殑 `(true,false,false)` 涓嶅悓锛屽疄娴嬪悓鏍蜂竴娆℃垚鍨嬶紱闅忓悗鐏屽博娴?+ 閾侀敪 鈫?tick 300 鈫?鐔旂倝缃愬唴 鈮?0 mB 鐔旈搧锛岄獙璇侀摳閫犵倝锛坒oundry锛夌殑鐙珛鍔犵儹/鐔斿寲璺緞锛夈€乣checkAllToolRecipes`锛堥亶鍘?`TinkerRecipeTypes.TINKER_STATION` 閲岀殑鍏ㄩ儴 `ToolBuildingRecipe`锛?6 鏉★級锛岀敤鍏ㄩ搧鏉愭枡 `ToolBuildHandler.buildItemFromMaterials(tool, MaterialNBT.of(materials))` 閫愪釜缁勮锛屾柇瑷€浜х墿灏辨槸閰嶆柟缁撴灉鐗╁搧涓?`ToolStack.from(stack).getStats().get(ToolStats.DURABILITY) > 0` 鈫?`built 26 of 26 tools from iron`锛岀瓑浜庝竴娆℃€ф壂杩囧叏閮ㄥ伐鍏风殑 definition 妯″潡鎺ョ嚎锛夈€乣checkEntityMelting`锛堝喍鐐肩倝 `(60,0)` 鎴愬瀷鍚庣亴宀╂祮锛屾妸涓€鍙兊灏镐涪杩?3脳3 鍐呰厰銆佹椂闂磋 18000锛岃繛 tick 200 娆?鈫?鍍靛案鎺夎鍒?16.06銆佺倝鍐呭嚭鐜?`10 mB 鐔旇瀺閾乣锛涙敞鎰?*瀹炰綋鐔斿寲蹇呴』瑕佹湁鐕冩枡**锛宍canMeltEntities()` 鍦ㄦ棤鐕冩枡鏃剁洿鎺ヨ繑鍥?false锛屾祴璇曢噷绗竴鐗堝繕浜嗙亴宀╂祮灏卞畬鍏ㄦ病鍙嶅簲锛岀棁鐘舵槸銆屽兊灏歌閲忎笉鍙樸€佺綈瀛愮┖銆嶏級銆乣checkFaucet`锛坄(49,101,0)` 鏀?`seared_tank(INGOT_TANK)` 瑁?90 mB 鐔旈搧銆乣(49,100,0)` 鏀?`seared_faucet`锛坄FaucetBlock.FACING = DOWN`锛岃緭鍏ュ彇涓婃柟鏂瑰潡銆佽緭鍑虹粰涓嬫柟鏂瑰潡锛夈€乣(49,99,0)` 鏀?`seared_table` 骞跺涓€涓?`ingot_cast`锛涜皟 `faucet.activate()` 鍚庤繛 tick 200 娆?鈫?鐔旈搧缁忛緳澶存敞鍏ユ祰閾稿彴骞跺喎鍗存垚 `1 iron_ingot`銆傛敞鎰忥細娴囬摳鍙版病鏈夐摳妯℃椂 `CastingFluidHandler.insert` 璧?`initNewCasting` 杩斿洖瀹归噺 0锛岄緳澶翠細銆屽€掍笉杩涘幓銆嶏紝蹇呴』鍏堟斁閾告ā锛夈€乣checkMelter`锛堟妸 `seared_melter` 鏀惧湪 `(40,100,0)`銆佷笅闈㈠灚涓€涓亴婊″博娴嗙殑 `seared_tank(FUEL_TANK)`锛岄搧閿斁 0 鍙锋Ы锛岃繛缁?tick 300 娆?鈫?鐔斿寲鐐夊偍缃愰噷 `90 mB 鐔旇瀺閾乣锛涙敞鎰忕啍鍖栫倝鐨勬垚鍨嬬姸鎬?`ControllerBlock.IN_STRUCTURE` 鏄?*鏀剧疆鏃?*鐢?`getStateForPlacement` 妫€鏌ヤ笅鏂规槸鍚︿负 `tconstruct:fuel_tanks` 鏍囩鏂瑰潡鍐欏叆鐨勶紝娴嬭瘯閲岀敤 `setBlockAndUpdate` 鐩存帴鏀炬柟鍧椾笉浼氳Е鍙戯紝蹇呴』鎵嬪姩 `setValue(IN_STRUCTURE, true)`锛屽惁鍒?`isFormed()` 鎭?false銆佹暣浣?tick 绌鸿浆锛夈€乣checkSmelteryMelting`锛坄buildSmelteryAt(level,20,0)` 鎼嚭 3脳3 鍐剁偧鐐夊悗锛屽線澧欎笂鐨?`seared_tank(FUEL_TANK)` 鐏?1 妗跺博娴嗗綋鐕冩枡锛屾妸閾侀敪濉炶繘 0 鍙风啍鍖栨Ы锛岃繛 tick 300 娆?鈫?鍐剁偧鐐夊偍缃愰噷鍑虹幇 `90 mB 鐔旇瀺閾乣锛涜繖鏉″悓鏃堕獙璇佷簡 `MultitankFuelModule` 鐕冩枡璇嗗埆/娓╁害銆乣MeltingModuleInventory.heatItems` 鐩镐綅鎺ㄨ繘涓?`SmelteryTank` 杈撳嚭锛夈€乣checkSmelteryStructure`锛堝湪 `y=98` 閾?5脳5 鐒﹂粦鐮栧湴鏉裤€乣y=99` 绔嬩竴鍦堝骞剁┖鍑?3脳3 鍐呴儴锛岃タ澧欐斁 `smeltery_controller`锛坒acing WEST锛? 涓滃鏀?`seared_tank(FUEL_TANK)`锛岃皟 `updateStructure()` 鍚?tick 涓€娆?鈫?`ControllerBlock.IN_STRUCTURE` 鍙?true锛屽嵆澶氭柟鍧楃粨鏋勮瘑鍒€氳繃锛涜繖鏄涓獙璇佸鏂瑰潡鎴愬瀷鐨勬柇瑷€锛夈€乣checkMelting`锛坄smeltery/melting/metal/iron/ingot`锛歚matches` 閫氳繃锛岃緭鍑?90 mB `tconstruct:molten_iron`銆乣temperature 800`銆乣time 60`锛涙湰杞叡鍔犺浇 850 鏉＄啍鍖栭厤鏂癸級銆乣checkAlloying`锛堝亣 `IAlloyTank`锛?000掳C锛?70 mB 鐔旈摐 + 90 mB 鐔旈敗锛夆啋 `smeltery/alloys/molten_bronze` 鐨?`matches` + `canPerform` 閫氳繃锛岃緭鍑?`tconstruct:molten_bronze` 360 mB锛屽搴?`temperature: 700`锛夈€乣checkCastingBasin`锛坄TinkerSmeltery.searedBasin` 鐏?810 mB 鐔旈搧 鈫?`1 iron_block`锛屼笌 `data/tconstruct/recipes/smeltery/casting/metal/iron/block.json` 鐨?`amount: 810` / `cooling_time: 180` 瀵瑰簲锛涜嚦姝ゆ祰閾稿彴涓庢祰閾哥泦涓ゆ潯娴囬摳閾捐矾閮芥湁鏂█锛夈€乣checkCraftingRepairKit`锛堝伐浣滃彴 2脳2 閲?鎹熷潖閾侀晲 + 閾佷慨澶嶅浠?鈫?鑰愪箙淇弧锛岃蛋 `CraftingTableRepairKitRecipe`锛坄tconstruct:crafting_table_repair`锛屾暟鎹湪 `data/tconstruct/recipes/tables/crafting_table_repair.json`锛夌殑 `matches` + `assemble`锛涢『甯︽妸鑷祴閲岄€犲嚭鏉ョ殑鏃犺繛鎺?`ServerPlayer` 鎹㈡垚瑕嗗啓 `displayClientMessage` 鐨?`fakePlayer(...)`锛屾秷鎺?Mantle `FluidTransferHelper.playEmptySound` 姣忔鎵撴《閮藉埛 `this.connection is null` 鍫嗘爤鈥斺€旈偅鏄嚜娴嬪亣鐜╁鐨勫櫔闊筹紝鐪熷疄娓告垙閲屼笉浼氬嚭鐜帮級銆乣checkStationRepair`锛堥搧闀?setDamageValue(100) + 4 閾侀敪杩涘伐浣滃彴 鈫?淇鍒?0锛岃蛋 `TinkerStationRepairRecipe` 鐨?`MaterialRepairToolHook` 璺緞锛夈€乣checkModifierApplication`锛堝伐浣滃彴閲屾妸 `tconstruct:diamond` 鍗囩骇閰嶆柟搴旂敤鍒伴搧闀愶細缁撴灉甯?diamond銆乣upgrades` 3鈫?銆佽€愪箙 275鈫?25锛岃蛋鐪熷疄 `ModifierRecipe#getValidatedResult` 璺緞 = 鎵ｆЫ + 鍔犱慨楗扮 + `tryValidate`锛夈€乣checkToolBuilding`锛堝伐浣滃彴妲戒綅 1/2/3 鏀?pick_head + tool_handle + tool_binding 鈫?`tconstruct:pickaxe`锛岃€愪箙 250 = 閾佸ご鍩虹鍊硷紝鏉愭枡 3 浠斤紱鍏抽敭鐐癸細**妲戒綅 0 鏄€屽緟鏀硅宸ュ叿銆嶆Ы**锛岄儴浠朵粠妲戒綅 1 寮€濮嬶紝`TinkerStationContainerWrapper.getInput(i)` 鏄浉瀵归儴浠舵Ы鐨勫亸绉伙紝鎴戞渶鍒濇妸閮ㄤ欢鏀捐繘 0/1/2 瀵艰嚧 `getTinkerableStack()` 闈炵┖鑰屾案涓嶅尮閰嶁€斺€斿凡鐢辨棩蹇?`tinkerable=1 pick_head` 瀹氫綅锛夈€乣checkToolSlots`锛堥搧闀愰粯璁?upgrades=3 / abilities=1 鏉ヨ嚜 `tconstruct:modifier_slots` 妯″潡锛涙寜 `ModifierRecipe#applyModifiers` 鐨勫啓娉?`getPersistentData().addSlots(SlotType.UPGRADE, -1)` 鍚庨檷鍒?2锛沗rebuildStats()` **涓嶄細**鍥炴粴鎵ｅ噺锛屾墸鍑忔槸鎸佷箙鏁版嵁 鈥斺€?鏈熼棿鑷祴鍏堟姤銆? instead of 2銆嶆濂芥毚闇蹭簡鎴戝妲戒綅璇箟鐨勯敊璇亣璁撅紝宸叉寜涓婃父 `ref/TinkersConstruct/.../ModifierRecipe.java:156` 淇鏂█锛夈€乣checkTankItemTransfer`锛堝皝闂偍缃愮墿鍝佽 1000 mB 鐔旇瀺閾?鈫?绌烘《鍙栨恫寰楀埌 `tconstruct:molten_iron_bucket`銆佺綈鍐呭綊闆讹紝璧?`FluidTransferHelper.interactWithStack` 涓庡彸閿鐞嗗櫒鍚屼竴璺緞锛夈€乣checkArmor`锛堥搧鏉垮ご鐩?armor=2.0 / durability=165 / toughness=0.0锛岀洿鎺ラ獙璇?`PlatingMaterialStats.apply` 涓?`tconstruct:plate_helmet` 鐨?`part_stats` 妯″潡閾捐矾锛夈€乣checkModifierModules`锛坔aste +4.00 鎸栨帢閫熷害銆乨iamond 275鈫?25 鑰愪箙 +2.00 閫熷害 +0.50 鏀诲嚮銆乻harpness +0.75 鏀诲嚮銆乺einforced/luck/lightspeed/reach/knockback 鍧囨甯告寕杞戒笉鎶ラ敊鈥斺€旇鐩?`stat_boost`/`reduce_tool_damage`/`conditional_melee_damage` 绛夋ā鍧楁棌锛夈€乣checkCasting`锛?0 mB 鐔旇瀺閾?+ 閾告ā 鈫?閾侀敪锛夈€乣checkPartBuilder`锛? 閾滈敪 + 绌虹櫧妯″叿 鈫?閾滈晲澶达紝璧?`PartBuilderBlockEntity.selectRecipe`/`calcResult` 鐪熷疄璺緞锛夈€乣checkToolLifecycle`锛堥捇鐭冲己鍖?275鈫?25銆佷激瀹?鎹熷潖/淇锛夈€乣checkTankEmptyBucket`锛堢┖妗跺彇娴佷綋锛夈€乣checkBiomeModifications`锛堥挻鐭?澶╃┖鏅舵礊锛夈€?
- **娉ㄦ剰锛堜笌涓婃父涓€鑷达級**锛氶搧/閾滅瓑鏉愭枡瀹氫箟閲?`craftable` 鍐冲畾鑳藉惁鍦ㄩ儴浠跺彴鐢ㄦ潗鏂欏仛閮ㄤ欢鈥斺€擿iron.json` 鏄?`craftable: false`锛堜笂娓稿悓鏍凤級锛屾墍浠ラ儴浠跺彴鑷祴蹇呴』鐢ㄩ摐銆?
- **鏈В/寰呭姙**锛歚TankItem` 杞Щ鍔╂墜宸茬敱 `checkTankItemTransfer` 鑷祴瑕嗙洊锛堣涓嬶級锛涚敤鎴锋姤鐨勩€屾煇鏂瑰潡闈笂鏈夋磱绾㈣壊鍧椼€嶏細鏍稿涓?`clear_glass/magenta/pane_*` 鐨?`colors` = `FFB24CD8`锛堝嵆鍝佺孩鑹查€氶€忕幓鐠冩澘鐨勬湰鑹诧級锛屼笖寰呮父鎴忓唴纭锛涙ā鍨嬭创鍥?id 鍏ㄩ儴瀛樺湪锛坄missing textures: 0`锛夛紱浠嶆湁 361 澶?`TODO(fabric)`锛沚allista 鎹㈠脊宸茬敤 `mixin/ProjectileWeaponMixin.java`锛堟敞鍏?`ProjectileWeaponItem.getHeldProjectile` 鐨?RETURN锛屽鍒讳笂娓?`LivingGetProjectileEvent` 鐨勬崲寮硅鍒欙級琛ラ綈锛沜rafting player 绾跨▼鏈湴 9 澶勪粛涓哄凡鐭ョ己鍙ｏ紙浠呭奖鍝嶉厤鏂归噷鐨勭帺瀹朵笂涓嬫枃锛夈€?

## 0. 2026-11-05 18:xx 绛栫暐閲嶇疆锛堟湰杞渶閲嶈鐨勪竴姝ワ級

**鍙戠幇锛歚Alpha-s-Stuff/TinkersConstruct` 鍒嗘敮 `1.20.1`锛圚ephaestus锛夊氨鏄尃榄?3.6.4 鐨勫畬鏁?Fabric 绉绘**锛坢od id 浠嶆槸 `tconstruct`锛屽寘鍚?`slimeknights.tconstruct`锛孧ojang 瀹樻柟鏄犲皠锛岃嚜甯?157 琛?access widener锛屽唴宓?Fabric 鐗?Mantle 1.9.291锛夈€傚凡 clone 鍒?`ref/Hephaestus-1.20.1`锛屽彲鐩存帴褰撱€屽姞杞藉櫒鑳舵按鍙傝€冨疄鐜般€嶃€?

**澶辫澶嶇洏**锛氭垜浠箣鍓嶇敤 **yarn** 鏄犲皠锛岃€?`ref/TinkersConstruct` 鐨?3.12.1 婧愮爜鏄?**Mojang 鏄犲皠**锛坄net.minecraft.world.level.Level` / `CompoundTag` / `getLevel()`锛夈€?5 涓唬鐞嗕簬鏄垎澶存妸绫诲悕銆佸寘鍚嶃€佹柟娉曞悕鎵嬫敼鎴?yarn锛坄NbtCompound` / `net.minecraft.block` / `getWorld`鈥︼級锛岄€犳垚 5231 涓敊璇噷绾?1/3 鏄€岃嚜閫犵殑鏀瑰悕鍊恒€嶏紝鑰屼笖鍜屽弬鑰冨疄鐜板啀涔熷涓嶄笂鍙枫€?

**宸叉墽琛岀殑閲嶇疆**锛?
1. `build.gradle`锛歚mappings` 浠?`net.fabricmc:yarn` 鎹㈡垚 **`loom.officialMojangMappings()`**锛堟槧灏勪笉褰卞搷杩愯鏃讹紝Loom 瀵煎嚭鏃剁粺涓€ remap 鍒?intermediary锛夈€?
2. 鏃?yarn 鐗堟簮鐮佹暣浣撶Щ鍒?`ref/port-v1-yarn-src/java` 瀛樻。锛沗src/main/java` 鏀逛负**浠?`ref/TinkersConstruct` 鍘熸牱澶嶅埗 1938 涓枃浠?*锛堥€愬瓧鎼繍锛岄浂鏀瑰悕锛夈€?
3. 瑁?access widener锛歚src/main/resources/tconstruct.accesswidener`锛堜粠鍙傝€冪Щ妞嶅鍒讹紝157 琛岋紝Mojang 鍚嶏級锛宍fabric.mod.json` 鍔?`"accessWidener": "tconstruct.accesswidener"`锛宍build.gradle` 璁?`accessWidenerPath`銆?
4. 鏂板宸ュ叿锛歚tools/measure_heph_overlap.py`锛堟瘮瀵?3.6.4/3.12.1/鍙傝€冪Щ妞?涓夋５鏍戯級銆乣tools/adopt_unchanged.py`锛堟妸銆?.6.4鈫?.12.1 鏃犳敼鍔ㄣ€嶇殑鏂囦欢鐩存帴鐢ㄥ弬鑰冪Щ妞嶇殑 Fabric 鐗堣鐩栵級銆?
5. 宸叉寜 `build/heph_unchanged.txt` 閲囩撼 **291 涓枃浠?*锛?001 涓笁鏂归兘鏈夌殑鏂囦欢閲岋紝29.1% 鍦?3.6.4鈫?.12.1 涔嬮棿鏈綋鏈彉锛夈€?

**鏂板熀绾匡細`./gradlew compileJava` = 11001 涓敊璇?*锛堥噸缃悗鍘熷 11370锛岄噰绾?291 鏂囦欢鍚?11001锛夈€傞敊璇幇鍦?*鍏ㄩ儴鏄?Forge 涓撴湁 API**锛坄net.minecraftforge.*`銆乣LazyOptional`銆乣Capability`銆乣IFluidHandler`銆乣BlockEntityPacket`鈥︼級锛屼笉鍐嶆湁鏄犲皠鏀瑰悕鍣０銆?

**涓嬩竴姝ユ墦娉?*锛氭寜鍖呭垎鎵癸紝姣忔壒鐢?`ref/Hephaestus-1.20.1/src/main/java/` 閲屽悓鍚嶆枃浠朵綔涓恒€岃兌姘存爣鍑嗙瓟妗堛€嶉€愭枃浠跺榻愶紱3.12.1 鏂板鐨勬枃浠讹紙3.6.4 閲屾病鏈夊搴旓級鍙兘鍙傜収 1.11 Mantle + Porting Lib 鎵嬪啓銆?

> 鈿狅笍 鐜鍧戯細杩欏彴鏈哄櫒涓?`python` 鎸囧悜 Microsoft Store 瀛樻牴锛?*闈欓粯涓嶆墽琛?*锛堥€€鍑虹爜 49锛夈€傚繀椤荤敤 `C:/Users/YamIpass/.dsh/dsh-runtimes/dsh-primary-runtime/dependencies/python/python.exe`銆?

## 1. 涓€鍙ヨ瘽鐜扮姸

**婧愮爜 100% 鎼畬锛?938 涓?java 鏂囦欢锛孧ojang 鏄犲皠閫愬瓧澶嶅埗锛夛紝缂栬瘧 0 閿欒锛宩ar 宸查儴缃插埌鐢ㄦ埛瀹炰緥锛屽鎴风鍙繘涓栫晫锛岀帺瀹跺弽棣堢殑鐣岄潰/浜や簰闂鍦ㄩ€愭潯鍏抽棴銆?*

### 1.0 2026-11-06 00:xx 淇鎵规锛堢敤鎴峰疄娴嬪弽棣堥┍鍔級

| 鐢ㄦ埛鍙嶉 | 鏍瑰洜 | 淇硶 |
| --- | --- | --- |
| 鎵€鏈夊伐鍏?閮ㄤ欢涓€鐗囬摱鑹?| 鏃╂湡鐑樼剻鑴氭湰鎶?`tconstruct:material`/`tconstruct:tool` 妯″瀷鍘嬪钩鎴?`item/generated`锛屾潗鏂欐煋鑹蹭俊鎭涪澶?| 杩樺師涓婃父 852 涓墿鍝佹ā鍨嬶紝涓?浜斾釜妯″瀷 loader锛坄material`/`tool`/`material_block`/`fluid_container`/`gui`/`mantle:item_layer`/`mantle:nbt_key`锛夊叏閮ㄦ敞鍐?|
| 鍌ㄧ綈绫荤墿鍝佸浘鏍囩┖鐧?| `mantle:item_layer` 涓?`mantle:nbt_key` 浠庢湭娉ㄥ唽锛?8 + 2 涓ā鍨嬶級 | `CommonsClientEvents.registerModelLoaders()` 娉ㄥ唽 |
| 鍐剁偧鐐変腑闂寸亴瑁呮寜閽笉鏄剧ず | `HeatingStructureScreen` 閲?5 涓?`ElementScreen` 鍙０鏄庢湭缁樺埗 | 琛ュ洖涓婃父 `renderLabels`/`renderTooltip` 缁樺埗娈典笌 `getBucketContainer()`/`getTransferDirection()` 鏄惧紡璁块棶鍣?|
| 宀╂祮鍙抽敭涓嶅惛 / 鐒﹂粦鐕冩枡鍌ㄧ綈鏃犱氦浜?| 鍏ㄤ粨娌℃湁 `FluidStorage.SIDED` 娉ㄥ唽锛屾墍鏈?SIDED 鏌ユ壘杩斿洖 null锛坄FluidTransferHelper.interactWithTank` 鍏ㄩ儴鐭矾锛?| `FabricEvents.registerStorageFallbacks()` 鐢?`FluidStorage.SIDED.registerFallback` / `ItemStorage.SIDED.registerFallback` 鎺ヤ笂 `SidedStorageBlockEntity` |
| 鏂版墜涔︽姤閿?`Font.width because "fr" is null` | 璧勬簮閲嶈浇閲嶅缓 `BookData` 鍚?`fontRenderer` 涓?null | Mantle `BookData.getFontRenderer()`锛坣ull 鍥炶惤鍘熺増瀛椾綋锛? 鎵€鏈夎鍙栫偣鏀圭敤瀹?|
| 澶?mod 鍐呭鎶ラ敊锛坱wilightforest 娴佷綋/瑁呭銆両E/create 娴佷綋锛?| Mantle 1.11 鍙 `fabric:load_conditions`锛屼笂娓告暟鎹槸瑁?`conditions` | `library/utils/ConditionHelper.java` 缁熶竴姹傚€硷紱`tools/drop_foreign_effects.py`銆乣tools/drop_foreign_fluid_transfers.py` 鍒犻櫎鏃犳硶鏉′欢鍖栫殑澶?mod 鏁版嵁锛堝凡杩?`tools/rebuild_resources.sh`锛?|

| 鍐剁偧鐐夋斁浜嗘祦浣撳氨闂€€ | Mantle 鑷甫 core shader锛坄assets/mantle/shaders/core/fluid.*`锛変粠鏈敞鍐岋紝`FLUID_SHADER` 鎭掍负 null锛岀敾娴佷綋鍗?NPE | `FabricEvents.initClient()` 娉ㄥ唽 `CoreShaderRegistrationCallback.EVENT.register(MantleShaders::registerShaders)`锛沗HeatingStructureBlockEntityRenderer` 鍐嶅姞娓叉煋鍏滃簳 try/catch |
| 宀╂祮妗跺€掍笉杩涘偍缃愶紙mB 涓?droplet 鍗曚綅涓嶄竴鑷达級 | Fabric `FluidConstants.BUCKET=81000` droplet锛岃€岀Щ妞嶅悗鐨勫偍缃?閰嶆柟鎸?Forge 鐨?mB锛? 妗?= 1000锛夛紱`interactWithFilledBucket` 鐢?81000 妯℃嫙蹇呯劧澶辫触 | Mantle `FluidTransferHelper.bucketAmountFor()`锛氬厛璇?81000锛屽け璐ラ€€鍥?`81000/81`(=1000 mB)锛屼袱鏉¤矾寰勶紙鏂瑰潡鍙抽敭 + 瀹瑰櫒妲芥嫋鎷斤級閮借蛋瀹?|
| 鎵€鏈夐鑹茬殑銆岄€氶€忕幓鐠冦€嶅彉鐧姐€佺劍榛戠幓鐠冩棤缂濆け鏁?| 鎶藉彇绠＄嚎鎶婃柟鍧楁ā鍨嬬殑 `"loader"` 閿悆鎺夛紙160 涓枃浠讹級锛宍mantle:connected`/`mantle:retextured`/`mantle:colored_block` 涓変釜 loader 涔熸病娉ㄥ唽 | `tools/restore_block_model_loaders.py --apply` 杩樺師 160 涓柟鍧楁ā鍨嬶紱`CommonsClientEvents.registerModelLoaders()` 娉ㄥ唽涓変釜 mantle loader锛沗tools/fix_composite_loaders.py` 鎶?`forge:composite` 鏀?`porting_lib:composite` |
| 鍌ㄧ綈鐗╁搧涔嬮棿娴佷綋浜掑€掞紙鍙抽敭鎷栨嫿锛夊け鏁?| `TankItem` 鐨?`overrideStackedOnOther`/`overrideOtherStackedOnMe`/`mayHaveFluid`/`updateHeldItem` 鍦ㄧЩ妞嶆椂鏁存涓㈠け | 宸叉寜涓婃父 1:1 琛ュ洖锛坄FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack))` 鍒ゆ柇鍙瓨娴佷綋锛沗playUISound` + `getOrTransferFilled` 澶嶇敤 Mantle 鍔╂墜锛?|
| 鍒涢€犺儗鍖呭垏鍒板尃榄傞〉闂€€ / 鍌ㄧ綈绫诲浘鏍囨祦浣撳眰绌虹櫧 | 鈶燻FluidVariantRendering` 浠庢湭娉ㄥ唽 handler锛宍getSprites()` 杩斿洖 null锛涒憽Mantle `FluidTextureManager` 浠庢湭 `init`锛宍mantle/fluid_texture/*.json` 娌¤浇鍏?| 鏂板 `library/client/fluid/FluidVariantTextureHandler`锛堣 `FluidTextureManager` 鐨?still/flowing/color锛? `FluidClientEvents.clientSetup()` 閲?`FluidTextureManager.init(...)` 骞跺鍏ㄩ儴 `tconstruct:*` 娴佷綋 `FluidVariantRendering.register(...)`锛沗TankModel`/`FluidContainerModel` 鍔?sprite 绌哄€煎厹搴曪紙`Unable to bake model` 浠?70 鏉￠檷鍒?0锛?|
| 鍐剁偧鐐夌儹閲忔潯鍙敾鍦ㄩ儴鍒嗘Ы浣嶏紙鏈€涓嬮潰涓ゆ牸娌℃湁钃濇潯锛?| `GuiMeltingModule.drawHeatBars/drawHeatTooltips` 鍐欐垚 `menu.slots.get(i)`锛屼涪浜嗕笂娓哥殑 `i + indexOffset`锛堜晶闈㈠簱瀛樺亸绉伙級 | 涓ゅ寰幆鏀瑰洖 `i + indexOffset`锛坄HeatingStructureScreen` = 2锛宍MelterScreen` = 0锛?|
| 鍙抽敭鐒﹂粦鍌ㄧ綈鍊掑博娴嗘棤鍙嶅簲锛堟棤澶村凡澶嶇幇锛?| vanilla 妗跺湪 Fabric 涓嬫病鏈?`FluidStorage.ITEM`锛宍FluidStorage.ITEM` 鍙毚闇?81000 droplet 鐨勬暣妗惰鍥撅紱`interactWithContainer` 鐨?item-handler 鍒嗘敮绌鸿浆鍗磋繑鍥?`CONTAINER`锛岀煭璺簡鑳芥甯稿伐浣滅殑 `interactWithFilledBucket` | Mantle `FluidTransferHelper.interactWithContainer(...)` 鍦?item-handler 鍒嗘敮鍓嶅姞鍘熺増 `BucketItem` 鍒嗘敮锛坢B 鎹㈢畻 `bucketAmountFor`銆佽妗?鍊掓《涓ゆ潯璺緞锛夛紝骞舵妸绌鸿浆鍏滃簳浠?`CONTAINER` 鏀规垚 `MISSING`锛沗PortSelfTest.checkTankRightClick` 浣滀负鍥炲綊娴嬭瘯 |

鏈嶅姟绔嚜娴嬶紙`PortSelfTest`锛宍./gradlew runServer -Pselftest=true`锛夎鐩栵細閰嶆柟绫诲瀷璁℃暟銆佸伐鍏峰畾涔夋暟銆佹潗鏂欐暟銆侀搧澶撮儴鏁板€笺€侀搧闀愭垚鍝佹暟鍊笺€?*鐔旂倝鍌ㄧ綈鐨?SIDED 娴佷綋鏌ユ壘涓庡博娴嗘《鍊掑叆**銆?

**褰撳墠 jar**锛歚build/libs/tconstruct-fabric-0.1.0.jar`锛?1,317,868 B锛?026-11-06 01:2x锛屽惈鐑噺鏉℃Ы浣嶄慨姝?+ 娴佷綋璐村浘娉ㄥ唽 + 鍘熺増妗跺垎鏀級锛屽凡瑕嗙洊閮ㄧ讲鍒?`D:\game\.minecraftersions.20.1-Fabric 0.20.1\mods	construct-fabric-0.1.0.jar`銆傚鎴风鏃ュ織閲岀殑鏁版嵁鍔犺浇閿欒宸叉竻闆讹紙`Couldn't load tag` / `Failed to load` / `Couldn't parse` / `Unable to bake model` 鍏ㄤ负 0锛屼粎鍓?Star 璋冭瘯搴撲笌 `No data fixer` 鍣煶锛夈€?

閲岀▼纰戯紙閮藉凡瀹炴祴锛夛細
- `./gradlew compileJava` **0 error**锛沗./gradlew build --offline -x test` BUILD SUCCESSFUL銆?
- `./gradlew runServer`锛歚Done (5.xs)`锛宍Parsed 0 recipe errors`锛屾潗璐?99 / 淇グ绗?291 / 宸ュ叿瀹氫箟 45 / 绔欎綅甯冨眬 23 鍏ㄩ儴杞藉叆銆?
- `./gradlew runClient`锛氳兘杩涗笘鐣屻€佺帺瀹跺彲鐧诲綍锛堚€滄棤鏁堢殑鐜╁鏁版嵁鈥濆凡淇級銆佸伐鍏锋爮涓嶅啀闂€€锛堥檮榄旇ˉ涓侀€掑綊宸蹭慨锛夈€佹祦浣撴《妯″瀷宸叉仮澶嶃€?
- 浠嶅緟澶勭悊锛歚tags/blocks` 绛?476 涓爣绛撅紙宸叉娊鍑猴紝寰呬笅娆″惎鍔ㄩ獙璇侊級銆丗orge 鎴樺埄鍝佹潯浠跺凡鏀瑰啓銆丗orge 鎵规牸寮忔垚灏卞凡鎷嗗寘锛涗互鍙?361 澶?`TODO(fabric)`锛堝鏁版槸 datagen / Forge 鐙崰 API锛夈€?
- **鐩爣瀹炰緥 `D:\game\.minecraft\versions\1.20.1-Fabric 0.20.1\mods\tconstruct-fabric-0.1.0.jar` 宸叉洿鏂颁负鏈疆鏂版瀯寤?*锛?1,311,096 B锛夛紱鏃х殑 05:06 鎵嬪啓鐗堜粎鐣欏湪 `dist/tconstruct-fabric-0.1.0-working.jar` 浣滃厹搴曘€?

> 2026-11-05 13:30 鐢ㄦ埛鍐冲畾锛?*涓嶅啀淇棫鐨勬墜鍐欑増鏈?*锛屽叏閮ㄧ簿鍔涙姇鍒?1:1 鎼繍鐗堟湰锛堟棫 jar 浠呬綔鍏滃簳澶囦唤锛夈€?

### 1.1 鏈疆锛?0:00 涔嬪悗锛夌殑鍏抽敭淇
- **access widener 琛ュ叏**锛氬師 AW 缂?28 澶勫師鐗堢鏈夋垚鍛橈紙`ThrownTrident.tridentItem`/`dealtDamage`/`ID_LOYALTY`銆乣AbstractArrow.life`銆乣FishingHook.life`銆乣Explosion.{fire,hitPlayers,damageCalculator}`銆乣Enchantment.slots`锛坄accessible mutable`锛夈€乣IntegerProperty.{min,max}`銆乣ItemEntity.pickupDelay`銆乣ItemInHandRenderer.MAP_*`銆乣ResourceLocation.ERROR_INVALID`銆乣Screen.font`銆乣MangroveRootPlacer.mangroveRootPlacement`锛屼互鍙?`Gui.renderSlot`銆乣RecipeManager.byType`銆乣LivingEntity.spawnItemParticles`銆乣ItemRenderer.renderModelLists`銆乣FishingHookRenderer.stringVertex`銆乣CraftingMenu.slotChangedCraftingGrid`銆乣SpriteSources.register`銆乣TextureAtlasHolder.getSprite`锛夆啋 涓€娆℃€ф秷鎺?78 涓敊璇紙492鈫?14锛夈€?
- **Mantle 1.11 琛?API**锛氫笂娓?3.12.1 鐢ㄧ殑 `Loadable.map(Function,Function)` / `Loadable.mapChecked(BiFunction,Function)`锛堜互鍙?`StringLoadable` 鐨勫悓鍚嶈鐩栵級鍦ㄦ湰 fork 閲屽彨 `flatXmap`/`comapFlatMap`锛屽凡鎸?Mantle 1.11.126 璇箟鍔犲洖鍒悕骞堕噸寤?jar锛坄libs/Mantle-1.20.1-1.11.DEV.eb1e9a5.jar`锛?,822,693 B锛夈€?
- **瀹㈡埛绔帴绾?*锛歚shared/CommonsClientEvents.registerModelLoaders()` 鐢?Porting Lib `RegisterGeometryLoadersCallback` 娉ㄥ唽 `gui` 妯″瀷 loader锛沗fluids/FluidClientEvents.registerModelLoaders()` 娉ㄥ唽 `fluid_container`锛沗tools/ToolClientEvents` 涓?`smeltery/SmelteryClientEvents` 涔嬪墠宸插悇鑷敞鍐?`material`/`tool`/`material_block` 涓?`tank`/`fluid_texture`銆?
- **`library/client/**` 宸叉竻闆?*锛堜唬鐞?373fe8c6锛歞elombok + `-proc:none` javac 楠岃瘉 0 閿欒锛夛紝鍖呮嫭 Forge 妯″瀷鍑犱綍 鈫?Porting Lib `IGeometryLoader`/`MantleBakedModel.Builder`銆乣IClientItemExtensions`鈫扨orting Lib `ArmorRenderer`銆佹潯浠垛啋Fabric `ConditionJsonProvider` 绛夈€?

### 1.2 褰撳墠 414 涓敊璇殑鍒嗗竷锛堝墠 12 涓枃浠讹級
`library/tools/item/ModifiableItem` 23銆乣smeltery/client/screen/AlloyerScreen` 22銆乣library/tools/item/armor/ModifiableArmorItem` 19銆乣smeltery/client/screen/MelterScreen` 19銆乣library/tools/item/ranged/ModifiableLauncherItem` 18銆乣smeltery/client/screen/module/GuiTankModule` 13銆乣shared/TinkerFood` 12銆乣smeltery/client/screen/HeatingStructureScreen` 11銆乣smeltery/data/Byproduct` 9銆乣smeltery/item/TankItem` 9銆乣tools/client/ModifierClientEvents` 9銆乣world/TinkerWorld` 8銆?

閿欒绫诲瀷锛歚鎵句笉鍒扮鍙穈 192銆乣鏂规硶涓嶄細瑕嗙洊` 84銆乣long鈫抜nt 鏈夋崯` 19銆乣PoseStack鈫扜uiGraphics` 15銆乣MobEffectInstance 涓嶆槸鍑芥暟鎺ュ彛` 12 绛?鈥斺€?閮芥槸 Forge 涓?Mojang/Porting Lib 鐨?API 褰㈢姸宸紓锛屼笉鏄帺娉曢€昏緫銆?

## 2. 鍏抽敭鍩虹璁炬柦锛堝凡鎵撻€氾紝杩欐槸鏈€闅剧殑涓€姝ワ級

| 缁勪欢 | 鐘舵€?| 璇存槑 |
|---|---|---|
| Fabric Mantle 1.11 | 鉁?鑷缓鎴愬姛 | 瀹樻柟娌℃湁 Fabric 鐗?1.11锛岀敤 `Alpha-s-Stuff/Mantle` 鐨?`1.20.1-update` 鍒嗘敮鑷缓锛歚ref/Mantle-fabric/build/libs/Mantle-1.20.1-1.11.DEV.eb1e9a5.jar`锛?01 class锛屽惈 `data/loadable`銆乣command`銆乣client/model` 鍏ㄥ锛夈€傛瀯寤洪渶 Gradle 8.14 + 鏈湴 maven 琛?`cardinal-components`銆?|
| Porting Lib 2.3.16-beta.81 | 鉁?| 23 涓ā鍧楁斁杩?`libs/`锛屾浛浠?Forge 鐨?capability / transfer / tool_action / tier-sorting 绛夈€?|
| JiJ 鍐呭祵 | 鉁?| `libs/` 鍏?34 涓?jar锛宍fabric.mod.json` 鐨?`jars` 鐢?`tools/sync_mantle_libs.py` 鐢熸垚锛汱oom 1.12 浜х殑 jar 宸插墺绂?`Fabric-*` manifest 鍏冩暟鎹互鍏煎 Loom 1.8.13銆?|
| Lombok | 鉁?| 鍏抽敭鍧戯細Loom 榛樿鎸傜殑 legacy Mixin AP锛坰ponge-mixin锛夊湪 JDK 21 涓?*闈欓粯绂佺敤 Lombok**锛岀瓑浜庢墍鏈?`@Getter`/`@RequiredArgsConstructor` 閮戒笉鐢熸垚銆傚凡鍦?`build.gradle` 璁?`loom { mixin { useLegacyMixinAp = false } }`銆?|
| 璧勬簮绠＄嚎 | 鉁?| `tools/rebuild_resources.sh`锛氭娊 3.12.1 鐨?assets/data 鈫?鐑樼剻 Forge 妯″瀷 loader 鈫?淇?atlas 鈫?鐢熸垚 forge 鏍囩/閾稿瀷閰嶆柟/琛ュ厖 lang銆?*鏀硅祫婧愬繀椤昏蛋杩欎釜鑴氭湰**銆?|
| 缂栬瘧绾緥 | 鈿狅笍 | 鍚屼竴鏃堕棿**鍙兘璺戜竴涓?Gradle**銆傚苟鍙戞瀯寤烘浘鎶?`minecraft-merged.jar` 鍐欐垚 22 瀛楄妭绌?zip锛屽鑷村嚑鍗冧釜鍋囬敊璇€傝窇涔嬪墠 `jps -l \| grep -i gradle`銆?|

## 3. 鎼繍瑕嗙洊锛堟枃浠舵暟锛?

| 鍖?| 鏂囦欢 | 鐘舵€?|
|---|---|---|
| `library/` | 976 | 鎼畬锛岀紪璇戦敊璇渶澶氾紙recipe/modifiers/tools/client锛?|
| `tools/` | 312 | 鎼畬 |
| `smeltery/` | 148 | 鎼畬锛坄block/**` 姝ｅ湪淇紝绾?1040 閿欒锛?|
| `tables/` | 86 | 鎼畬锛堝惈 menu/slot/network/client 灞忥級 |
| `shared/` | 70 | 鎼畬 |
| `world/` | 64 | 鎼畬 |
| `gadgets/` | 36 | 鎼畬 |
| `common/` | 25 | 鎼畬 |
| `fluids/` | 24 | 鎼畬 |
| `client/` | 1 | Fabric 瀹㈡埛绔叆鍙ｏ紙鏂板锛屼笂娓告棤瀵瑰簲锛?|

`TODO(fabric)` 鏍囪鍏?**281 涓枃浠?*锛岄兘鏄€孎orge 鏈夈€丗abric 娌℃湁銆嶇殑璇箟缂哄け锛岄€愭潯鍐欎簡娉ㄩ噴銆?

## 4. 褰撳墠閿欒鍒嗗竷锛坄tools/errsum.sh /tmp/d13.log`锛?231 鏉★級

```
tools\entity\ThrownTool            63   smeltery\block\entity\CastingBlockEntity   57
smeltery\item\TankItem             52   tools\logic\ModifierEvents                 50
tools\logic\ToolEvents             44   smeltery\menu\HeatingStructureContainerMenu 44
smeltery\client\screen\HeatingStructureScreen 41   smeltery\client\screen\AlloyerScreen 40
smeltery\block\entity\controller\HeatingStructureBlockEntity 38   world\TinkerWorld 36
```

閿欒绫诲瀷缁熻锛坄grep -oE "閿欒: ..." | sort | uniq -c`锛夛細`鎵句笉鍒扮鍙穈 3499銆乣鏂规硶涓嶄細瑕嗙洊鎴栧疄鐜拌秴绫诲瀷鐨勬柟娉昤 489銆乣鏂规硶寮曠敤鏃犳晥` 48銆丗orge `crafting.conditions`/`capabilities` 绛夊寘缂哄け ~60銆?

鏈€楂橀缂哄け绗﹀彿锛歚getId()` 163锛圠ombok 瀵归儴鍒嗙被涓嶇敓鏁堬紝瑙?搂4.1锛夈€乣getEntity()` 76銆乣getWorld()` 57銆乣getLevel()` 56銆乣getVariant()` 48銆乣ForgeCapabilities` 38銆乣getLivingTarget()` 34銆乣FluidAction` 27銆乣ICondition` 26銆?

### 4.1 鏈疆鏂版煡鏄庣殑涓変釜鐪熼棶棰?

1. **Lombok 鍙閮ㄥ垎绫荤敓鏁?*锛氬悓涓€濂?Gradle 宸ュ叿閾句笅锛屽線 `src/main/java` 閲屽涓€涓?`@Getter` 鎺㈤拡绫烩€斺€旀甯哥敓鎴愶紱浣?`JsonRedirect`(`@Data`)銆乣LazyModifier`(`@Getter`) 绛夌被渚濇棫鎶?`getId()` 涓嶅瓨鍦ㄣ€傚凡鎺掗櫎锛氭敞瑙ｅ鐞嗗櫒璺緞锛堟墽琛屾湡瀹炴祴鍙湁 `lombok-1.18.34.jar`锛夈€乣-proc:full`銆乣--release 17`銆乣--rerun-tasks` 鍏ㄩ噺閲嶇紪銆乣lombok.config`锛堜笉瀛樺湪锛夈€傗啋 涓嬩竴姝ヨ涔堝杩欏嚑涓被鎵嬪啓鎴愬憳锛岃涔堢户缁煡锛坄build.gradle` 宸?pin `options.annotationProcessorPath = configurations.annotationProcessor`锛夈€?
2. **`libs/Mantle` 缂?`BlockEntityPacket`**锛氫笂娓告墍鏈夌綉缁滃寘锛坄smeltery/network/*`銆乣tables/network/*`鈥︼級閮界户鎵垮畠锛孎abric 鐗?Mantle 1.11 鍙繚鐣欎簡 `ISimplePacket`/`IThreadsafePacket` 绯诲垪銆傞渶瑕佸啓涓€灞傝杽閫傞厤锛堟妸 `BlockEntityPacket` 璇箟鏄犲皠鍒?Mantle 1.11 鐨勫寘鍩虹璁炬柦锛夛紝**涓嶆槸鏈烘鏀瑰悕**銆?
3. **涓€涓?access widener 閮芥病鏈?*锛歚fabric.mod.json` 娌℃湁 `accessWidener` 瀛楁锛宍src/main/resources` 涓嬩篃娌℃湁 `.accesswidener` 鈫?`TridentEntity.tridentItem`銆乣PersistentProjectileEntity.life/piercedEntities`銆乣WorldRenderer.blockBreakingInfos`銆乣CraftingScreenHandler.updateResult`銆乣ApplyBonusLootFunction.*` 杩欎簺鍘熺増绉佹湁鎴愬憳鍏ㄦ姤銆屾壘涓嶅埌绗﹀彿銆嶃€?*杩欐槸褰撳墠鎬т环姣旀渶楂樼殑涓€姝?*锛堜竴娆¤兘娓呭嚑鐧炬潯锛夈€?

## 5. 褰撳墠鍙帺鐗堟湰

- 鏋勫缓浜х墿锛歚build/libs/tconstruct-fabric-0.1.0.jar`锛?1,575,969 B锛?026-11-06 04:1x锛夈€?
- 宸查儴缃诧細`D:/game/.minecraft/versions/1.20.1-Fabric 0.20.1/mods/tconstruct-fabric-0.1.0.jar`锛堝彟鏈?`fabric-api-0.92.12+1.20.1.jar`锛夈€?
- 鍐呭锛?744+ 涓笂娓哥被閫愮被鎼繍锛坈ommon/library/tables/smeltery/tools/world/gadgets/shared锛夛紝3018 鏉￠厤鏂广€?5 涓伐鍏峰畾涔夈€?6 绉嶆潗鏂欍€?00 涓姩鎬佷慨楗扮銆? 鏈寚鍗椾功銆? 涓敓鐗╃兢绯讳慨鏀癸紙浠ｇ爜瀹炵幇锛夈€?0 绉嶆祦浣擄紙鍚啍鍖?娴囬摳/鍚堥噾锛夈€?
- 宸查獙璇侊細鏈嶅姟绔?20 椤硅嚜娴嬪叏缁匡紙`all checks passed`锛夈€佸鎴风鍙繘涓栫晫涓旀棩蹇楁棤鍔熻兘閿欒銆佹棤宕╂簝鎶ュ憡銆?

## 6. 涓嬩竴姝ワ紙鎸変紭鍏堢骇锛?

1. **瑙嗚楠屾敹**锛氱敤閮ㄧ讲濂界殑瀹炰緥杩囦竴閬嶁€斺€斿伐鍏?閮ㄤ欢璐村浘銆佸偍缃愪笌娴佷綋妗躲€佸喍鐐肩倝澶氭柟鍧椼€侀摳閫犲彴銆佸伐浣滃彴 GUI銆佹寚鍗椾功銆侀€氶€忕幓鐠冧笌鏃犵紳鐜荤拑銆?
2. **娲嬬孩鑹插潡**锛氱敤鎴锋埅鍥鹃噷鏌愪釜鏂瑰潡闈㈠憟娲嬬孩锛堣创鍥?id 鍏ㄩ儴瀛樺湪锛宍missing textures: 0`锛夛紝闇€鍦ㄥ疄鏈哄畾浣嶅叿浣撴柟鍧椾笌妯″瀷璺緞锛堟€€鐤戣繛閫氬彉浣撴垨缂虹渷 tint锛夈€?
3. **ballista 鎹㈠脊**锛欶abric/Porting Lib 鏃?`LivingGetProjectileEvent` 绛変环浜嬩欢锛岄渶瑕?mixin `ProjectileWeaponItem`/`CrossbowItem` 鐨勫彇寮硅矾寰勶紙涓婃父瀹炵幇宸插湪 `tools/logic/ModifierEvents` 娉ㄩ噴鍧椾繚鐣欙級銆?
4. **crafting player 绾跨▼鏈湴**锛歚ForgeHooks.setCraftingPlayer` 9 澶勶紙`tables/block/entity/table/CraftingStationBlockEntity.java` 绛夛級锛屽奖鍝嶉厤鏂归噷 `getCraftingPlayer` 鐩稿叧琛屼负銆?
5. **鍓╀綑 `TODO(fabric)`**锛?61 澶勶紝閫愭潯璇勪及鏄惁闇€瑕佺湡瀹炲疄鐜帮紙澶氭暟鏄?Forge capability 鐨勭瓑浠锋浛鎹㈠凡鐢熸晥锛夈€?
6. 鏀跺熬锛歚tools/audit_missing_methods.py` 澶嶆牳 190 涓€屼笂娓告湁鎴戜滑娌℃湁銆嶇殑鏂规硶鍚嶏紝纭鍏ㄩ儴涓烘湁鎰忔浛鎹€?

## 7. 鏋勫缓鍛戒护

```bash
cd tconstruct-fabric
./gradlew compileJava --console=plain            # 缂栬瘧
./gradlew test build --console=plain             # 娴嬭瘯 + 鍑?jar
bash tools/rebuild_resources.sh                  # 閲嶆柊鐢熸垚璧勬簮锛堜細瑕嗙洊鎵嬪伐鏀瑰姩锛岃皑鎱庯級
# Mantle 閲嶅缓锛堟敼 libs 鏃讹級
cd ref/Mantle-fabric && ~/gradle-8.14/bin/gradle build -x test --console=plain --no-daemon
cd ../.. && python tools/sync_mantle_libs.py     # 鍚屾 libs/ + fabric.mod.json
```

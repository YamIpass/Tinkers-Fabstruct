## 0.3.0 — JEI 集成

第三个公开版本。之前的移植版**完全没有 JEI 插件**，所以 JEI 里只有原版合成表里的匠魂物品，匠魂自己的工作站配方一个都看不到。这一版补上。

### JEI 插件（第一阶段）

- 新增六个配方分类：工具装配、工具改造、强化、部件制造台、模具、强化工作台。新世界的加载计数：26 / 206 / 270 / 204 / 58 / 25。
- 注册了材料变体、强化、模具、槽位四种 ingredient 类型，配方页能画出和方块 GUI 一致的图标。
- 工作站与工具栏的配方转移（一键放入材料）已接上；催化剂覆盖四张工作台、砧和强化工作台。
- 材料配方的 shaped / shapeless / 工具改造扩展也接回来了，材料变体的合成配方会和工作站配方分到一组，而不是散落在原版分类里。

### JEI 标签页标题

- JEI 的标签页标题走翻译键 `tag.<注册表>.<命名空间>.<路径>`，取不到就显示原始 id（`#tconstruct:tooltips/metal`）。上游一个这样的键都没提供。
- 现在本模组的 **885 个标签**都有中英文可读名称，例如 `tooltips/metal` → 「工具提示 · 金属」、`casts/single_use/ingot` → 「铸模 · 一次性 · 锭」。
- 标题下方那行灰色小字是 JEI 自己渲染的原始 id，语言文件改不了。

### 构建

- JEI 改为仅编译期依赖：玩家自备 JEI，jar 里不内嵌任何东西。离线构建优先用本地 `libs-compile/`，没有时回退到 Modrinth 的 maven。
- 内嵌的 Mantle jar 不再声明它自己都没打包的 `jei_mod_plugin` 入口（原来 JEI 会报 `Mantle specified an invalid entrypoint`）。
- 补了一个最小的 `slimeknights.mantle.plugin.jei.MantleJEIConstants`：上游的材料/工具改造扩展需要它，而裁剪过的 Mantle jar 里没有这个类。

### 尚未完成

- 流体类配方分类（熔炼、浇铸、合金、实体熔炼、断头、燃料）还没有注册。Fabric 版 JEI 用 `FabricTypes.FLUID_STACK` + `JeiFluidIngredient(Fluid, long, CompoundTag)` 表示流体，而上游插件是照 Forge 的 `FluidStack` 写的，需要改写后才能搬，放在下一阶段。

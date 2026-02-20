# AE2 Pattern Generator 进度追踪

## 1. 已完成功能 (Completed Features)

### 核心模组 (Core Module)
- [x] **ItemPatternGenerator**:
  - 新增 `Pattern Generator` 物品。
  - 注册 `ItemPatternGenerator` 到 GregTech 依赖环境。
  - **右键交互**: 打开 GUI (Configuration)。
  - **Shift+右键交互**: 打开 Storage GUI (Pattern Management)。
  - **Shift+右键方块**: 批量导出样板到目标容器。
- [x] **Tooltips**:
  - 基础名称显示。
  - Shift 按下显示详细操作说明。

### GUI (Client Side)
- [x] **GuiPatternGen (Generator Interface)**:
  - **现代化 UI 设计**: 全新的深色主题，发光边框，卡片式布局。
  - **输入字段**: Recipe Map, Output OreDict, Input OreDict, NC Item, Blacklist (Input/Output)。
  - **GuiComboBox (New)**: 自定义下拉框控件，用于选择电压等级 (ULV - MAX)。
  - **交互**: 列表查询、预览匹配数、生成请求发送。
  - **配置项**: 快速打开配置文件按钮。
- [x] **GuiPatternStorage (Management Interface)**:
  - *功能实现中*: 用于查看和删除已生成的样板。

### 后端逻辑 (Server Side)
- [x] **Pattern Generation Logic**:
  - `PacketGeneratePatterns`: 接收客户端参数。
  - `GTRecipeSource`: 集成 GregTech API，遍历 `GT_Recipe_Map`。
  - **Regex Filtering**: 支持正则表达式匹配输入/输出矿辞。
  - **Tier Filtering**: 根据配方 EU/t (`mEUt`) 计算电压等级并过滤。
  - **Encoding**: 转换为 AE2 `Encoded Pattern` (支持合成与处理模式)。
- [x] **Storage System**:
  - `PatternStorage`: 基于 NBT 的自定义存储系统 (位于 `mods/AE2PatternGen/storage/`)。
  - 支持每个玩家独立的样板缓存。

### 杂项 (Misc)
- [x] **Configuration**: `ReplacementConfig` 实现矿辞替换规则 (加载 `.cfg` 文件)。
- [x] **Recipe**: 添加了有序合成表 (HV Motor + Blank Pattern + ME Interface)。

---

## 2. 核心架构逻辑 (Core Architecture)

### 数据流 (Data Flow)
1. **客户端 (GUI)**: 用户输入筛选条件 (Machine Name, OreDicts, Tier) -> 发送 `packet`。
2. **服务端 (Logic)**:
   - 解析 `Recipe Map` 名称 (如 "implosion_compressor")。
   - 遍历 GT 配方列表。
   - 应用过滤器:
     - **正则匹配**: Input/Output 物品是否匹配正则表达式。
     - **电压匹配**: 配方 EU/t 是否在所选等级范围内。
     - **黑名单**: 排除特定物品。
     - **替换规则**: 应用 `ReplacementConfig` 替换输入材料。
   - **生成样板**: 将匹配的配方转换为 AE2 编码样板。
   - **保存**: 写入 `PatternStorage` (文件持久化)。
3. **客户端 (Feedback)**: 聊天栏提示生成结果 (成功数量 / 失败原因)。

### 网络通信 (Networking)
- `SimpleNetworkWrapper` channel based.
- `PacketGeneratePatterns`: Client -> Server (触发生成)。
- `PacketSaveFields`: Client -> Server (保存 NBT 设置)。
- `PacketStorageAction`: Client -> Server (删除/清空样板)。

---

## 3. 未解决 Bug (Unresolved Bugs)

### 🔴 严重 (Critical)
- **GUI 无法打开 (Silent Failure)**:
  - 表现: 手持物品右键无反应，无崩溃日志。
  - 状态: **已修复 (Fixed)**。
  - 原因: GuiHandler 缺乏网络注册中心登记 (`NetworkRegistry.registerGuiHandler`)，并且因 `@Mod.Instance` 初期手动错误赋值导致 FML 包路由异常。已全线加装 FMLLog 跟踪并平稳修复了此注册断层。



### 🟢 轻微 (Minor)
- **Build Workaround**:
  - `GuiComboBox` 中 `ResourceLocation` 构建报错 (方法签名不匹配)，暂时禁用了点击音效以通过编译。
- **Lint Errors**:
  - 项目存在大量 IDE 警告 (Unused imports, Raw types)，但不影响运行。

---

## 4. 下一步计划 (Next Steps)

### 🛠️ 修复与调试 (Fix & Debug)
1. **分析 GUI 启动日志**: 检查用户提供的 `latest.log`，定位 GUI 打开失败的根本原因 (类缺失? 构造函数异常?)。
2. **修复音效代码**: 找到 `PositionedSoundRecord` 在 1.7.10 环境下的正确构造方式，恢复下拉框音效。

### 🚀 功能增强 (Enhancements)
1. **空白样板消耗限制**: 已实现从手持与网络请求执行样板生成时的数量判定。系统现在会严格按照玩家持有的 `AE2 Blank Pattern` 的数额来按量结算。如果背包余量匮乏，系统将拦截操作。

2. **GuiPatternStorage 完善**:
   - 确保分页和删除功能完全正常。
   - 增加 "一键清空" 确认弹窗。
3. **性能优化**:
   - 缓存 GT 配方查询结果，避免重复遍历。

### 📦 发布 (Release)
- 确认 GUI 修复且包含新过滤机制及能稳定计算扣减后，发布 **v1.4.0 Release**。
- 更新 CurseForge/Modrinth 页面文档。

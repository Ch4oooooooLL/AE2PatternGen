# AE2 Pattern Generator

AE2 Pattern Generator 是一个为 Minecraft 1.7.10 (GregTech New Horizons 2.8.4) 设计的补丁类 Mod。
它的核心功能是从 GregTech 的配方映射中批量收集配方，并自动编码到 AE2 的样板（Encoded Pattern）中，极大简化了大规模自动化的初始化工作。

## ✨ 主要功能

- **批量编码**: 选择一个 GregTech 机器（如化学浸取器），自动将其全量配方导出为样板。
- **配置化过滤**: 
  - 支持通过正则表达式匹配输出/输入物品的矿物辞典。
  - 支持对 NC (Nuclear Control) 物品进行特殊过滤（由 `ID:Meta` 定义）。
  - **黑名单系统**: 允许通过物品 ID 或矿辞排除特定的配方。
- **现代化 GUI**: 模拟 AE2 1.21+ 的极简发光风格界面。
- **数据持久化**: 所有配置均保存于样板生成器物品的 NBT 中。

## 🚀 安装与使用

1. 将生成的 `.jar` 文件放入 `mods` 文件夹。
2. 在游戏中合成或通过创造模式获取 **AE2 Pattern Generator** 物品。
3. **右键打开 GUI**: 在界面中输入 `Recipe Map` 名称、矿辞过滤关键词或黑名单规则。
4. **预览与生成**: 点击 `Preview` 查看即将生成的配方数量，点击 `Generate`（需潜行状态下）开始批量导出到背包中的空白样板。

## 🛠️ 构建

项目使用 RetroFuturaGradle 构建系统：

```bash
./gradlew build
```

构建产物位于 `build/libs` 目录下。

## 📄 许可

本项目遵循 MIT 许可证。

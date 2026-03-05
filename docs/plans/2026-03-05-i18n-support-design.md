# i18n Support Design

## Context
项目目前只有 `item.ae2patterngen.pattern_generator.name` 使用 `lang` 文件，其余 GUI/聊天/命令反馈均为硬编码中文，导致无法根据客户端语言切换。

## Goals
- 为核心玩家可见文本提供中英双语切换（`zh_CN` / `en_US`）
- 新增统一翻译调用入口，避免后续继续硬编码
- 不改变现有业务流程，只替换显示层文案

## Non-Goals
- 不处理注释/Javadoc
- 不做额外 UI 结构重构
- 不引入第三方 i18n 框架

## Approaches
1. 仅替换 GUI 文本，不改聊天与命令
- 优点: 变更最小
- 缺点: 聊天和命令仍硬编码，体验不完整

2. 统一翻译工具 + GUI + 关键聊天/命令迁移（推荐）
- 优点: 改动可控，覆盖主要玩家可见文本，便于继续扩展
- 缺点: 需要补较多 lang 键

3. 全量改造所有玩家可见字符串（含全部网络包）
- 优点: 最彻底
- 缺点: 本次改动面过大，回归成本高

## Selected Design
采用方案 2。

- 新增 `com.github.ae2patterngen.util.I18nUtil`
  - `tr(key)`
  - `tr(key, args...)`
  - `trOr(key, fallback, args...)`（缺失键时回退）
- GUI 使用 `I18nUtil` 拉取文本
- 服务器消息与命令优先改为 `ChatComponentTranslation`
- 新增并维护统一前缀键 `ae2patterngen.prefix`
- 为已迁移文本补齐 `zh_CN.lang` 与 `en_US.lang`

## Error Handling
- 缺失键时不崩溃，回退到 fallback 或键名
- 参数格式错误时回退到基础翻译文本

## Testing Strategy
- 对 `I18nUtil` 做单元测试（键命中、缺失键、格式化）
- 对核心构建任务做编译验证

## Rollout
- 第一批覆盖: 4 个 GUI + 关键聊天消息 + 关键命令输出
- 后续增量把剩余文本继续迁移

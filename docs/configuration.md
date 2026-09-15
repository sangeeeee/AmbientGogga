# 配置文件

本模组只注册两份配置，默认位于游戏或服务器的 `config` 目录：

| 文件 | 配置内容 |
| --- | --- |
| `ambientgogga-client.toml` | `[shichieichou]` 七影蝶外观与粉尘；`[fireflies]` 陆地萤火虫；`[seaFire]` 海萤火 |
| `ambientgogga-server.toml` | `[butterflies]` 普通蝴蝶生成规则，包括 `spawnSeason` 季节限制 |

客户端配置仅在客户端加载，粒子季节限制也位于客户端配置中。服务端配置在单机世界或服务器启动时加载，多人游戏由服务器决定并同步给客户端；七影蝶不受普通蝴蝶的生成季节限制。独立服务器不生成客户端配置。

当前 NeoForge 1.21.1 默认从全局 `config` 目录读取服务端配置。如果手动在某个存档的 `serverconfig` 目录放入同名文件，NeoForge 会优先使用该存档的覆盖文件。本模组不主动生成额外的存档配置副本。

## 从旧版升级

启动时将旧的 `ambientgogga-fireflies-client.toml`、`ambientgogga-sea-fire-client.toml` 合入客户端配置，将 `ambientgogga-butterflies-common.toml` 合入服务端配置。原本 `ambientgogga-client.toml` 中的七影蝶设置继续使用。各配置段和参数名称保持不变，默认数值没有因合并而调整。

新文件中已经存在的参数优先保留，旧文件只补充缺少的参数。保存成功后，旧配置收进 `config/ambientgogga-legacy/`；已有目标文件在迁移前也备份到这里。备份不会覆盖之前的备份，且不会被当作生效配置加载。后续正常启动不会重复迁移或生成旧文件。

无法解析的旧文件保留在原处并记录日志；其他可读取文件仍可迁移。无法读取目标文件或无法完成写入时，迁移程序保留尚未迁移的原件。非法参数仍由 NeoForge 按配置范围修复，原始文件可以从备份中找回。

构建检查覆盖合并后配置格式、自定义值保留、新旧冲突优先级、重复启动、备份不覆盖，以及损坏文件的处理。未启动完整 Minecraft 客户端验证升级流程。

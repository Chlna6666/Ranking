# Ranking
![图片](https://bstats.org/signatures/bukkit/Ranking.svg)

这是一个 Minecraft 服务器排行榜插件，根据玩家的统计数据（如放置方块数量、破坏方块数量、死亡次数、击杀数量和在线时长等）生成排行榜，并在游戏中显示。

![图片](docs/img/scoreboards.png)

## 功能特点
- **多种排行榜类型**: 插件支持放置、破坏、死亡、击杀和在线时长等多种排行榜类型。
- **可自定义开关**: 每种排行榜类型可根据服务器需求进行自定义开启或关闭。
- **多种数据存储方式**: 支持 JSON 文件、数据库和 MySQL 数据库等多种数据存储方式。
- **多语言支持**: 插件提供英文和简体中文两种语言支持，可根据服务器语言偏好进行选择。

## 配置文件介绍

```yaml
# 排行榜插件配置文件
# 选择语言
language: zh_CN  # 可选值: en_US (English), zh_CN (简体中文) ./Ranking/language/ 可以放入其他语言包使用

# bStats 开关
bstats:
  enabled: true  # 是否启用 bStats 统计

# 更新检查器设置
update_checker:
  enabled: true   # 是否启用更新检查器
  notify_on_login: true   # 管理员登录时是否提醒更新

# 动态记分板轮换间隔（单位：分钟）
dynamic:
  rotation_interval_minutes: 5

# 排行榜开关设置
leaderboards:
  place: true   # 是否启用放置榜
  destroys: true   # 是否启用挖掘榜
  deads: true   # 是否启用死亡榜
  mobdie: true   # 是否启用击杀榜
  onlinetime: true   # 是否启用在线时长榜
  break_bedrock: true

# 数据存储方式设置
data_storage:
  method: json   # 数据存储方式，可选值: mysql (MySQL 数据库), json (JSON 文件)

  # MySQL 配置
  mysql:
    host: localhost        # 数据库地址
    port: 3306             # 数据库端口
    username: your_username  # 数据库用户名
    password: your_password  # 数据库密码
    database: ranking_database  # 数据库名称
    table_prefix: ranking_      # 表前缀，可选项

  location: /plugins/Ranking/data   # 仅在使用 json 存储方式时有效
  save_delay: 100  # 保存延迟，单位为ticks (1 tick = 50 ms)
  regular_save_interval: 1200  # 定期保存间隔，单位为ticks (1 tick = 50 ms)


```

## 使用方法
1. 将插件放置在服务器的插件目录中。
2. 根据需要编辑配置文件，配置排行榜开关和数据存储方式。
3. 在游戏中使用相应的命令查看排行榜。

## 插件命令
![图片](docs/img/help.png)
- `/ranking <子命令>`: 主命令，用于查看排行榜和其他功能。
  - 别名: `/rk`
  - 用法: `/ranking <子命令>`
  - 子命令可选项:
    - `place`: 查看放置榜排行榜。
    - `destroys`: 查看挖掘榜排行榜。
    - `deads`: 查看死亡榜排行榜。
    - `mobdie`: 查看击杀榜排行榜。
    - `onlinetime`: 查看在线时长榜排行榜。
    - `dynamic`: 切换动态显示排行榜。
    - `help`: 查看帮助信息。
    - `all`: 查看全部排行榜信息。
    - `my`: 查看自己的排行榜信息。
    - `list <子命令>`:查看对应的排行榜信息。
# PAPI
![图片](docs/img/papi.png)



### 功能说明：
- 获取玩家排名数据
- 使用占位符 {ranking_place} 可以获取当前玩家的排名。
- 使用占位符 {ranking_place_<rank>} 可以获取指定排名的玩家数据，例如 {ranking_place_1} 获取排名第一的玩家数据。

#### 获取玩家破坏数、死亡数、怪物击杀数和在线时间：
- 使用占位符 {ranking_destroys} 获取当前玩家的破坏数。
- 使用占位符 {ranking_destroys_<rank>} 获取指定排名的玩家破坏数，例如 {ranking_destroys_1} 获取排名第一的玩家破坏数。
- 同样的方式可以应用于死亡数 {ranking_deads}, 怪物击杀数 {ranking_mobdie} 和在线时间 {ranking_onlinetime}。

### 示例用法：
- 获取当前玩家的（自己）排名：{ranking_place}
- 获取排名第一的玩家破坏数：{ranking_destroys_1}
- 获取排名第三的玩家在线时间：{ranking_onlinetime_3}
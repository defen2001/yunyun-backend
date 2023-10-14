# 芸芸偕行 (后端) - yunyun-backen
## 📝 项目介绍

芸芸偕行，乃以标签和队伍的社交平台，志在协助用户轻松寻觅志趣相投之同道伙伴，联袂出发，共同踏上旅程之路。

此为该项目的后端部分, 前端部分请移步 [yunyun-frontend](https://github.com/defen2001/yunyun-frontend)

## 🔧 项目技术栈

- **Redisson**
- SpringBoot
- MySQL
- MyBatis-Plus
- Redis
- Knife4j
- Hutool
- Tencent COS

## 🔍 主要功能

### 👤 用户相关

- 账号密码注册、登录
- ✏编辑个人信息
- 上传 / 修改头像
- 换绑手机 / 更改密码
- 热门标签推荐
- 搜索与标签相关联的伙伴
- 伙伴信息展示
- 滚动查询用户消息列表
- 未读消息标记 / 数量统计
- 用户推荐

### 👥 队伍相关

- 创建 / 加入 / 退出 / 解散队伍
- 设置队伍可见性为公开 / 私密
- 设置队伍人数上限
- 设置队伍过期时间
- 邀请用户入队 / 接受入队邀请
- 更改队伍信息
- 队伍信息展示
- 队伍推荐

## ✨ 功能亮点

1. 通过 Redisson 访问 Redis
    - 全部使用 `RedissonClient` 访问 Redis
    - 与 JDK 中常用的数据结构操作相同, 相比 `StringRedisTemplate` 更加方便
    - 提供了分布式锁, 方便实现分布式的互斥操作
2. 接口权限校验
    - 使用自定义注解 `@RoleCheck` 对接口所需的权限进行标注
    - 配合 `AuthInterceptor` 拦截器对用户进行权限校验
    - 通过配置 Knife4j 实现对不同权限接口的分组展示
3. 预缓存任务
    - 使用 `@Scheduled` 注解实现每日定时任务
    - 使用 Redisson 分布式锁实现预缓存任务的互斥
4. 用户消息
    - 使用 Redis 存储用户消息
    - 通过传入 `scrollId` 实现消息的滚动查询
    - 通过消息的 `isUnread` 字段实现对未读的标记
5. 队伍查询
    - 通过编写 Mapper XML 实现复杂的队伍查询
    - 通过 `association` 标签实现对复杂查询结果的映射
    - 通过 `sql` 标签抽取公共 SQL 语句

## 🚀 项目运行

1. 执行 `sql/yunyun_companion.sql` 中的 SQL 语句创建数据库
2. 在 `src/main/resources` 目录下创建 `application-dev.yml` 文件, 并填写相关配置, 样例如下

```yml
spring:
  application:
    name: yunyun-backend
  # DataSource Config
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/yunyun_companion
    username: root
    password: 123456
  mvc:
    pathmatch:
      matching-strategy: ANT_PATH_MATCHER
  # session 失效时间（分钟）
  session:
    timeout: 86400
    store-type: redis
  # redis 配置
  redis:
    port: 6379
    host: localhost
    database: 0
server:
  port: 8080
  servlet:
    context-path: /api
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: isDelete # 全局逻辑删除的实体字段名(since 3.3.0,配置后可以忽略不配置步骤2)
      logic-delete-value: 1 # 逻辑已删除值(默认为 1)
      logic-not-delete-value: 0 # 逻辑未删除值(默认为 0)
# 对象存储
cos:
  secretId: ******
  secretKey: ******
  bucketName: image-fenapi-1319981817
  region: ap-guangzhou
  endpoint: cos.ap-guangzhou.myqcloud.com

```

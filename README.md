<!-- Plugin description -->
# idea-kartos-plugin

idea奎托斯插件

# TODO

- [x] 注入生成
- [ ] pb.go生成
- [ ] biz/service/data模板
- [ ] tool window 一键生成

# 功能

## 1. 注入生成

* 在单例对象构造方法上添加注释wired
* 在目录下新建.wire文件
* 点击.wire文件的运行符号即可生成provider set和wire.go

.wire配置项:  

| key | value | desc |
| ---- | ---- | ---- |
| location | string | wire.go生成目录 |

example:
```properties
location=cmd/post
```

## 2. pb.go生成

TODO

## 3. biz/service/data模板

TODO

## 4. tool window 一键生成

TODO

# 使用
idea 内点击Gradle  -> idea-plugin-go-demo [buildPlugin]后在build/distributions可找到插件文件


<!-- Plugin description end -->
<!-- Plugin description -->
# idea-kratos-plugin

idea奎托斯插件

# TODO

- [x] 注入生成
- [x] pb.go生成
- [ ] biz/service/data模板
- [x] 一键生成wire/provider set/pb.go

# 功能

## 1. 注入生成

* 在单例对象构造方法上添加注释wired
* 在目录下新建.kratos文件
* 点击.kratos文件的运行符号即可生成provider set和wire.go

.wire配置项:  

| key          | value | desc |
|--------------| ---- | ---- |
| wireLocation | string | wire.go生成目录 |

example:
```properties
wireLocation=cmd/post
```

## 2. pb.go生成

## clients
* 在pb文件添加注释//kratos:clients
* 点击一键生成则会运行对应的kratos proto client

## pb
* 在pb文件添加注释//kratos:pb
* (optional) 添加注释depends如:
> //depends:./third_party
* 点击一键生成则会生成对应的pb.go

## 3. biz/service/data模板

TODO

## 4. 一键生成wire/provider set/pb.go

点击run旁边的go logo会运行生成provider set/wire.go/kartos proto clients/pb.go

# build
idea 内点击Gradle  -> idea-plugin-go-demo [buildPlugin]后在build/distributions可找到插件文件


<!-- Plugin description end -->
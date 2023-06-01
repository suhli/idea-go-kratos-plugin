idea[奎托斯](https://github.com/go-kratos/kratos)插件


# 功能

## 1. 注入生成

* 在单例对象构造方法上添加注释wired
* 在目录下新建.kratos文件
* 点击.kratos文件的运行符号即可生成provider set和wire.go

.kratos配置项:

| key          | value | desc |
|--------------| ---- | -- |
| wireLocation | string | wire.go生成目录 |
| layoutRepository | string | kratos cli -r参数 |

example:
```properties
wireLocation=cmd/post
layoutRepository=https://gitee.com/go-kratos/kratos-layout.git
```

## 2. pb.go生成

## clients
* 在pb文件添加注释//kratos:clients

## pb
* 在pb文件添加注释//kratos:pb
* (optional) 添加注释depends以添加protoc proto_path参数,如:
> //depends:./third_party
* (optional) 添加注释additional以添加额外的protoc参数,如:
> //additional:--validate_out=lang=go,paths=source_relative:./
## 3. biz/service/data模板

TODO

## 4. 生成wire/provider set/pb.go

点击run旁边的go logo会运行生成provider set/wire.go/kartos proto clients/pb.go

# build
idea 内点击Gradle  -> idea-plugin-go-demo [buildPlugin]后在build/distributions可找到插件文件
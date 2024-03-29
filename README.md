# Sutils

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://github.com/your-username/your-repository/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.steadon/utils.svg)](https://mvnrepository.com/artifact/io.github.steadon/utils)

一个神奇的springboot项目工具包，目前包含注解式jwt及注解式方法测速工具。

### 使用

你可以通过maven导入以下依赖进而使用该工具：

```xml
<dependency>
    <groupId>io.github.steadon</groupId>
    <artifactId>sutils</artifactId>
    <version>3.0.1</version>
</dependency>
```

通过以下方式配置签名、有效时间和密钥：

- sign：签发jwt所用的签名，默认值为root
- keyStr：加密载荷所使用的密钥（16/24/32位字符），默认不加密
- time：有效时间（单位：秒），支持除了括号运算符以外的二元表达式，默认15天

```yml
token:
  sign: wbrprivate
  key-str: secretkeyofmyaes
  time: 15 * 24 * 60 * 60
```

使用 `@Token` 注解标记需要放入载荷中的字段：

```java
public class LoginParam {
    @Token
    private String username;
    @Token
    private String password;
}
```

初始化方式如下所示（支持自动注入）：

```java
@Autowired
private TokenUtils tokenUtils;
```

相关方法如下所示：

```java
//创建LoginParam模拟登录传参
LoginParam loginParam=new LoginParam();
loginParam.setUsername("steadon");
loginParam.setPassword("123456");

//生成token
String token=tokenUtils.createToken(loginParam);
log.info("token: "+token);

//校验token
if(tokenUtils.checkToken(token))log.info("success");

//解析token
LoginParam param=tokenUtils.parseToken(token, LoginParam.class);
log.info("username: "+param.getUsername());
log.info("password: "+param.getPassword());
```

为加密载荷时同普通token无差别，启用加密后将会得到形如以下加密token：

```json
{
  "token": "tFMPYVYxYkLoq2hGVE9AIw==:HMu0WvHyBDn56yjBATKvS/9dI4ljwqfTtvW2gzGuuFqlVrRGr4PQePhNbe5+Dh1Ii9flM2cjcqh9ITb3jmKuI7q7zJ5BhCPl/sjfNWcSDELi7X9SgVXJVLHIRQuDmB4SykAw09dgKbmpDATliuX5CXKtBt8bVS+fAtL3+p5CapoyG8SDjuz3Fwt6S7kVz+pc4yR6iU2E9IPuS1gRGUcelg=="
}
```

该工具持续更新中，请保持关注！你的star就是我的动力！

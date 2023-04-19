# JWTUtils

一个快速生成、验证、解析JWT的轻量工具。

### 使用

你可以通过maven导入以下依赖进而使用该工具：

```xml

<dependency>
    <groupId>io.github.steadon</groupId>
    <artifactId>utils</artifactId>
    <version>2.0.3</version>
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
private JWTUtils jwtUtils;
```

相关方法如下所示：

```java
//创建LoginParam模拟登录传参
LoginParam loginParam=new LoginParam();
loginParam.setUsername("steadon");
loginParam.setPassword("123456");

//生成token
String token=jwtUtils.createToken(loginParam);
log.info("token: "+token);

//校验token
if(jwtUtils.checkToken(token))log.info("成功");

//解析token
LoginParam param=jwtUtils.parseToken(token, LoginParam.class);
log.info("username: "+param.getUsername());
log.info("password: "+param.getPassword());
```

该工具持续更新中，请保持关注！你的star就是我的动力！

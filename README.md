# JWTUtils
一个快速生成、验证、解析JWT的轻量工具。

### 使用
你可以通过maven导入以下依赖进而使用该工具：

```xml
<dependency>
    <groupId>io.github.steadon</groupId>
    <artifactId>utils</artifactId>
    <version>1.2.0</version> 
</dependency>
```

通过以下方式配置签名和过期时间（单位：秒）：
```yml
token：
    sign：wbrprivate
    time：3600
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
LoginParam loginParam = new LoginParam();
loginParam.setUsername("steadon");
loginParam.setPassword("123456");

//生成token
String token = jwtUtils.createToken(loginParam);
log.info("token: " + token);

//校验token
if (jwtUtils.checkToken(token)) log.info("成功");

//解析token
LoginParam param = jwtUtils.parseToken(token, LoginParam.class);
log.info("username: " + param.getUsername());
log.info("password: " + param.getPassword());
```
该工具持续优化中，请保持关注！你的star就是我的动力！

# JWTUtils
一个快速生成、验证、解析JWT的轻量工具

### 使用
您可以通过maven导入一下依赖进而使用该工具：

```xml
<dependency>
    <groupId>io.github.steadon</groupId>
    <artifactId>utils</artifactId>
    <version>1.0</version> 
</dependency>
```

通过以下方式配置签名和过期时间（单位：秒）：
```yml
token：
    sign：wbrprivate
    time：60
```

通过`@Token`注解标记需要放入载荷中的字段：
```java
import com.steadon.Token;
import lombok.Data;

@Data
public class LoginParam {
    @Token
    private String username;
    @Token
    private String password;
}
```
基本方法如下所示：
```java
//创建JWTUtils对象
JWTUtils jwtUtils = new JWTUtils();
jwtUtils.setSign("wbr");
jwtUtils.setTime(10);

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

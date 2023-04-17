package com.steadon;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "token")
public class JWTUtils {
    private String sign;

    private int time;

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public JWTUtils() {

    }

    /**
     * 默认方式创建token字符串
     *
     * @param t   载荷字段所属对象
     * @param <T> 泛型
     * @return 已创建的token字符串
     */
    public <T> String createToken(T t) {
        //jackson的序列化工具
        ObjectMapper objectMapper = new ObjectMapper();
        //auth0的token生成工具
        JWTCreator.Builder builder = JWT.create();
        //获取class对象的属性
        Field[] declaredFields = t.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Token.class)) {
                field.setAccessible(true);
                Object o;
                try {
                    o = field.get(t);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                try {
                    builder.withClaim(field.getName(), objectMapper.writeValueAsString(o));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Calendar instance = Calendar.getInstance();
        //添加过期时间并使用签名生成token
        instance.add(Calendar.SECOND, this.time);
        return builder.withExpiresAt(instance.getTime()).sign(Algorithm.HMAC256(this.sign));
    }

    /**
     * 从token中获取对象
     *
     * @param token  待解析的token
     * @param tClass 映射类型
     * @param <T>    泛型
     * @return 映射对象
     */
    public <T> T parseToken(String token, Class<T> tClass) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Claim> claims = JWT.require(Algorithm.HMAC256(this.sign)).build().verify(token).getClaims();
        T t = null;
        try {
            t = tClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Field[] declaredFields = tClass.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Token.class)) {
                field.setAccessible(true);
                if (claims.containsKey(field.getName())) {
                    try {
                        field.set(t, objectMapper.readValue(claims.get(field.getName()).asString(), field.getType()));
                    } catch (IllegalAccessException | JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return t;
    }

    /**
     * 验证token
     *
     * @param token 要验证的token
     * @return 验证结果
     */
    public boolean checkToken(String token) {
        try {
            JWT.require(Algorithm.HMAC256(this.sign)).build().verify(token);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}

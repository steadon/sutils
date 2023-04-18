package com.steadon;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Map;
import java.util.Stack;

/**
 * A lightweight tool for generating tokens quickly
 *
 * @author Steadon
 * @version 1.3.0
 */
@Component
@ConfigurationProperties(prefix = "token")
public class JWTUtils {
    private String sign;

    private String time;

    private int _time;

    public JWTUtils() {
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
        this._time = calculate(this.time);
    }

    /**
     * The default way is to create a token string
     * The target field is automatically identified based on the incoming object with @Token annotation field
     * And read the signature and expiration _time in the configuration file to generate a token
     *
     * @param t   The object to which the payload field belongs
     * @param <T> Any type
     * @return The token string that is created
     */
    public <T> String createToken(T t) {
        ObjectMapper objectMapper = new ObjectMapper();
        JWTCreator.Builder builder = JWT.create();
        Field[] declaredFields = t.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Token.class)) {
                field.setAccessible(true);
                Object o = null;
                try {
                    o = field.get(t);
                    builder.withClaim(field.getName(), objectMapper.writeValueAsString(o));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.SECOND, this._time);
        return builder.withExpiresAt(instance.getTime()).sign(Algorithm.HMAC256(this.sign));
    }

    /**
     * Parses from the incoming token and maps to an object of the specified type
     *
     * @param token  The token to be parsed
     * @param tClass Mapping type
     * @param <T>    Any type
     * @return Map object with payload information
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
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return t;
    }

    /**
     * Verify that the token is signed and has not expired
     *
     * @param token A token that needs to be verified
     * @return The result of the verification (true or false)
     */
    public boolean checkToken(String token) {
        try {
            JWT.require(Algorithm.HMAC256(this.sign)).build().verify(token);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Binary operation on time expressions in configuration files
     * bracket operators are not supported
     *
     * @param time Token expressions
     * @return Total seconds
     */
    public int calculate(String time) {
        Stack<Integer> nums = new Stack<>();

        int num = 0;
        char prevOp = '+';

        for (int i = 0; i < time.length(); i++) {
            char c = time.charAt(i);
            if (Character.isDigit(c)) {
                num = num * 10 + c - '0';
            }
            if (!Character.isDigit(c) && c != ' ' || i == time.length() - 1) {
                switch (prevOp) {
                    case '+' -> nums.push(num);
                    case '-' -> nums.push(-num);
                    case '*' -> nums.push(nums.pop() * num);
                    case '/' -> nums.push(nums.pop() / num);
                }
                prevOp = c;
                num = 0;
            }
        }
        //此处复用num节约内存
        while (!nums.isEmpty()) {
            num += nums.pop();
        }
        return num;
    }
}

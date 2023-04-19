package com.steadon;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A lightweight tool for generating tokens quickly
 *
 * @author Steadon
 * @version 2.0.0
 */
@Component
@ConfigurationProperties(prefix = "token")
public class JWTUtils {
    private String sign = "root";

    private String time = "15 * 24 * 60 * 60";

    private int _time = 15 * 24 * 60 * 60;

    //TODO 强加密
    private String keyStr = "";

    public JWTUtils() {
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getKeyStr() {
        return keyStr;
    }

    public void setKeyStr(String keyStr) {
        this.keyStr = keyStr;
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
        String token = builder.withExpiresAt(instance.getTime()).sign(Algorithm.HMAC256(this.sign));
        if (!Objects.equals(keyStr, "")) return encrypt(token);
        return token;
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
        if (!Objects.equals(keyStr, "")) token = encrypt(token);
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
        if (!Objects.equals(keyStr, "")) token = encrypt(token);
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
    private int calculate(String time) {
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

    /**
     * AES symmetric encryption of the payload portion of the JWT
     *
     * @param token JWT before encryption
     * @return Encrypted JWT
     * @apiNote Use the same function to encrypt or decrypt your JWT
     */
    private String encrypt(String token) {
        String[] parts = token.split("\\.");
        String encryptedPayload;
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keyStr.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptedBytes = cipher.doFinal(Base64.getDecoder().decode(parts[1]));
            encryptedPayload = Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return String.format("%s.%s.%s", parts[0], encryptedPayload, parts[2]);
    }
}

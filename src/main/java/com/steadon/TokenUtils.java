package com.steadon;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * A lightweight tool for generating tokens quickly
 *
 * @author Steadon
 * @version 2.1.3
 */
@Component
@ConfigurationProperties(prefix = "token")
public class TokenUtils {
    private String sign = "root";
    private String time = "15 * 24 * 60 * 60";
    private int _time = 15 * 24 * 60 * 60;
    private String keyStr = "";
    private final Cipher cipherEncrypt;
    private final Cipher cipherDecrypt;
    private final SecretKeySpec keySpec;

    public TokenUtils() {
        try {
            cipherEncrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
            keySpec = new SecretKeySpec(keyStr.getBytes(StandardCharsets.UTF_8), "AES");
            // Initialize vectors or other parameters as required for CBC or GCM modes
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException("Error initializing ciphers", e);
        }
    }

    public String getSign() {
        return sign;
    }

    public String getTime() {
        return time;
    }

    public String getKeyStr() {
        return keyStr;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public void setKeyStr(String keyStr) {
        this.keyStr = keyStr;
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
        if (t == null) {
            throw new IllegalArgumentException("Input parameter cannot be null");
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JWTCreator.Builder builder = JWT.create();
            Field[] declaredFields = t.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.isAnnotationPresent(Token.class)) {
                    field.setAccessible(true);
                    Object value = field.get(t);
                    builder.withClaim(field.getName(), objectMapper.writeValueAsString(value));
                }
            }
            Date expiryDate = getExpiryDate();
            String token = builder.withExpiresAt(expiryDate).sign(Algorithm.HMAC256(this.sign));
            return (keyStr != null && !keyStr.isEmpty()) ? encrypt(token) : token;
        } catch (IllegalAccessException | JsonProcessingException e) {
            throw new IllegalStateException("Error creating token", e);
        }
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
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        if (tClass == null) {
            throw new IllegalArgumentException("Class type cannot be null");
        }
        try {
            if (!Objects.equals(keyStr, "")) {
                token = decrypt(token);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Claim> claims = JWT.require(Algorithm.HMAC256(this.sign)).build().verify(token).getClaims();
            T instance = tClass.getDeclaredConstructor().newInstance();
            for (Field field : tClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Token.class) && claims.containsKey(field.getName())) {
                    field.setAccessible(true);
                    Claim claim = claims.get(field.getName());
                    field.set(instance, objectMapper.readValue(claim.asString(), field.getType()));
                }
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | JWTVerificationException |
                 JsonProcessingException | InvocationTargetException e) {
            throw new IllegalStateException("Error processing token", e);
        }
    }

    /**
     * Verify that the token is signed and has not expired
     *
     * @param token A token that needs to be verified
     * @return The result of the verification (true or false)
     */
    public boolean checkToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String processedToken = (!Objects.equals(keyStr, "")) ? decrypt(token) : token;
        try {
            JWT.require(Algorithm.HMAC256(this.sign)).build().verify(processedToken);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    /**
     * Binary operation on expression expressions in configuration files
     * bracket operators are not supported
     *
     * @param expression Token expressions
     * @return Total seconds
     */
    private int calculate(String expression) {
        if (expression == null || expression.isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be null or empty");
        }
        Stack<Integer> nums = new Stack<>();
        int curr = 0;
        char prevOp = '+';

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isDigit(c)) {
                curr = curr * 10 + c - '0';
            }
            if (!Character.isDigit(c) && c != ' ' || i == expression.length() - 1) {
                switch (prevOp) {
                    case '+' -> nums.push(curr);
                    case '-' -> nums.push(-curr);
                    case '*' -> nums.push(nums.pop() * curr);
                    case '/' -> nums.push(nums.pop() / curr);
                }
                prevOp = c;
                curr = 0;
            }
        }
        int result = 0;
        while (!nums.isEmpty()) {
            result += nums.pop();
        }
        return result;
    }

    /**
     * AES symmetric encryption of the payload portion of the JWT
     *
     * @param token JWT before encryption
     * @return Encrypted JWT
     */
    private String encrypt(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid token format");
        }
        try {
            cipherEncrypt.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptedBytes = cipherEncrypt.doFinal(Base64.getDecoder().decode(parts[1]));
            String encryptedPayload = Base64.getEncoder().encodeToString(encryptedBytes);
            return String.format("%s.%s.%s", parts[0], encryptedPayload, parts[2]);
        } catch (IllegalBlockSizeException | InvalidKeyException | BadPaddingException e) {
            throw new IllegalStateException("Error during encryption", e);
        }
    }

    /**
     * AES symmetric decryption of the payload portion of the JWT
     *
     * @param token JWT before decryption
     * @return Decrypted JWT
     */
    private String decrypt(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid token format");
        }
        try {
            cipherDecrypt.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decryptedBytes = cipherDecrypt.doFinal(Base64.getDecoder().decode(parts[1]));
            String decryptedPayload = Base64.getEncoder().encodeToString(decryptedBytes);
            return String.format("%s.%s.%s", parts[0], decryptedPayload, parts[2]);
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new IllegalStateException("Error during decryption", e);
        }
    }

    /**
     * Get expiry date
     *
     * @return Expiry date
     */
    private Date getExpiryDate() {
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.SECOND, this._time);
        return instance.getTime();
    }
}
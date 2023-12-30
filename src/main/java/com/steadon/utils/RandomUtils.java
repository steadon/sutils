package com.steadon.utils;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * Utility class for generating random strings. This class provides several constructors to generate random strings
 * using different character sets and random number generators.
 */
public class RandomUtils {

    /**
     * Upper-case alphabet characters.
     */
    public static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Lower-case alphabet characters.
     */
    public static final String lower = upper.toLowerCase(Locale.ROOT);

    /**
     * Numeric characters.
     */
    public static final String digits = "0123456789";

    /**
     * Combination of upper-case, lower-case alphabet and numeric characters.
     */
    public static final String alphanum = upper + lower + digits;

    private final Random random;
    private final char[] symbols;
    private final char[] buf;

    /**
     * Constructs a RandomUtils instance with the specified length, random number generator, and character set.
     *
     * @param length  Length of the random strings to be generated.
     * @param random  Random number generator to be used.
     * @param symbols Character set to use for generating random strings.
     * @throws IllegalArgumentException if length is less than 1 or symbols length is less than 2.
     */
    public RandomUtils(int length, Random random, String symbols) {
        if (length < 1) throw new IllegalArgumentException("Length must be at least 1");
        if (symbols.length() < 2)
            throw new IllegalArgumentException("Symbols string must contain at least two characters");
        this.random = Objects.requireNonNull(random);
        this.symbols = symbols.toCharArray();
        this.buf = new char[length];
    }

    /**
     * Constructs a RandomUtils instance with the specified length and random number generator, using alphanumeric characters.
     *
     * @param length Length of the random strings to be generated.
     * @param random Random number generator to be used.
     */
    public RandomUtils(int length, Random random) {
        this(length, random, alphanum);
    }

    /**
     * Constructs a RandomUtils instance with the specified length, using a secure random number generator and alphanumeric characters.
     *
     * @param length Length of the random strings to be generated.
     */
    public RandomUtils(int length) {
        this(length, new SecureRandom());
    }

    /**
     * Constructs a RandomUtils instance with a default length of 8, using a secure random number generator and alphanumeric characters.
     */
    public RandomUtils() {
        this(8);
    }

    /**
     * Generates a random string.
     *
     * @return A randomly generated string.
     */
    public String nextString() {
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }
}
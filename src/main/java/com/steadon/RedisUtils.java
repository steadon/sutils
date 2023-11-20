package com.steadon;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class RedisUtils {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    /**
     * Sets a value in the cache with an optional timeout.
     *
     * @param key     the cache key under which the value is stored.
     * @param value   the value to be stored.
     * @param timeout the time duration for the value to be stored in cache.
     * @param unit    the time unit of the timeout.
     * @param <T>     the type of the value.
     * @throws IllegalArgumentException if the key is null or empty.
     */
    public <T> void setValue(String key, T value, long timeout, TimeUnit unit) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        if (timeout > 0) {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    /**
     * Retrieves a value from the cache.
     *
     * @param key   the cache key associated with the value.
     * @param clazz the expected type of the value.
     * @param <T>   the type of the value.
     * @return an Optional containing the value if found and if it matches the expected type; empty otherwise.
     * @throws IllegalArgumentException if the key is null or empty.
     */
    public <T> Optional<T> getValue(String key, Class<T> clazz) {
        Object o = redisTemplate.opsForValue().get(key);
        if (clazz.isInstance(o)) {
            return Optional.of(clazz.cast(o));
        }
        return Optional.empty();
    }

    /**
     * Deletes a value from the cache.
     *
     * @param key the cache key associated with the value to be deleted.
     * @throws IllegalArgumentException if the key is null or empty.
     */
    public void deleteValue(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        redisTemplate.delete(key);
    }

    /**
     * Retrieves a string value from the cache.
     *
     * @param key the cache key associated with the string value.
     * @return an Optional containing the string value if found; empty otherwise.
     * @throws IllegalArgumentException if the key is null or empty.
     */
    public Optional<String> getString(String key) {
        if (key == null || key.isEmpty()) {
            return Optional.empty();
        }
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof String) {
            return Optional.of((String) value);
        }
        return Optional.empty();
    }

    /**
     * Retrieves an integer value from the cache.
     *
     * @param key the cache key associated with the integer value.
     * @return an Optional containing the integer value if found; empty otherwise.
     * @throws IllegalArgumentException if the key is null or empty.
     */
    public Optional<Integer> getInteger(String key) {
        if (key == null || key.isEmpty()) {
            return Optional.empty();
        }
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Integer) {
            return Optional.of((Integer) value);
        }
        return Optional.empty();
    }

    /**
     * Stores a list of objects in the cache.
     *
     * @param key     the cache key under which the list is stored.
     * @param list    the list to be stored in the cache.
     * @param timeout the time duration for the list to be stored in cache.
     * @param unit    the time unit of the timeout.
     * @param <T>     the type of elements in the list.
     * @throws IllegalArgumentException if the key is null, empty, or the list is null.
     */
    public <T> void setList(String key, List<T> list, long timeout, TimeUnit unit) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        if (list == null) {
            throw new IllegalArgumentException("List cannot be null");
        }
        if (list.isEmpty()) {
            redisTemplate.delete(key);
        } else {
            redisTemplate.opsForList().rightPushAll(key, list.toArray());
            redisTemplate.expire(key, timeout, unit);
        }
    }

    /**
     * Retrieves a list of objects of a specific type from the cache.
     *
     * @param key   the cache key associated with the list.
     * @param clazz the class of the elements in the list.
     * @param <T>   the type of elements in the list.
     * @return an Optional containing the list if found; empty otherwise.
     * @throws IllegalArgumentException if the key is null or empty.
     */
    public <T> Optional<List<T>> getList(String key, Class<T> clazz) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        List<Object> rawList = redisTemplate.opsForList().range(key, 0, -1);
        if (rawList == null || rawList.isEmpty()) {
            return Optional.empty();
        }
        List<T> typedList = rawList.stream().filter(clazz::isInstance).map(clazz::cast).toList();
        return Optional.of(typedList);
    }

    /**
     * Checks if a specified key exists in the cache.
     *
     * @param key the cache key to check.
     * @return true if the key exists in the cache; false otherwise.
     * @throws IllegalArgumentException if the key is null or empty.
     */
    public boolean hasKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        Boolean result = redisTemplate.hasKey(key);
        return result != null && result;
    }

    /**
     * Flushes all entries in the cache.
     */
    public void flushAll() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Retrieves a non-collection type of data from the cache or through a query function.
     * This method first attempts to retrieve the data from the cache using the specified cache key.
     * If the data is not found in the cache, the provided query function is executed,
     * and its result is cached and returned.
     *
     * @param cacheKey      the key used for cache access.
     * @param clazz         the class of the object to be retrieved.
     * @param queryFunction a {@code Supplier} function that fetches the data if not present in cache.
     * @param <T>           the type of the data to be retrieved.
     * @return the retrieved data of type {@code T}.
     * @throws IllegalArgumentException if the data is not found in the cache or via the query function.
     */
    public <T> T getCacheOrQuery(String cacheKey, Class<T> clazz, Supplier<T> queryFunction) {
        return getFromCacheOrQuery(
                cacheKey,
                () -> getValue(cacheKey, clazz),
                data -> setValue(cacheKey, data, 1, TimeUnit.HOURS),
                queryFunction
        );
    }

    /**
     * Retrieves a list of objects of type {@code T} from the cache or through a query function.
     * Similar to {@code getCacheOrQuery}, this method attempts to retrieve the data from the cache first.
     * If the cache miss occurs, the provided query function is used to fetch the data,
     * which is then cached and returned.
     *
     * @param cacheKey      the key used for cache access.
     * @param clazz         the class of the objects in the list to be retrieved.
     * @param queryFunction a {@code Supplier} function that fetches the list of data if not present in cache.
     * @param <T>           the type of the data in the list to be retrieved.
     * @return the retrieved list of data of type {@code T}.
     * @throws IllegalArgumentException if the data is not found in the cache or via the query function.
     */
    public <T> List<T> getCachedListOrQuery(String cacheKey, Class<T> clazz, Supplier<List<T>> queryFunction) {
        return getFromCacheOrQuery(
                cacheKey,
                () -> getList(cacheKey, clazz),
                dataList -> setList(cacheKey, dataList, 1, TimeUnit.HOURS),
                queryFunction
        );
    }

    /**
     * Retrieves data from the cache or executes a query function if the cache is missed.
     * This method provides a common routine to fetch data, either by getting it from the cache
     * or by obtaining it through a supplied query function, then caching the result.
     *
     * @param cacheKey      the key used to access the cache.
     * @param cacheGetter   a {@code Supplier} that attempts to retrieve the cached data.
     * @param cacheSetter   a {@code Consumer} that caches data if the query function is executed.
     * @param queryFunction a {@code Supplier} function that fetches data if cache miss occurs.
     * @param <T>           the type of the data to be retrieved.
     * @return the retrieved or queried data.
     * @throws IllegalArgumentException if the data is not found in the cache or via the query function.
     */
    private <T> T getFromCacheOrQuery(String cacheKey, Supplier<Optional<T>> cacheGetter, Consumer<T> cacheSetter, Supplier<T> queryFunction) {
        return cacheGetter.get().orElseGet(() -> {
            synchronized (locks.computeIfAbsent(cacheKey, k -> new Object())) {
                return cacheGetter.get().orElseGet(() -> {
                    T result = queryFunction.get();
                    if (result != null) {
                        cacheSetter.accept(result);
                        return result;
                    }
                    throw new IllegalArgumentException("Data not found for key: " + cacheKey);
                });
            }
        });
    }
}
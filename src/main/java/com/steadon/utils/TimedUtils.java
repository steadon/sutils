package com.steadon.utils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

@Aspect
@Component
public class TimedUtils {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(TimedUtils.class);

    /**
     * Intercepts all methods annotated with {@link com.steadon.annotation.Timed} and logs their execution time.
     * <p>
     * This method uses Spring AOP's {@link org.aspectj.lang.annotation.Around} annotation to define an around advice.
     * It executes before and after the execution of the annotated method to measure and log the execution time.
     * <p>
     *
     * @param pjp Provides access to the intercepted method, allowing it to execute the target method and obtain its return value.
     * @return The return value of the intercepted method.
     * @throws Throwable Re-throws any exceptions thrown by the intercepted method.
     * @see ProceedingJoinPoint#proceed()
     */
    @Around("@annotation(com.steadon.annotation.Timed)")
    public Object profile(ProceedingJoinPoint pjp) throws Throwable {
        Instant start = Instant.now();
        Object output = pjp.proceed();
        Instant end = Instant.now();
        logger.info("Method execution time: " + Duration.between(start, end).toMillis() + " ms");
        return output;
    }
}

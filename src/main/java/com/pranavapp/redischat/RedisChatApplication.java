package com.pranavapp.redischat;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootApplication
public class RedisChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisChatApplication.class, args);
    }

    @Bean
    CommandLineRunner checkRedisConnection(StringRedisTemplate redisTemplate) {
        return args -> {
            String response = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            System.out.println("Redis check: " + response);
        };
    }

}

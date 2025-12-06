package com.whatsapp.android.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whatsapp.android.constant.RedisConstantOne;
import com.whatsapp.android.constant.RedisConstantTwo;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author sunnoc
 */
@Configuration
public class RedisConfig {

    @Autowired
    RedisConstantTwo redisConstantTwo;
    @Autowired
    RedisConstantOne redisConstantOne;


    @Bean
    @Primary
    @ConditionalOnClass(RedisOperations.class)
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory1());

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        /**
         * 低版本采用这种方式
         * mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
         * 高版本的spring boot采用activateDefaultTyping方式
         */
        //mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(mapper);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // key采用 String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的 key也采用 String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用 jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的 value序列化方式采用 jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }


    @Bean("redisTemplate2")
    public RedisTemplate<String, Object> redisTemplateTwo() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory2());

        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        /**
         * 低版本采用这种方式
         * mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
         * 高版本的spring boot采用activateDefaultTyping方式
         */
        //mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(mapper);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        // key采用 String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的 key也采用 String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用 jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的 value序列化方式采用 jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean("redisConnectionFactory1")
    @Primary
    LettuceConnectionFactory redisConnectionFactory1() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisConstantOne.getHost());
        configuration.setPort(redisConstantOne.getPort());
        configuration.setDatabase(redisConstantOne.getDatabase());
        configuration.setPassword(RedisPassword.of(redisConstantOne.getPassword()));

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMinIdle(redisConstantOne.getMinIdle());
        config.setMaxIdle(redisConstantOne.getMaxIdle());
        config.setMaxTotal(redisConstantOne.getMaxActive());
        config.setMaxWaitMillis(redisConstantOne.getMaxWait());

        LettuceClientConfiguration clientConfiguration = LettucePoolingClientConfiguration.builder().poolConfig(config).build();
        return new LettuceConnectionFactory(configuration, clientConfiguration);
    }


    @Bean("redisConnectionFactory2")
    LettuceConnectionFactory redisConnectionFactory2() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisConstantTwo.getHost());
        configuration.setPort(redisConstantTwo.getPort());
        configuration.setDatabase(redisConstantTwo.getDatabase());
        configuration.setPassword(RedisPassword.of(redisConstantTwo.getPassword()));

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMinIdle(redisConstantTwo.getMinIdle());
        config.setMaxIdle(redisConstantTwo.getMaxIdle());
        config.setMaxTotal(redisConstantTwo.getMaxActive());
        config.setMaxWaitMillis(redisConstantTwo.getMaxWait());

        LettuceClientConfiguration clientConfiguration = LettucePoolingClientConfiguration.builder().poolConfig(config).build();
        return new LettuceConnectionFactory(configuration, clientConfiguration);
    }

}

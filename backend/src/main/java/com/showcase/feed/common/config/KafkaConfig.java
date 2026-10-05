package com.showcase.feed.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * KafkaTemplate<String, String> itself is autoconfigured by Spring Boot from spring.kafka.* in
 * application.yml — nothing to declare here. This NewTopic bean just documents the topic shape;
 * Aiven's free Kafka also auto-creates topics on first publish, so it's a nice-to-have, not
 * required to compile or run.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic postCreatedTopic() {
        return TopicBuilder.name("post.created").partitions(3).replicas(1).build();
    }
}

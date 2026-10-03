package com.rahul.cinemaservice.Kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String SEATS_RESERVED_TOPIC = "seats-reserved";

    @Bean
    public NewTopic seatsReservedTopic() {
        return TopicBuilder.name(SEATS_RESERVED_TOPIC).partitions(3).replicas(1).build();
    }
}
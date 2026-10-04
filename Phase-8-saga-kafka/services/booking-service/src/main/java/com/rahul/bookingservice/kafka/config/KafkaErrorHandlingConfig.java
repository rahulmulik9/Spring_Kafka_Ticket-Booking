package com.rahul.bookingservice.kafka.config;

import com.rahul.bookingservice.exception.BookingNotFoundException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaErrorHandlingConfig {

    // Spring Boot finds this bean and uses it for every @KafkaListener.
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // After the last retry, publish the failed message to "<original topic>.DLT".
        // Partition -1 means "let Kafka choose", so the DLT does not need the same partition count.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));

        // 3 retries: wait 1s, then 2s, then 4s. After that the message goes to the DLT.
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // A booking that does not exist will not appear on the 2nd try. Go straight to the DLT.
        handler.addNotRetryableExceptions(BookingNotFoundException.class);
        return handler;
    }
}
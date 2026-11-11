package com.chaula.dental.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.config.TopicBuilder;
import com.chaula.dental.service.WhatsAppPermanentDeliveryException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class WhatsAppKafkaConfig {
    @Bean
    public NewTopic whatsappInboundTopic(
            @Value("${whatsapp.kafka.inbound-topic:whatsapp-inbound}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic whatsappOutboundTopic(
            @Value("${whatsapp.kafka.outbound-topic:whatsapp-outbound}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic whatsappDocumentTopic(
            @Value("${whatsapp.kafka.document-topic:whatsapp-outbound-documents}") String topic
    ) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic whatsappInboundDeadLetterTopic(
            @Value("${whatsapp.kafka.inbound-topic:whatsapp-inbound}") String topic
    ) {
        return TopicBuilder.name(topic + ".DLT").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic whatsappOutboundDeadLetterTopic(
            @Value("${whatsapp.kafka.outbound-topic:whatsapp-outbound}") String topic
    ) {
        return TopicBuilder.name(topic + ".DLT").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic whatsappDocumentDeadLetterTopic(
            @Value("${whatsapp.kafka.document-topic:whatsapp-outbound-documents}") String topic
    ) {
        return TopicBuilder.name(topic + ".DLT").partitions(1).replicas(1).build();
    }

    @Bean
    public CommonErrorHandler whatsappKafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate),
                new FixedBackOff(1000L, 3L)
        );
        errorHandler.addNotRetryableExceptions(WhatsAppPermanentDeliveryException.class);
        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            CommonErrorHandler whatsappKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(whatsappKafkaErrorHandler);
        return factory;
    }
}

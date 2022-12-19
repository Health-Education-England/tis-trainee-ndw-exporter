package uk.nhs.hee.tis.trainee.ndw.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;


/**
 * SQS configuration beans.
 */
@Configuration
public class AmazonSqsConfig {

  /**
   * Create a default {@link AmazonSQSAsync} bean.
   *
   * @return The created bean.
   */
  @Bean
  @Primary
  public AmazonSQSAsync amazonSqsAsync() {
    return AmazonSQSAsyncClientBuilder.defaultClient();
  }

  /**
   * Create a default {@link QueueMessagingTemplate} bean.
   *
   * @return The created bean.
   */
  @Bean
  public QueueMessagingTemplate queueMessagingTemplate() {
    return new QueueMessagingTemplate(amazonSqsAsync());
  }

  /**
   * Create a message converter bean with an object mapper.
   *
   * @param objectMapper The object mapper to set.
   * @return The created message converter.
   */
  @Bean
  public MessageConverter messageConverter(ObjectMapper objectMapper) {
    var converter = new MappingJackson2MessageConverter();
    converter.setObjectMapper(objectMapper);
    return converter;
  }
}

/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.ndw.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishRequest.Builder;
import software.amazon.awssdk.services.sns.model.SnsException;
import uk.nhs.hee.tis.trainee.ndw.config.EventNotificationProperties;
import uk.nhs.hee.tis.trainee.ndw.config.EventNotificationProperties.SnsRoute;
import uk.nhs.hee.tis.trainee.ndw.dto.FormBroadcastEventDto;
import uk.nhs.hee.tis.trainee.ndw.dto.FormContentDto;

/**
 * A service for broadcasting form events to SNS.
 */
@Slf4j
@Service
public class FormBroadcastService {

  private final SnsClient snsClient;

  private final EventNotificationProperties eventNotificationProperties;

  FormBroadcastService(SnsClient snsClient,
      EventNotificationProperties eventNotificationProperties) {
    this.snsClient = snsClient;
    this.eventNotificationProperties = eventNotificationProperties;
  }

  /**
   * Broadcast a form event using the form broadcast service.
   *
   * @param formName       The name of the form.
   * @param formType       The type of the form (e.g. formr-a, formr-b).
   * @param traineeId      The trainee TIS ID.
   * @param lifecycleState The lifecycle state of the form (e.g. SUBMITTED, DELETED).
   * @param formContentDto The form content.
   */
  public void broadcastFormEvent(String formName, String formType, String traineeId,
                                  String lifecycleState, FormContentDto formContentDto) {
    if (formContentDto != null) {
      log.info("Broadcasting event for form {}", formName);
      FormBroadcastEventDto formBroadcastEventDto = new FormBroadcastEventDto(
          formName, lifecycleState, traineeId, formType, Instant.now(), formContentDto);
      publishFormBroadcastEvent(formBroadcastEventDto);
    } else {
      log.warn("No content in form {} of type {}, skipping event broadcast.", formName, formType);
    }
  }

  /**
   * Publish a form broadcast event to SNS.
   *
   * @param formBroadcastEventDto The form broadcast event DTO to publish.
   */
  public void publishFormBroadcastEvent(FormBroadcastEventDto formBroadcastEventDto) {
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    PublishRequest request = null;
    SnsRoute snsTopic = eventNotificationProperties.formUpdatedEvent();

    if (snsTopic != null && formBroadcastEventDto != null) {
      JsonNode eventJson = objectMapper.valueToTree(formBroadcastEventDto);
      request = buildSnsRequest(eventJson, snsTopic, formBroadcastEventDto.formType(),
          formBroadcastEventDto.formName(), formBroadcastEventDto.traineeId());
    }

    if (request != null) {
      try {
        snsClient.publish(request);
        log.info("Broadcast event sent to SNS for form {}.", formBroadcastEventDto.formName());
      } catch (SnsException e) {
        String message = String.format("Failed to broadcast event to SNS topic '%s' for form '%s'",
            snsTopic, formBroadcastEventDto.formName());
        log.error(message, e);
      }
    }
  }

  /**
   * Build an SNS publish request.
   *
   * @param eventJson The SNS message contents.
   * @param snsTopic  The SNS topic to send the message to.
   * @param formType  The form type.
   * @param formName  The form name.
   * @param traineeId The trainee ID.
   * @return the built request.
   */
  private PublishRequest buildSnsRequest(JsonNode eventJson, SnsRoute snsTopic, String formType,
      String formName, String traineeId) {
    Builder request = PublishRequest.builder()
        .message(eventJson.toString())
        .topicArn(snsTopic.arn());

    if (snsTopic.messageAttribute() != null) {
      MessageAttributeValue messageAttributeValue = MessageAttributeValue.builder()
          .dataType("String")
          .stringValue(snsTopic.messageAttribute())
          .build();
      request.messageAttributes(Map.of("event_type", messageAttributeValue));
    }

    if (snsTopic.arn().endsWith(".fifo")) {
      // Create a message group to ensure FIFO per unique object.
      String messageGroup = String.format("%s_%s_%s", traineeId, formType, formName);
      request.messageGroupId(messageGroup);
    }

    return request.build();
  }
}

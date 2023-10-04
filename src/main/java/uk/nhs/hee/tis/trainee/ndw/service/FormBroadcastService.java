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

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.ndw.config.EventNotificationProperties;
import uk.nhs.hee.tis.trainee.ndw.config.EventNotificationProperties.SnsRoute;
import uk.nhs.hee.tis.trainee.ndw.dto.FormBroadcastEventDto;

@Slf4j
@Service
public class FormBroadcastService {

  private final AmazonSNS snsClient;

  private final EventNotificationProperties eventNotificationProperties;

  FormBroadcastService(AmazonSNS snsClient,
      EventNotificationProperties eventNotificationProperties) {
    this.snsClient = snsClient;
    this.eventNotificationProperties = eventNotificationProperties;
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
    SnsRoute snsTopic = eventNotificationProperties.updateFormEvent();

    if (snsTopic != null && formBroadcastEventDto != null) {
      JsonNode eventJson = objectMapper.valueToTree(formBroadcastEventDto);
      request = buildSnsRequest(eventJson, snsTopic, formBroadcastEventDto.getFormType(),
          formBroadcastEventDto.getFormName(), formBroadcastEventDto.getTraineeId());
    }

    if (request != null) {
      try {
        snsClient.publish(request);
        log.info("Broadcast event sent to SNS for form {}.", formBroadcastEventDto.getFormName());
      } catch (AmazonSNSException e) {
        String message = String.format("Failed to broadcast event to SNS topic '%s' for form '%s'",
            snsTopic, formBroadcastEventDto.getFormName());
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
    PublishRequest request = new PublishRequest()
        .withMessage(eventJson.toString())
        .withTopicArn(snsTopic.arn());
    if (snsTopic.messageAttribute() != null) {
      MessageAttributeValue messageAttributeValue = new MessageAttributeValue()
          .withDataType("String")
          .withStringValue(snsTopic.messageAttribute());
      request.addMessageAttributesEntry("event_type", messageAttributeValue);
    }

    if (snsTopic.arn().endsWith(".fifo")) {
      // Create a message group to ensure FIFO per unique object.
      String messageGroup = String.format("%s_%s_%s", traineeId, formType, formName);
      request.setMessageGroupId(messageGroup);
    }
    return request;
  }
}

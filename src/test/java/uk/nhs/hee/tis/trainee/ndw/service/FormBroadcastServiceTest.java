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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.tis.trainee.ndw.config.EventNotificationProperties;
import uk.nhs.hee.tis.trainee.ndw.config.EventNotificationProperties.SnsRoute;
import uk.nhs.hee.tis.trainee.ndw.dto.FormBroadcastEventDto;
import uk.nhs.hee.tis.trainee.ndw.dto.FormContentDto;

/**
 * Test class for the Form Broadcast Service.
 */
class FormBroadcastServiceTest {

  private static final String FORM_NAME_VALUE = "123.json";
  private static final String FORM_TYPE_VALUE = "form-type-value";
  private static final String FORM_TRAINEE_VALUE = "trainee-value";
  private static final String FORM_LIFECYCLE_STATE_VALUE = "lifecycle-state-value";
  private static final Instant FORM_EVENT_DATE_VALUE = Instant.now();
  private static final FormContentDto FORM_CONTENT_VALUE = new FormContentDto();
  private static final String FORM_CONTENT_FIELD = "field1";
  private static final String FORM_CONTENT_FIELD_VALUE = "value1";

  private static final String MESSAGE_ATTRIBUTE = "message-attribute";
  private static final String MESSAGE_ARN = "the-arn";

  private FormBroadcastService service;

  private ObjectMapper objectMapper;
  private AmazonSNS amazonSns;
  private EventNotificationProperties eventNotificationProperties;

  @BeforeEach
  void setUp() {
    amazonSns = mock(AmazonSNS.class);
    SnsRoute snsRoute = new SnsRoute(MESSAGE_ARN, MESSAGE_ATTRIBUTE);
    eventNotificationProperties = new EventNotificationProperties(snsRoute);
    objectMapper = new ObjectMapper();
    service = new FormBroadcastService(amazonSns, eventNotificationProperties);

    FORM_CONTENT_VALUE.fields.put(FORM_CONTENT_FIELD, FORM_CONTENT_FIELD_VALUE);
  }

  @Test
  void shouldNotPublishFormBroadcastEventIfSnsIsNull() {
    FormBroadcastEventDto formBroadcastEventDto
        = new FormBroadcastEventDto(FORM_NAME_VALUE, FORM_LIFECYCLE_STATE_VALUE, FORM_TRAINEE_VALUE,
        FORM_TYPE_VALUE, FORM_EVENT_DATE_VALUE, FORM_CONTENT_VALUE);

    eventNotificationProperties = new EventNotificationProperties(null);
    service = new FormBroadcastService(amazonSns, eventNotificationProperties);

    service.publishFormBroadcastEvent(formBroadcastEventDto);

    verifyNoInteractions(amazonSns);
  }

  @Test
  void shouldNotPublishFormBroadcastEventIfEventDtoIsNull() {
    service.publishFormBroadcastEvent(null);

    verifyNoInteractions(amazonSns);
  }

  @Test
  void shouldPublishFormBroadcastEvent() throws JsonProcessingException {
    FormBroadcastEventDto formBroadcastEventDto
        = new FormBroadcastEventDto(FORM_NAME_VALUE, FORM_LIFECYCLE_STATE_VALUE, FORM_TRAINEE_VALUE,
        FORM_TYPE_VALUE, FORM_EVENT_DATE_VALUE, FORM_CONTENT_VALUE);

    service.publishFormBroadcastEvent(formBroadcastEventDto);

    ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    verify(amazonSns).publish(requestCaptor.capture());

    PublishRequest request = requestCaptor.getValue();
    assertThat("Unexpected topic ARN.", request.getTopicArn(), is(MESSAGE_ARN));

    Map<String, Object> message = objectMapper.readValue(request.getMessage(),
        new TypeReference<>() {
        });
    assertThat("Unexpected message form name.", message.get("formName"),
        is(FORM_NAME_VALUE));
    assertThat("Unexpected message lifecycle state.", message.get("lifecycleState"),
        is(FORM_LIFECYCLE_STATE_VALUE));
    assertThat("Unexpected message trainee Id.", message.get("traineeId"),
        is(FORM_TRAINEE_VALUE));
    assertThat("Unexpected message form type.", message.get("formType"),
        is(FORM_TYPE_VALUE));
    assertThat("Unexpected message event date.", message.get("eventDate"),
        is(FORM_EVENT_DATE_VALUE.toString()));

    LinkedHashMap<?,?> formContent
        = objectMapper.convertValue(message.get("formContentDto"), LinkedHashMap.class);
    assertThat("Unexpected message form content.",
        formContent.get(FORM_CONTENT_FIELD), is(FORM_CONTENT_FIELD_VALUE));

    Map<String, MessageAttributeValue> messageAttributes = request.getMessageAttributes();
    assertThat("Unexpected message attribute value.",
        messageAttributes.get("event_type").getStringValue(), is(MESSAGE_ATTRIBUTE));
    assertThat("Unexpected message attribute data type.",
        messageAttributes.get("event_type").getDataType(), is("String"));

    verifyNoMoreInteractions(amazonSns);
  }

  @Test
  void shouldNotThrowSnsExceptionsWhenBroadcastingEvent() {
    FormBroadcastEventDto formBroadcastEventDto
        = new FormBroadcastEventDto(FORM_NAME_VALUE, FORM_LIFECYCLE_STATE_VALUE, FORM_TRAINEE_VALUE,
        FORM_TYPE_VALUE, FORM_EVENT_DATE_VALUE, FORM_CONTENT_VALUE);

    when(amazonSns.publish(any())).thenThrow(new AmazonSNSException("publish error"));

    assertDoesNotThrow(() -> service.publishFormBroadcastEvent(formBroadcastEventDto));
  }

  @Test
  void shouldSetMessageGroupIdOnIssuedEventWhenFifoQueue() {
    FormBroadcastEventDto formBroadcastEventDto
        = new FormBroadcastEventDto(FORM_NAME_VALUE, FORM_LIFECYCLE_STATE_VALUE, FORM_TRAINEE_VALUE,
        FORM_TYPE_VALUE, FORM_EVENT_DATE_VALUE, FORM_CONTENT_VALUE);
    SnsRoute fifoSns = new SnsRoute(MESSAGE_ARN + ".fifo", MESSAGE_ATTRIBUTE);
    eventNotificationProperties = new EventNotificationProperties(fifoSns);
    service = new FormBroadcastService(amazonSns, eventNotificationProperties);

    service.publishFormBroadcastEvent(formBroadcastEventDto);

    ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    verify(amazonSns).publish(requestCaptor.capture());

    PublishRequest request = requestCaptor.getValue();
    assertThat("Unexpected message group id.", request.getMessageGroupId(),
        is(FORM_TRAINEE_VALUE + "_" + FORM_TYPE_VALUE + "_" + FORM_NAME_VALUE));

    verifyNoMoreInteractions(amazonSns);
  }

  @Test
  void shouldNotSetMessageAttributeIfNotRequired() {
    FormBroadcastEventDto formBroadcastEventDto
        = new FormBroadcastEventDto(FORM_NAME_VALUE, FORM_LIFECYCLE_STATE_VALUE, FORM_TRAINEE_VALUE,
        FORM_TYPE_VALUE, FORM_EVENT_DATE_VALUE, FORM_CONTENT_VALUE);

    eventNotificationProperties
        = new EventNotificationProperties(new SnsRoute(MESSAGE_ARN, null));
    service = new FormBroadcastService(amazonSns, eventNotificationProperties);

    service.publishFormBroadcastEvent(formBroadcastEventDto);

    ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
    verify(amazonSns).publish(requestCaptor.capture());

    PublishRequest request = requestCaptor.getValue();

    Map<String, MessageAttributeValue> messageAttributes = request.getMessageAttributes();
    assertNull(messageAttributes.get("event_type"), "Unexpected message attribute value.");

    verifyNoMoreInteractions(amazonSns);
  }
}

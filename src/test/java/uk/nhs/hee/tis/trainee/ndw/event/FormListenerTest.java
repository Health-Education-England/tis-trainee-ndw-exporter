/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.trainee.ndw.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import uk.nhs.hee.tis.trainee.ndw.dto.FormContentDto;
import uk.nhs.hee.tis.trainee.ndw.dto.JsonFormEventDto;
import uk.nhs.hee.tis.trainee.ndw.dto.S3FormEventDto;
import uk.nhs.hee.tis.trainee.ndw.service.FormBroadcastService;
import uk.nhs.hee.tis.trainee.ndw.service.FormService;

class FormListenerTest {

  private FormListener listener;
  private FormService<S3FormEventDto> s3Service;
  private FormService<JsonFormEventDto> jsonService;
  private FormBroadcastService formBroadcastService;

  @BeforeEach
  void setUp() {
    s3Service = mock(FormService.class);
    jsonService = mock(FormService.class);
    formBroadcastService = mock(FormBroadcastService.class);
    listener = new FormListener(s3Service, jsonService, formBroadcastService);
  }

  @Test
  void shouldProcessS3Event() throws IOException {
    S3FormEventDto event = new S3FormEventDto();

    listener.getS3FormEvent(event);

    verify(s3Service).processFormEvent(event);
    verifyNoInteractions(formBroadcastService);
  }

  @Test
  void shouldProcessJsonEventWhenIdNotNull() throws IOException {
    JsonFormEventDto event = new JsonFormEventDto();
    event.set("id", "123");
    event.set("traineeTisId", "47165");
    event.set("field1", "value1");

    listener.getLtftFormEvent(event);

    ArgumentCaptor<JsonFormEventDto> eventCaptor = ArgumentCaptor.captor();
    verify(jsonService).processFormEvent(eventCaptor.capture());

    JsonFormEventDto capturedEvent = eventCaptor.getValue();
    assertThat("Unexpected form name.", capturedEvent.getFormName(), is("123.json"));
    assertThat("Unexpected form type.", capturedEvent.getFormType(), is("ltft"));

    Map<String, Object> eventContent = capturedEvent.fields;
    assertThat("Unexpected form content size.", eventContent.keySet(), hasSize(3));
    assertThat("Unexpected form ID.", eventContent.get("id"), is("123"));
    assertThat("Unexpected trainee ID.", eventContent.get("traineeTisId"), is("47165"));
    assertThat("Unexpected form field.", eventContent.get("field1"), is("value1"));

    verifyNoInteractions(formBroadcastService);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldThrowExceptionProcessIngJsonEventWhenIdNull(String id) throws IOException {
    JsonFormEventDto event = new JsonFormEventDto();
    event.set("id", id);
    event.set("traineeTisId", "47165");
    event.set("field1", "value1");

    assertThrows(IllegalArgumentException.class, () -> listener.getLtftFormEvent(event));

    verify(jsonService, never()).processFormEvent(any());
    verifyNoInteractions(formBroadcastService);
  }

  @Test
  void shouldProcessFormREventWhenIdAndFormTypeNotNull() throws IOException {
    JsonFormEventDto event = new JsonFormEventDto();
    event.set("id", "456");
    event.set("traineeTisId", "12345");
    event.set("field2", "value2");

    Message<JsonFormEventDto> message = MessageBuilder.withPayload(event)
        .setHeader("formType", "formr-a")
        .build();

    listener.getFormRFormEvent(message);

    ArgumentCaptor<JsonFormEventDto> eventCaptor = ArgumentCaptor.captor();
    verify(jsonService).processFormEvent(eventCaptor.capture());

    JsonFormEventDto capturedEvent = eventCaptor.getValue();
    assertThat("Unexpected form name.", capturedEvent.getFormName(), is("456.json"));
    assertThat("Unexpected form type.", capturedEvent.getFormType(), is("formr-a"));

    Map<String, Object> eventContent = capturedEvent.fields;
    assertThat("Unexpected form content size.", eventContent.keySet(), hasSize(3));
    assertThat("Unexpected form ID.", eventContent.get("id"), is("456"));
    assertThat("Unexpected trainee ID.", eventContent.get("traineeTisId"), is("12345"));
    assertThat("Unexpected form field.", eventContent.get("field2"), is("value2"));

    verify(formBroadcastService).broadcastFormEvent(any(), any(), any(), any(), any());
  }

  @Test
  void shouldProcessFormREventWithFormRbType() throws IOException {
    JsonFormEventDto event = new JsonFormEventDto();
    event.set("id", "789");
    event.set("traineeTisId", "67890");

    Message<JsonFormEventDto> message = MessageBuilder.withPayload(event)
        .setHeader("formType", "formr-b")
        .build();

    listener.getFormRFormEvent(message);

    ArgumentCaptor<JsonFormEventDto> eventCaptor = ArgumentCaptor.captor();
    verify(jsonService).processFormEvent(eventCaptor.capture());

    JsonFormEventDto capturedEvent = eventCaptor.getValue();
    assertThat("Unexpected form type.", capturedEvent.getFormType(), is("formr-b"));

    verify(formBroadcastService).broadcastFormEvent(any(), any(), any(), any(), any());
  }

  @Test
  void shouldBroadcastFormREvent() throws IOException {
    JsonFormEventDto event = new JsonFormEventDto();
    event.set("id", "456");
    event.set("traineeTisId", "12345");
    event.set("lifecycleState", "SUBMITTED");

    Message<JsonFormEventDto> message = MessageBuilder.withPayload(event)
        .setHeader("formType", "formr-a")
        .build();

    FormContentDto mockFormContentDto = new FormContentDto();
    when(jsonService.processFormEvent(any())).thenReturn(mockFormContentDto);
    listener.getFormRFormEvent(message);

    verify(formBroadcastService).broadcastFormEvent("456.json", "formr-a",
        "12345", "SUBMITTED", mockFormContentDto);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldThrowExceptionProcessingFormREventWhenIdNull(String id) throws IOException {
    JsonFormEventDto event = new JsonFormEventDto();
    event.set("id", id);
    event.set("traineeTisId", "12345");

    Message<JsonFormEventDto> message = MessageBuilder.withPayload(event)
        .setHeader("formType", "formr-a")
        .build();

    assertThrows(IllegalArgumentException.class, () -> listener.getFormRFormEvent(message));

    verify(jsonService, never()).processFormEvent(any());
    verifyNoInteractions(formBroadcastService);
  }

  @Test
  void shouldThrowExceptionProcessingFormREventWhenFormTypeNull() throws IOException {
    JsonFormEventDto event = new JsonFormEventDto();
    event.set("id", "456");
    event.set("traineeTisId", "12345");

    Message<JsonFormEventDto> message = MessageBuilder.withPayload(event).build();

    assertThrows(IllegalArgumentException.class, () -> listener.getFormRFormEvent(message));

    verify(jsonService, never()).processFormEvent(any());
    verifyNoInteractions(formBroadcastService);
  }
}

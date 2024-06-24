/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.ndw.service.NotificationService.DATALAKE_NOTIFICATIONS_ROOT;

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.ndw.dto.NotificationEventDto;
import uk.nhs.hee.tis.trainee.ndw.dto.NotificationEventDto.RecipientInfo;
import uk.nhs.hee.tis.trainee.ndw.dto.NotificationEventDto.TemplateInfo;
import uk.nhs.hee.tis.trainee.ndw.dto.NotificationEventDto.TisReferenceInfo;

/**
 * Test class for the Notification Service.
 */
class NotificationServiceTest {

  private static final String ROOT_DIRECTORY = "root";
  private static final String EVENT_ID = "eventId";
  private static final String REFERENCE_TYPE = "referenceType";
  private static final String REFERENCE_ID = "referenceId";
  private static final String TYPE = "type";
  private static final String RECIPIENT_ID = "recipientId";
  private static final String RECIPIENT_TYPE = "recipientType";
  private static final String RECIPIENT_CONTACT = "recipientContact";
  private static final String TEMPLATE_NAME = "templateName";
  private static final String TEMPLATE_VERSION = "1.2.3";
  private static final Instant SENT_AT = Instant.MIN;
  private static final Instant READ_AT = Instant.now();
  private static final String STATUS = "READ";
  private static final String STATUS_DETAIL = null;
  private static final Instant LAST_RETRY = null;

  private NotificationService service;

  private DataLakeFacade dataLakeFacade;

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    dataLakeFacade = mock(DataLakeFacade.class);
    service = new NotificationService(ROOT_DIRECTORY, mapper, dataLakeFacade);
  }

  @Test
  void shouldNotProcessNullEvent() throws JsonProcessingException {
    service.processNotificationEvent(null);

    verifyNoInteractions(dataLakeFacade);
  }

  @Test
  void shouldNotProcessEventWithNullId() throws JsonProcessingException {
    TisReferenceInfo tisReferenceInfo = new TisReferenceInfo(REFERENCE_TYPE, REFERENCE_ID);
    RecipientInfo recipientInfo
        = new RecipientInfo(RECIPIENT_ID, RECIPIENT_TYPE, RECIPIENT_CONTACT);
    Map<String, Object> templateMap = Map.of("key1", "value1");
    TemplateInfo templateInfo = new TemplateInfo(TEMPLATE_NAME, TEMPLATE_VERSION, templateMap);
    NotificationEventDto event = new NotificationEventDto(null, tisReferenceInfo, TYPE,
        recipientInfo, templateInfo, SENT_AT, READ_AT, STATUS, STATUS_DETAIL, LAST_RETRY);

    service.processNotificationEvent(event);

    verifyNoInteractions(dataLakeFacade);
  }

  @Test
  void shouldProcessEvent() throws JsonProcessingException {
    TisReferenceInfo tisReferenceInfo = new TisReferenceInfo(REFERENCE_TYPE, REFERENCE_ID);
    RecipientInfo recipientInfo
        = new RecipientInfo(RECIPIENT_ID, RECIPIENT_TYPE, RECIPIENT_CONTACT);
    Map<String, Object> templateMap = Map.of("key1", "value1");
    TemplateInfo templateInfo = new TemplateInfo(TEMPLATE_NAME, TEMPLATE_VERSION, templateMap);
    NotificationEventDto event = new NotificationEventDto(EVENT_ID, tisReferenceInfo, TYPE,
        recipientInfo, templateInfo, SENT_AT, READ_AT, STATUS, STATUS_DETAIL, LAST_RETRY);

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);
    when(dataLakeFacade.createYearMonthDaySubDirectories(any())).thenReturn(directoryClient);

    service.processNotificationEvent(event);

    verify(dataLakeFacade).createSubDirectory(ROOT_DIRECTORY, DATALAKE_NOTIFICATIONS_ROOT);
    verify(dataLakeFacade).createYearMonthDaySubDirectories(directoryClient);

    String expectedContent = mapper.writeValueAsString(event);
    verify(dataLakeFacade).saveToDataLake(EVENT_ID, expectedContent, directoryClient);
  }
}
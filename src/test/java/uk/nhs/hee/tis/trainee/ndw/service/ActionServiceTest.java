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

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.ndw.dto.ActionEventDto;
import uk.nhs.hee.tis.trainee.ndw.dto.ActionEventDto.TisReferenceInfo;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.ndw.service.ActionService.DATALAKE_ACTIONS_ROOT;

/**
 * Test class for the Action Service.
 */
class ActionServiceTest {

  private static final String ROOT_DIRECTORY = "root";
  private static final String EVENT_ID = "eventId";
  private static final String TYPE = "type";
  private static final String TRAINEE_ID = "traineeId";
  private static final String REFERENCE_ID = "referenceId";
  private static final String REFERENCE_TYPE = "referenceType";
  private static final LocalDate AVAILABLE_FROM = now();
  private static final LocalDate DUE_BY = now();
  private static final Instant COMPLETED = Instant.now();
  private static final String STATUS = "CURRENT";
  private static final Instant STATUS_DATETIME = Instant.now();

  private ActionService service;

  private DataLakeFacade dataLakeFacade;

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    dataLakeFacade = mock(DataLakeFacade.class);
    service = new ActionService(ROOT_DIRECTORY, mapper, dataLakeFacade);
  }

  @Test
  void shouldNotProcessNullEvent() throws JsonProcessingException {
    service.processActionEvent(null);

    verifyNoInteractions(dataLakeFacade);
  }

  @Test
  void shouldNotProcessEventWithNullId() throws JsonProcessingException {
    TisReferenceInfo tisReferenceInfo = new TisReferenceInfo(REFERENCE_ID, REFERENCE_TYPE);
    ActionEventDto event = new ActionEventDto(null, TYPE, TRAINEE_ID, tisReferenceInfo,
        AVAILABLE_FROM, DUE_BY, COMPLETED, STATUS, STATUS_DATETIME);

    service.processActionEvent(event);

    verifyNoInteractions(dataLakeFacade);
  }

  @Test
  void shouldProcessEvent() throws JsonProcessingException {
    TisReferenceInfo tisReferenceInfo = new TisReferenceInfo(REFERENCE_ID, REFERENCE_TYPE);
    ActionEventDto event = new ActionEventDto(EVENT_ID, TYPE, TRAINEE_ID, tisReferenceInfo,
        AVAILABLE_FROM, DUE_BY, COMPLETED, STATUS, STATUS_DATETIME);

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);
    when(dataLakeFacade.createYearMonthDaySubDirectories(any())).thenReturn(directoryClient);

    service.processActionEvent(event);

    verify(dataLakeFacade).createSubDirectory(ROOT_DIRECTORY, DATALAKE_ACTIONS_ROOT);
    verify(dataLakeFacade).createYearMonthDaySubDirectories(directoryClient);

    String expectedContent = mapper.writeValueAsString(event);
    verify(dataLakeFacade).saveToDataLake(EVENT_ID, expectedContent, directoryClient);
  }
}

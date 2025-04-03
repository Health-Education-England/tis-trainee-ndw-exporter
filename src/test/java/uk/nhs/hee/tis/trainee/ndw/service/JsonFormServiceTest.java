/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.tis.trainee.ndw.dto.FormContentDto;
import uk.nhs.hee.tis.trainee.ndw.dto.JsonFormEventDto;

/**
 * Test class for the JSON Form Service.
 */
class JsonFormServiceTest {

  private static final String FORM_NAME_VALUE = "123.json";
  private static final String FORM_TYPE_UNCHECKED_VALUE = "form-type-value";

  private static final String ROOT_DIR = "test-directory";

  private JsonFormService service;
  private DataLakeFacade dataLakeFacade;

  @BeforeEach
  void setUp() {
    dataLakeFacade = mock(DataLakeFacade.class);
    service = new JsonFormService(dataLakeFacade, ROOT_DIR, new ObjectMapper());
  }

  @Test
  void shouldNotExportFormWhenUnsupportedFormType() {
    JsonFormEventDto formEvent = new JsonFormEventDto();
    formEvent.setFormName(FORM_NAME_VALUE);
    formEvent.setFormType(FORM_TYPE_UNCHECKED_VALUE);

    service.processFormEvent(formEvent);

    verifyNoInteractions(dataLakeFacade);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      formr-a | part-a
      formr-b | part-b
      ltft    | ltft
      """)
  void shouldExportFormWhenSupportedFormType(String formType, String subDirectory) {
    JsonFormEventDto formEvent = new JsonFormEventDto();
    formEvent.setFormName(FORM_NAME_VALUE);
    formEvent.setFormType(formType);
    formEvent.set("field1", "value1ท");

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);
    when(dataLakeFacade.createYearMonthDaySubDirectories(any())).thenReturn(directoryClient);

    service.processFormEvent(formEvent);

    verify(dataLakeFacade).createSubDirectory(ROOT_DIR, subDirectory);
    verify(dataLakeFacade).createYearMonthDaySubDirectories(directoryClient);
    verify(dataLakeFacade).saveToDataLake(FORM_NAME_VALUE, "{\"field1\":\"value1ท\"}",
        directoryClient);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      formr-a | part-a
      formr-b | part-b
      ltft    | ltft
      """)
  void shouldUploadToSpecifiedDirectory(String formType, String directory) {
    JsonFormEventDto formEvent = new JsonFormEventDto();
    formEvent.setFormName(FORM_NAME_VALUE);
    formEvent.setFormType(formType);

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);

    service.processFormEvent(formEvent);

    verify(dataLakeFacade).createSubDirectory(ROOT_DIR, directory);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      formr-a | part-a
      formr-b | part-b
      ltft    | ltft
      """)
  void shouldUploadToCorrectSubDirectories(String formType, String directory) {
    JsonFormEventDto formEvent = new JsonFormEventDto();
    formEvent.setFormName(FORM_NAME_VALUE);
    formEvent.setFormType(formType);

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);
    when(dataLakeFacade.createYearMonthDaySubDirectories(any())).thenReturn(directoryClient);

    service.processFormEvent(formEvent);

    verify(dataLakeFacade).createSubDirectory(ROOT_DIR, directory);
    verify(dataLakeFacade).createYearMonthDaySubDirectories(directoryClient);
    verify(dataLakeFacade).saveToDataLake(eq(FORM_NAME_VALUE), any(), eq(directoryClient));
  }

  @ParameterizedTest
  @ValueSource(strings = {"formr-a", "formr-b", "ltft"})
  void shouldStripTrailingWhitespaceWhenExporting(String formType) throws JsonProcessingException {
    JsonFormEventDto formEvent = new JsonFormEventDto();
    formEvent.setFormName(FORM_NAME_VALUE);
    formEvent.setFormType(formType);

    // In theory, there are a number of whitespace characters which should be stripped, but only
    // the common ones are tested for below
    formEvent.set("field1", "  value1  ");
    formEvent.set("field2", "    ");
    formEvent.set("field3", "value2 \t \n \r \f");
    formEvent.set("field4", 123);
    formEvent.set("field5", "{\"field5_1\": \"value 3 \", \"field5_2\": 12.5}");

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);
    when(dataLakeFacade.createYearMonthDaySubDirectories(any())).thenReturn(directoryClient);

    String contentsClean = """
        {
          "field1": "  value1",
          "field2": "",
          "field3": "value2",
          "field4": 123,
          "field5": {"field5_1": "value 3", "field5_2": 12.5}
        }
        """;

    service.processFormEvent(formEvent);

    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
    verify(dataLakeFacade).saveToDataLake(any(), stringCaptor.capture(), any());

    String uploadedString = stringCaptor.getValue();
    ObjectMapper mapper = new ObjectMapper();
    FormContentDto formContentDto = mapper.readValue(uploadedString, FormContentDto.class);
    FormContentDto expectedFormDto = mapper.readValue(contentsClean, FormContentDto.class);
    assertEquals(expectedFormDto.fields.get("field1"), formContentDto.fields.get("field1"));
    assertEquals(expectedFormDto.fields.get("field2"), formContentDto.fields.get("field2"));
    assertEquals(expectedFormDto.fields.get("field3"), formContentDto.fields.get("field3"));
    assertEquals(expectedFormDto.fields.get("field4"), formContentDto.fields.get("field4"));
    assertEquals(expectedFormDto.fields.get("field5_1"), formContentDto.fields.get("field5_1"));
    assertEquals(expectedFormDto.fields.get("field5_2"), formContentDto.fields.get("field5_2"));
  }
}

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

package uk.nhs.hee.tis.trainee.ndw.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import uk.nhs.hee.tis.trainee.ndw.dto.FormContentDto;
import uk.nhs.hee.tis.trainee.ndw.dto.FormEventDto;

/**
 * Test class for the Form Service.
 */
class FormServiceTest {

  private static final String BUCKET = "bucket123";
  private static final String KEY = "abc/123.json";
  private static final String VERSION = "123";

  private static final String FORM_NAME_KEY = "name";
  private static final String FORM_NAME_VALUE = "123.json";
  private static final String FORM_TYPE_KEY = "formtype";
  private static final String FORM_TYPE_VALID_VALUE = "formr-a";
  private static final String FORM_TYPE_UNCHECKED_VALUE = "form-type-value";
  private static final String FORM_TRAINEE_KEY = "traineeid";
  private static final String FORM_TRAINEE_VALUE = "trainee-value";
  private static final String FORM_LIFECYCLE_STATE_KEY = "lifecyclestate";
  private static final String FORM_LIFECYCLE_STATE_VALUE = "lifecycle-state-value";

  private static final String FORMR_ROOT = "dev";

  private FormService service;

  private S3Client s3Client;
  private DataLakeFacade dataLakeFacade;
  private FormBroadcastService formBroadcastService;

  @BeforeEach
  void setUp() {
    s3Client = mock(S3Client.class);
    dataLakeFacade = mock(DataLakeFacade.class);
    formBroadcastService = mock(FormBroadcastService.class);
    service = new FormService(s3Client, dataLakeFacade, "dev", formBroadcastService,
        new ObjectMapper());
  }

  @Test
  void shouldThrowExceptionWhenNoFormNameFound() {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        "not-name", FORM_NAME_VALUE,
        FORM_TYPE_KEY, FORM_TYPE_VALID_VALUE,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        new byte[0]);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    assertThrows(IOException.class, () -> service.processFormEvent(formEvent));
  }

  @Test
  void shouldThrowExceptionWhenNoFormTypeFound() {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        "not-form-type", FORM_TYPE_VALID_VALUE,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        new byte[0]);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    assertThrows(IOException.class, () -> service.processFormEvent(formEvent));
  }

  @Test
  void shouldThrowExceptionWhenNoTraineeIdFound() {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, FORM_TYPE_UNCHECKED_VALUE,
        "not-trainee-id", FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        new byte[0]);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    assertThrows(IOException.class, () -> service.processFormEvent(formEvent));
  }

  @Test
  void shouldThrowExceptionWhenNoLifecycleStateFound() {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, FORM_TYPE_UNCHECKED_VALUE,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        "not-form-lifecycle", FORM_LIFECYCLE_STATE_VALUE
    );

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    String formContents = """
        {
          "field1": "value1"
        }""";
    byte[] contents = formContents.getBytes(StandardCharsets.UTF_8);

    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        contents);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    assertThrows(IOException.class, () -> service.processFormEvent(formEvent));
  }

  @Test
  void shouldNotThrowExceptionWhenFormNameAndTypeAndTraineeAndLifecycleFound() {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, FORM_TYPE_UNCHECKED_VALUE,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    String formContents = """
        {
          "field1": "value1"
        }""";
    byte[] contents = formContents.getBytes(StandardCharsets.UTF_8);
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        contents);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    assertDoesNotThrow(() -> service.processFormEvent(formEvent));
  }

  @Test
  void shouldNotThrowExceptionWhenEventVersionNotLatest() {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, FORM_TYPE_UNCHECKED_VALUE,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId("latestVersion")
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        new byte[0]);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    assertDoesNotThrow(() -> service.processFormEvent(formEvent));
  }

  @Test
  void shouldNotExportFormWhenUnsupportedFormType() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, FORM_TYPE_UNCHECKED_VALUE,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        new byte[0]);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    service.processFormEvent(formEvent);

    verifyNoInteractions(dataLakeFacade);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      formr-a | part-a
      formr-b | part-b
      """)
  void shouldExportFormWhenFormTypeIsFormr(String formType, String subDirectory)
      throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, formType,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);
    when(dataLakeFacade.createYearMonthDaySubDirectories(any())).thenReturn(directoryClient);

    String formContents = "{\"field1\":\"value1ท\"}";
    byte[] contents = formContents.getBytes(StandardCharsets.UTF_8);

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        contents);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    service.processFormEvent(formEvent);

    verify(dataLakeFacade).createSubDirectory(FORMR_ROOT, subDirectory);
    verify(dataLakeFacade).createYearMonthDaySubDirectories(directoryClient);
    verify(dataLakeFacade).saveToDataLake(FORM_NAME_VALUE, formContents, directoryClient);
  }

  @ParameterizedTest
  @ValueSource(strings = {"formr-a", "formr-b"})
  void shouldNotThrowExceptionWhenExportedFormCannotBeRead(String formType) {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, formType,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        "invalid".getBytes());
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    assertDoesNotThrow(() -> service.processFormEvent(formEvent));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      formr-a | part-a
      formr-b | part-b
      """)
  void shouldUploadToSpecifiedDirectory(String formType, String directory) throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, formType,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        new byte[0]);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    FormService service = new FormService(s3Client, dataLakeFacade, "test-directory",
        formBroadcastService, new ObjectMapper());
    service.processFormEvent(formEvent);

    verify(dataLakeFacade).createSubDirectory("test-directory", directory);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      formr-a | part-a
      formr-b | part-b
      """)
  void shouldUploadToCorrectSubDirectories(String formType, String directory) throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, formType,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);
    when(dataLakeFacade.createYearMonthDaySubDirectories(any())).thenReturn(directoryClient);

    String contentsString = "{\"field1\":\"value1\"}";
    byte[] contents = contentsString.getBytes(StandardCharsets.UTF_8);

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        contents);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    FormService service = new FormService(s3Client, dataLakeFacade, "test-directory",
        formBroadcastService, new ObjectMapper());
    service.processFormEvent(formEvent);

    verify(dataLakeFacade).createSubDirectory("test-directory", directory);
    verify(dataLakeFacade).createYearMonthDaySubDirectories(directoryClient);
    verify(dataLakeFacade)
        .saveToDataLake(eq(FORM_NAME_VALUE), eq(contentsString), eq(directoryClient));
  }

  @Test
  void shouldStripTrailingWhitespaceWhenExporting() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, "formr-a",
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);
    when(dataLakeFacade.createYearMonthDaySubDirectories(any())).thenReturn(directoryClient);

    // In theory, there are a number of whitespace characters which should be stripped, but only
    // the common ones are tested for below
    byte[] contents = """
        {
          "field1": "  value1  ",
          "field2": "    ",
          "field3": "value2 \\t \\n \\r \\f",
          "field4": 123,
          "field5": {"field5_1": "value 3 ", "field5_2": 12.5}
        }
        """.getBytes(StandardCharsets.UTF_8);

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        contents);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

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

  @Test
  void shouldBroadcastValidFormEvent() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, FORM_TYPE_VALID_VALUE,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);
    when(dataLakeFacade.createYearMonthDaySubDirectories(any())).thenReturn(directoryClient);

    byte[] contents = """
        {
          "field1": "value1"
        }
        """.getBytes(StandardCharsets.UTF_8);

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        contents);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    FormService service = new FormService(s3Client, dataLakeFacade, "test-directory",
        formBroadcastService, new ObjectMapper());
    service.processFormEvent(formEvent);

    verify(formBroadcastService).publishFormBroadcastEvent(any());
  }

  @Test
  void shouldNotBroadcastFormEventIfNoFormContent() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, FORM_TYPE_VALID_VALUE,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);
    when(dataLakeFacade.createYearMonthDaySubDirectories(any())).thenReturn(directoryClient);

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        new byte[0]);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    FormService service = new FormService(s3Client, dataLakeFacade, "test-directory",
        formBroadcastService, new ObjectMapper());
    service.processFormEvent(formEvent);

    verifyNoInteractions(formBroadcastService);
  }

  @Test
  void shouldNotBroadcastFormEventIfUnrecognizedFormType() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    Map<String, String> metadata = Map.of(
        FORM_NAME_KEY, FORM_NAME_VALUE,
        FORM_TYPE_KEY, FORM_TYPE_UNCHECKED_VALUE,
        FORM_TRAINEE_KEY, FORM_TRAINEE_VALUE,
        FORM_LIFECYCLE_STATE_KEY, FORM_LIFECYCLE_STATE_VALUE
    );

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeFacade.createSubDirectory(any(), any())).thenReturn(directoryClient);
    when(dataLakeFacade.createYearMonthDaySubDirectories(any())).thenReturn(directoryClient);

    byte[] contents = """
        {
          "field1": "value1"
        }
        """.getBytes(StandardCharsets.UTF_8);

    GetObjectResponse response = GetObjectResponse.builder()
        .metadata(metadata)
        .versionId(VERSION)
        .build();
    ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(response,
        contents);
    when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

    FormService service = new FormService(s3Client, dataLakeFacade, "test-directory",
        formBroadcastService, new ObjectMapper());
    service.processFormEvent(formEvent);

    verifyNoInteractions(formBroadcastService);
  }
}

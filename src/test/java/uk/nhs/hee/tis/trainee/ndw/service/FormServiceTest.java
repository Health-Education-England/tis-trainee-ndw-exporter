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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.ndw.FormEventDto;

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
  private static final String FORM_TYPE_VALUE = "form-type-value";

  private static final String FORMR_ROOT = "formr/bronze";

  private FormService service;

  private AmazonS3 amazonS3;
  private DataLakeFileSystemClient dataLakeClient;

  @BeforeEach
  void setUp() {
    amazonS3 = mock(AmazonS3.class);
    dataLakeClient = mock(DataLakeFileSystemClient.class);
    service = new FormService(amazonS3, dataLakeClient);
  }

  @Test
  void shouldThrowExceptionWhenNoFormNameFound() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setHeader(Headers.S3_VERSION_ID, VERSION);
    metadata.addUserMetadata("not-name", FORM_NAME_VALUE);
    metadata.addUserMetadata(FORM_TYPE_KEY, FORM_TYPE_VALUE);

    try (S3Object document = new S3Object();
        InputStream contentStream = new ByteArrayInputStream(new byte[0])) {
      document.setObjectMetadata(metadata);
      document.setObjectContent(contentStream);

      when(amazonS3.getObject(BUCKET, KEY)).thenReturn(document);

      assertThrows(IOException.class, () -> service.processFormEvent(formEvent));
    }
  }

  @Test
  void shouldThrowExceptionWhenNoFormTypeFound() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setHeader(Headers.S3_VERSION_ID, VERSION);
    metadata.addUserMetadata(FORM_NAME_KEY, FORM_NAME_VALUE);
    metadata.addUserMetadata("not-form-type", FORM_TYPE_VALUE);

    try (S3Object document = new S3Object();
        InputStream contentStream = new ByteArrayInputStream(new byte[0])) {
      document.setObjectMetadata(metadata);
      document.setObjectContent(contentStream);

      when(amazonS3.getObject(BUCKET, KEY)).thenReturn(document);

      assertThrows(IOException.class, () -> service.processFormEvent(formEvent));
    }
  }

  @Test
  void shouldNotThrowExceptionWhenFormNameAndTypeFound() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setHeader(Headers.S3_VERSION_ID, VERSION);
    metadata.addUserMetadata(FORM_NAME_KEY, FORM_NAME_VALUE);
    metadata.addUserMetadata(FORM_TYPE_KEY, FORM_TYPE_VALUE);

    try (S3Object document = new S3Object();
        InputStream contentStream = new ByteArrayInputStream(new byte[0])) {
      document.setObjectMetadata(metadata);
      document.setObjectContent(contentStream);

      when(amazonS3.getObject(BUCKET, KEY)).thenReturn(document);

      assertDoesNotThrow(() -> service.processFormEvent(formEvent));
    }
  }

  @Test
  void shouldNotThrowExceptionWhenEventVersionNotLatest() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setHeader(Headers.S3_VERSION_ID, "latestVersion");
    metadata.addUserMetadata(FORM_NAME_KEY, FORM_NAME_VALUE);
    metadata.addUserMetadata(FORM_TYPE_KEY, FORM_TYPE_VALUE);

    try (S3Object document = new S3Object();
        InputStream contentStream = new ByteArrayInputStream(new byte[0])) {
      document.setObjectMetadata(metadata);
      document.setObjectContent(contentStream);

      when(amazonS3.getObject(BUCKET, KEY)).thenReturn(document);

      assertDoesNotThrow(() -> service.processFormEvent(formEvent));
    }
  }

  @Test
  void shouldNotExportFormWhenUnsupportedFormType() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setHeader(Headers.S3_VERSION_ID, VERSION);
    metadata.addUserMetadata(FORM_NAME_KEY, FORM_NAME_VALUE);
    metadata.addUserMetadata(FORM_TYPE_KEY, FORM_TYPE_VALUE);

    try (S3Object document = new S3Object();
        InputStream contentStream = new ByteArrayInputStream(new byte[0])) {
      document.setObjectMetadata(metadata);
      document.setObjectContent(contentStream);

      when(amazonS3.getObject(BUCKET, KEY)).thenReturn(document);

      service.processFormEvent(formEvent);

      verifyNoInteractions(dataLakeClient);
    }
  }

  @Test
  void shouldExportFormWhenFormTypeIsFormrPartA() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setHeader(Headers.S3_VERSION_ID, VERSION);
    metadata.addUserMetadata(FORM_NAME_KEY, FORM_NAME_VALUE);
    metadata.addUserMetadata(FORM_TYPE_KEY, "formr-a");

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeClient.getDirectoryClient(FORMR_ROOT)).thenReturn(directoryClient);
    when(directoryClient.createSubdirectoryIfNotExists("part-a")).thenReturn(directoryClient);

    DataLakeFileClient fileClient = mock(DataLakeFileClient.class);
    when(directoryClient.createFileIfNotExists(FORM_NAME_VALUE)).thenReturn(fileClient);

    byte[] contents = """
        {
          "field1": "value1"
        }
        """.getBytes(StandardCharsets.UTF_8);

    try (S3Object document = new S3Object();
        InputStream contentStream = new ByteArrayInputStream(contents)) {
      document.setObjectMetadata(metadata);
      document.setObjectContent(contentStream);

      when(amazonS3.getObject(BUCKET, KEY)).thenReturn(document);

      service.processFormEvent(formEvent);

      verify(fileClient).upload(document.getObjectContent(), 0L, true);
    }
  }

  @Test
  void shouldExportFormWhenFormTypeIsFormrPartB() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setHeader(Headers.S3_VERSION_ID, VERSION);
    metadata.addUserMetadata(FORM_NAME_KEY, FORM_NAME_VALUE);
    metadata.addUserMetadata(FORM_TYPE_KEY, "formr-b");

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeClient.getDirectoryClient(FORMR_ROOT)).thenReturn(directoryClient);
    when(directoryClient.createSubdirectoryIfNotExists("part-b")).thenReturn(directoryClient);

    DataLakeFileClient fileClient = mock(DataLakeFileClient.class);
    when(directoryClient.createFileIfNotExists(FORM_NAME_VALUE)).thenReturn(fileClient);

    byte[] contents = """
        {
          "field1": "value1"
        }
        """.getBytes(StandardCharsets.UTF_8);

    try (S3Object document = new S3Object();
        InputStream contentStream = new ByteArrayInputStream(contents)) {
      document.setObjectMetadata(metadata);
      document.setObjectContent(contentStream);

      when(amazonS3.getObject(BUCKET, KEY)).thenReturn(document);

      service.processFormEvent(formEvent);

      verify(fileClient).upload(document.getObjectContent(), 0L, true);
    }
  }

  @Test
  void shouldNotThrowExceptionWhenExportedFormCannotBeClosed() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setHeader(Headers.S3_VERSION_ID, VERSION);
    metadata.addUserMetadata(FORM_NAME_KEY, FORM_NAME_VALUE);
    metadata.addUserMetadata(FORM_TYPE_KEY, "formr-a");

    DataLakeDirectoryClient directoryClient = mock(DataLakeDirectoryClient.class);
    when(dataLakeClient.getDirectoryClient(any())).thenReturn(directoryClient);
    when(directoryClient.createSubdirectoryIfNotExists(any())).thenReturn(directoryClient);

    DataLakeFileClient fileClient = mock(DataLakeFileClient.class);
    when(directoryClient.createFileIfNotExists(any())).thenReturn(fileClient);

    InputStream is = mock(InputStream.class);
    doThrow(IOException.class).when(is).close();

    S3Object document = new S3Object();
    document.setObjectMetadata(metadata);
    document.setObjectContent(is);

    when(amazonS3.getObject(BUCKET, KEY)).thenReturn(document);

    assertDoesNotThrow(() -> service.processFormEvent(formEvent));

    verify(is).close();
  }
}

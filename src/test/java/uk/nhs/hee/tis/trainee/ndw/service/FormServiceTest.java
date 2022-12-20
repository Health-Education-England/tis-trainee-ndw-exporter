package uk.nhs.hee.tis.trainee.ndw.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

  private static final String FORM_ID = "456";
  private static final String FORM_CLASS = "uk.nhs.tis.trainee.FormClass";

  private FormService service;

  private AmazonS3 amazonS3;

  @BeforeEach
  void setUp() {
    amazonS3 = mock(AmazonS3.class);
    service = new FormService(amazonS3, new ObjectMapper());
  }

  @Test
  void shouldThrowExceptionWhenNoFormIdFound() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setHeader(Headers.S3_VERSION_ID, VERSION);

    String content = """
          {
            "_class": "%s"
          }
        """.formatted(FORM_CLASS);

    try (S3Object document = new S3Object();
        InputStream contentStream = new ByteArrayInputStream(content.getBytes())) {
      document.setObjectMetadata(metadata);
      document.setObjectContent(contentStream);

      when(amazonS3.getObject(BUCKET, KEY)).thenReturn(document);

      assertThrows(IOException.class, () -> service.processFormEvent(formEvent));
    }
  }

  @Test
  void shouldThrowExceptionWhenNoFormClassFound() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setHeader(Headers.S3_VERSION_ID, VERSION);

    String content = """
          {
            "_id": "%s"
          }
        """.formatted(FORM_ID);

    try (S3Object document = new S3Object();
        InputStream contentStream = new ByteArrayInputStream(content.getBytes())) {
      document.setObjectMetadata(metadata);
      document.setObjectContent(contentStream);

      when(amazonS3.getObject(BUCKET, KEY)).thenReturn(document);

      assertThrows(IOException.class, () -> service.processFormEvent(formEvent));
    }
  }

  @Test
  void shouldNotThrowExceptionWhenFormIdAndClassFound() throws IOException {
    FormEventDto formEvent = new FormEventDto();
    formEvent.setBucket(BUCKET);
    formEvent.setKey(KEY);
    formEvent.setVersionId(VERSION);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setHeader(Headers.S3_VERSION_ID, VERSION);

    String content = """
          {
            "_id": "%s",
            "_class": "%s"
          }
        """.formatted(FORM_ID, FORM_CLASS);

    try (S3Object document = new S3Object();
        InputStream contentStream = new ByteArrayInputStream(content.getBytes())) {
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

    String content = """
          {
            "_id": "%s",
            "_class": "%s"
          }
        """.formatted(FORM_ID, FORM_CLASS);

    try (S3Object document = new S3Object();
        InputStream contentStream = new ByteArrayInputStream(content.getBytes())) {
      document.setObjectMetadata(metadata);
      document.setObjectContent(contentStream);

      when(amazonS3.getObject(BUCKET, KEY)).thenReturn(document);

      assertDoesNotThrow(() -> service.processFormEvent(formEvent));
    }
  }
}

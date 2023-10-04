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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.ndw.dto.FormBroadcastEventDto;
import uk.nhs.hee.tis.trainee.ndw.dto.FormContentDto;
import uk.nhs.hee.tis.trainee.ndw.dto.FormEventDto;

/**
 * A service for processing S3 Form events.
 */
@Slf4j
@Service
public class FormService {

  private static final String FORM_TYPE_METADATA_FIELD = "formtype";
  private static final String NAME_METADATA_FIELD = "name";
  private static final String LIFECYCLE_STATE_METADATA_FIELD = "lifecyclestate";
  private static final String TRAINEE_ID_METADATA_FIELD = "traineeid";

  private final AmazonS3 amazonS3;
  private final DataLakeFileSystemClient dataLakeClient;
  private FormBroadcastService formBroadcastService;

  private final String getDataLakeFormrRoot;

  FormService(AmazonS3 amazonS3, DataLakeFileSystemClient dataLakeClient,
      @Value("${application.ndw.directory}") String directory,
      FormBroadcastService formBroadcastService) {
    this.amazonS3 = amazonS3;
    this.dataLakeClient = dataLakeClient;
    this.getDataLakeFormrRoot = directory;
    this.formBroadcastService = formBroadcastService;
  }

  /**
   * Process the given S3 Event.
   *
   * @param event The S3 event to process.
   * @throws IOException when the form contents could not be read, or were not correctly
   *                     structured.
   */
  public void processFormEvent(FormEventDto event) throws IOException {
    log.info("Processing form event for {}/{}", event.getBucket(), event.getKey());

    S3Object document = amazonS3.getObject(event.getBucket(), event.getKey());
    String latestVersionId = document.getObjectMetadata().getVersionId();

    if (!latestVersionId.equals(event.getVersionId())) {
      log.info("The form has been modified since the event was published, using latest content.");
      log.debug("Event version: {}\nLatest version: {}", event.getVersionId(), latestVersionId);
    }

    Map<String, String> userMetadata = document.getObjectMetadata().getUserMetadata();

    if (userMetadata.containsKey(NAME_METADATA_FIELD) && userMetadata.containsKey(
        FORM_TYPE_METADATA_FIELD)) {
      String formName = userMetadata.get(NAME_METADATA_FIELD);
      String formType = userMetadata.get(FORM_TYPE_METADATA_FIELD);
      log.info("Retrieved form {} of type {}.", formName, formType);

      exportToDataLake(formName, formType, document);

      if (userMetadata.containsKey(TRAINEE_ID_METADATA_FIELD)
          && userMetadata.containsKey(LIFECYCLE_STATE_METADATA_FIELD)) {
        String traineeId = userMetadata.get(TRAINEE_ID_METADATA_FIELD);
        String lifecycleState = userMetadata.get(LIFECYCLE_STATE_METADATA_FIELD);
        broadcastFormEvent(formName, formType, traineeId, lifecycleState);
      } else {
        log.error("File {}/{} did not have the expected metadata for broadcasting the event.",
            event.getBucket(), event.getKey());
        throw new IOException("Unexpected document contents.");
      }
    } else {
      log.error("File {}/{} did not have the expected metadata.", event.getBucket(),
          event.getKey());
      throw new IOException("Unexpected document contents.");
    }
  }

  /**
   * Export a form to the data lake.
   *
   * @param formName The file name of the form.
   * @param formType The form's type.
   * @param document The S3 document to upload.
   */
  private void exportToDataLake(String formName, String formType, S3Object document) {
    DataLakeDirectoryClient directoryClient;

    switch (formType) {
      case "formr-a" -> directoryClient = dataLakeClient
          .getDirectoryClient(getDataLakeFormrRoot)
          .createSubdirectoryIfNotExists("part-a");
      case "formr-b" -> directoryClient = dataLakeClient
          .getDirectoryClient(getDataLakeFormrRoot)
          .createSubdirectoryIfNotExists("part-b");
      default -> {
        log.error("{} is not an exportable form type.", formType);
        return;
      }
    }

    try (S3ObjectInputStream content = document.getObjectContent()) {
      if (content != null) {
        ObjectMapper mapper = new ObjectMapper();
        FormContentDto formContentDto = mapper.readValue(content, FormContentDto.class);
        FormContentDto formContentDtoClean = cleanFormContent(formContentDto);
        String cleanedString = mapper.writeValueAsString(formContentDtoClean);

        log.info("Exporting form {} of type {}.", formName, formType);
        S3ObjectInputStream cleanStream = new S3ObjectInputStream(
            IOUtils.toInputStream(cleanedString, StandardCharsets.UTF_8), null);

        directoryClient
            .createFileIfNotExists(formName)
            .upload(cleanStream, cleanedString.getBytes(StandardCharsets.UTF_8).length, true);
        log.info("Exported form {} of type {}.", formName, formType);
      } else {
        log.warn("Skipping empty form {} of type {}.", formName, formType);
      }
    } catch (IOException e) {
      log.warn("Unable to close stream for form {} of type {}.", formName, formType);
    }
  }

  /**
   * Clean form content to remove trailing whitespace in text fields.
   *
   * @param formContentDto the dirty form content Dto.
   * @return a cleaned FormContentDto object.
   */
  private FormContentDto cleanFormContent(FormContentDto formContentDto) {
    FormContentDto cleanFormContentDto = new FormContentDto();
    formContentDto.fields.forEach((f, v)
        -> cleanFormContentDto.fields.put(f, removeTrailingWhitespace(v)));
    return cleanFormContentDto;
  }

  /**
   * Remove trailing whitespace from a string object.
   *
   * @param o the object to process.
   * @return a copy of the object with trailing whitespace removed if it is a string, otherwise the
   *         unchanged object.
   */
  private Object removeTrailingWhitespace(Object o) {
    if (o instanceof String s) {
      return s.stripTrailing();
    } else if (o instanceof HashMap<?, ?> h) {
      HashMap<String, Object> hashMap = new HashMap<>();
      h.forEach((f, v) -> hashMap.put((String) f, removeTrailingWhitespace(v)));
      return hashMap;
    }
    return o;
  }

  /**
   * Broadcast a form event using the form broadcast service.
   *
   * @param formName       The name of the form.
   * @param formType       The type of the form (e.g. formr-a, formr-b).
   * @param traineeId      The trainee TIS ID.
   * @param lifecycleState The lifecycle state of the form (e.g. SUBMITTED, DELETED).
   */
  private void broadcastFormEvent(String formName, String formType, String traineeId,
      String lifecycleState) {
    log.info("Broadcasting event for form {}", formName);
    FormBroadcastEventDto formBroadcastEventDto
        = new FormBroadcastEventDto(formName, formType, lifecycleState, traineeId, Instant.now());
    formBroadcastService.publishFormBroadcastEvent(formBroadcastEventDto);
  }
}

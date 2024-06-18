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

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
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

  private final S3Client s3Client;
  private final FormBroadcastService formBroadcastService;
  private final DataLakeFacade dataLakeFacade;

  private final String getDataLakeFormrRoot;

  private final ObjectMapper mapper;

  /**
   * Initialise the form service.
   *
   * @param s3Client             The S3 client to use.
   * @param directory            The root directory.
   * @param formBroadcastService The form broadcast service to use.
   * @param mapper               The object mapper to use.
   * @param dataLakeFacade      The data lake service to use.
   */
  FormService(S3Client s3Client, DataLakeFacade dataLakeFacade,
      @Value("${application.ndw.directory}") String directory,
      FormBroadcastService formBroadcastService, ObjectMapper mapper) {
    this.s3Client = s3Client;
    this.getDataLakeFormrRoot = directory;
    this.formBroadcastService = formBroadcastService;
    this.dataLakeFacade = dataLakeFacade;
    this.mapper = mapper;
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
    GetObjectRequest request = GetObjectRequest.builder()
        .bucket(event.getBucket())
        .key(event.getKey())
        .build();

    ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request);
    String latestVersionId = responseBytes.response().versionId();

    if (!Objects.equals(event.getVersionId(), latestVersionId)) {
      log.info("The form has been modified since the event was published, using latest content.");
      log.debug("Event version: {}\nLatest version: {}", event.getVersionId(), latestVersionId);
    }

    Map<String, String> userMetadata = responseBytes.response().metadata();

    if (userMetadata.containsKey(NAME_METADATA_FIELD) && userMetadata.containsKey(
        FORM_TYPE_METADATA_FIELD)) {
      String formName = userMetadata.get(NAME_METADATA_FIELD);
      String formType = userMetadata.get(FORM_TYPE_METADATA_FIELD);
      log.info("Retrieved form {} of type {}.", formName, formType);

      FormContentDto formContentDto = exportToDataLake(formName, formType,
          responseBytes.asByteArray());

      if (userMetadata.containsKey(TRAINEE_ID_METADATA_FIELD)
          && userMetadata.containsKey(LIFECYCLE_STATE_METADATA_FIELD)) {
        String traineeId = userMetadata.get(TRAINEE_ID_METADATA_FIELD);
        String lifecycleState = userMetadata.get(LIFECYCLE_STATE_METADATA_FIELD);
        broadcastFormEvent(formName, formType, traineeId, lifecycleState, formContentDto);
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
   * @param formName     The file name of the form.
   * @param formType     The form's type.
   * @param contentBytes The form content to upload.
   * @return the form content DTO.
   */
  private FormContentDto exportToDataLake(String formName, String formType, byte[] contentBytes) {
    DataLakeDirectoryClient directoryClient = createSubDirectories(formType);

    if (directoryClient == null) {
      return null;
    }

    FormContentDto formContentDtoClean = null;

    try {
      if (contentBytes.length > 0) {
        FormContentDto formContentDto = mapper.readValue(contentBytes, FormContentDto.class);
        formContentDtoClean = cleanFormContent(formContentDto);
        String cleanedString = mapper.writeValueAsString(formContentDtoClean);

        log.info("Exporting form {} of type {}.", formName, formType);
        dataLakeFacade.saveToDataLake(formName, cleanedString, directoryClient);
      } else {
        log.warn("Skipping empty form {} of type {}.", formName, formType);
      }
    } catch (IOException e) {
      log.warn("Unable to export content for form {} of type {}.", formName, formType);
    }
    return formContentDtoClean;
  }

  /**
   * Create the required subdirectories based on form type and current date.
   *
   * @param formType The form type being uploaded.
   * @return The directory client for the required subdirectory, or null if form type not supported.
   */
  private DataLakeDirectoryClient createSubDirectories(String formType) {
    DataLakeDirectoryClient directoryClient;

    switch (formType) {
      case "formr-a" -> directoryClient = dataLakeFacade
          .createSubDirectory(getDataLakeFormrRoot, "part-a");
      case "formr-b" -> directoryClient = dataLakeFacade
          .createSubDirectory(getDataLakeFormrRoot, "part-b");
      default -> {
        log.error("{} is not an exportable form type.", formType);
        return null;
      }
    }

    return dataLakeFacade.createYearMonthDaySubDirectories(directoryClient);
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
   * unchanged object.
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
   * @param formContentDto The form content.
   */
  private void broadcastFormEvent(String formName, String formType, String traineeId,
      String lifecycleState, FormContentDto formContentDto) {
    if (formContentDto != null) {
      log.info("Broadcasting event for form {}", formName);
      FormBroadcastEventDto formBroadcastEventDto = new FormBroadcastEventDto(
          formName, lifecycleState, traineeId, formType, Instant.now(), formContentDto);
      formBroadcastService.publishFormBroadcastEvent(formBroadcastEventDto);
    } else {
      log.warn("No content in form {} of type {}, skipping event broadcast.", formName, formType);
    }
  }
}

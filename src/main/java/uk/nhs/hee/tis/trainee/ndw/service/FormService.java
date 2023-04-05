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
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.ndw.FormEventDto;

/**
 * A service for processing S3 Form events.
 */
@Slf4j
@Service
public class FormService {

  private static final String FORM_TYPE_METADATA_FIELD = "formtype";
  private static final String NAME_METADATA_FIELD = "name";

  private static final String DATA_LAKE_FORMR_ROOT = "formr/bronze";

  private final AmazonS3 amazonS3;
  private final DataLakeFileSystemClient dataLakeClient;

  FormService(AmazonS3 amazonS3, DataLakeFileSystemClient dataLakeClient) {
    this.amazonS3 = amazonS3;
    this.dataLakeClient = dataLakeClient;
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
          .getDirectoryClient(DATA_LAKE_FORMR_ROOT)
          .createSubdirectoryIfNotExists("part-a");
      case "formr-b" -> directoryClient = dataLakeClient
          .getDirectoryClient(DATA_LAKE_FORMR_ROOT)
          .createSubdirectoryIfNotExists("part-b");
      default -> {
        log.error("{} is not an exportable form type.", formType);
        return;
      }
    }

    try (S3ObjectInputStream content = document.getObjectContent()) {
      log.info("Exporting form {} of type {}.", formName, formType);
      directoryClient
          .createFileIfNotExists(formName)
          .upload(content, document.getObjectMetadata().getContentLength(), true);
      log.info("Exported form {} of type {}.", formName, formType);
    } catch (IOException e) {
      log.warn("Unable to close stream for form {} of type {}.", formName, formType);
    }
  }
}

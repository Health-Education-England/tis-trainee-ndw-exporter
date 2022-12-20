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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.ndw.FormEventDto;

/**
 * A service for processing S3 Form events.
 */
@Slf4j
@Service
public class FormService {

  private static final String FORM_ID_FIELD = "_id";
  private static final String FORM_CLASS_FIELD = "_class";

  private final AmazonS3 amazonS3;
  private final ObjectMapper mapper;

  FormService(AmazonS3 amazonS3, ObjectMapper mapper) {
    this.amazonS3 = amazonS3;
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

    S3Object document = amazonS3.getObject(event.getBucket(), event.getKey());
    String latestVersionId = document.getObjectMetadata().getVersionId();

    if (!latestVersionId.equals(event.getVersionId())) {
      log.info("The form has been modified since the event was published, using latest content.");
      log.debug("Event version: {}\nLatest version: {}", event.getVersionId(), latestVersionId);
    }

    JsonNode content = mapper.readTree(document.getObjectContent());

    if (content.has(FORM_ID_FIELD) && content.has(FORM_CLASS_FIELD)) {
      // TODO: ID can be in different formats depending whether the form was ever saved as draft.
      String formId = content.get(FORM_ID_FIELD).textValue();
      String formType = content.get(FORM_CLASS_FIELD).textValue();
      log.info("Retrieved form id {} of type {}", formId, formType);
    } else {
      log.error("File {}/{} did not have the expected form structure.", event.getBucket(),
          event.getKey());
      throw new IOException("Unexpected document contents.");
    }
  }
}

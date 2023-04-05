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

package uk.nhs.hee.tis.trainee.ndw;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * A representation of a form event.
 */
@Data
public class FormEventDto {

  private String bucket;
  private String key;
  private String versionId;

  /**
   * Unpack an S3 event notification to get the FormEventDto properties.
   *
   * @param records The "Records" node of the S3 event notification.
   */
  @JsonProperty("Records")
  private void unpackRecord(JsonNode records) {
    if (records.size() > 1) {
      // S3 events are singular so this should never happen, but we want to know if it ever does.
      throw new UnsupportedOperationException("Multi-record events are not supported.");
    }

    JsonNode s3 = records.get(0).get("s3");
    bucket = s3.get("bucket").get("name").textValue();

    JsonNode object = s3.get("object");
    key = object.get("key").asText();
    versionId = object.get("versionId").textValue();
  }
}

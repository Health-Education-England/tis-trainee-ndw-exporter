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

package uk.nhs.hee.tis.trainee.ndw.dto;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class S3FormEventDtoTest {

  @Test
  void shouldThrowExceptionIfMultipleRecords() {
    String json = """
        {
          "Records": [
            {},
            {}
          ]
        }
        """;

    ObjectMapper mapper = new ObjectMapper();
    assertThrows(IOException.class, () -> mapper.readValue(json, FormEventDto.class));
  }

  @Test
  void shouldUnpackRecord() throws JsonProcessingException {
    String json = """
        {
          "Records":[
            {
              "eventVersion":"2.1",
              "eventSource":"aws:s3",
              "awsRegion":"eu-west-2",
              "eventTime":"2022-12-15T15:51:25.372Z",
              "eventName":"ObjectCreated:Put",
              "userIdentity":{
                "principalId":"AWS:code:guid"
              },
              "requestParameters":{
                "sourceIPAddress":"1.1.1.1"
              },
              "responseElements":{
                "x-amz-request-id":"REQ_ID",
                "x-amz-id-2":"ANOTHER_ID"
              },
              "s3":{
                "s3SchemaVersion":"1.0",
                "configurationId":"tf-s3-topic-20221214190721304400000001",
                "bucket":{
                  "name":"tis-test-bucket",
                  "ownerIdentity":{
                    "principalId":"PRINCIPLE"
                  },
                  "arn":"arn:aws:s3:::tis-trainee-documents-test"
                },
                "object":{
                  "key":"47165/forms/formr-a/file.json",
                  "size":1081,
                  "eTag":"TAG",
                  "versionId":"bYTJtCjXR_S2apA.8IqpxiVm_zM9HUkM",
                  "sequencer":"SEQUENCER"
                }
              }
            }
          ]
        }
        """;

    ObjectMapper mapper = new ObjectMapper();
    S3FormEventDto dto = mapper.readValue(json, S3FormEventDto.class);

    assertThat("Unexpected bucket.", dto.getBucket(), is("tis-test-bucket"));
    assertThat("Unexpected key.", dto.getKey(), is("47165/forms/formr-a/file.json"));
    assertThat("Unexpected version id.", dto.getVersionId(),
        is("bYTJtCjXR_S2apA.8IqpxiVm_zM9HUkM"));
  }
}

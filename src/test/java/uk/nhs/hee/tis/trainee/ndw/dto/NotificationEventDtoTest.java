/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotificationEventDtoTest {

  @Test
  void shouldThrowExceptionIfInvalidRecord() {
    String json = """
        {
          "id": "validId",
          "tisReference": "invalid"
        }
        """;

    ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    assertThrows(IOException.class, () -> mapper.readValue(json, NotificationEventDto.class));
  }

  @Test
  void shouldUnpackRecord() throws JsonProcessingException {
    String json = """
        {
          "id": "666af7a19ae0a125e169fce1",
          "tisReference": {
            "id": "09460f82-a305-41fd-b212-ae8caa3c70f3",
            "type": "PROGRAMME_MEMBERSHIP"
          },
          "type": "PROGRAMME_CREATED",
          "recipient": {
            "id": "123456",
            "type": "EMAIL",
            "contact": "test@test.com"
          },
          "template": {
            "name": "programme-created",
            "version": "v1.0.0",
            "variables": {
              "contactHref": "url",
              "hashedEmail": "f2cb7e855b7a3ef8a31d3bf2f112b8b3",
              "givenName": "Bob",
              "programmeNumber": "EOE8945",
              "gmcNumber": "102030405",
              "notificationType": "PROGRAMME_CREATED",
              "isValidGmc": true,
              "programmeName": "Cardiology",
              "title": "Dr",
              "tisId": "09460f82-a305-41fd-b212-ae8caa3c70f3",
              "localOfficeWebsite": "https://heeoe.hee.nhs.uk/medical-training/trainee-well-being-hub",
              "familyName": "Tester",
              "domain": "",
              "localOfficeName": "Health Education England East of England",
              "personId": "1234567",
              "isRegistered": false,
              "localOfficeContact": "https://heeoe.hee.nhs.uk/medical-training/trainee-well-being-hub",
              "startDate": "2024-09-01T23:00:00.000Z",
              "email": "test@email.com"
            }
          },
          "sentAt": "2024-06-13T13:44:02.048Z",
          "status": "SENT"
        }
        """;

    ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    NotificationEventDto dto = mapper.readValue(json, NotificationEventDto.class);

    assertThat("Unexpected id.", dto.id(), is("666af7a19ae0a125e169fce1"));
    assertThat("Unexpected tis reference id.", dto.tisReference().id(),
        is("09460f82-a305-41fd-b212-ae8caa3c70f3"));
    assertThat("Unexpected recipient id.", dto.recipient().id(), is("123456"));
    assertThat("Unexpected sent at.", dto.sentAt(),
        is(Instant.parse("2024-06-13T13:44:02.048Z")));
    assertThat("Unexpected status.", dto.status(), is("SENT"));
  }

}

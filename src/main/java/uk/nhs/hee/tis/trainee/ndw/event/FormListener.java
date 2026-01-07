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

package uk.nhs.hee.tis.trainee.ndw.event;

import io.awspring.cloud.sqs.annotation.SqsListener;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.ndw.dto.FormContentDto;
import uk.nhs.hee.tis.trainee.ndw.dto.JsonFormEventDto;
import uk.nhs.hee.tis.trainee.ndw.service.FormBroadcastService;
import uk.nhs.hee.tis.trainee.ndw.service.FormService;

/**
 * A listener for Form Events.
 */
@Slf4j
@Component
public class FormListener {

  private final FormService<JsonFormEventDto> jsonFormService;
  private final FormBroadcastService formBroadcastService;

  FormListener(
      FormService<JsonFormEventDto> jsonFormService, FormBroadcastService formBroadcastService) {
    this.jsonFormService = jsonFormService;
    this.formBroadcastService = formBroadcastService;
  }

  /**
   * Listen for LTFT Events on the SQS queue.
   *
   * @param event the LTFT Event
   * @throws IOException when the form contents could not be read, or were not correctly
   *                     structured.
   */
  @SqsListener(value = "${application.aws.sqs.form.ltft}")
  void getLtftFormEvent(JsonFormEventDto event) throws IllegalArgumentException, IOException {
    String id = (String) event.fields.get("id");

    if (Strings.isBlank(id)) {
      throw new IllegalArgumentException("ID must not be null.");
    }

    log.debug("Received LTFT event for form ID {}.", id);
    event.setFormName(id + ".json");
    event.setFormType("ltft");
    jsonFormService.processFormEvent(event);
  }

  @SqsListener(value = "${application.aws.sqs.form.formr}")
  void getFormRFormEvent(Message<JsonFormEventDto> message)
      throws IllegalArgumentException, IOException {
    JsonFormEventDto event = message.getPayload();
    MessageHeaders attributes = message.getHeaders();
    String id = (String) event.fields.get("id");

    if (Strings.isBlank(id)) {
      throw new IllegalArgumentException("ID must not be null.");
    }

    log.debug("Received FormR event for form ID {}.", id);
    String formName = id + ".json";
    event.setFormName(formName);
    if (attributes.get("formType") == null) {
      throw new IllegalArgumentException("Trigger attribute must not be null.");
    }

    String formType = (String) attributes.get("formType"); //should be formr-a or formr-b
    String traineeId = (String) event.fields.get("traineeTisId");
    String lifecycleState = (String) event.fields.get("lifecycleState");
    event.setFormType(formType);
    FormContentDto formContentDto = jsonFormService.processFormEvent(event);
    formBroadcastService.broadcastFormEvent(formName, formType, traineeId, lifecycleState,
        formContentDto);
  }
}

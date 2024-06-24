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

package uk.nhs.hee.tis.trainee.ndw.event;

import io.awspring.cloud.sqs.annotation.SqsListener;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.ndw.dto.ActionEventDto;
import uk.nhs.hee.tis.trainee.ndw.service.ActionService;


/**
 * A listener for Action Events.
 */
@Slf4j
@Component
public class ActionListener {

  private final ActionService actionService;

  ActionListener(ActionService actionService) {
    this.actionService = actionService;
  }

  /**
   * Listen for Action Events on the SQS queue.
   *
   * @param event the Action Event
   * @throws IOException when the action contents could not be read, or were not correctly
   *                     structured.
   */
  @SqsListener(value = "${application.aws.sqs.action}")
  void getActionEvent(ActionEventDto event) throws IOException {
    log.debug("Received action event {}.", event);
    actionService.processActionEvent(event);
  }
}

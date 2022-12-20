package uk.nhs.hee.tis.trainee.ndw.event;

import static io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.ndw.FormEventDto;
import uk.nhs.hee.tis.trainee.ndw.service.FormService;

/**
 * A listener for S3 Form Events.
 */
@Slf4j
@Component
public class FormListener {

  private final FormService formService;

  FormListener(FormService formService) {
    this.formService = formService;
  }

  /**
   * Listen for S3 Events on the SQS queue.
   *
   * @param event the S3 Event
   * @throws IOException when the form contents could not be read, or were not correctly structured.
   */
  @SqsListener(value = "${application.aws.sqs.form}", deletionPolicy = ON_SUCCESS)
  void getFormEvent(FormEventDto event) throws IOException {
    log.debug("Received form event {}.", event);
    formService.processFormEvent(event);
  }
}

package uk.nhs.hee.tis.trainee.ndw.event;

import static io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

import com.amazonaws.services.s3.event.S3EventNotification;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
   */
  @SqsListener(value = "${application.aws.sqs.form}", deletionPolicy = ON_SUCCESS)
  void getS3Event(S3EventNotification event) {
    log.debug("Received S3 event message {}.", event.toJson());
    formService.processFormEvent(event);
  }

}

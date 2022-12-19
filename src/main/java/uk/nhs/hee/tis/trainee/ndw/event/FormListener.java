package uk.nhs.hee.tis.trainee.ndw.event;

import static io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

import com.amazonaws.services.s3.event.S3EventNotification;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.ndw.service.FormService;

@Slf4j
@Component
public class FormListener {

  private final FormService formService;

  FormListener(FormService formRService) {
    this.formService = formRService;
  }

  @SqsListener(value = "${application.aws.sqs.form}", deletionPolicy = ON_SUCCESS)
  void getS3Event(/* Message message */ S3EventNotification event) {
    //log.debug("Received Message {}.", message);
    //S3EventNotification event = S3EventNotification.parseJson(message.getBody());
    log.debug("Parsed to S3 event {}.", event.toJson());

    if (event.getRecords() == null) {
      return;
    }
//  if (event.getRecords().size() > 0) {
//    String bucket = event.getRecords().get(0).getS3().getBucket().getName();
//    String key = event.getRecords().get(0).getS3().getObject().getKey();
//  }

    formService.processS3Event(event);
  }

}

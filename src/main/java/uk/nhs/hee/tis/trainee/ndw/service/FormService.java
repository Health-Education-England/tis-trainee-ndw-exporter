package uk.nhs.hee.tis.trainee.ndw.service;

import com.amazonaws.services.s3.event.S3EventNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * A service for processing S3 Form events.
 */
@Slf4j
@Service
public class FormService {

  private final ApplicationContext context;

  FormService(ApplicationContext context) {
    this.context = context;
  }

  /**
   * Process the given S3 Event.
   *
   * @param event The S3 event to process.
   */
  public void processFormEvent(S3EventNotification event) {
    log.info("Now process S3 event");
  }
}

package uk.nhs.hee.tis.trainee.ndw.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.s3.event.S3EventNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Test class for the Form Service.
 */
class FormServiceTest {

  private FormService service;

  private ApplicationContext context;

  @BeforeEach
  void setUp() {
    context = mock(ApplicationContext.class);
    service = new FormService(context);
  }

  @Test
  void shouldNotThrowExceptionWhenFormEventIsNull() {
    //placeholder test
    S3EventNotification event = null;

    assertDoesNotThrow(() -> service.processFormEvent(event));
  }
}

package uk.nhs.hee.tis.trainee.ndw.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.ndw.FormEventDto;
import uk.nhs.hee.tis.trainee.ndw.service.FormService;

class FormListenerTest {

  private FormListener listener;
  private FormService service;

  @BeforeEach
  void setUp() {
    service = mock(FormService.class);
    listener = new FormListener(service);
  }

  @Test
  void shouldProcessEvent() throws IOException {
    FormEventDto event = new FormEventDto();

    listener.getFormEvent(event);

    verify(service).processFormEvent(event);
  }
}

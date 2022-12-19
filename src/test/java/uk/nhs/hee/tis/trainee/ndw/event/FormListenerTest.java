package uk.nhs.hee.tis.trainee.ndw.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.util.json.Jackson;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  void shouldProcessEvent() {

    String json =
        """
        {"eventVersion":"2.1","eventSource":"aws:s3","awsRegion":"eu-west-2","eventTime":"2022-12-15T15:51:25.372Z","eventName":"ObjectCreated:Put","userIdentity":{"principalId":"AWS:code:guid"},"requestParameters":{"sourceIPAddress":"1.1.1.1"},"responseElements":{"x-amz-request-id":"REQ_ID","x-amz-id-2":"ANOTHER_ID"},"s3":{"s3SchemaVersion":"1.0","configurationId":"tf-s3-topic-20221214190721304400000001","bucket":{"name":"tis-test-bucket","ownerIdentity":{"principalId":"PRINCIPLE"},"arn":"arn:aws:s3:::tis-trainee-documents-test"},"object":{"key":"47165/forms/formr-a/file.json","size":1081,"eTag":"TAG","versionId":"bYTJtCjXR_S2apA.8IqpxiVm_zM9HUkM","sequencer":"SEQUENCER"}}}]
        """;
    S3EventNotification.S3EventNotificationRecord record
        = Jackson.fromJsonString(json, S3EventNotification.S3EventNotificationRecord.class);
    S3EventNotification event = new S3EventNotification(Collections.singletonList(record));

    listener.getS3Event(event);

    verify(service).processS3Event(event);
  }
}

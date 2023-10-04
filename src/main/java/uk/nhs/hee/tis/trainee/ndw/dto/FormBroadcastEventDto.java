package uk.nhs.hee.tis.trainee.ndw.dto;

import lombok.Data;
import org.joda.time.Instant;

/**
 * A DTO for broadcasting form update events (submitted/unsubmitted).
 */
@Data
public class FormBroadcastEventDto { //TODO: use Record?
  private String formName;
  private String lifecycleState;
  private String traineeId;
  private String formType;
  private Instant eventDate;
  //all metadata fields from form S3 metadata except eventDate
}

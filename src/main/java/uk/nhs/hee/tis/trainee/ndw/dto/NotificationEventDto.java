package uk.nhs.hee.tis.trainee.ndw.dto;

import java.time.Instant;
import java.util.Map;

/**
 * A representation of the notification event.
 *
 * @param id           The id.
 * @param tisReference The TIS reference.
 * @param type         The type.
 * @param recipient    The recipient.
 * @param template     The template content.
 * @param sentAt       When sent.
 * @param readAt       When read.
 * @param status       The notification status.
 * @param statusDetail Details of the notification status.
 * @param lastRetry    When last retried (for email).
 */
public record NotificationEventDto(
    String id,
    TisReferenceInfo tisReference,
    String type,
    RecipientInfo recipient,
    TemplateInfo template,
    Instant sentAt,
    Instant readAt,
    String status,
    String statusDetail,
    Instant lastRetry) {

  /**
   * A representation of a notified recipient.
   *
   * @param id      The identifier of the recipient.
   * @param type    The type of message sent.
   * @param contact The contact details used to send the notification.
   */
  public record RecipientInfo(String id, String type, String contact) {

  }

  /**
   * A representation of the template information used to generate a notification.
   *
   * @param name      The name of the template.
   * @param version   The version of the template.
   * @param variables The variables to process with the template.
   */
  public record TemplateInfo(String name, String version, Map<String, Object> variables) {

  }

  /**
   * A representation of the TIS record that prompted the notification.
   *
   * @param type The TIS reference type for the entity that prompted the notification.
   * @param id   The TIS ID of the entity that prompted the notification.
   */
  public record TisReferenceInfo(String type, String id) {

  }
}
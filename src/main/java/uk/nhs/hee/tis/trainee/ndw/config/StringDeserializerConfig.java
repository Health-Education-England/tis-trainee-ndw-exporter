package uk.nhs.hee.tis.trainee.ndw.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import java.io.IOException;
import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class StringDeserializerConfig extends StringDeserializer {

  @Override
  public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String value = super.deserialize(p, ctxt);
    return value != null ? value.stripTrailing() : null;
  }
}

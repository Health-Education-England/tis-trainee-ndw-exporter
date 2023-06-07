package uk.nhs.hee.tis.trainee.ndw.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;

public class FormContentDto {

  @JsonIgnore
  public Map<String, Object> fields = new HashMap<>();

  // "any getter" needed for serialization
  @JsonAnyGetter
  public Map<String,Object> any() {
    return fields;
  }

  @JsonAnySetter
  public void set(String name, Object value) {
    fields.put(name, value);
  }
}

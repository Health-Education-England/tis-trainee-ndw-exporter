/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.ndw.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.ndw.dto.FormContentDto;
import uk.nhs.hee.tis.trainee.ndw.dto.JsonFormEventDto;

/**
 * A service for processing raw JSON form events.
 */
@Slf4j
@Service
public class JsonFormService extends AbstractFormService<JsonFormEventDto> {

  /**
   * Initialise the form service.
   *
   * @param dataLakeFacade The data lake service to use.
   * @param directory      The root directory.
   * @param mapper         The object mapper to use.
   */
  JsonFormService(DataLakeFacade dataLakeFacade,
      @Value("${application.ndw.directory}") String directory, ObjectMapper mapper) {
    super(dataLakeFacade, directory, mapper);
  }

  @Override
  public FormContentDto processFormEvent(JsonFormEventDto event) {
    log.info("Processing form event for {}", event);
    return exportToDataLake(event.getFormName(), event.getFormType(), event);
  }
}

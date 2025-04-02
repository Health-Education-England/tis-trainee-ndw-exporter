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

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import uk.nhs.hee.tis.trainee.ndw.dto.FormContentDto;
import uk.nhs.hee.tis.trainee.ndw.dto.FormEventDto;

/**
 * An abstract for shared FormService behaviour.
 */
@Slf4j
public abstract class AbstractFormService<T extends FormEventDto> implements FormService<T> {

  private final DataLakeFacade dataLakeFacade;

  private final String dataLakeRoot;

  private final ObjectMapper mapper;

  /**
   * Initialise the form service.
   *
   * @param directory      The root directory.
   * @param mapper         The object mapper to use.
   * @param dataLakeFacade The data lake service to use.
   */
  AbstractFormService(DataLakeFacade dataLakeFacade,
      @Value("${application.ndw.directory}") String directory, ObjectMapper mapper) {
    this.dataLakeRoot = directory;
    this.dataLakeFacade = dataLakeFacade;
    this.mapper = mapper;
  }

  /**
   * Export a form to the data lake.
   *
   * @param formName     The file name of the form.
   * @param formType     The form's type.
   * @param contentBytes The form content to upload.
   * @return the form content DTO.
   */
  public FormContentDto exportToDataLake(String formName, String formType, byte[] contentBytes) {
    DataLakeDirectoryClient directoryClient = createSubDirectories(formType);

    if (directoryClient == null) {
      return null;
    }

    FormContentDto formContentDtoClean = null;

    try {
      if (contentBytes.length > 0) {
        FormContentDto formContentDto = mapper.readValue(contentBytes, FormContentDto.class);
        formContentDtoClean = cleanFormContent(formContentDto);
        String cleanedString = mapper.writeValueAsString(formContentDtoClean);

        log.info("Exporting form {} of type {}.", formName, formType);
        dataLakeFacade.saveToDataLake(formName, cleanedString, directoryClient);
      } else {
        log.warn("Skipping empty form {} of type {}.", formName, formType);
      }
    } catch (IOException e) {
      log.warn("Unable to export content for form {} of type {}.", formName, formType);
    }
    return formContentDtoClean;
  }

  /**
   * Create the required subdirectories based on form type and current date.
   *
   * @param formType The form type being uploaded.
   * @return The directory client for the required subdirectory, or null if form type not supported.
   */
  private DataLakeDirectoryClient createSubDirectories(String formType) {
    DataLakeDirectoryClient directoryClient;

    switch (formType) {
      case "formr-a" -> directoryClient = dataLakeFacade
          .createSubDirectory(dataLakeRoot, "part-a");
      case "formr-b" -> directoryClient = dataLakeFacade
          .createSubDirectory(dataLakeRoot, "part-b");
      default -> {
        log.error("{} is not an exportable form type.", formType);
        return null;
      }
    }

    return dataLakeFacade.createYearMonthDaySubDirectories(directoryClient);
  }

  /**
   * Clean form content to remove trailing whitespace in text fields.
   *
   * @param formContentDto the dirty form content Dto.
   * @return a cleaned FormContentDto object.
   */
  private FormContentDto cleanFormContent(FormContentDto formContentDto) {
    FormContentDto cleanFormContentDto = new FormContentDto();
    formContentDto.fields.forEach((f, v)
        -> cleanFormContentDto.fields.put(f, removeTrailingWhitespace(v)));
    return cleanFormContentDto;
  }

  /**
   * Remove trailing whitespace from a string object.
   *
   * @param o the object to process.
   * @return a copy of the object with trailing whitespace removed if it is a string, otherwise the
   *     unchanged object.
   */
  private Object removeTrailingWhitespace(Object o) {
    if (o instanceof String s) {
      return s.stripTrailing();
    } else if (o instanceof HashMap<?, ?> h) {
      HashMap<String, Object> hashMap = new HashMap<>();
      h.forEach((f, v) -> hashMap.put((String) f, removeTrailingWhitespace(v)));
      return hashMap;
    }
    return o;
  }
}

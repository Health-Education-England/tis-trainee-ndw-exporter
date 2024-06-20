/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.ndw.dto.NotificationEventDto;

/**
 * A service for processing Notification events.
 */
@Slf4j
@Service
public class NotificationService {

  protected static final String DATALAKE_NOTIFICATIONS_ROOT = "notifications";

  private final DataLakeFacade dataLakeFacade;

  private final String dataLakeRoot;

  private final ObjectMapper mapper;

  NotificationService(@Value("${application.ndw.directory}") String directory, ObjectMapper mapper,
      DataLakeFacade dataLakeFacade) {
    this.dataLakeRoot = directory;
    this.mapper = mapper;
    this.dataLakeFacade = dataLakeFacade;
  }

  /**
   * Process the given Notification Event.
   *
   * @param event The Notification event to process.
   */
  public void processNotificationEvent(NotificationEventDto event) throws JsonProcessingException {
    if (event != null) {
      String type = event.type();
      String id = event.id();
      String status = event.status();
      if (id != null) {
        DataLakeDirectoryClient directoryClient = createSubDirectories();

        log.info("Exporting notification event {} (type {}, {})", id, type, status);

        String eventString = mapper.writeValueAsString(event);
        dataLakeFacade.saveToDataLake(id, eventString, directoryClient);
      } else {
        log.warn("No notification id: {}.", event);
      }
    } else {
      log.warn("No content in notification.");
    }

  }

  /**
   * Create the required subdirectories based on current date.
   *
   * @return The directory client for the required subdirectory.
   */
  private DataLakeDirectoryClient createSubDirectories() {
    DataLakeDirectoryClient directoryClient
        = dataLakeFacade.createSubDirectory(dataLakeRoot, DATALAKE_NOTIFICATIONS_ROOT);
    return dataLakeFacade.createYearMonthDaySubDirectories(directoryClient);
  }
}

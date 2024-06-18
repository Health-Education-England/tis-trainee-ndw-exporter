package uk.nhs.hee.tis.trainee.ndw.service;

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * A facade for interactions with the data lake.
 */
@Slf4j
@Service
public class DataLakeFacade {

  private final DataLakeFileSystemClient dataLakeClient;

  DataLakeFacade(DataLakeFileSystemClient dataLakeClient) {
    this.dataLakeClient = dataLakeClient;
  }

  /**
   * Create the required year-month-day subdirectories based on current date.
   *
   * @return The directory client for the day subdirectory.
   */
  public DataLakeDirectoryClient createYearMonthDaySubDirectories(
      DataLakeDirectoryClient root) {
    Instant now = Instant.now();
    ZoneId utcZone = ZoneId.of("UTC");

    return root
        .createSubdirectoryIfNotExists(
            DateTimeFormatter.ofPattern("'year='yyyy").withZone(utcZone).format(now))
        .createSubdirectoryIfNotExists(
            DateTimeFormatter.ofPattern("'month='yyyyMM").withZone(utcZone).format(now))
        .createSubdirectoryIfNotExists(
            DateTimeFormatter.ofPattern("'day='yyyyMMdd").withZone(utcZone).format(now));
  }

  /**
   * Create the required subdirectory.
   *
   * @return The directory client for the required subdirectory.
   */
  public DataLakeDirectoryClient createSubDirectory(String root, String name) {

    return dataLakeClient
        .getDirectoryClient(root)
        .createSubdirectoryIfNotExists(name);
  }

  /**
   * Save a file to the data lake.
   *
   * @param filename The filename to use.
   * @param content  The file content.
   */
  public void saveToDataLake(String filename, String content,
      DataLakeDirectoryClient directoryClient) {
    byte[] cleanedBytes = content.getBytes(StandardCharsets.UTF_8);

    ByteArrayInputStream cleanStream = new ByteArrayInputStream(cleanedBytes);
    directoryClient
        .createFileIfNotExists(filename)
        .upload(cleanStream, cleanedBytes.length, true);
    log.info("Exported file {} to path {}.", filename, directoryClient.getDirectoryPath());
  }
}

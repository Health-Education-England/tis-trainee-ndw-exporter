package uk.nhs.hee.tis.trainee.ndw.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class DataLakeFacadeTest {

  private DataLakeFileSystemClient dataLakeClient;
  DataLakeDirectoryClient directoryClient;
  private DataLakeFacade facade;

  @BeforeEach
  void setUp() {
    directoryClient = mock(DataLakeDirectoryClient.class);
    dataLakeClient = mock(DataLakeFileSystemClient.class);
    facade = new DataLakeFacade(dataLakeClient);
  }

  @Test
  void shouldCreateYearMonthDaySubDirectories() {
    when(directoryClient.createSubdirectoryIfNotExists(any())).thenReturn(directoryClient);

    facade.createYearMonthDaySubDirectories(directoryClient);

    verify(directoryClient, times(3)).createSubdirectoryIfNotExists(any());

    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
    int year = now.getYear();
    String month = String.format("%02d", now.getMonthValue());

    InOrder orderVerifier = inOrder(directoryClient);
    orderVerifier.verify(directoryClient)
        .createSubdirectoryIfNotExists("year=" + year);
    orderVerifier.verify(directoryClient)
        .createSubdirectoryIfNotExists("month=" + year + month);
    orderVerifier.verify(directoryClient)
        .createSubdirectoryIfNotExists("day=" + year + month + now.getDayOfMonth());
  }

  @Test
  void shouldCreateSubDirectory() {
    when(directoryClient.createSubdirectoryIfNotExists(any())).thenReturn(directoryClient);
    when(dataLakeClient.getDirectoryClient(any())).thenReturn(directoryClient);

    facade.createSubDirectory("root", "directory");

    verify(dataLakeClient).getDirectoryClient("root");
    verify(directoryClient).createSubdirectoryIfNotExists("directory");
  }

  @Test
  void shouldSaveToDataLake() throws IOException {
    DataLakeFileClient fileClient = mock(DataLakeFileClient.class);
    when(directoryClient.createFileIfNotExists("filename")).thenReturn(fileClient);

    //content includes a multibyte character to test that length is correctly calculated
    String contents = "{\"field1\":\"value1ท\"}";

    facade.saveToDataLake("filename", contents, directoryClient);

    ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
    long contentsLength = contents.getBytes(StandardCharsets.UTF_8).length;
    verify(fileClient).upload(streamCaptor.capture(), eq(contentsLength), eq(true));

    InputStream uploadedStream = streamCaptor.getValue();
    String uploadedContent = new String(uploadedStream.readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(contents, uploadedContent);
  }
}

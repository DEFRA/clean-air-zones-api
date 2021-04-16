package uk.gov.caz.vcc.service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.vcc.domain.service.UkVrnTestingService;

/**
 * A test class for performing VRN analysis. To use this test, please uncomment the disabled attribute below.
 *
 */
@Disabled
@ExtendWith(MockitoExtension.class)
public class DataAnalysisTest {

  FileWriter csvWriter;

  static final String INPUT_FILE_NAME = "/input.csv";
  static final String OUTPUT_FILE_NAME = "output.csv";
  
  /**
   * Prepare a formatted CSV output file in which analysis results to be captured.
   * @throws IOException exception encountered when attempting to write the output CSV file with headers.
   */
  @BeforeAll
  static void prepareFileAndCsvHeaders() throws IOException {
    Path fileToDeletePath = Paths.get(OUTPUT_FILE_NAME);
    try {
      Files.delete(fileToDeletePath); 
    } catch(IOException io) {}
    
    FileWriter csvWriter = new FileWriter(OUTPUT_FILE_NAME, true);
    csvWriter.append("Is recognised UK format?");
    csvWriter.append(",");
    csvWriter.append("Would be recognised UK format without leading 0?");
    csvWriter.append(",");
    csvWriter.append("Is potential diplomatic embassy vehicle?");
    csvWriter.append(",");
    csvWriter.append("Commonly misread by ANPR cameras?");
    csvWriter.append(",");
    csvWriter.append("Other");
    csvWriter.append(",");
    csvWriter.append("Contains Q?");
    csvWriter.append(",");
    csvWriter.append("Contains 01?");
    csvWriter.append(",");
    csvWriter.append("Contains I?");
    csvWriter.append(",");
    csvWriter.append("Contains Z in first two letters?");
    csvWriter.append(",");
    csvWriter.append("Contains double zero?");
    csvWriter.append("\n");
    
    
    
    csvWriter.flush();
    csvWriter.close();
  }
  
  @BeforeEach
  void init() throws IOException {
    csvWriter = new FileWriter(OUTPUT_FILE_NAME, true);
  }

  @AfterEach
  void post() throws IOException {
    csvWriter.flush();
    csvWriter.close();
  }

  /**
   * Test to consume an input CSV under the given name and process to an output
   * file.
   * 
   * An input file is required on the test/resources path which contains a
   * single column of VRNs (with a header row). This can be done with a simple
   * copy command (example assumes vagrant vm with mapped drive)
   * 
   * cp /vagrant/input.csv ./src/test/resources/input.csv
   * 
   * Maven can then be used to run a scoped test (i.e. just this test)
   * 
   * mvn clean test -Dtest=DataAnalysisTest
   * 
   * The analysis output can then be copied back out to a secure host using the
   * below command (again assuming use of vagrant)
   * 
   * cp output.csv /vagrant
   * 
   * @param vrn a row in the CSV containing a VRN.
   * @throws Exception an exception encountered in parsing or wrting a file
   *         input/output.
   */
  @ParameterizedTest
  @CsvFileSource(resources = INPUT_FILE_NAME, numLinesToSkip = 1)
  public void testVrns(String vrn) throws Exception {
    
    boolean reasonAlreadyIdentified = false;
    
    csvWriter.append(UkVrnTestingService.isPotentialUkVrn(vrn) ? "Yes" : "No");
    csvWriter.append(",");
    
    // Test for plates that could be valid if they were void of a leading 0
    if(vrn.startsWith("0")) {
      String trimmedVrn = vrn.substring(1);
      csvWriter.append(UkVrnTestingService.isPotentialUkVrn(trimmedVrn) ? "Yes" : "No"); 
      
      if (UkVrnTestingService.isPotentialUkVrn(trimmedVrn)) {
        reasonAlreadyIdentified = true;
      }
    } else {
      csvWriter.append("No"); 
    }
    csvWriter.append(",");
    
    // Check if plate length is 7 digits and fourth character is X or D to identify diplomatic embassy vehicles
    // See https://www.mg-cars.org.uk/imgytr/pdf/ukregistrationplates.pdf for information on format.
    boolean isPotentialDiplomaticEmbassyVehicle;
    
    try {
      // Note the 4th character appears at position 3 as this is 0 indexed
      char fourthCharacter = vrn.charAt(3);
      isPotentialDiplomaticEmbassyVehicle = (vrn.length() == 7 && (fourthCharacter == 'X' || fourthCharacter == 'D'));
      
      if (isPotentialDiplomaticEmbassyVehicle && !reasonAlreadyIdentified) {
        csvWriter.append("Yes");
        reasonAlreadyIdentified = true;
      } else {
        csvWriter.append("No");
      }
    } catch (StringIndexOutOfBoundsException ex) {
      // Write value of No to CSV output as plate in case where out of bounds is found at 
      // plate is not long enough to be a diplomatic embassy plate
      csvWriter.append("No");
    }
    
    csvWriter.append(",");
    
    // Common misread candidate (https://uhra.herts.ac.uk/bitstream/handle/2299/10162/Real_world_number_plates_final.pdf;jsessionid=9925ED59ABC1EF52A4C22A46AE0D8F61?sequence=1)
    String[] commonMisreadCharacters = new String[] { "G", "S", "O", "C", "4", "D", "K", "7", "M" };
    boolean commonlyMisread = Arrays.stream(commonMisreadCharacters).anyMatch(vrn::contains);
    
    if (commonlyMisread && !reasonAlreadyIdentified) {
      csvWriter.append("Yes");
      reasonAlreadyIdentified = true;
    } else {
      csvWriter.append("No");
    }
    
    csvWriter.append(",");
    csvWriter.append(reasonAlreadyIdentified ? "No" : "Yes");
    csvWriter.append(",");
    
    // No VRNs should ever contain Q
    boolean containsQ = vrn.contains("Q");
    
    if (!reasonAlreadyIdentified && containsQ) {
      csvWriter.append("Yes");
      reasonAlreadyIdentified = true;
    } else {
      csvWriter.append("No");
    }
    csvWriter.append(",");
    
    // No VRNs should ever contain 00
    boolean containsZeroOne = vrn.contains("01");
    
    if (!reasonAlreadyIdentified && containsZeroOne) {
      csvWriter.append("Yes");
      reasonAlreadyIdentified = true;
    } else {
      csvWriter.append("No");
    }
    csvWriter.append(",");
    
    // Contains I – Not issues since 1963
    boolean containsI= vrn.contains("I");
    
    if (!reasonAlreadyIdentified && containsI) {
      csvWriter.append("Yes");
      reasonAlreadyIdentified = true;
    } else {
      csvWriter.append("No");
    }
    csvWriter.append(",");
    
    // Contains Z in the first two letters – Not issued since 2001
    boolean containsZFirstTwoLetters;
    try {
      char firstCharacter = vrn.charAt(0);
      char secondCharacter = vrn.charAt(1);
      containsZFirstTwoLetters = firstCharacter == 'Z' || secondCharacter == 'Z';
      if (!reasonAlreadyIdentified && containsZFirstTwoLetters) {
        csvWriter.append("Yes");
        reasonAlreadyIdentified = true;
      } else {
        csvWriter.append("No");
      }
    } catch (StringIndexOutOfBoundsException ex) {
      // Write value of No to CSV output as plate in case where out of bounds is found at 
      // plate is not long enough to be a diplomatic embassy plate
      csvWriter.append("No");
    }
    csvWriter.append(",");
    
    // Cannot contain 00 – not issued since September 2001
    boolean containsDoubleZero= vrn.contains("00");
    
    if (!reasonAlreadyIdentified && containsDoubleZero) {
      csvWriter.append("Yes");
      reasonAlreadyIdentified = true;
    } else {
      csvWriter.append("No");
    }
    csvWriter.append(",");  
    csvWriter.append("\n");
  }

}
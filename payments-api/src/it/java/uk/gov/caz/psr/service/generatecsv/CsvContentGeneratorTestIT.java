package uk.gov.caz.psr.service.generatecsv;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.psr.ExternalCallsIT;
import uk.gov.caz.psr.annotation.IntegrationTest;

@IntegrationTest
@Sql(scripts = {"classpath:data/sql/clear-all-payments.sql",
    "classpath:data/sql/csv-export/test-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-all-payments.sql",
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class CsvContentGeneratorTestIT extends ExternalCallsIT {

  @Autowired
  private CsvContentGenerator csvGeneratorService;

  @Test
  public void shouldGetAllCsvRowsWithHeaderForAccount() {
    // given
    UUID accountId = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67a");
    UUID accountUserId = null;
    mockVccsCleanAirZonesCall();
    mockAccountServiceGetAllUsersCall(accountId.toString(), 200);

    // when
    List<String[]> csvRowResults = csvGeneratorService.generateCsvRows(accountId, accountUserId);

    // then
    assertThat(csvRowResults).hasSize(8);
    assertThat(String.join(",", csvRowResults.get(0))).isEqualTo(
        "Date of payment,Payment made by,Clean Air Zone,Number plate,Date of entry,Charge,"
            + "Payment reference,GOV.UK payment ID,Entries paid for,Total amount paid,"
            + "Status,Date received from local authority,Case reference");
    assertThat(String.join(",", csvRowResults.get(1))).isEqualTo(
        "2019-11-25,Jan Kowalski,Birmingham,RD84VSX,2019-11-06,£28.00,1881,ext-payment-id-3,1,"
            + "£28.00,,,");
    assertThat(String.join(",", csvRowResults.get(2))).isEqualTo(
        "2019-11-24,Deleted user,Birmingham,PD84VSX,2019-11-04,£11.00,998,ext-payment-id-2,2,"
            + "£37.00,CHARGEBACK," + LocalDate.now().toString() + ",");
    assertThat(String.join(",", csvRowResults.get(3))).isEqualTo(
        "2019-11-24,Deleted user,Birmingham,QD84VSX,2019-11-05,£26.00,998,ext-payment-id-2,2,"
            + "£37.00,,,");
    assertThat(String.join(",", csvRowResults.get(4))).isEqualTo(
        "2019-11-23,Administrator,Birmingham,ND84VSX,2019-11-01,£8.00,87,ext-payment-id-1,4,£35.00,"
            + "REFUNDED," + LocalDate.now().toString() + ",");
    assertThat(String.join(",", csvRowResults.get(5))).isEqualTo(
        "2019-11-23,Administrator,Birmingham,ND84VSX,2019-11-02,£8.00,87,ext-payment-id-1,4,£35.00,"
            + "CHARGEBACK," + LocalDate.now().toString() + ",");
    assertThat(String.join(",", csvRowResults.get(6))).isEqualTo(
        "2019-11-23,Administrator,Birmingham,OD84VSX,2019-11-03,£8.00,87,ext-payment-id-1,4,"
            + "£35.00,,,");
    assertThat(String.join(",", csvRowResults.get(7))).isEqualTo(
        "2019-11-23,Administrator,Birmingham,PD84VSX,2019-11-04,£11.00,87,ext-payment-id-1,4,"
            + "£35.00,,,");
  }

  @Test
  public void shouldGetAllCsvRowsWithHeaderForAccountUser() {
    // given
    UUID accountId = UUID.fromString("1f30838f-69ee-4486-95b4-7dfcd5c6c67a");
    UUID accountUserId = UUID.fromString("88732cca-a5c7-4ad6-a60d-7edede935915");
    mockVccsCleanAirZonesCall();
    mockAccountServiceGetAllUsersCall(accountId.toString(), 200);

    // when
    List<String[]> csvRowResults = csvGeneratorService.generateCsvRows(accountId, accountUserId);

    // then
    assertThat(csvRowResults).hasSize(2);
    assertThat(String.join(",", csvRowResults.get(0))).isEqualTo(
        "Date of payment,Payment made by,Clean Air Zone,Number plate,Date of entry,Charge,"
            + "Payment reference,GOV.UK payment ID,Entries paid for,Total amount paid,"
            + "Status,Date received from local authority,Case reference");
    assertThat(String.join(",", csvRowResults.get(1))).isEqualTo(
        "2019-11-25,Jan Kowalski,Birmingham,RD84VSX,2019-11-06,£28.00,1881,ext-payment-id-3,1,"
            + "£28.00,,,");
  }
}
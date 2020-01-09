package uk.gov.caz.vcc.controller;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.assertj.core.util.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.vcc.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.vcc.domain.CazClass;
import uk.gov.caz.vcc.domain.TariffDetails;
import uk.gov.caz.vcc.domain.VehicleType;
import uk.gov.caz.vcc.domain.VehicleTypeCharge;
import uk.gov.caz.vcc.dto.ChargeDto;
import uk.gov.caz.vcc.dto.InformationUrlsDto;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.dto.VehicleTypeCazChargesDto;
import uk.gov.caz.vcc.repository.TariffDetailsRepository;
import uk.gov.caz.vcc.service.CazTariffService;
import uk.gov.caz.vcc.service.NationalTaxiRegisterService;
import uk.gov.caz.vcc.service.UnrecognizedVehicleChargeCalculationService;

/**
 * Integration tests for the VehicleController layer: https://spring.io/guides/gs/testing-web/
 */
@MockedMvcIntegrationTest
@Slf4j
public class VehicleControllerTestIT {

  @MockBean
  private NationalTaxiRegisterService ntrService;

  @MockBean
  private CazTariffService tariffService;

  @MockBean
  private TariffDetailsRepository tariffDetailsRepository;

  @MockBean
  private UnrecognizedVehicleChargeCalculationService unrecognizedVehicleChargeCalculationService;

  @Autowired
  private MockMvc mockMvc;

  private HttpHeaders headers;
  private InformationUrlsDto informationUrls;
  private String leedsCaz = "39e54ed8-3ed2-441d-be3f-38fc9b70c8d3";
  private String birminghamCaz = "5cd7441d-766f-48ff-b8ad-1809586fea37";

  @BeforeEach
  public void setup() {
    this.headers = new HttpHeaders();
    this.headers.add("X-Correlation-ID", "test");
  }

  public boolean csvToBoolean(String bool) {
    if (bool.equals("f")) {
      return false;
    } else if (bool.equals("t")) {
      return true;
    } else {
      throw new UnsupportedOperationException(
          format("Could not interpret boolean value %s from CSV.", bool));
    }
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/test_data.csv", numLinesToSkip = 1)
  public void vehicleDetails(String registrationnumber, String vehicletype,
      String iswav, String colour, String dateoffirstregistration,
      String eurostatus, String typeapproval, String massinservice,
      String bodytype, String make, String model, String grossweight,
      String seatingcapacity, String standingcapacity, String taxclass,
      String fueltype, String secondarycolour, String expectedexempt,
      String expectedcompliant, String expectedtype) throws Exception {

    String testVrn = registrationnumber;
    String expectedType = (expectedtype == null) ? "null" : expectedtype;
    String typeApproval = (typeapproval == null || typeapproval.equals("null")) ? "" : typeapproval;
    String testUrl = "/v1/compliance-checker/vehicles/{vrn}/details";

    when(ntrService.getLicenseInformation(testVrn)).thenReturn(Optional.empty());

    try {
      if (csvToBoolean(expectedexempt)) {
        mockMvc.perform(get(testUrl, testVrn).headers(this.headers))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.registrationNumber", is(registrationnumber)))
        .andExpect(jsonPath("$.typeApproval", is(typeApproval)))
        .andExpect(jsonPath("$.make", is(make)))
        .andExpect(jsonPath("$.model", is(model)))
        .andExpect(jsonPath("$.colour", is(colour)))
        .andExpect(jsonPath("$.fuelType", is(fueltype)))
        .andExpect(jsonPath("$.exempt", is(csvToBoolean(expectedexempt))));
      } else {
        mockMvc.perform(get(testUrl, testVrn).headers(this.headers))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.registrationNumber", is(registrationnumber)))
        .andExpect(jsonPath("$.typeApproval", is(typeApproval)))
        .andExpect(jsonPath("$.type", is(expectedType)))
        .andExpect(jsonPath("$.make", is(make)))
        .andExpect(jsonPath("$.model", is(model)))
        .andExpect(jsonPath("$.colour", is(colour)))
        .andExpect(jsonPath("$.fuelType", is(fueltype)))
        .andExpect(jsonPath("$.taxiOrPhv", is(false)))
        .andExpect(jsonPath("$.exempt", is(csvToBoolean(expectedexempt))));
    }
  } catch (AssertionError e) {
      log.error("Assertion error for test case with VRN: {}", testVrn);
      throw e;
    }
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/test_compliance_data.csv", numLinesToSkip = 1)
  public void vehicleCompliance(String registrationNumber, String expectedType,
      String expectedExempt) throws Exception {
    String vrn = registrationNumber;
    String zones = leedsCaz + "," + birminghamCaz;
    String testUrl = "/v1/compliance-checker/vehicles/{vrn}/compliance";
    boolean exempt = csvToBoolean(expectedExempt);

    this.informationUrls = InformationUrlsDto
        .builder()
        .additionalInfo("www.test.uk")
        .build();

    if (expectedType != null) {
      VehicleTypeCharge vehicleTypeCharge = new VehicleTypeCharge() {
        {
          setVehicleType(VehicleType.valueOf(expectedType));
          setCharge(0.0f);
        }
      };

      List<VehicleTypeCharge> rates = new ArrayList<VehicleTypeCharge>();
      rates.add(vehicleTypeCharge);

      when(ntrService.getLicenseInformation(vrn)).thenReturn(Optional.empty());
      when(tariffDetailsRepository.getTariffDetails(Mockito.any()))
          .thenReturn(Optional.of(new TariffDetails() {
            {
              setCazId(UUID.fromString(leedsCaz));
              setName("TEST");
              setTariff(CazClass.D);
              setInformationUrls(informationUrls);
              setRates(rates);
            }
          }));

      if (exempt) {
        mockMvc.perform(get(testUrl, vrn).param("zones", zones).headers(this.headers))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.registrationNumber", is(registrationNumber)))
            .andExpect(jsonPath("$.isExempt", is(exempt)))
            .andExpect(jsonPath("$.complianceOutcomes", is(hasSize(0))));
      } else {
        mockMvc.perform(get(testUrl, vrn).param("zones", zones).headers(this.headers))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.registrationNumber", is(registrationNumber)))
            .andExpect(jsonPath("$.isExempt", is(exempt)))
            .andExpect(jsonPath("$.complianceOutcomes[0].cleanAirZoneId",
                is(leedsCaz)))
            .andExpect(jsonPath("$.complianceOutcomes[1].cleanAirZoneId",
                is(birminghamCaz)))
            .andExpect(jsonPath("$.complianceOutcomes[1].informationUrls.additionalInfo",
                is("www.test.uk")));
      }

    } else {
      mockMvc.perform(get(testUrl, vrn).headers(this.headers))
          .andExpect(status().is4xxClientError());
    }
  }

  @Test
  public void shouldReturnVehicleDetails() throws Exception {
    // given
    String vrn = "CAS310";
    mockNtrResponseWithLicensingAuthoritiesNames(vrn);

    // then
    mockMvc.perform(get("/v1/compliance-checker/vehicles/{vrn}/details", vrn).headers(this.headers))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.registrationNumber", is("CAS310")))
        .andExpect(jsonPath("$.taxiOrPhv", is(true)))
        .andExpect(jsonPath("$.licensingAuthoritiesNames[0]", is("la-1")))
        .andExpect(jsonPath("$.licensingAuthoritiesNames[1]", is("la-2")));
  }

  @Test
  public void shouldReturnStatusCode422() throws Exception {
    // given
    String vrn = "ERR422";
    String zones = leedsCaz + "," + birminghamCaz;
    when(ntrService.getLicenseInformation(vrn)).thenReturn(Optional.empty());
    
    VehicleTypeCharge vehicleTypeCharge = new VehicleTypeCharge() {
      {
        setVehicleType(VehicleType.PRIVATE_CAR);
        setCharge(0.0f);
      }
    };

    List<VehicleTypeCharge> rates = new ArrayList<VehicleTypeCharge>();
    rates.add(vehicleTypeCharge);

    when(tariffDetailsRepository.getTariffDetails(Mockito.any()))
          .thenReturn(Optional.of(new TariffDetails() {
            {
              setCazId(UUID.fromString(leedsCaz));
              setName("TEST");
              setTariff(CazClass.D);
              setInformationUrls(informationUrls);
              setRates(rates);
            }
          }));
    // then
    mockMvc.perform(get("/v1/compliance-checker/vehicles/{vrn}/compliance", vrn).param("zones", zones).headers(this.headers))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void shouldReturnVehicleDetailsWithoutLicensingAuthoritiesNames() throws Exception {
    // given
    String vrn = "CAS310";
    mockNtrResponseWithoutLicensingAuthoritiesNames(vrn);

    // then
    mockMvc.perform(get("/v1/compliance-checker/vehicles/{vrn}/details", vrn)
        .headers(this.headers))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.registrationNumber", is("CAS310")))
        .andExpect(jsonPath("$.taxiOrPhv", is(false)))
        .andExpect(jsonPath("$.licensingAuthoritiesNames", is(emptyList())));
  }

  @Test
  public void shouldTestUnrecognisedVehicleEndpoint() throws Exception {
    //given
    String vehicleType = "buS";
    String cazId = "8023312c-a9e7-11e9-a2a3-2a2ae2dbcce4";
    float chargeValue = 5.0f;
    String chargeName = "Bus";
    mockServiceCall(vehicleType, cazId, chargeValue, chargeName);

    //then
    mockMvc
        .perform(get("/v1/compliance-checker/vehicles/unrecognised/{type}/compliance", vehicleType)
            .headers(this.headers)
            .param("zones", cazId))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.charges[0].cleanAirZoneId", is(cazId)))
        .andExpect(jsonPath("$.charges[0].name", is(chargeName)))
        .andExpect(jsonPath("$.charges[0].charge", is((double) chargeValue)));
  }

  private void mockServiceCall(String vehicleType,
      String cazId, float chargeValue, String chargeName) {
    when(unrecognizedVehicleChargeCalculationService.getCharges(
        vehicleType, Collections.singletonList(UUID.fromString(cazId))
    )).thenReturn(
        new VehicleTypeCazChargesDto(
            Collections.singletonList(
                new ChargeDto(UUID.fromString(cazId), chargeName, chargeValue)
            )
        )
    );
  }
  
  @Test
  public void shouldReturn404IfDetailsNotFound() throws Exception {
    String testUrl = "/v1/compliance-checker/vehicles/CAS404/details";

    when(ntrService.getLicenseInformation("CAS404")).thenReturn(Optional.empty());

    mockMvc.perform(get(testUrl).headers(this.headers))
      .andExpect(status().isNotFound());
  }

  private void mockNtrResponseWithLicensingAuthoritiesNames(String vrn) {
    Optional<TaxiPhvLicenseInformationResponse> licenseInfo = Optional
        .of(TaxiPhvLicenseInformationResponse
            .builder()
            .active(true)
            .wheelchairAccessible(false)
            .licensingAuthoritiesNames(newArrayList("la-1", "la-2"))
            .build());

    when(ntrService.getLicenseInformation(vrn)).thenReturn(licenseInfo);
  }

  private void mockNtrResponseWithoutLicensingAuthoritiesNames(String vrn) {
    Optional<TaxiPhvLicenseInformationResponse> licenseInfo = Optional
        .of(TaxiPhvLicenseInformationResponse
            .builder()
            .active(false)
            .wheelchairAccessible(false)
            .build());

    when(ntrService.getLicenseInformation(vrn)).thenReturn(licenseInfo);
  }
}

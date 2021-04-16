package uk.gov.caz.whitelist.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.whitelist.controller.WhitelistController.BASE_PATH;
import static uk.gov.caz.whitelist.controller.WhitelistController.X_MODIFIER_EMAIL_HEADER;
import static uk.gov.caz.whitelist.controller.WhitelistController.X_MODIFIER_ID_HEADER;
import static uk.gov.caz.whitelist.model.CategoryType.EARLY_ADOPTER;
import static uk.gov.caz.whitelist.model.CategoryType.EXEMPTION;
import static uk.gov.caz.whitelist.model.CategoryType.NON_UK_VEHICLE;
import static uk.gov.caz.whitelist.model.CategoryType.OTHER;
import static uk.gov.caz.whitelist.model.CategoryType.PROBLEMATIC_VRN;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import uk.gov.caz.whitelist.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.whitelist.dto.WhitelistedVehicleRequestDto;
import uk.gov.caz.whitelist.model.CategoryType;
import uk.gov.caz.whitelist.model.WhitelistVehicle;
import uk.gov.caz.whitelist.repository.WhitelistVehiclePostgresRepository;
import uk.gov.caz.whitelist.service.WhitelistService;

@MockedMvcIntegrationTest
@Sql(scripts = {"classpath:data/sql/clear-whitelist-vehicles-data.sql",
    "classpath:data/sql/whitelist-vehicles-data.sql"}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear-whitelist-vehicles-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class WhitelistControllerTestIT {

  private static final String SOME_CORRELATION_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1c";
  private static final String SOME_MODIFIER_ID = "a6ce833d-1798-434d-88a9-d7ac6452fbc6";
  private static final String SOME_MODIFIER_EMAIL = RandomStringUtils.randomAlphabetic(10);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WhitelistVehiclePostgresRepository whitelistVehiclePostgresRepository;

  @Autowired
  private WhitelistService whitelistService;

  @Test
  public void shouldReturnWhitelistedVehicleDetailsWithAddedTimestamp() throws Exception {
    mockMvc.perform(get(BASE_PATH + "/CAS310")
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vrn", is("CAS310")))
        .andExpect(jsonPath("$.reasonUpdated", is("reasonUpdated")))
        .andExpect(jsonPath("$.addedTimestamp", is("2020-12-22 13:40:21")))
        .andExpect(jsonPath("$.category", is("Other")))
        .andExpect(jsonPath("$.email", is("test@gov.uk")))
        .andExpect(jsonPath("$.uploaderId", is("2c01df02-da8d-4d92-ad24-7c20bf6617e7")));
  }

  @Test
  public void shouldReturnWhitelistedVehicleDetailsWithoutAddedTimestamp() throws Exception {
    mockMvc.perform(get(BASE_PATH + "/EB12QMD")
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vrn", is("EB12QMD")))
        .andExpect(jsonPath("$.reasonUpdated", is("reasonUpdated")))
        .andExpect(jsonPath("$.addedTimestamp", nullValue()))
        .andExpect(jsonPath("$.category", is("Other")))
        .andExpect(jsonPath("$.email", is("test@gov.uk")))
        .andExpect(jsonPath("$.uploaderId", is("2c01df02-da8d-4d92-ad24-7c20bf6617e7")));
  }

  @Test
  public void shouldRemoveWhitelistedVehicleAndReturnItsDetails() throws Exception {
    WhitelistVehicle vehicle = addVehicle();

    mockMvc.perform(delete(BASE_PATH + "/" + vehicle.getVrn())
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID)
        .header(X_MODIFIER_ID_HEADER, SOME_MODIFIER_ID)
        .header(X_MODIFIER_EMAIL_HEADER, SOME_MODIFIER_EMAIL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vrn", is(vehicle.getVrn())))
        .andExpect(jsonPath("$.reasonUpdated", is(vehicle.getReasonUpdated())))
        .andExpect(jsonPath("$.category", is(vehicle.getCategory())))
        .andExpect(jsonPath("$.email", is(vehicle.getUploaderEmail())))
        .andExpect(jsonPath("$.uploaderId", is(vehicle.getUploaderId().toString())));

    assertThat(whitelistService.findBy(vehicle.getVrn())).isEmpty();
  }

  @Test
  public void shouldReturn404IfVehicleDoesntExist() throws Exception {
    String vrn = "doesntexist";
    mockMvc.perform(delete(BASE_PATH + "/" + vrn)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID)
        .header(X_MODIFIER_ID_HEADER, SOME_MODIFIER_ID)
        .header(X_MODIFIER_EMAIL_HEADER, SOME_MODIFIER_EMAIL))
        .andExpect(status().isNotFound());

    assertThat(whitelistService.findBy(vrn)).isEmpty();
  }

  @Test
  public void shouldReturnNotFound() throws Exception {
    mockMvc.perform(get(BASE_PATH + "/CAS311")
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().isNotFound());
  }

  @Test
  public void whenCalledWithoutModifierEmailHeaderShouldNotAcceptRequest400() throws Exception {
    String vrn = RandomStringUtils.randomAlphabetic(10);
    mockMvc.perform(delete(BASE_PATH + "/" + vrn)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID)
        .header(X_MODIFIER_ID_HEADER, SOME_MODIFIER_ID))
        .andExpect(status().isBadRequest())
    ;
  }

  @ParameterizedTest
  @MethodSource("vrnsTypeCategoriesAndManufacturers")
  public void shouldSaveWhitelistedVehicleAndReturnDetails(String vrn,
      CategoryType categoryType, String manufacturer) throws Exception {
    mockMvc.perform(post(BASE_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(createPayload(vrn, categoryType, manufacturer))
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().isCreated())
        .andDo(MockMvcResultHandlers.print())
        .andExpect(jsonPath("$.vrn", is(vrn)))
        .andExpect(jsonPath("$.reasonUpdated", is("Reason")))
        .andExpect(jsonPath("$.manufacturer", is(manufacturer)))
        .andExpect(jsonPath("$.category", is(categoryType.getCategory())))
        .andExpect(jsonPath("$.uploaderId", is("4490996d-eb18-4c41-ac50-299cf7defbcb")))
        .andExpect(jsonPath("$.email", is("test@gov.uk")));

    whenSaveWhitelistVehicle(vrn)
        .thenWhitelistVehicleFromDB()
        .hasCategory(categoryType.getCategory())
        .hasManufacturer(manufacturer)
        .hasEmail("test@gov.uk")
        .isExempt(categoryType.isExempt())
        .isCompliant(categoryType.isCompliant());
  }

  @SneakyThrows
  private String createPayload(String vrn, CategoryType categoryType, String manufacturer) {
    return objectMapper
        .writeValueAsString(createWhitelistedVehicleRequestDto(vrn, categoryType, manufacturer));
  }

  private WhitelistVehicle addVehicle() {
    return whitelistService.save(WhitelistVehicle.builder()
        .reasonUpdated("Reason")
        .manufacturer("Manufacturer")
        .category("Category")
        .vrn("CAS311")
        .uploaderId(UUID.randomUUID())
        .uploaderEmail(UUID.randomUUID() + "@gov.uk")
        .compliant(true)
        .exempt(false)
        .build());
  }

  private WhitelistedVehicleRequestDto createWhitelistedVehicleRequestDto(String vrn,
      CategoryType categoryType, String manufacturer) {
    return WhitelistedVehicleRequestDto.builder()
        .vrn(vrn)
        .manufacturer(manufacturer)
        .category(categoryType.getCategory())
        .reasonUpdated("Reason")
        .uploaderId(UUID.fromString("4490996d-eb18-4c41-ac50-299cf7defbcb"))
        .email("test@gov.uk")
        .build();
  }

  private static Stream<Arguments> vrnsTypeCategoriesAndManufacturers() {
    return Stream.of(
        Arguments.of("CAS331", EARLY_ADOPTER, "Manu1"),
        Arguments.of("CAS332", NON_UK_VEHICLE, "Manu2"),
        Arguments.of("CAS333", PROBLEMATIC_VRN, null),
        Arguments.of("CAS334", EXEMPTION, null),
        Arguments.of("CAS335", OTHER, "Manu3")
    );
  }

  private WhitelistVehicleAssertion whenSaveWhitelistVehicle(String vrn) {
    return WhitelistVehicleAssertion
        .whenSaveWhitelistVehicle(vrn, whitelistVehiclePostgresRepository);
  }

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  private static class WhitelistVehicleAssertion {

    private final String vrn;
    private final WhitelistVehiclePostgresRepository whitelistVehiclePostgresRepository;
    private String category;
    private String manufacturer;
    private String email;
    private boolean exempt;
    private boolean compliant;

    static WhitelistVehicleAssertion whenSaveWhitelistVehicle(String vrn,
        WhitelistVehiclePostgresRepository whitelistVehiclePostgresRepository) {
      return new WhitelistVehicleAssertion(vrn, whitelistVehiclePostgresRepository);
    }

    public WhitelistVehicleAssertion thenWhitelistVehicleFromDB() {
      WhitelistVehicle whitelistVehicle = whitelistVehiclePostgresRepository.findOneByVrn(vrn)
          .get();
      this.category = whitelistVehicle.getCategory();
      this.manufacturer = whitelistVehicle.getManufacturer().orElse(null);
      this.email = whitelistVehicle.getUploaderEmail();
      this.exempt = whitelistVehicle.isExempt();
      this.compliant = whitelistVehicle.isCompliant();
      return this;
    }

    public WhitelistVehicleAssertion hasCategory(String category) {
      assertThat(this.category).isEqualTo(category);
      return this;
    }

    public WhitelistVehicleAssertion hasManufacturer(String manufacturer) {
      assertThat(this.manufacturer).isEqualTo(manufacturer);
      return this;
    }

    public WhitelistVehicleAssertion hasEmail(String email) {
      assertThat(this.email).isEqualTo(email);
      return this;
    }

    public WhitelistVehicleAssertion isExempt(boolean exempt) {
      assertThat(this.exempt).isEqualTo(exempt);
      return this;
    }

    public WhitelistVehicleAssertion isCompliant(boolean compliant) {
      assertThat(this.compliant).isEqualTo(compliant);
      return this;
    }
  }
}
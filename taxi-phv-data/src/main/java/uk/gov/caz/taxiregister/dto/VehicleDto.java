package uk.gov.caz.taxiregister.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableList;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.util.StringUtils;
import uk.gov.caz.taxiregister.dto.VehicleDto.VehicleDtoBuilder;
import uk.gov.caz.taxiregister.dto.validation.LicenceDatesValidator;
import uk.gov.caz.taxiregister.dto.validation.LicenceTypeValidator;
import uk.gov.caz.taxiregister.dto.validation.LicenceValidator;
import uk.gov.caz.taxiregister.dto.validation.LicensePlateNumberValidator;
import uk.gov.caz.taxiregister.dto.validation.LicensingAuthorityNameValidator;
import uk.gov.caz.taxiregister.dto.validation.VrmValidator;
import uk.gov.caz.taxiregister.dto.validation.WheelchairAccessibleVehicleValidator;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = VehicleDtoBuilder.class)
public class VehicleDto {

  private static final List<LicenceValidator> VALIDATORS = ImmutableList.of(
      new VrmValidator(),
      new LicenceDatesValidator(),
      new LicenceTypeValidator(),
      new LicensingAuthorityNameValidator(),
      new LicensePlateNumberValidator(),
      new WheelchairAccessibleVehicleValidator()
  );

  @JsonIgnore 
  private RegisterJobTrigger registerJobTrigger;

  /**
   * Getter method for registerJobTrigger.
   * 
   * <p>The registerJobTrigger value is only set when reading CSV files - we do not
   * expect it in the body of API submissions, so the field is JsonIgnored. This
   * method will return registerJobTrigger if not null (i.e. {@code CSV_FROM_S3}), else
   * will assume that, because the registerJobTrigger is null, the trigger is
   * {@code API_CALL}.</p>
   *  
   * @return {@link RegisterJobTrigger}
   */
  @JsonIgnore
  public RegisterJobTrigger getRegisterJobTrigger() {
    return Optional.ofNullable(this.registerJobTrigger).orElse(RegisterJobTrigger.API_CALL);
  }

  @ToString.Exclude
  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle.vrm}")
  @NotNull
  @Size(min = 1, max = 7)
  @Pattern(regexp = VrmValidator.REGEX)
  String vrm;

  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicle.start}")
  @NotNull
  @DateTimeFormat(iso = ISO.DATE)
  @Size(min = 8, max = 10)
  String start;

  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicle.end}")
  @NotNull
  @DateTimeFormat(iso = ISO.DATE)
  @Size(min = 8, max = 10)
  String end;

  @ApiModelProperty(
      notes = "${swagger.model.descriptions.vehicle.taxi-or-phv}"
  )
  @NotNull
  @JsonProperty("taxiOrPHV")
  String description;

  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicle.licensing-authority-name}")
  @NotNull
  @Size(min = 1, max = 50)
  String licensingAuthorityName;

  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicle.license-plate-number}")
  @NotNull
  @Size(min = 1, max = 15)
  String licensePlateNumber;

  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicle.wheelchair-accessible-vehicle}")
  String wheelchairAccessibleVehicle;

  @ApiModelProperty(hidden = true)
  @JsonIgnore
  int lineNumber;

  /**
   * Validates this instance.
   *
   * @return a list of validation errors if there are any. An empty list is returned if validation
   *     succeeds.
   */
  public List<ValidationError> validate() {
    return VALIDATORS.stream()
        .map(validator -> validator.validate(this))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class VehicleDtoBuilder {

    /**
     * Custom method to always remove all whitespaces from VRN.
     */
    public VehicleDto.VehicleDtoBuilder vrm(final String vrm) {
      this.vrm = StringUtils.trimAllWhitespace(vrm);
      return this;
    }
    
    /**
     * Custom method to always remove all whitespaces from the licensingAuthorityName.
     */
    public VehicleDto.VehicleDtoBuilder licensingAuthorityName(final String licAuthorityName) {
      this.licensingAuthorityName = StringUtils.trimWhitespace(licAuthorityName);
      return this;
    }
  }
}

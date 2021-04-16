package uk.gov.caz.vcc.repository;

import static uk.gov.caz.vcc.repository.LicenseAndVehicleRepository.defaultTimeoutInSeconds;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestTemplate;
import uk.gov.caz.async.rest.AsyncOp;
import uk.gov.caz.async.rest.AsyncRestService;
import uk.gov.caz.vcc.domain.exceptions.ExternalServiceCallException;
import uk.gov.caz.vcc.dto.LicencesInformation;
import uk.gov.caz.vcc.dto.TaxiPhvLicenseInformationResponse;
import uk.gov.caz.vcc.dto.ntr.GetLicencesInfoRequestDto;
import uk.gov.caz.vcc.dto.ntr.GetLicencesInfoResponseDto;

@Slf4j
@Repository
public class NationalTaxiRegisterRepository {

  static final int MAX_BATCH_SIZE = 150;

  private final RestTemplate nationalTaxiRegisterRestTemplate;
  private final NationalTaxiRegisterAsyncRepository asyncRepository;
  private final AsyncRestService asyncRestService;

  /**
   * Default constructor.
   */
  public NationalTaxiRegisterRepository(RestTemplateBuilder restTemplateBuilder,
      @Value("${services.national-taxi-register.root-url}") String nationalTaxiRegisterRootUri,
      NationalTaxiRegisterAsyncRepository asyncRepository,
      AsyncRestService asyncRestService) {
    this.asyncRepository = asyncRepository;
    this.asyncRestService = asyncRestService;
    this.nationalTaxiRegisterRestTemplate =
        restTemplateBuilder.rootUri(nationalTaxiRegisterRootUri).build();
  }

  /**
   * Get license info for a vehicle from the National Taxi Register.
   *
   * @param vrn The vehicle registration number.
   * @return A LicenseInfoResponse (optional).
   */
  @Cacheable(value = "licenseInfo", key = "#vrn", cacheManager = "licenseInfoCacheManager")
  public Optional<TaxiPhvLicenseInformationResponse> getLicenseInfo(String vrn) {
    try {
      ResponseEntity<TaxiPhvLicenseInformationResponse> responseEntity =
          nationalTaxiRegisterRestTemplate.getForEntity(
              "/v1/vehicles/{vrn}/licence-info",
              TaxiPhvLicenseInformationResponse.class, vrn);

      return Optional.ofNullable(responseEntity.getBody());
    } catch (NotFound e) {
      return Optional.empty();
    } catch (Exception e) {
      throw new ExternalServiceCallException(e);
    }
  }

  /**
   * Gets licenses info for vehicles from the National Taxi Register.
   *
   * @param vrns List of vehicle registration numbers.
   * @return An instance of {@link LicencesInformation}.
   */
  public LicencesInformation getLicensesInformation(Collection<String> vrns) {
    List<AsyncOp<GetLicencesInfoResponseDto>> asyncOps = createBatchedCalls(vrns);
    callNtr(asyncOps);

    Optional<AsyncOp<GetLicencesInfoResponseDto>> optionalFailed = findFailed(asyncOps);
    if (optionalFailed.isPresent()) {
      AsyncOp<GetLicencesInfoResponseDto> failed = optionalFailed.get();
      return LicencesInformation.failure(failed.getHttpStatus(), failed.getError());
    }

    return LicencesInformation.success(mergeResults(asyncOps));
  }

  private Map<String, TaxiPhvLicenseInformationResponse> mergeResults(
      List<AsyncOp<GetLicencesInfoResponseDto>> asyncOps) {
    Builder<String, TaxiPhvLicenseInformationResponse> resultBuilder = ImmutableMap.builder();
    for (AsyncOp<GetLicencesInfoResponseDto> asyncOp : asyncOps) {
      resultBuilder.putAll(asyncOp.getResult().getLicencesInformation());
    }
    return resultBuilder.build();
  }

  private List<AsyncOp<GetLicencesInfoResponseDto>> createBatchedCalls(Collection<String> vrns) {
    Iterable<List<String>> batches = Iterables.partition(vrns, MAX_BATCH_SIZE);

    return StreamSupport.stream(batches.spliterator(), false)
        .map(this::processGetLicensesInformationBatch)
        .collect(Collectors.toList());
  }

  private Optional<AsyncOp<GetLicencesInfoResponseDto>> findFailed(
      List<AsyncOp<GetLicencesInfoResponseDto>> asyncOps) {
    return asyncOps.stream().filter(AsyncOp::hasError).findFirst();
  }

  private AsyncOp<GetLicencesInfoResponseDto> processGetLicensesInformationBatch(
      List<String> vrns) {
    return asyncRepository.findByRegistrationNumbersAsync(new GetLicencesInfoRequestDto(vrns));
  }

  /**
   * Method for evicting a cached licenseInfo for given vrn.
   */
  @CacheEvict(value = "licenseInfo", allEntries = true, cacheManager = "licenseInfoCacheManager")
  public void cacheEvictLicenseInfo() {
    log.debug("Evicting cached license info for taxi VRNs in response to an upload being made.");
  }

  /**
   * Starts and awaits for all async requests to NTR.
   */
  private void callNtr(List<AsyncOp<GetLicencesInfoResponseDto>> asyncOps) {
    long timeout = defaultTimeoutInSeconds();
    try {
      asyncRestService.startAndAwaitAll(asyncOps, timeout, TimeUnit.SECONDS);
    } catch (Exception exception) {
      throw new ExternalServiceCallException(exception);
    }
  }
}

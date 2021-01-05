package uk.gov.caz.accounts.repository;

import java.sql.Timestamp;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.caz.accounts.model.AccountVehicleBare;
import uk.gov.caz.accounts.model.VehicleChargeability;
import uk.gov.caz.accounts.model.VehicleChargeabilityId;
import uk.gov.caz.accounts.model.VehiclesToCalculateChargeability;

/**
 * JPA operations over T_VEHICLE_CHARGEABILITY table.
 */
@Repository
public interface VehicleChargeabilityRepository extends
    CrudRepository<VehicleChargeability, VehicleChargeabilityId>,
    VehicleChargeabilityRepositoryCustom {

  /**
   * Finds all account vehicle IDs combined with VRNs of vehicles that do not have chargeability
   * calculated in all CAZes (or just don't have any calculations done yet).
   *
   * @param accountId ID of Account/Fleet.
   * @param cazesCount Current count of CAZes in the system.
   * @return Set of Object[] array which has Account Vehicle ID as string on 0 index and VRN on 1
   *     index.
   */
  @Query(value = Sql.FIND_ALL_FLEET_VRNS_THAT_DO_NOT_HAVE_CHARGEABILITY_CACHE_IN_EACH_CAZ,
      nativeQuery = true)
  Set<Object[]> findAllFleetVehiclesRawThatDoNotHaveChargeabilityCacheInEachCaz(UUID accountId,
      int cazesCount);

  /**
   * Finds all vehicle IDs combined with VRNs of vehicles that do not have chargeability calculated
   * in all CAZes (or just don't have any calculations done yet).
   *
   * @param cazesCount Current count of CAZes in the system.
   * @return Set of Object[] array which has Account Vehicle ID as string on 0 index and VRN on 1
   *     index.
   */
  @Query(value = Sql.FIND_ALL_VRNS_THAT_DO_NOT_HAVE_CHARGEABILITY_CACHE_IN_EACH_CAZ,
      nativeQuery = true)
  Set<Object[]> findAllVehiclesRawThatDoNotHaveChargeabilityCacheInEachCaz(int cazesCount);

  /**
   * Finds all vehicle IDs combined with VRNs of vehicles that have expired chargeability cached.
   *
   * @param expiredTimestamp the date when expired chargeability cache.
   * @return Set of Object[] array which has Account Vehicle ID as string on 0 index and VRN on 1
   *     index.
   */
  @Query(value = Sql.FIND_ALL_VRNS_THAT_HAVE_EXPIRED_CHARGEABILITY_CACHE,
      nativeQuery = true)
  Set<Object[]> findAllVehiclesRawThatHaveExpiredChargeabilityCache(Timestamp expiredTimestamp);

  /**
   * Finds all {@link AccountVehicleBare} instances (account vehicle IDs combined with VRNs) with
   * vehicles that do not have chargeability calculated in all CAZes (or just don't have any
   * calculations done yet).
   *
   * @param accountId ID of Account/Fleet.
   * @param cazesCount Current count of CAZes in the system.
   * @return {@link VehiclesToCalculateChargeability} with vehicles that do not have chargeability
   *     calculated in all CAZes.
   */
  default VehiclesToCalculateChargeability findAllForFleetThatDoNotHaveChargeabilityCacheInEachCaz(
      UUID accountId, int cazesCount) {
    return new VehiclesToCalculateChargeability(
        findAllFleetVehiclesRawThatDoNotHaveChargeabilityCacheInEachCaz(accountId, cazesCount)
            .stream().map(rawVehicle -> AccountVehicleBare
            .from(rawVehicle[0].toString(), rawVehicle[1].toString())).collect(Collectors.toSet()));
  }

  /**
   * Finds all {@link AccountVehicleBare} instances (account vehicle IDs combined with VRNs) with
   * vehicles that do not have chargeability calculated in all CAZes (or just don't have any
   * calculations done yet) or has expired chargeability in db.
   *
   * @param cazesCount Current count of CAZes in the system.
   * @return {@link VehiclesToCalculateChargeability} with vehicles that do not have chargeability
   *     calculated in all CAZes.
   */
  default VehiclesToCalculateChargeability findAllThatDoNotHaveChargeabilityCacheInEachCazOrExpired(
      int cazesCount, Timestamp timestamp) {
    Stream<AccountVehicleBare> vrnWithMissingCacheStream =
        findAllVehiclesRawThatDoNotHaveChargeabilityCacheInEachCaz(cazesCount).stream()
            .map(rawVehicle -> AccountVehicleBare
                .from(rawVehicle[0].toString(), rawVehicle[1].toString()));

    Stream<AccountVehicleBare> vrnWithExpiredCacheStream =
        findAllVehiclesRawThatHaveExpiredChargeabilityCache(timestamp)
            .stream()
            .map(rawVehicle -> AccountVehicleBare
                .from(rawVehicle[0].toString(), rawVehicle[1].toString()));

    Set<AccountVehicleBare> combined = Stream
        .concat(vrnWithMissingCacheStream, vrnWithExpiredCacheStream).collect(Collectors.toSet());

    return new VehiclesToCalculateChargeability(combined);
  }

  /**
   * Static inner class holding custom native SQL queries.
   */
  @UtilityClass
  class Sql {

    private static final String
        FIND_ALL_FLEET_VRNS_THAT_DO_NOT_HAVE_CHARGEABILITY_CACHE_IN_EACH_CAZ =
        "       SELECT cast(av.account_vehicle_id as varchar), av.vrn "
            + " FROM caz_account.t_account_vehicle av"
            + " LEFT OUTER JOIN caz_account.t_vehicle_chargeability vc"
            + " ON av.account_vehicle_id = vc.account_vehicle_id"
            + " WHERE av.account_id = ?#{[0]}"
            + " GROUP BY av.vrn, av.account_vehicle_id "
            + " HAVING COUNT(DISTINCT vc.caz_id) != ?#{[1]}";

    private static final String
        FIND_ALL_VRNS_THAT_DO_NOT_HAVE_CHARGEABILITY_CACHE_IN_EACH_CAZ =
        "       SELECT cast(av.account_vehicle_id as varchar), av.vrn "
            + " FROM caz_account.t_account_vehicle av"
            + " LEFT OUTER JOIN caz_account.t_vehicle_chargeability vc"
            + " ON av.account_vehicle_id = vc.account_vehicle_id"
            + " GROUP BY av.vrn, av.account_vehicle_id "
            + " HAVING COUNT(DISTINCT vc.caz_id) != ?#{[0]}";

    private static final String
        FIND_ALL_VRNS_THAT_HAVE_EXPIRED_CHARGEABILITY_CACHE =
        "       SELECT cast(av.account_vehicle_id as varchar), av.vrn "
            + " FROM caz_account.t_account_vehicle av"
            + " LEFT OUTER JOIN caz_account.t_vehicle_chargeability vc"
            + " ON av.account_vehicle_id = vc.account_vehicle_id"
            + " WHERE REFRESH_TIMESTMP < ?#{[0]}";
  }
}

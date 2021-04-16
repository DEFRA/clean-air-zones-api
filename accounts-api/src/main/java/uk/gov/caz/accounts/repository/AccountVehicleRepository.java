package uk.gov.caz.accounts.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.caz.accounts.model.AccountVehicle;

@Repository
public interface AccountVehicleRepository extends
    PagingAndSortingRepository<AccountVehicle, Integer> {

  @Query(value = "select distinct av from AccountVehicle av left join fetch av.vehicleChargeability"
      + " where av.accountVehicleId in (?1)")
  List<AccountVehicle> findAllWithChargeability(List<UUID> accountVehicleIds);

  Page<AccountVehicle> findAllByAccountId(UUID accountId, Pageable pageable);

  Page<AccountVehicle> findAllByAccountIdAndVrnContaining(UUID accountId, String vrn,
      Pageable pageable);

  @Query(value = "select count (distinct av) from AccountVehicle av "
      + "left join av.vehicleChargeability vc "
      + "where vc.charge is null "
      + "and av.accountId = ?1")
  long countVehiclesWithUndeterminedChargeabilityFor(UUID accountId);

  @Query(value = "select count (distinct av) from AccountVehicle av "
      + "left join av.vehicleChargeability vc "
      + "where vc.charge is null "
      + "and av.accountId = ?1 "
      + "and vc.cazId = ?2")
  long countVehiclesWithUndeterminedChargeabilityForAccountInCaz(UUID accountId, UUID cazId);

  @Modifying
  @Query(value = "delete from AccountVehicle av where av.accountId = ?1")
  void deleteInBulkByAccountId(UUID accountId);

  @Query(value = Sql.VEHICLES_FOR_ACCOUNT_WITH_CHARGEABILITY,
      countQuery = Sql.VEHICLES_FOR_ACCOUNT_WITH_CHARGEABILITY_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllWithChargeabilityFor(UUID accountId, Pageable pageable);

  @Query(value = Sql.VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_CHARGEABILITY,
      countQuery = Sql.VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_CHARGEABILITY_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllByVrnContainingWithChargeabilityFor(UUID accountId, String query,
      Pageable pageable);

  @Query(value = Sql.VEHICLES_FOR_ACCOUNT_WITH_DETERMINED_CHARGE,
      countQuery = Sql.VEHICLES_FOR_ACCOUNT_WITH_DETERMINED_CHARGE_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllDeterminedWithChargeabilityFor(UUID accountId, Pageable pageable);

  @Query(value = Sql.VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_DETERMINED_CHARGE,
      countQuery = Sql.VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_DETERMINED_CHARGE_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllDeterminedByVrnContainingWithChargeabilityFor(UUID accountId,
      String query, Pageable pageable);

  @Query(value = Sql.VEHICLES_FOR_ACCOUNT_WITH_DETERMINED_CHARGE_AND_CHARGEABLE,
      countQuery = Sql.VEHICLES_FOR_ACCOUNT_WITH_DETERMINED_CHARGE_AND_CHARGEABLE_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllDeterminedChargeableWithChargeabilityFor(UUID accountId,
      PageRequest page);

  @Query(value = Sql.VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_DETERMINED_CHARGE_AND_CHARGEABLE,
      countQuery = Sql.VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_DETERMINED_CHARGE_AND_CHARGEABLE_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllDeterminedChargeableByVrnContainingWithChargeabilityFor(
      UUID accountId, String query, PageRequest page);

  @Query(value = Sql.VEHICLES_FOR_ACCOUNT_BY_CAZ,
      countQuery = Sql.VEHICLES_FOR_ACCOUNT_BY_CAZ_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllByAccountIdAndCaz(UUID accountId, UUID cazId, Pageable pageable);

  @Query(value = Sql.VEHICLES_FOR_ACCOUNT_BY_VRN_AND_CAZ,
      countQuery = Sql.VEHICLES_FOR_ACCOUNT_BY_VRN_AND_CAZ_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllByAccountIdAndVrnContainingInCaz(UUID accountId, UUID cazId,
      String vrn, Pageable pageable);

  @Query(value = Sql.VEHICLES_CHARGEABLE_FOR_ACCOUNT_BY_CAZ,
      countQuery = Sql.VEHICLES_CHARGEABLE_FOR_ACCOUNT_BY_CAZ_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllChargeableForAccountInCaz(UUID accountId, UUID cazId,
      Pageable pageable);

  @Query(value = Sql.VEHICLES_CHARGEABLE_FOR_ACCOUNT_BY_VRN_AND_CAZ,
      countQuery = Sql.VEHICLES_CHARGEABLE_FOR_ACCOUNT_BY_VRN_AND_CAZ_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllChargeableByVrnForAccountInCaz(UUID accountId, UUID cazId, String vrn,
      Pageable pageable);

  @Query(value = Sql.VEHICLES_DETERMINED_FOR_ACCOUNT_BY_CAZ,
      countQuery = Sql.VEHICLES_DETERMINED_FOR_ACCOUNT_BY_CAZ_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllDeterminedForAccountInCaz(UUID accountId, UUID cazId,
      Pageable pageable);

  @Query(value = Sql.VEHICLES_DETERMINED_FOR_ACCOUNT_BY_VRN_AND_CAZ,
      countQuery = Sql.VEHICLES_DETERMINED_FOR_ACCOUNT_BY_VRN_AND_CAZ_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllDeterminedByVrnForAccountInCaz(UUID accountId, UUID cazId, String vrn,
      Pageable pageable);

  @Query(value = Sql.VEHICLES_DETERMINED_CHARGEABLE_FOR_ACCOUNT_BY_CAZ,
      countQuery = Sql.VEHICLES_DETERMINED_CHARGEABLE_FOR_ACCOUNT_BY_CAZ_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllDeterminedChargeableForAccountInCaz(UUID accountId, UUID cazId,
      Pageable pageable);

  @Query(value = Sql.VEHICLES_DETERMINED_CHARGEABLE_FOR_ACCOUNT_BY_VRN_AND_CAZ,
      countQuery = Sql.VEHICLES_DETERMINED_CHARGEABLE_FOR_ACCOUNT_BY_VRN_AND_CAZ_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllDeterminedChargeableByVrnForAccountInCaz(UUID accountId, UUID cazId,
      String vrn, Pageable pageable);

  Long deleteByVrnAndAccountId(String vrn, UUID accountId);

  Optional<AccountVehicle> findByAccountIdAndVrn(UUID accountId, String vrn);

  @Query(value = "select distinct av from AccountVehicle av left join fetch av.vehicleChargeability"
      + " where av.accountId = ?1 and av.vrn = ?2")
  Optional<AccountVehicle> findByAccountIdAndVrnWithChargeability(UUID accountId, String vrn);

  @Query(value = "select distinct av from AccountVehicle av left join fetch av.vehicleChargeability"
      + " where av.accountId = ?1"
      + " order by av.vrn ASC")
  List<AccountVehicle> findByAccountIdWithChargeabilityAndOrderByVrnAsc(UUID accountId);

  @Modifying
  @Query(value = "update AccountVehicle av set av.cazVehicleType = :vehicleType "
      + "where av.accountVehicleId = :accountVehicleId")
  void updateVehicleType(@Param("accountVehicleId") UUID accountVehicleId,
      @Param("vehicleType") String vehicleType);

  /**
   * Static inner class holding custom native SQL queries.
   */
  @UtilityClass
  class Sql {

    private static final String VEHICLES_FOR_ACCOUNT_WITH_CHARGEABILITY =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND (vc.charge is null OR vc.charge > 0) "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_FOR_ACCOUNT_WITH_CHARGEABILITY_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND (vc.charge is null OR vc.charge > 0)";

    private static final String VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_CHARGEABILITY =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND av.vrn LIKE %?#{[1]}% "
            + "AND (vc.charge is null OR vc.charge > 0) "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_CHARGEABILITY_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND av.vrn LIKE %?#{[1]}% "
            + "AND (vc.charge is null OR vc.charge > 0)";

    private static final String VEHICLES_FOR_ACCOUNT_WITH_DETERMINED_CHARGE =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND vc.charge is not null "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_FOR_ACCOUNT_WITH_DETERMINED_CHARGE_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND vc.charge is not null";

    private static final String VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_DETERMINED_CHARGE =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND av.vrn LIKE %?#{[1]}% "
            + "AND vc.charge is not null "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_DETERMINED_CHARGE_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND av.vrn LIKE %?#{[1]}% "
            + "AND vc.charge is not null";

    private static final String VEHICLES_FOR_ACCOUNT_WITH_DETERMINED_CHARGE_AND_CHARGEABLE =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND (vc.charge is not null AND vc.charge > 0) "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_FOR_ACCOUNT_WITH_DETERMINED_CHARGE_AND_CHARGEABLE_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND (vc.charge is not null AND vc.charge > 0)";

    private static final String
        VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_DETERMINED_CHARGE_AND_CHARGEABLE =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND av.vrn LIKE %?#{[1]}% "
            + "AND (vc.charge is not null AND vc.charge > 0) "
            + "ORDER BY av.vrn ASC";

    private static final String
        VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_DETERMINED_CHARGE_AND_CHARGEABLE_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND av.vrn LIKE %?#{[1]}% "
            + "AND (vc.charge is not null AND vc.charge > 0)";

    private static final String VEHICLES_FOR_ACCOUNT_BY_CAZ =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND vc.caz_id = ?#{[1]} "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_FOR_ACCOUNT_BY_CAZ_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND vc.caz_id = ?#{[1]}";

    private static final String VEHICLES_FOR_ACCOUNT_BY_VRN_AND_CAZ =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND vc.caz_id = ?#{[1]} "
            + "AND av.vrn LIKE %?#{[2]}% "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_FOR_ACCOUNT_BY_VRN_AND_CAZ_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND vc.caz_id = ?#{[1]} "
            + "AND av.vrn LIKE %?#{[2]}%";

    private static final String VEHICLES_CHARGEABLE_FOR_ACCOUNT_BY_CAZ =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND (vc.charge is null OR vc.charge > 0) "
            + "AND vc.caz_id = ?#{[1]} "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_CHARGEABLE_FOR_ACCOUNT_BY_CAZ_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND (vc.charge is null OR vc.charge > 0) "
            + "AND vc.caz_id = ?#{[1]}";

    private static final String VEHICLES_CHARGEABLE_FOR_ACCOUNT_BY_VRN_AND_CAZ =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND (vc.charge is null OR vc.charge > 0) "
            + "AND vc.caz_id = ?#{[1]} "
            + "AND av.vrn LIKE %?#{[2]}% "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_CHARGEABLE_FOR_ACCOUNT_BY_VRN_AND_CAZ_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND (vc.charge is null OR vc.charge > 0) "
            + "AND vc.caz_id = ?#{[1]} "
            + "AND av.vrn LIKE %?#{[2]}%";

    private static final String VEHICLES_DETERMINED_FOR_ACCOUNT_BY_CAZ =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND vc.charge is not null "
            + "AND vc.caz_id = ?#{[1]} "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_DETERMINED_FOR_ACCOUNT_BY_CAZ_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND vc.charge is not null "
            + "AND vc.caz_id = ?#{[1]} ";

    private static final String VEHICLES_DETERMINED_FOR_ACCOUNT_BY_VRN_AND_CAZ =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND vc.charge is not null "
            + "AND vc.caz_id = ?#{[1]} "
            + "AND av.vrn LIKE %?#{[2]}% "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_DETERMINED_FOR_ACCOUNT_BY_VRN_AND_CAZ_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND vc.charge is not null "
            + "AND vc.caz_id = ?#{[1]} "
            + "AND av.vrn LIKE %?#{[2]}%";

    private static final String VEHICLES_DETERMINED_CHARGEABLE_FOR_ACCOUNT_BY_CAZ =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND (vc.charge is not null AND vc.charge > 0) "
            + "AND vc.caz_id = ?#{[1]} "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_DETERMINED_CHARGEABLE_FOR_ACCOUNT_BY_CAZ_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND (vc.charge is not null AND vc.charge > 0) "
            + "AND vc.caz_id = ?#{[1]} ";

    private static final String VEHICLES_DETERMINED_CHARGEABLE_FOR_ACCOUNT_BY_VRN_AND_CAZ =
        "SELECT distinct av.* "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND (vc.charge is not null AND vc.charge > 0) "
            + "AND vc.caz_id = ?#{[1]} "
            + "AND av.vrn LIKE %?#{[2]}% "
            + "ORDER BY av.vrn ASC";

    private static final String VEHICLES_DETERMINED_CHARGEABLE_FOR_ACCOUNT_BY_VRN_AND_CAZ_COUNT =
        "SELECT count(distinct av) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc ON "
            + "vc.account_vehicle_id = av.account_vehicle_id "
            + "WHERE av.account_id = ?#{[0]} "
            + "AND (vc.charge is not null AND vc.charge > 0) "
            + "AND vc.caz_id = ?#{[1]} "
            + "AND av.vrn LIKE %?#{[2]}%";
  }
}
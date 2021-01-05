package uk.gov.caz.accounts.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
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

  @Modifying
  @Query(value = "delete from AccountVehicle av where av.accountId = ?1")
  void deleteInBulkByAccountId(UUID accountId);

  @Query(value = Sql.CURSOR_NEXT, nativeQuery = true)
  List<UUID> findPageByAccountIdAndCursorNextPage(UUID accountId,
      String vrn, UUID cazId, long limit);

  @Query(value = Jpql.CURSOR_NEXT_WITH_CHARGEABILITY)
  List<AccountVehicle> findAllWithChargeabilityByAccountVehicleIdsCursorNext(
      List<UUID> accountVehicleIds);

  @Query(value = Sql.CURSOR_PREV, nativeQuery = true)
  List<UUID> findPageByAccountIdAndCursorPrevPage(UUID accountId,
      String vrn, UUID cazId, long limit);

  @Query(value = Jpql.CURSOR_PREV_WITH_CHARGEABILITY)
  List<AccountVehicle> findAllWithChargeabilityByAccountVehicleIdsCursorPrev(
      List<UUID> accountVehicleIds);

  @Query(value = Sql.NULL_VRN, nativeQuery = true)
  List<UUID> findPageByAccountIdAndCursorWithEmptyVrn(UUID accountId, UUID cazId, long limit);

  @Query(value = Jpql.VRN_WITH_CHARGEABILITY)
  List<AccountVehicle> findAllWithChargeabilityWith(List<UUID> accountVehicleIds);

  @Query(value = Sql.VEHICLES_FOR_ACCOUNT_WITH_CHARGEABILITY,
      countQuery = Sql.VEHICLES_FOR_ACCOUNT_WITH_CHARGEABILITY_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllWithChargeabilityFor(UUID accountId, Pageable pageable);

  @Query(value = Sql.VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_CHARGEABILITY,
      countQuery = Sql.VEHICLES_FOR_ACCOUNT_BY_QUERY_WITH_CHARGEABILITY_COUNT,
      nativeQuery = true)
  Page<AccountVehicle> findAllByVrnContainingWithChargeabilityFor(UUID accountId, String query,
      Pageable pageable);

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

    private static final String CURSOR_NEXT =
        "SELECT Cast(av.account_vehicle_id as varchar)"
            + " FROM caz_account.t_account_vehicle av"
            + " LEFT JOIN caz_account.t_vehicle_chargeability vc"
            + " ON av.account_vehicle_id = vc.account_vehicle_id"
            + " WHERE av.account_id=?#{[0]} AND av.vrn > ?#{[1]}"
            + " AND vc.caz_id=?#{[2]} "
            + " AND (vc.charge is not null AND vc.charge > 0)"
            + " ORDER BY av.vrn ASC"
            + " LIMIT ?#{[3]}";

    private static final String CURSOR_PREV =
        "SELECT Cast(av.account_vehicle_id as varchar)"
            + " FROM caz_account.t_account_vehicle av"
            + " LEFT JOIN caz_account.t_vehicle_chargeability vc"
            + " ON av.account_vehicle_id = vc.account_vehicle_id"
            + " WHERE av.account_id=?#{[0]} AND av.vrn < ?#{[1]}"
            + " AND vc.caz_id=?#{[2]}"
            + " AND (vc.charge is not null AND vc.charge > 0)"
            + " ORDER BY av.vrn DESC"
            + " LIMIT ?#{[3]}";

    private static final String NULL_VRN =
        "SELECT Cast(av.account_vehicle_id as varchar) "
            + "FROM caz_account.t_account_vehicle av "
            + "LEFT JOIN caz_account.t_vehicle_chargeability vc "
            + "ON av.account_vehicle_id = vc.account_vehicle_id "
            + "WHERE av.account_id=?#{[0]} "
            + "AND vc.caz_id=?#{[1]} "
            + "AND (vc.charge is not null AND vc.charge > 0) "
            + "ORDER BY av.vrn ASC "
            + "LIMIT ?#{[2]}";


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
  }

  /**
   * Static inner class holding custom JPQL queries.
   */
  @UtilityClass
  class Jpql {

    private static final String CURSOR_NEXT_WITH_CHARGEABILITY = "select distinct av "
        + "from AccountVehicle av "
        + "left join fetch av.vehicleChargeability "
        + "where av.accountVehicleId in (?1) "
        + "ORDER BY av.vrn ASC";

    private static final String CURSOR_PREV_WITH_CHARGEABILITY = "select distinct av "
        + "from AccountVehicle av "
        + "left join fetch av.vehicleChargeability "
        + "where av.accountVehicleId in (?1) "
        + "ORDER BY av.vrn DESC";

    private static final String VRN_WITH_CHARGEABILITY = "select distinct av "
        + "from AccountVehicle av "
        + "left join fetch av.vehicleChargeability "
        + "where av.accountVehicleId in (?1) "
        + "ORDER BY av.vrn ASC";
  }
}
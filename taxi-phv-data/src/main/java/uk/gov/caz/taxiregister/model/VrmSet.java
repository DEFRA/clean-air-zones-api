package uk.gov.caz.taxiregister.model;

import com.google.common.collect.Sets;
import java.util.Set;
import lombok.ToString;
import lombok.Value;

/**
 * Wraps {@link Set} of {@link String} instances with VRMs.
 */
@Value
public class VrmSet {

  @ToString.Exclude
  Set<String> vrms;

  /**
   * Constructs new instance of {@link VrmSet} from {@link Set} of {@link String} with VRMs.
   *
   * @param vrms Set of VRMs.
   * @return New instance of {@link VrmSet}.
   */
  public static VrmSet from(Set<String> vrms) {
    return new VrmSet(vrms);
  }

  /**
   * Constructs new empty instance of {@link VrmSet}.
   *
   * @return New empty instance of {@link VrmSet}.
   */
  public static VrmSet empty() {
    return new VrmSet(Sets.newHashSet());
  }

  /**
   * Modifies current instance of {@link VrmSet} - creates union of current instance and provided
   * {@link Set} of {@link String} with VRMs.
   *
   * @param vrms Set of VRMs.
   * @return Current (now modified) instance of {@link VrmSet}. Can be used for chained calls.
   */
  public VrmSet union(Set<String> vrms) {
    this.vrms.addAll(vrms);
    return this;
  }

  /**
   * Modifies current instance of {@link VrmSet} - creates union of current instance and provided
   * {@link VrmSet}.
   *
   * @param vrmSet other instance of {@link VrmSet}.
   * @return Current (now modified) instance of {@link VrmSet}. Can be used for chained calls.
   */
  public VrmSet union(VrmSet vrmSet) {
    this.vrms.addAll(vrmSet.vrms);
    return this;
  }
}

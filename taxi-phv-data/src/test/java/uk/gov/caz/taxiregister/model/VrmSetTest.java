package uk.gov.caz.taxiregister.model;


import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VrmSetTest {

  @Test
  public void creationOfEmptyVrmSet() {
    // when
    VrmSet emptySet = VrmSet.empty();

    // then
    assertThat(emptySet.getVrms()).isEmpty();
  }

  @Test
  public void creationOfFilledVrmSet() {
    // given
    Set<String> setOfVrms = Sets.newHashSet("A1", "B2");

    // when
    VrmSet vrmSet = VrmSet.from(setOfVrms);

    // then
    assertThat(vrmSet.getVrms()).hasSize(2);
    assertThat(vrmSet.getVrms()).containsExactlyInAnyOrder("A1", "B2");
  }

  @Test
  public void unionOfSetOfVrms() {
    // given
    VrmSet vrmSet = VrmSet.from(Sets.newHashSet("A1", "B2"));
    Set<String> additionalSetOfVrms = Sets.newHashSet("B2", "C3");

    // when
    vrmSet.union(additionalSetOfVrms);

    // then
    assertThat(vrmSet.getVrms()).hasSize(3);
    assertThat(vrmSet.getVrms()).containsExactlyInAnyOrder("A1", "B2", "C3");
  }

  @Test
  public void unionOfVrmSet() {
    // given
    VrmSet vrmSet = VrmSet.from(Sets.newHashSet("A1", "B2"));
    VrmSet additionalVrmSet = VrmSet.from(Sets.newHashSet("B2", "C3"));

    // when
    vrmSet.union(additionalVrmSet);

    // then
    assertThat(vrmSet.getVrms()).hasSize(3);
    assertThat(vrmSet.getVrms()).containsExactlyInAnyOrder("A1", "B2", "C3");
  }
}
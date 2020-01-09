package uk.gov.caz.taxiregister.model;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import lombok.NonNull;
import lombok.Value;

@Value
public class LicensingAuthority implements Serializable {

  private static final long serialVersionUID = 8832627611087606403L;
  Integer id;

  @NonNull
  String name;

  public static LicensingAuthority withNameOnly(String name) {
    Preconditions.checkNotNull(name, "Name cannot be null");
    return new LicensingAuthority(null, name);
  }
}

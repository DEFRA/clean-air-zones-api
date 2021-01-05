package uk.gov.caz.accounts.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An entity for a prohibited term in the account's name.
 */
@Entity
@Table(schema = "caz_account", name = "t_prohibited_language")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProhibitedTerm {
  @Id
  @Column(name = "prohibited_language_id")
  Long id;

  String term;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  ProhibitedTermType type;
}

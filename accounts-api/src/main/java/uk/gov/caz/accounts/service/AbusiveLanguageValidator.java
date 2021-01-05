package uk.gov.caz.accounts.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.caz.accounts.model.ProhibitedTerm;
import uk.gov.caz.accounts.model.ProhibitedTermType;
import uk.gov.caz.accounts.repository.ProhibitedTermRepository;
import uk.gov.caz.accounts.service.exception.AbusiveNameException;

/**
 * Checks whether the passed input is valid in terms of proper language.
 */
@Component
@AllArgsConstructor
public class AbusiveLanguageValidator {

  private final ProhibitedTermRepository prohibitedTermRepository;
  private List<PatternWithTermType> prohibitedTermPatterns;
  private static final String NOT_ALPHANUMERIC_OR_SPACES = "[^A-Za-z0-9 ]";

  /**
   * Validates {@code accountName} whether it is abusive or contains a profanity.
   *
   * @throws AbusiveNameException if {@code accountName} is abusive or contains a profanity.
   */
  public void validate(String accountName) {
    initialiseCachedPatterns();
    Optional<PatternWithTermType> foundTerm = prohibitedTermPatterns.stream()
        .filter(prohibitedTermPattern -> matches(accountName, prohibitedTermPattern))
        .findFirst();
    foundTerm.ifPresent(prohibitedTerm -> {
      throw new AbusiveNameException(prohibitedTerm.getType());
    });
  }

  /**
   * Initialises the internal cache of patterns of prohibited words if not initialised.
   */
  private void initialiseCachedPatterns() {
    if (CollectionUtils.isEmpty(prohibitedTermPatterns)) {
      prohibitedTermPatterns = getProhibitedTermsPatterns();
    }
  }

  /**
   * Checks whether the {@code prohibitedTermPattern} matches {@code accountName}.
   */
  private boolean matches(String accountName, PatternWithTermType prohibitedTermPattern) {
    String accountNameStripped = stripExtraCharacters(accountName);
    Matcher matcher = prohibitedTermPattern.getPattern().matcher(accountNameStripped);
    return matcher.find();
  }

  /**
   * Returns {@code accountName} without extra characters, to make obscenity/abusive language
   * detection more productive.
   */
  private String stripExtraCharacters(String accountName) {
    return Normalizer.normalize(accountName, Normalizer.Form.NFD)
        .replaceAll(NOT_ALPHANUMERIC_OR_SPACES, "");
  }

  /**
   * Computes patterns of prohibited words.
   */
  private List<PatternWithTermType> getProhibitedTermsPatterns() {
    return StreamSupport.stream(prohibitedTermRepository.findAll().spliterator(), false)
        .map(prohibitedTerm -> PatternWithTermType.of(
            termToPattern(prohibitedTerm), prohibitedTerm.getType()
        )).collect(Collectors.toList());
  }

  /**
   * Maps {@code prohibitedTerm} to {@link Pattern}.
   */
  private Pattern termToPattern(ProhibitedTerm prohibitedTerm) {
    return Pattern.compile(termToRegex(prohibitedTerm), Pattern.CASE_INSENSITIVE);
  }

  /**
   * Maps {@code prohibitedTerm} to a regex.
   */
  private String termToRegex(ProhibitedTerm prohibitedTerm) {
    return "\\b" + prohibitedTerm.getTerm() + "\\b";
  }

  /**
   * Helper class for value objects holding a {@link Pattern} alongside an instance of {@link
   * ProhibitedTermType}.
   */
  @Value(staticConstructor = "of")
  private static class PatternWithTermType {

    Pattern pattern;
    ProhibitedTermType type;
  }
}

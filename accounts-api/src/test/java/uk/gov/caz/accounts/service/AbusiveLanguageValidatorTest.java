package uk.gov.caz.accounts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.accounts.model.ProhibitedTerm;
import uk.gov.caz.accounts.model.ProhibitedTermType;
import uk.gov.caz.accounts.repository.ProhibitedTermRepository;
import uk.gov.caz.accounts.service.exception.AbusiveNameException;

@ExtendWith(MockitoExtension.class)
class AbusiveLanguageValidatorTest {

  @Mock
  private ProhibitedTermRepository prohibitedTermRepository;

  @InjectMocks
  private AbusiveLanguageValidator validator;

  @Nested
  class CaseSensitivity {

    @ParameterizedTest
    @ValueSource(strings = {
        "sentence with baDwOrD",
        "baDWoRD must be a whole word",
        "BadworD and something else"
    })
    public void testMatching(String input) {
      // given
      mockProhibitedTermsRepositoryWith("badword");

      // when
      Throwable throwable = catchThrowable(() -> validator.validate(input));

      // then
      assertThat(throwable).isInstanceOf(AbusiveNameException.class);
    }
  }

  @Nested
  class ExactMatches {

    @ParameterizedTest
    @ValueSource(strings = {
        "sentence withbadword",
        "badwordmust be a whole word",
    })
    public void testNotMatching(String input) {
      // given
      mockProhibitedTermsRepositoryWith("badword");

      // when
      Throwable throwable = catchThrowable(() -> validator.validate(input));

      // then
      assertThat(throwable).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "sentence with badword",
        "badword must be a whole word",
        "badword and something else"
    })
    public void testMatching(String input) {
      // given
      mockProhibitedTermsRepositoryWith("badword");

      // when
      Throwable throwable = catchThrowable(() -> validator.validate(input));

      // then
      assertThat(throwable).isInstanceOf(AbusiveNameException.class);
    }

    @Nested
    class WhenTermConsistsOfMoreThanOneWord {

      @ParameterizedTest
      @ValueSource(strings = {
          "abcbad word",
          "bad wordmust be a whole word",
          "bad wordc"
      })
      public void testNotMatching(String input) {
        // given
        mockProhibitedTermsRepositoryWith("bad word");

        // when
        Throwable throwable = catchThrowable(() -> validator.validate(input));

        // then
        assertThat(throwable).isNull();
      }

      @ParameterizedTest
      @ValueSource(strings = {
          "abc bad word",
          "bad word must be a whole word",
          "bad word c"
      })
      public void testMatching(String input) {
        // given
        mockProhibitedTermsRepositoryWith("bad word");

        // when
        Throwable throwable = catchThrowable(() -> validator.validate(input));

        // then
        assertThat(throwable).isInstanceOf(AbusiveNameException.class);
      }
    }
  }

  @Nested
  class MatchesOfTextContainingSpecialOrLocalCharacters {

    @ParameterizedTest
    @ValueSource(strings = {
        "baD.wO.rD with dots",
        "bad-wo-rd with dashes",
        "bad!word!! with exclamation marks",
        "bad_wor_d with underscores",
        "bad,w,ord with commas"
    })
    public void matchesTextWithDashesDotsOrExclamationMarks(String input) {
      // given
      mockProhibitedTermsRepositoryWith("badword");

      // when
      Throwable throwable = catchThrowable(() -> validator.validate(input));

      // then
      assertThat(throwable).isInstanceOf(AbusiveNameException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "non-standard bądwórd",
        "bąd-wór-d with local characters and dashes"
    })
    public void matchesTermsUsingLocalCharacters(String input) {
      // given
      mockProhibitedTermsRepositoryWith("badword");

      // when
      Throwable throwable = catchThrowable(() -> validator.validate(input));

      // then
      assertThat(throwable).isInstanceOf(AbusiveNameException.class);
    }
  }

  private void mockProhibitedTermsRepositoryWith(String term) {
    given(prohibitedTermRepository.findAll()).willReturn(
        Collections.singleton(new ProhibitedTerm(1L, term, ProhibitedTermType.PROFANITY)));
  }
}
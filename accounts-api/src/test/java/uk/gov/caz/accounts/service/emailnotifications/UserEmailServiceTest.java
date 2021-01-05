package uk.gov.caz.accounts.service.emailnotifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import uk.gov.caz.accounts.dto.SendEmailRequest;

class UserEmailServiceTest {

  private static final String URL_ATTRIBUTE_NAME = "test-token";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final UserEmailService service = new TestEmailService("some-template-id", new ObjectMapper(),
      URL_ATTRIBUTE_NAME);

  @Test
  public void shouldOverrideUrlAttributeNameFromSubclass() {
    // given
    String verificationTokenUrl = "http://localhost";

    // when
    SendEmailRequest request = service.buildSendEmailRequest("a@b.com", verificationTokenUrl, EmailContext.empty());

    // then
    Map<String, String> personalisation = readToMap(request.getPersonalisation());
    assertThat(personalisation).containsEntry(URL_ATTRIBUTE_NAME, verificationTokenUrl);
  }

  @SneakyThrows
  private Map<String, String> readToMap(String body) {
    return OBJECT_MAPPER.readValue(body, new TypeReference<Map<String, String>>() {});
  }

  private static class TestEmailService extends UserEmailService {

    private final String localUrlAttributeName;

    public TestEmailService(String templateId,
        ObjectMapper objectMapper, String urlAttributeName) {
      super(templateId, objectMapper, urlAttributeName);
      localUrlAttributeName = urlAttributeName;
    }

    @Override
    protected Map<String, Object> createPersonalisationMap(EmailContext context) {
      return Collections.singletonMap(localUrlAttributeName, "someValue");
    }
  }
}
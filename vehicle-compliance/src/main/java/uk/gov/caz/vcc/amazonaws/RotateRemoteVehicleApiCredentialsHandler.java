package uk.gov.caz.vcc.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.awslambda.AwsHelpers;
import uk.gov.caz.vcc.Application;
import uk.gov.caz.vcc.domain.authentication.CredentialRotationRequest;
import uk.gov.caz.vcc.domain.authentication.VehicleApiCredentialRotationManager;

/**
 * Lambda handler for API credential rotation.
 *
 */
@Slf4j
public class RotateRemoteVehicleApiCredentialsHandler implements RequestStreamHandler {

  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
  private VehicleApiCredentialRotationManager vehicleApiCredentialRotationManager;
  private ObjectMapper obj = new ObjectMapper();

  /**
   * Handler override for consuming Lambda input object.
   */
  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler(Application.class);
      vehicleApiCredentialRotationManager =
          getBean(handler, VehicleApiCredentialRotationManager.class);
    }

    try {
      log.info("Deserializing input object");
      CredentialRotationRequest request =
          obj.readValue(inputStream, CredentialRotationRequest.class);
      vehicleApiCredentialRotationManager.generateNewCredential(request);
    } catch (JsonMappingException jme) {
      log.error("Failed to deserialize input object");
      log.error(jme.getMessage());
    } catch (Exception e) {
      log.error("Error invoking credential rotation method");
      log.error(e.getMessage());
    }
  }

  /**
   * Private helper for instantiating a VehicleApiCredentialRotationManager
   * instance.
   */
  private <T> T getBean(
      SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> exampleServiceClass) {
    return WebApplicationContextUtils
        .getWebApplicationContext(handler.getServletContext())
        .getBean(exampleServiceClass);
  }
}

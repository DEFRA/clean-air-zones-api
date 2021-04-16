package uk.gov.caz.vcc.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.mockito.Matchers.eq;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.amazonaws.serverless.proxy.RequestReader.API_GATEWAY_CONTEXT_PROPERTY;

import com.amazonaws.serverless.proxy.model.ApiGatewayRequestIdentity;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;

public class VrnLogFormatterTest {

	private VrnLogFormatter<HttpServletRequest, HttpServletResponse> formatter = new VrnLogFormatter<HttpServletRequest, HttpServletResponse>();
	private HttpServletRequest mockServletRequest;
	private HttpServletResponse mockServletResponse;
	private AwsProxyRequest proxyRequest;
	private AwsProxyRequestContext context;
	private String filteredRequestStringIdentifer = "Filtered Request";

	@Before
	public void setup() {
		proxyRequest = new AwsProxyRequest();
		mockServletRequest = mock(HttpServletRequest.class);
		context = new AwsProxyRequestContext();
		context.setIdentity(new ApiGatewayRequestIdentity());
		when(mockServletRequest.getAttribute(eq(API_GATEWAY_CONTEXT_PROPERTY))).thenReturn(context);
		mockServletResponse = mock(HttpServletResponse.class);
		proxyRequest.setRequestContext(context);
		
		when(mockServletRequest.getMethod()).thenReturn("GET");
	}
	
	@Test
	public void nonMatchingTextIsNotFiltered() {
		// given
		String testString = "/non-matching";
		when(mockServletRequest.getRequestURI()).thenReturn(testString);

		// then
		String output = formatter.format(mockServletRequest, mockServletResponse, null);
		Assert.assertTrue(output.contains(testString));
		Assert.assertFalse(output.contains(filteredRequestStringIdentifer));
	}
	
	@Test
	public void complianceCheckerDetailsGetsMasked() {
		// given
		String testString = "https://test.com/v1/compliance-checker/vehicles/abc123/details";
		when(mockServletRequest.getRequestURI()).thenReturn(testString);
		
		// then
		String output = formatter.format(mockServletRequest, mockServletResponse, null);
		Assert.assertFalse(output.contains(testString));
		Assert.assertTrue(output.contains(filteredRequestStringIdentifer));
		
	}
	
	@Test
	public void complianceCheckerComplianceGetsMasked() {
		//given
		String testString = "https://test.com/v1/compliance-checker/vehicles/abc123/compliance";
		when(mockServletRequest.getRequestURI()).thenReturn(testString);
		
		//then
		String output = formatter.format(mockServletRequest, mockServletResponse, null);
		Assert.assertFalse(output.contains(testString));
		Assert.assertTrue(output.contains(filteredRequestStringIdentifer));
	}
	
	@Test
	public void complianceCheckerExternalDetailsGetsMasked( ) {
		// given
		String testString = "https://test.com/v1/compliance-checker/vehicles/abc123/external-details";
		when(mockServletRequest.getRequestURI()).thenReturn(testString);
		
		//then
		String output = formatter.format(mockServletRequest, mockServletResponse, null);
		Assert.assertFalse(output.contains(testString));
		Assert.assertTrue(output.contains(filteredRequestStringIdentifer));
		
	}

	@Test
	public void complianceCheckerRegisterDetailsGetsMasked() {
		// given
		String testString = "https://test.com/v1/compliance-checker/vehicles/abc123/register-details";
		when(mockServletRequest.getRequestURI()).thenReturn(testString);
		
		// then
		String output = formatter.format(mockServletRequest, mockServletResponse, null);
		Assert.assertFalse(output.contains(testString));
		Assert.assertTrue(output.contains(filteredRequestStringIdentifer));
	}
	
	@Test
	public void vehiclesLicenceInfoGetsMasked() {
		// given
		String testString = "https://test.com/v1/vehicles/abc123/licence-info";
		when(mockServletRequest.getRequestURI()).thenReturn(testString);
		
		// then 
		String output = formatter.format(mockServletRequest, mockServletResponse, null);
		Assert.assertFalse(output.contains(testString));
		Assert.assertTrue(output.contains(filteredRequestStringIdentifer));
	}
}
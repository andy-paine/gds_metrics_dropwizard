package engineering.reliability.gds.metrics.filter;

import engineering.reliability.gds.metrics.config.Configuration;
import engineering.reliability.gds.metrics.mock.MockHttpServletRequest;
import engineering.reliability.gds.metrics.mock.MockHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import java.io.IOException;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Configuration.class})
public class AuthenticationFilterTest {

	@InjectMocks
	private AuthenticationFilter authenticationFilter;

	@Mock
	private FilterChain chain;

	@Mock
	private FilterConfig config;

	@Mock
	private Configuration configuration;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@Before
	public void setUp() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	@Test
	public void notRelevantPath() throws ServletException, IOException {
		request.setRequestURI("/index");
		Mockito.when(configuration.getPrometheusMetricsPath()).thenReturn("/metrics");

		prepareConfigurationMock();
		testFilter();

		assertThat(response.getStatus(), equalTo(200));
	}

	@Test
	public void relevantPathAndAuthDisabled() throws ServletException, IOException {
		request.setRequestURI("/metrics");
		Mockito.when(configuration.getPrometheusMetricsPath()).thenReturn("/metrics");
		Mockito.when(configuration.isAuthEnable()).thenReturn(false);

		prepareConfigurationMock();
		testFilter();

		assertThat(response.getStatus(), equalTo(200));
	}

	@Test
	public void relevantPathAndAuthEnabledWithWrongToken() throws ServletException, IOException {
		request.setRequestURI("/metrics");
		request.setHeader("Authorization", "Bearer correct");
		Mockito.when(configuration.getPrometheusMetricsPath()).thenReturn("/metrics");
		Mockito.when(configuration.isAuthEnable()).thenReturn(true);
		Mockito.when(configuration.getApplicationId()).thenReturn("wrong");

		prepareConfigurationMock();
		testFilter();

		assertThat(response.getStatus(), equalTo(401));
	}

	@Test
	public void relevantPathAndAuthEnabledWithCorrectToken() throws ServletException, IOException {
		Mockito.when(configuration.getPrometheusMetricsPath()).thenReturn("/metrics");
		request.setRequestURI("/metrics");
		request.setHeader("Authorization", "Bearer correct");
		Mockito.when(configuration.isAuthEnable()).thenReturn(true);
		Mockito.when(configuration.getApplicationId()).thenReturn("correct");

		prepareConfigurationMock();
		testFilter();

		assertThat(response.getStatus(), equalTo(200));
	}

	private void prepareConfigurationMock() {
		mockStatic(Configuration.class);
		when(Configuration.getInstance()).thenReturn(configuration);
	}

	private void testFilter() throws ServletException, IOException {
		authenticationFilter.init(config);
		authenticationFilter.doFilter(request, response, chain);
		authenticationFilter.destroy();
	}
}

package edu.iu.uits.lms.viewem;

/*-
 * #%L
 * lms-canvas-viewem
 * %%
 * Copyright (C) 2015 - 2026 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvasoauth2.CanvasOAuth2Registration;
import edu.iu.uits.lms.canvasoauth2.security.CanvasOAuth2AuthorizedClientRepository;
import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import edu.iu.uits.lms.viewem.config.SecurityConfig;
import edu.iu.uits.lms.viewem.config.ToolConfig;
import edu.iu.uits.lms.viewem.controller.MainController;
import edu.iu.uits.lms.viewem.repository.SheetRepository;
import edu.iu.uits.lms.viewem.repository.SheetUserRepository;
import edu.iu.uits.lms.viewem.repository.SystemUserRepository;
import edu.iu.uits.lms.viewem.service.SystemUserService;
import edu.iu.uits.lms.viewem.service.ViewemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestTemplate;
import uk.ac.ox.ctl.lti13.lti.Claims;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = MainController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@ContextConfiguration(classes = {ToolConfig.class, MainController.class, SecurityConfig.class,
        edu.iu.uits.lms.canvasoauth2.controller.OAuth2ConsentControllerAdvice.class,
        edu.iu.uits.lms.canvasoauth2.controller.CanvasOAuth2ConsentText.class,
        AppLaunchSecurityTest.TestConfig.class})
public class AppLaunchSecurityTest {

    private static final String REGISTRATION_ID = "lms_canvas_oauth2_viewem";

    /**
     * {@code CanvasOAuth2Registration} is no longer a {@code @Component} Spring can auto-detect by
     * listing its class directly - it's only ever produced by {@code CanvasOAuth2ClientConfig}'s
     * {@code @Bean} method, which itself needs {@code @EnableCanvasOAuth2Client}'s real
     * {@code ImportAware} wiring to know its suffix. This narrow {@code @WebMvcTest} slice
     * deliberately avoids pulling in the full {@code CanvasOAuth2ClientConfig} (with its JPA/DataSource
     * machinery), so it supplies the one bean it actually needs directly instead.
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        public CanvasOAuth2Registration canvasOAuth2Registration() {
            return new CanvasOAuth2Registration("viewem", "/app/jsrivet");
        }

        /**
         * A plain {@code @Bean} rather than {@code @MockitoBean}: {@code CanvasOAuth2AuthorizedClientRepository}
         * also implements {@code OAuth2AuthorizedClientRepository}, which
         * {@code OAuth2ClientWebSecurityAutoConfiguration} auto-configures its own default bean for via
         * {@code @ConditionalOnMissingBean}. That condition check doesn't recognize a same-named
         * {@code @MockitoBean} of the narrower concrete type as already satisfying it, so both beans get
         * created and autowiring the interface type elsewhere becomes ambiguous. A regular {@code @Bean}
         * factory method participates in that condition check correctly and satisfies both the concrete
         * type ({@code OAuth2ConsentControllerAdvice}'s dependency) and the interface type
         * ({@code SecurityConfig}'s filter chain) from a single instance.
         */
        @Bean
        public CanvasOAuth2AuthorizedClientRepository canvasOAuth2AuthorizedClientRepository() {
            return mock(CanvasOAuth2AuthorizedClientRepository.class);
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LmsDefaultGrantedAuthoritiesMapper lmsDefaultGrantedAuthoritiesMapper;
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;
    @MockitoBean(name = ServerInfo.BEAN_NAME)
    private ServerInfo serverInfo;

    @MockitoBean
    private ViewemService viewemService;
    @MockitoBean
    private ResourceBundleMessageSource messageSource;
    @MockitoBean
    private SheetRepository sheetRepository;
    @MockitoBean
    private SheetUserRepository sheetUserRepository;
    @MockitoBean
    private SystemUserRepository systemUserRepository;
    @MockitoBean
    private CourseSessionService courseSessionService;
    @MockitoBean
    private SystemUserService systemUserService;
    @MockitoBean
    private CourseService courseService;
    @MockitoBean(name = "CanvasRestTemplateAsUser")
    private RestTemplate canvasRestTemplateAsUser;
    // SecurityConfig now @Autowired-injects this from CanvasOAuth2ClientConfig, which this narrow
    // @WebMvcTest slice deliberately doesn't pull in (see TestConfig above) - it's never invoked by
    // any of these tests, only needed to satisfy the filter chain's dependency at context-build time.
    @MockitoBean
    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> canvasOAuth2AccessTokenResponseClient;
    // Provided by TestConfig's @Bean (not @MockitoBean - see its javadoc). Satisfies both
    // OAuth2ConsentControllerAdvice's concrete-type dependency and SecurityConfig's interface-type one.
    @Autowired
    private CanvasOAuth2AuthorizedClientRepository canvasOAuth2AuthorizedClientRepository;

    @BeforeEach
    void resetCanvasOAuth2AuthorizedClientRepositoryMock() {
        // TestConfig's @Bean isn't a @MockitoBean, so it doesn't get Mockito's automatic reset-between-
        // tests behavior - do it manually, since the ApplicationContext (and this same mock instance) is
        // cached and reused across every test method in this class.
        reset(canvasOAuth2AuthorizedClientRepository);
        // Defaults to "resolvable" so every test below - all built from fully-populated
        // OidcAuthenticationTokens - is unaffected by OAuth2ConsentControllerAdvice's fail-fast check.
        when(canvasOAuth2AuthorizedClientRepository.hasResolvableCanvasUserId(any())).thenReturn(true);
    }

    @Test
    public void appAuthnLaunchRequiresCanvasOAuth2ConsentWhenNoAuthorizedClient() throws Exception {
        when(canvasOAuth2AuthorizedClientRepository.loadAuthorizedClient(eq(REGISTRATION_ID), any(), any())).thenReturn(null);

        Map<String, Object> extraAttributes = new HashMap<>();
        Map<String, Object> platformObject = new HashMap<>();
        platformObject.put(LTIConstants.CLAIMS_PLATFORM_GUID_KEY, "systemId");
        extraAttributes.put(Claims.PLATFORM_INSTANCE, platformObject);

        Map<String, Object> customMap = new HashMap<>();
        customMap.put(LTIConstants.CUSTOM_CANVAS_COURSE_ID_KEY, "1234");

        OidcAuthenticationToken token = TestUtils.buildToken("userId", LTIConstants.INSTRUCTOR_AUTHORITY,
                extraAttributes, customMap);

        mvc.perform(get("/app/launch")
                        .with(authentication(token))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("connectCanvas"))
                .andExpect(MockMvcResultMatchers.model().attribute("authorizationUri", "/oauth2/authorization/" + REGISTRATION_ID));

        // The roster call must never happen before Canvas OAuth2 consent has been established.
        verifyNoInteractions(courseService);
    }

    @Test
    public void appAuthnLaunchFetchesRosterWithPerUserRestTemplateWhenAuthorizedClientExists() throws Exception {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId("test-client")
                .clientSecret("test-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://canvas.test/login/oauth2/auth")
                .tokenUri("https://canvas.test/login/oauth2/token")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "test-access-token", Instant.now(), Instant.now().plusSeconds(3600));
        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(clientRegistration, "userId", accessToken);
        when(canvasOAuth2AuthorizedClientRepository.loadAuthorizedClient(eq(REGISTRATION_ID), any(), any()))
                .thenReturn(authorizedClient);

        Map<String, Object> extraAttributes = new HashMap<>();
        Map<String, Object> platformObject = new HashMap<>();
        platformObject.put(LTIConstants.CLAIMS_PLATFORM_GUID_KEY, "systemId");
        extraAttributes.put(Claims.PLATFORM_INSTANCE, platformObject);

        Map<String, Object> customMap = new HashMap<>();
        customMap.put(LTIConstants.CUSTOM_CANVAS_COURSE_ID_KEY, "1234");

        OidcAuthenticationToken token = TestUtils.buildToken("userId", LTIConstants.INSTRUCTOR_AUTHORITY,
                extraAttributes, customMap);

        //Instructor with a valid Canvas OAuth2 authorized client should reach the roster-fetching success path
        mvc.perform(get("/app/launch")
                        .with(authentication(token))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("listSheets"));

        // The migration's actual behavior change: the roster call must use the per-user RestTemplate,
        // not the shared admin one.
        verify(courseService).getRosterForCourseAsUser(eq("1234"), isNull(), isNull(), eq(canvasRestTemplateAsUser));
    }

    @Test
    public void appAuthnLaunchAsStudentDoesNotRequireCanvasOAuth2Consent() throws Exception {
        Map<String, Object> extraAttributes = new HashMap<>();
        Map<String, Object> platformObject = new HashMap<>();
        platformObject.put(LTIConstants.CLAIMS_PLATFORM_GUID_KEY, "systemId");
        extraAttributes.put(Claims.PLATFORM_INSTANCE, platformObject);

        Map<String, Object> customMap = new HashMap<>();
        customMap.put(LTIConstants.CUSTOM_CANVAS_COURSE_ID_KEY, "1234");

        OidcAuthenticationToken token = TestUtils.buildToken("userId", LTIConstants.STUDENT_AUTHORITY,
                extraAttributes, customMap);

        // A student never reaches the instructor-gated roster fetch, so it must not be forced through
        // the Canvas OAuth2 consent breakout either - only the instructor path needs a token at all.
        mvc.perform(get("/app/launch")
                        .with(authentication(token))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("listSheets"));

        verifyNoInteractions(courseService);
        verifyNoInteractions(canvasOAuth2AuthorizedClientRepository);
    }

    @Test
    public void appNoAuthnLaunch() throws Exception {
        //This is a secured endpoint and should not allow access without authn
        mvc.perform(get("/app/index/1234")
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void appAuthnWrongContextLaunch() throws Exception {
        OidcAuthenticationToken token = TestUtils.buildToken("userId", "asdf", LTIConstants.INSTRUCTOR_AUTHORITY);

        // Context in token ("asdf") does not match the request URL ("1234"), so the
        // tokenError page is rendered
        mvc.perform(get("/app/1234/list")
                        .with(authentication(token))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("tokenError"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("exception"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("stackTrace"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("timestamp"));
    }

    @Test
    public void appAuthnLaunch() throws Exception {
        Map<String, Object> extraAttributes = new HashMap<>();

        Map<String, Object> platformObject = new HashMap<>();
        platformObject.put(LTIConstants.CLAIMS_PLATFORM_GUID_KEY, "systemId");
        extraAttributes.put(Claims.PLATFORM_INSTANCE, platformObject);

        Map<String, Object> customMap = new HashMap<>();
        customMap.put(LTIConstants.CUSTOM_CANVAS_COURSE_ID_KEY, "1234");

        OidcAuthenticationToken token = TestUtils.buildToken("userId", LTIConstants.INSTRUCTOR_AUTHORITY,
                extraAttributes, customMap);

        //Instructor with matching context should successfully reach the list view
        mvc.perform(get("/app/1234/list")
                        .with(authentication(token))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void randomUrlNoAuth() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(null);
        //This is a secured endpoint and should not allow access without authn
        mvc.perform(get("/asdf/foobar")
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void randomUrlWithAuth() throws Exception {
        OidcAuthenticationToken token = TestUtils.buildToken("userId", "foo", TestUtils.defaultAuthority());

        // This should pass authentication and then 404 because no controller mapping exists.
        mvc.perform(get("/asdf/foobar")
                        .with(authentication(token))
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

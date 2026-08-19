package edu.iu.uits.lms.viewem.config;

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

import edu.iu.uits.lms.lti.config.EnableLtiClient;
import edu.iu.uits.lms.lti.service.LtiAuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.ac.ox.ctl.lti13.nrps.NamesRoleService;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Narrow slice test for the shared {@code lms_canvas_oauth2} client registration.
 * <p>
 * A bare {@code @Configuration} annotated only with {@code @EnableLtiClient} is NOT enough to load
 * successfully under {@code @SpringBootTest(classes = ...)}: unlike the real {@code WebApplication}
 * (a {@code @SpringBootApplication}), a plain {@code @Configuration} class does not trigger Spring
 * Boot's auto-configuration, so {@code LtiClientConfig}'s own {@code @EnableJpaRepositories} /
 * {@code EntityManagerFactoryBuilder}-consuming beans have nothing to satisfy their JPA/DataSource
 * dependencies (verified experimentally: without {@code @EnableAutoConfiguration} the context fails
 * with "No qualifying bean of type EntityManagerFactoryBuilder", not the intended "registration not
 * found" failure). Adding {@code @EnableAutoConfiguration} pulls in DataSource/JPA auto-configuration,
 * which then requires the same workarounds documented in {@code WebApplicationTests}: a mocked
 * {@link DataSource} (so no real DB connection is attempted) and a mocked {@link NamesRoleService}
 * (since {@code LtiClientConfig.namesRoleService()} eagerly calls {@code Lti13Service.getJKS()},
 * which runs a real JPA query against the mocked datasource during singleton pre-instantiation).
 */
@SpringBootTest(classes = CanvasOAuth2RegistrationTest.TestConfig.class)
@TestPropertySource(properties = {
        "app.env=test",
        "canvas.host=canvas.test",
        "canvas.oauth2.clientId=test-client-id",
        "canvas.oauth2.clientSecret=test-client-secret",
        "lms.db.poolType=",
        "oauth.tokenprovider.url=http://foo"
})
class CanvasOAuth2RegistrationTest {

    @Configuration
    @EnableAutoConfiguration
    @EnableLtiClient
    static class TestConfig {
    }

    @MockitoBean
    private DataSource dataSource;

    @MockitoBean
    private NamesRoleService namesRoleService;

    @MockitoBean
    private BufferingApplicationStartup bufferingApplicationStartup;

    // LmsClientRegistrationRepository.buildRegistrations() unconditionally calls
    // LtiAuthorizationService.findByRegistrationsPrefixesEnvActive(...) - a real JPA query - to look
    // up any DB-backed LTI tool registrations *before* merging in the standard-Spring-Boot-property
    // registrations this test cares about. Against the mocked DataSource above that query would NPE
    // on a null JDBC connection (verified experimentally). Mock the service out; Mockito's default
    // answer for a List-returning method is an empty list, so buildRegistrations() simply sees no
    // DB-backed registrations and falls through to the OAuth2ClientPropertiesMapper merge.
    @MockitoBean
    private LtiAuthorizationService ltiAuthorizationService;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void resolvesSharedCanvasOAuth2Registration() {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("lms_canvas_oauth2");

        assertNotNull(registration);
        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, registration.getAuthorizationGrantType());
        assertEquals("test-client-id", registration.getClientId());
    }
}

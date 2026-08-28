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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.ac.ox.ctl.lti13.nrps.NamesRoleService;

import javax.sql.DataSource;

/**
 * Full-context smoke test for the real {@link WebApplication} class.
 * <p>
 * Unlike {@code AppLaunchSecurityTest} (a narrow {@code @WebMvcTest} slice restricted to
 * {@code ToolConfig}/{@code MainController}/{@code SecurityConfig}) and {@code SwaggerSuiteTest}
 * (loads a hand-curated {@code ViewemSwaggerConfig}, not the real application), this test boots
 * {@link WebApplication} itself with no {@code classes=} override, so every {@code @Enable*}
 * annotation on it - including {@code @EnableCanvasOAuth2Client} - actually runs, exactly as it
 * would in production. This is the only test in this module that would catch a bean failing to
 * construct at real application startup, e.g. {@code CanvasOAuth2AuthorizedClientRepository}
 * throwing an NPE out of {@code Encryptors.delux(null, null)} when
 * {@code canvas.oauth2.encryptionPassword}/{@code canvas.oauth2.encryptionSalt} aren't supplied -
 * a failure {@code @EnableCanvasOAuth2Client}'s own component scan does not gate behind
 * {@code canvas.oauth2.enabled}, so it happens regardless of that flag.
 * <p>
 * Follows the same pattern as {@code app-dashboard}'s {@code GitDashboardApplicationTests}: mock
 * out the tool's own primary {@link DataSource} bean (so its {@code @Bean} factory method, which
 * needs a real {@code lms.db.url}, never runs) and {@link BufferingApplicationStartup} (only ever
 * registered by {@code WebApplication.main()}, which {@code @SpringBootTest} does not invoke).
 * Every other JPA-backed module here (viewem's own {@code PostgresDBConfig}, lti-framework's
 * {@code LtiClientConfig}, canvas-oauth2-client's {@code CanvasOAuth2ClientConfig}) reuses that
 * same mocked datasource via {@code @ConditionalOnMissingBean}, so no other datasource stand-in
 * is needed. {@code canvas.oauth2.encryptionPassword}/{@code encryptionSalt} are supplied here as
 * test-only property overrides - exactly what a real deployment's {@code security.properties}
 * would otherwise provide - never hardcoded into {@code application.yml}.
 */
@SpringBootTest(properties = {
        "oauth.tokenprovider.url=http://foo",
        "lms.db.poolType=",
        "canvas.token=test-only-placeholder-token",
        "catalog.token=test-only-placeholder-token",
        "lti.errorcontact.name=foo",
        "lti.errorcontact.link=foo",
        "canvas.oauth2.encryptionPassword=test-only-placeholder-password",
        "canvas.oauth2.encryptionSalt=deadbeef"
})
class WebApplicationTests {

    @MockitoBean
    @Qualifier("viewemDataSource")
    private DataSource dataSource;

    @MockitoBean
    private BufferingApplicationStartup bufferingApplicationStartup;

    // LtiClientConfig.namesRoleService() eagerly calls Lti13Service.getJKS(), which runs a real
    // JPA query (findFirstByOrderByIdAsc()) against the (mocked) datasource during singleton
    // pre-instantiation - unlike every other JPA repository bean here, it can't just ride along
    // on lazy Hibernate bootstrapping against a mocked DataSource. Overriding the bean entirely
    // skips that factory method (and its eager query) altogether.
    @MockitoBean
    private NamesRoleService namesRoleService;

    @Test
    void contextLoads() {
        // Intentionally empty - just needs the ApplicationContext to refresh successfully.
    }

}

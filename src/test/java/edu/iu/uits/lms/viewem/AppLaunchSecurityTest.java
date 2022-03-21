package edu.iu.uits.lms.viewem;

/*-
 * #%L
 * lms-canvas-viewem
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
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

import com.nimbusds.jose.shaded.json.JSONObject;
import edu.iu.uits.lms.canvas.config.CanvasClientTestConfig;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.LtiClientTestConfig;
import edu.iu.uits.lms.lti.service.TestUtils;
import edu.iu.uits.lms.viewem.config.ToolConfig;
import edu.iu.uits.lms.viewem.controller.MainController;
import edu.iu.uits.lms.viewem.repository.SheetRepository;
import edu.iu.uits.lms.viewem.repository.SheetUserRepository;
import edu.iu.uits.lms.viewem.repository.SystemUserRepository;
import edu.iu.uits.lms.viewem.service.SystemUserService;
import edu.iu.uits.lms.viewem.service.ViewemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.ac.ox.ctl.lti13.lti.Claims;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = MainController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@Import({ToolConfig.class, CanvasClientTestConfig.class, LtiClientTestConfig.class})
public abstract class AppLaunchSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ViewemService viewemService = null;
    @MockBean
    private ResourceBundleMessageSource messageSource = null;
    @MockBean
    private SheetRepository sheetRepository = null;
    @MockBean
    private SheetUserRepository sheetUserRepository = null;
    @MockBean
    private SystemUserRepository systemUserRepository = null;
    @MockBean
    private CourseSessionService courseSessionService = null;
    @MockBean
    private SystemUserService systemUserService = null;
    @MockBean
    private CourseService courseService = null;

    @Test
    public void appNoAuthnLaunch() throws Exception {
        //This is a secured endpoint and should not not allow access without authn
        mvc.perform(get("/app/index/1234")
                        .header(HttpHeaders.USER_AGENT, edu.iu.uits.lms.lti.service.TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void appAuthnWrongContextLaunch() throws Exception {
        OidcAuthenticationToken token = edu.iu.uits.lms.lti.service.TestUtils.buildToken("userId", "asdf", LTIConstants.INSTRUCTOR_AUTHORITY);

        SecurityContextHolder.getContext().setAuthentication(token);

        //This is a secured endpoint and should not allow access without authn
        mvc.perform(get("/app/1234/list")
                        .header(HttpHeaders.USER_AGENT, edu.iu.uits.lms.lti.service.TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.view().name ("tokenError"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("exception"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("stackTrace"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("timestamp"));
    }

    @Test
    public void appAuthnLaunch() throws Exception {
        Map<String, Object> extraAttributes = new HashMap<>();

        JSONObject platformObject = new JSONObject();
        platformObject.put(LTIConstants.CLAIMS_PLATFORM_GUID_KEY, "systemId");
        extraAttributes.put(Claims.PLATFORM_INSTANCE, platformObject);

        JSONObject customMap = new JSONObject();
        customMap.put(LTIConstants.CUSTOM_CANVAS_COURSE_ID_KEY, "1234");

        OidcAuthenticationToken token = edu.iu.uits.lms.lti.service.TestUtils.buildToken("userId", LTIConstants.INSTRUCTOR_AUTHORITY,
                extraAttributes, customMap);

        SecurityContextHolder.getContext().setAuthentication(token);

        //This is a secured endpoint and should not not allow access without authn
        mvc.perform(get("/app/1234/list")
                        .header(HttpHeaders.USER_AGENT, edu.iu.uits.lms.lti.service.TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void randomUrlNoAuth() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(null);
        //This is a secured endpoint and should not not allow access without authn
        mvc.perform(get("/asdf/foobar")
                        .header(HttpHeaders.USER_AGENT, edu.iu.uits.lms.lti.service.TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void randomUrlWithAuth() throws Exception {
        OidcAuthenticationToken token = edu.iu.uits.lms.lti.service.TestUtils.buildToken("userId", "foo", edu.iu.uits.lms.lti.service.TestUtils.defaultRole());
        SecurityContextHolder.getContext().setAuthentication(token);

        //This is a secured endpoint and should not not allow access without authn
        mvc.perform(get("/asdf/foobar")
                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

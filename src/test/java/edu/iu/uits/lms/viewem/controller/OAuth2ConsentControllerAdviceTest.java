package edu.iu.uits.lms.viewem.controller;

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

import edu.iu.uits.lms.canvasoauth2.CanvasOAuth2Constants;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link OAuth2ConsentControllerAdvice}. Instantiated directly (no Spring context)
 * since the class under test has no Spring-managed dependencies of its own.
 */
class OAuth2ConsentControllerAdviceTest {

    private final OAuth2ConsentControllerAdvice advice = new OAuth2ConsentControllerAdvice();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handleClientAuthorizationRequired_returnsConnectViewAndRemembersCourse() {
        OidcAuthenticationToken token = TestUtils.buildToken("userId", "course-123", LTIConstants.INSTRUCTOR_AUTHORITY);
        SecurityContextHolder.getContext().setAuthentication(token);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/app/launch");
        ClientAuthorizationRequiredException exception = new ClientAuthorizationRequiredException("some-registration-id");

        ModelAndView mav = advice.handleClientAuthorizationRequired(request, exception);

        assertEquals("connectCanvas", mav.getViewName());
        assertEquals("/oauth2/authorization/some-registration-id", mav.getModel().get("authorizationUri"));
        assertEquals("course-123", request.getSession().getAttribute(OAuth2ConsentControllerAdvice.PENDING_COURSE_ID_SESSION_KEY));
        // TestUtils.buildToken's 3-arg overload doesn't populate canvas_external_tool_id - this proves
        // the older/simpler registration shape still works and doesn't leave a stale tool id behind.
        assertNull(request.getSession().getAttribute(OAuth2ConsentControllerAdvice.PENDING_TOOL_ID_SESSION_KEY));
        // The captured launch path doesn't depend on any token claim - it's whatever request this
        // tool's own launch endpoint happens to be, regardless of tool.
        assertEquals("/app/launch", request.getSession().getAttribute(OAuth2ConsentControllerAdvice.PENDING_LAUNCH_PATH_SESSION_KEY));
    }

    @Test
    void handleClientAuthorizationRequired_requestHasQueryString_launchPathIncludesIt() {
        OidcAuthenticationToken token = TestUtils.buildToken("userId", "course-123", LTIConstants.INSTRUCTOR_AUTHORITY);
        SecurityContextHolder.getContext().setAuthentication(token);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/some/other/tools/launch-path");
        request.setQueryString("foo=bar&baz=qux");
        ClientAuthorizationRequiredException exception = new ClientAuthorizationRequiredException("some-registration-id");

        advice.handleClientAuthorizationRequired(request, exception);

        assertEquals("/some/other/tools/launch-path?foo=bar&baz=qux",
                request.getSession().getAttribute(OAuth2ConsentControllerAdvice.PENDING_LAUNCH_PATH_SESSION_KEY));
    }

    @Test
    void handleClientAuthorizationRequired_tokenHasToolId_remembersToolIdToo() {
        Map<String, Object> customAttributes = new HashMap<>();
        customAttributes.put(LTIConstants.CUSTOM_CANVAS_COURSE_ID_KEY, "course-123");
        customAttributes.put(LTIConstants.CUSTOM_CANVAS_EXTERNAL_TOOL_ID_KEY, "tool-789");
        OidcAuthenticationToken token = TestUtils.buildToken("userId", LTIConstants.INSTRUCTOR_AUTHORITY,
                new HashMap<>(), customAttributes);
        SecurityContextHolder.getContext().setAuthentication(token);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/app/launch");
        ClientAuthorizationRequiredException exception = new ClientAuthorizationRequiredException("some-registration-id");

        advice.handleClientAuthorizationRequired(request, exception);

        assertEquals("course-123", request.getSession().getAttribute(OAuth2ConsentControllerAdvice.PENDING_COURSE_ID_SESSION_KEY));
        assertEquals("tool-789", request.getSession().getAttribute(OAuth2ConsentControllerAdvice.PENDING_TOOL_ID_SESSION_KEY));
    }

    @Test
    void handleOAuth2AuthorizationException_returnsConnectViewAndRemembersCourse() {
        OidcAuthenticationToken token = TestUtils.buildToken("userId", "course-456", LTIConstants.INSTRUCTOR_AUTHORITY);
        SecurityContextHolder.getContext().setAuthentication(token);

        MockHttpServletRequest request = new MockHttpServletRequest();
        OAuth2AuthorizationException exception = new OAuth2AuthorizationException(new OAuth2Error("invalid_token"));

        ModelAndView mav = advice.handleOAuth2AuthorizationException(request, exception);

        assertEquals("connectCanvas", mav.getViewName());
        assertEquals("/oauth2/authorization/" + CanvasOAuth2Constants.REGISTRATION_ID, mav.getModel().get("authorizationUri"));
        assertEquals("course-456", request.getSession().getAttribute(OAuth2ConsentControllerAdvice.PENDING_COURSE_ID_SESSION_KEY));
    }

    @Test
    void handleOAuth2AuthorizationException_doesNotOverwriteAlreadyRememberedLaunchPath() {
        // This handler runs on the /login/oauth2/code/... callback request itself, whose own URI must
        // never clobber the launch path captured earlier by handleClientAuthorizationRequired.
        OidcAuthenticationToken token = TestUtils.buildToken("userId", "course-456", LTIConstants.INSTRUCTOR_AUTHORITY);
        SecurityContextHolder.getContext().setAuthentication(token);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/login/oauth2/code/" + CanvasOAuth2Constants.REGISTRATION_ID);
        request.getSession(true).setAttribute(OAuth2ConsentControllerAdvice.PENDING_LAUNCH_PATH_SESSION_KEY, "/app/launch");
        OAuth2AuthorizationException exception = new OAuth2AuthorizationException(new OAuth2Error("invalid_token"));

        advice.handleOAuth2AuthorizationException(request, exception);

        assertEquals("/app/launch",
                request.getSession().getAttribute(OAuth2ConsentControllerAdvice.PENDING_LAUNCH_PATH_SESSION_KEY));
    }

    @Test
    void handleClientAuthorizationRequired_nonOidcPrincipal_stillReturnsConnectViewButDoesNotRememberCourse() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("someUser", "creds"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/app/launch");
        ClientAuthorizationRequiredException exception = new ClientAuthorizationRequiredException("some-registration-id");

        ModelAndView mav = advice.handleClientAuthorizationRequired(request, exception);

        assertEquals("connectCanvas", mav.getViewName());
        assertEquals("/oauth2/authorization/some-registration-id", mav.getModel().get("authorizationUri"));
        assertNull(request.getSession().getAttribute(OAuth2ConsentControllerAdvice.PENDING_COURSE_ID_SESSION_KEY));
        assertNull(request.getSession().getAttribute(OAuth2ConsentControllerAdvice.PENDING_TOOL_ID_SESSION_KEY));
        // The launch path is captured from the request itself, independent of principal type.
        assertEquals("/app/launch", request.getSession().getAttribute(OAuth2ConsentControllerAdvice.PENDING_LAUNCH_PATH_SESSION_KEY));
    }
}

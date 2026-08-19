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
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

/**
 * Catches OAuth2-consent-required conditions raised while resolving
 * {@code @RegisteredOAuth2AuthorizedClient} controller parameters (see MainController) and renders
 * the "connect your Canvas account" full-page breakout interstitial instead of an error page.
 */
@ControllerAdvice
@Slf4j
public class OAuth2ConsentControllerAdvice {

    static final String PENDING_COURSE_ID_SESSION_KEY = "viewem_oauth2_pending_course_id";
    static final String PENDING_TOOL_ID_SESSION_KEY = "viewem_oauth2_pending_tool_id";
    static final String PENDING_LAUNCH_PATH_SESSION_KEY = "viewem_oauth2_pending_launch_path";

    @ExceptionHandler(ClientAuthorizationRequiredException.class)
    public ModelAndView handleClientAuthorizationRequired(HttpServletRequest request, ClientAuthorizationRequiredException exception) {
        log.info("Canvas OAuth2 consent required for registration {}", exception.getClientRegistrationId());
        rememberPendingLaunchClaims(request);
        rememberPendingLaunchPath(request);
        return buildConnectView(exception.getClientRegistrationId());
    }

    @ExceptionHandler(OAuth2AuthorizationException.class)
    public ModelAndView handleOAuth2AuthorizationException(HttpServletRequest request, OAuth2AuthorizationException exception) {
        log.warn("Canvas OAuth2 authorization failed: {}", exception.getError(), exception);
        // Deliberately does NOT call rememberPendingLaunchPath: this handler runs on the OAuth2
        // callback request itself (/login/oauth2/code/...) when the token exchange fails, not on the
        // tool's original launch request - capturing *this* request's own URI here would clobber the
        // already-remembered launch path (from handleClientAuthorizationRequired) with the callback
        // URL instead.
        rememberPendingLaunchClaims(request);
        return buildConnectView(CanvasOAuth2Constants.REGISTRATION_ID);
    }

    /**
     * Remembers course/tool ids from the launch token, so {@code OAuth2CallbackController} can build
     * a Canvas relaunch URL as a fallback if no pending launch path was captured (see
     * {@link #rememberPendingLaunchPath}).
     */
    private void rememberPendingLaunchClaims(HttpServletRequest request) {
        Authentication principal = SecurityContextHolder.getContext().getAuthentication();
        if (principal instanceof OidcAuthenticationToken oidcAuthenticationToken) {
            OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(oidcAuthenticationToken);
            request.getSession().setAttribute(PENDING_COURSE_ID_SESSION_KEY, oidcTokenUtils.getCourseId());
            request.getSession().setAttribute(PENDING_TOOL_ID_SESSION_KEY, oidcTokenUtils.getExternalToolId());
        } else {
            log.debug("Principal is not an OidcAuthenticationToken ({}); skipping pending launch context tracking",
                    principal != null ? principal.getClass().getName() : "null");
        }
    }

    /**
     * Remembers the exact request URI (path + query string) that triggered the OAuth2 consent
     * requirement, so {@code OAuth2CallbackController} can redirect straight back into wherever this
     * tool's own launch endpoint happens to be - without hardcoding a tool-specific path. Doesn't
     * depend on the principal type since it's derived purely from the current request, not the token.
     */
    private void rememberPendingLaunchPath(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String launchPath = StringUtils.hasText(queryString)
                ? request.getRequestURI() + "?" + queryString
                : request.getRequestURI();
        request.getSession().setAttribute(PENDING_LAUNCH_PATH_SESSION_KEY, launchPath);
    }

    ModelAndView buildConnectView(String registrationId) {
        ModelAndView mav = new ModelAndView();
        mav.addObject("authorizationUri", "/oauth2/authorization/" + registrationId);
        mav.setViewName("connectCanvas");
        return mav;
    }
}

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

import edu.iu.uits.lms.canvas.config.CanvasConfiguration;
import edu.iu.uits.lms.canvasoauth2.CanvasOAuth2Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Handles the browser's return trip to {@code /login/oauth2/code/lms_canvas_oauth2} once Canvas has
 * finished (or refused) the user's consent to the {@code lms_canvas_oauth2} Developer Key.
 * <p>
 * Spring Security's {@code OAuth2AuthorizationCodeGrantFilter} intercepts every request to this same
 * URL first. On a successful code exchange it saves the {@code OAuth2AuthorizedClient} (via
 * {@code CanvasOAuth2AuthorizedClientRepository}) and then lets the filter chain continue, which is
 * what reaches this controller. But when Canvas rejects the authorization code - expired, reused,
 * invalid, or the user declined consent - the filter's own
 * {@code processAuthorizationResponse(...)} catches that {@code OAuth2AuthorizationException}
 * internally and issues an HTTP redirect back to this exact same URL with {@code error},
 * {@code error_description}, and {@code error_uri} query parameters attached (verified by reading the
 * real 7.0.6 {@code OAuth2AuthorizationCodeGrantFilter} source: it never lets the exception propagate,
 * and it never saves an authorized client on that path). That redirected request also lands here, so
 * this controller must treat a present {@code error} parameter as a distinct, expected outcome rather
 * than assuming every hit on this mapping means a successful connection.
 */
@Controller
@Slf4j
public class OAuth2CallbackController {

    @Autowired
    private CanvasConfiguration canvasConfiguration = null;
    @Autowired
    private OAuth2ConsentControllerAdvice oAuth2ConsentControllerAdvice = null;

    @GetMapping("/login/oauth2/code/" + CanvasOAuth2Constants.REGISTRATION_ID)
    public ModelAndView connected(HttpServletRequest request,
                                   @RequestParam(required = false) String error) {
        if (StringUtils.hasText(error)) {
            // Canvas rejected the authorization code; OAuth2AuthorizationCodeGrantFilter redirected
            // back here instead of completing the connection. Give the user another chance rather
            // than showing an error page - re-render the same "connect your Canvas account"
            // interstitial so they can retry from a clean authorization request.
            log.warn("Canvas OAuth2 callback reported an error instead of completing the connection: " +
                            "error={}, error_description={}, error_uri={}",
                    error, request.getParameter("error_description"), request.getParameter("error_uri"));
            return oAuth2ConsentControllerAdvice.buildConnectView(CanvasOAuth2Constants.REGISTRATION_ID);
        }

        String courseId = null;
        String toolId = null;
        String launchPath = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            courseId = (String) session.getAttribute(OAuth2ConsentControllerAdvice.PENDING_COURSE_ID_SESSION_KEY);
            toolId = (String) session.getAttribute(OAuth2ConsentControllerAdvice.PENDING_TOOL_ID_SESSION_KEY);
            launchPath = (String) session.getAttribute(OAuth2ConsentControllerAdvice.PENDING_LAUNCH_PATH_SESSION_KEY);
            session.removeAttribute(OAuth2ConsentControllerAdvice.PENDING_COURSE_ID_SESSION_KEY);
            session.removeAttribute(OAuth2ConsentControllerAdvice.PENDING_TOOL_ID_SESSION_KEY);
            session.removeAttribute(OAuth2ConsentControllerAdvice.PENDING_LAUNCH_PATH_SESSION_KEY);
        }

        String returnUrl = buildReturnUrl(courseId, toolId, launchPath);

        log.info("Canvas OAuth2 account connected; returning user to {}", returnUrl);

        ModelAndView mav = new ModelAndView("canvasConnected");
        mav.addObject("returnUrl", returnUrl);
        return mav;
    }

    /**
     * Prefers an internal redirect straight back to the tool's own launch request (the exact request
     * URI captured when consent was first required - see
     * {@code OAuth2ConsentControllerAdvice#rememberPendingLaunchPath}), since it needs no Canvas-side
     * configuration and works regardless of what this (or any other) tool's launch endpoint happens to
     * be. Falls back to a Canvas relaunch URL ({@code toolId} is only available when the Developer Key
     * requests {@code $Canvas.externalTool.id}), then a bare course page, then the base Canvas URL, for
     * the rare case a launch path wasn't captured (e.g. the browser navigated to the authorization
     * endpoint directly rather than through a real launch).
     */
    private String buildReturnUrl(String courseId, String toolId, String launchPath) {
        if (StringUtils.hasText(launchPath)) {
            return launchPath;
        }
        if (StringUtils.hasText(courseId) && StringUtils.hasText(toolId)) {
            return canvasConfiguration.getBaseUrl() + "/courses/" + courseId + "/external_tools/" + toolId;
        }
        if (StringUtils.hasText(courseId)) {
            return canvasConfiguration.getBaseUrl() + "/courses/" + courseId;
        }
        return canvasConfiguration.getBaseUrl();
    }
}

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

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Regression test for {@link SecurityConfig#canvasOAuth2AccessTokenResponseClient(RestClient)}.
 * <p>
 * An earlier implementation of that method hand-rolled an {@link OAuth2AccessTokenResponseClient}
 * with a bare {@code RestClient.retrieve()} call and no status handler. Under that implementation, a
 * normal/expected failure - Canvas rejecting an expired, invalid, or already-used authorization code
 * with a 400 and an OAuth2 error body - surfaced as a raw {@link HttpClientErrorException} instead of
 * {@link OAuth2AuthorizationException}, which {@code OAuth2AuthorizationCodeGrantFilter} specifically
 * catches to produce a clean error redirect; an uncaught {@code HttpClientErrorException} would
 * instead likely surface as an unhandled 500. This test simulates exactly that Canvas response
 * (via {@link MockRestServiceServer}, so no real network call is made) and asserts the failure comes
 * out as {@link OAuth2AuthorizationException} with Canvas's own {@code error} code preserved -
 * confirming the current implementation (which reuses Spring Security's own
 * {@code RestClientAuthorizationCodeTokenResponseClient} rather than replacing it) keeps that
 * built-in translation intact.
 */
class CanvasOAuth2AccessTokenResponseClientTest {

    @Test
    void expiredAuthorizationCodeSurfacesAsOAuth2AuthorizationException() {
        RestClient.Builder restClientBuilder = SecurityConfig.canvasOAuth2AccessTokenResponseRestClientBuilder();
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restClientBuilder);

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("lms_canvas_oauth2")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://viewem.test/login/oauth2/code/lms_canvas_oauth2")
                .authorizationUri("https://canvas.test/login/oauth2/auth")
                .tokenUri("https://canvas.test/login/oauth2/token")
                .build();

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                .clientId(clientRegistration.getClientId())
                .redirectUri(clientRegistration.getRedirectUri())
                .state("test-state")
                .build();

        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("expired-or-reused-code")
                .redirectUri(clientRegistration.getRedirectUri())
                .state("test-state")
                .build();

        OAuth2AuthorizationCodeGrantRequest grantRequest = new OAuth2AuthorizationCodeGrantRequest(clientRegistration,
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse));

        mockServer.expect(requestTo(clientRegistration.getProviderDetails().getTokenUri()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\",\"error_description\":\"authorization code has expired or already been used\"}"));

        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient =
                SecurityConfig.canvasOAuth2AccessTokenResponseClient(restClientBuilder.build());

        OAuth2AuthorizationException exception = assertThrows(OAuth2AuthorizationException.class,
                () -> accessTokenResponseClient.getTokenResponse(grantRequest));

        assertEquals("invalid_grant", exception.getError().getErrorCode());

        mockServer.verify();
    }
}

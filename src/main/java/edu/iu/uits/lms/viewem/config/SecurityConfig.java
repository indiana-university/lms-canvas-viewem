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

import edu.iu.uits.lms.common.oauth.CustomJwtAuthenticationConverter;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import uk.ac.ox.ctl.lti13.Lti13Configurer;
import uk.ac.ox.ctl.oauth2.client.endpoint.CanvasOAuth2AuthorizationCodeGrantRequestEntityConverter;
import uk.ac.ox.ctl.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;

import static edu.iu.uits.lms.lti.LTIConstants.BASE_USER_AUTHORITY;
import static edu.iu.uits.lms.lti.LTIConstants.WELL_KNOWN_ALL;
import static uk.ac.ox.ctl.oauth2.client.endpoint.CanvasOAuth2AuthorizationCodeGrantRequestEntityConverter.REPLACE_TOKENS;
import static uk.ac.ox.ctl.oauth2.client.endpoint.CanvasOAuth2AuthorizationCodeGrantRequestEntityConverter.REPLACE_TOKENS_VALUE;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private LmsDefaultGrantedAuthoritiesMapper lmsDefaultGrantedAuthoritiesMapper;

    @Order(4)
    @Bean
    public SecurityFilterChain staticResourcesFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/app/jsrivet/**", "/app/webjars/**", "/app/css/**", "/app/js/**", "/favicon.ico")
                .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(5)
    public SecurityFilterChain restFilterChain(HttpSecurity http) throws Exception {

        http.cors(Customizer.withDefaults())
                .securityMatcher("/rest/**", "/api/**")
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/rest/**")
                        .access(new WebExpressionAuthorizationManager("hasAuthority('SCOPE_lms:rest') and hasAuthority('ROLE_LMS_REST_ADMINS')"))
                        .requestMatchers("/api/**").permitAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new CustomJwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    @Order(6)
    public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(WELL_KNOWN_ALL, "/error", "/app/**")
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(WELL_KNOWN_ALL, "/error").permitAll()
                        .requestMatchers("/**").hasAuthority(BASE_USER_AUTHORITY)
                )
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("style-src 'self' 'unsafe-inline'; form-action 'self'; frame-ancestors 'self' https://*.instructure.com"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                );
        return http.build();
    }

    @Bean
    @Order(7)
    public SecurityFilterChain catchallFilterChain(HttpSecurity http) throws Exception {
        //Setup the LTI handshake
        http.with(new Lti13Configurer(), lti ->
                lti.setSecurityContextRepository(new HttpSessionSecurityContextRepository())
                        .grantedAuthoritiesMapper(lmsDefaultGrantedAuthoritiesMapper));

        http.securityMatcher("/**")
                .authorizeHttpRequests((authz) -> authz.anyRequest().authenticated())
                .oauth2Client(oauth2 -> oauth2
                        .authorizationCodeGrant(codeGrant -> codeGrant
                                .accessTokenResponseClient(canvasOAuth2AccessTokenResponseClient())))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("style-src 'self' 'unsafe-inline'; form-action 'self'; frame-ancestors 'self' https://*.instructure.com"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                );

        return http.build();
    }

    /**
     * Applies the Canvas-specific quirks (replace_tokens=true, non-standard token response JSON)
     * to the authorization_code token exchange used only by the lms_canvas_oauth2_viewem registration -
     * the LTI 1.3 launch's own (implicit-grant) registration is handled entirely separately by
     * Lti13Configurer and is unaffected by this.
     * <p>
     * Spring Security 7.x's {@code RestClientAuthorizationCodeTokenResponseClient} no longer accepts
     * a full request-entity converter - its extension points are narrower: a headers converter, a
     * parameters converter, and a {@link RestClient}. That means it can't directly host
     * {@link CanvasOAuth2AuthorizationCodeGrantRequestEntityConverter}, which builds an entire
     * {@code RequestEntity} from scratch. An earlier version of this method worked around that by
     * implementing {@link OAuth2AccessTokenResponseClient} from scratch with a hand-rolled lambda -
     * but {@code RestClientAuthorizationCodeTokenResponseClient} (via its superclass
     * {@code AbstractRestClientOAuth2AccessTokenResponseClient}) wraps every non-2xx response and
     * every null body into an {@link org.springframework.security.oauth2.core.OAuth2AuthorizationException},
     * which {@code OAuth2AuthorizationCodeGrantFilter} specifically catches to turn an expired,
     * invalid, or reused authorization code - a normal, expected occurrence, not an edge case - into a
     * clean error redirect. A from-scratch lambda has none of that translation, so the same failure
     * would surface as a raw {@code HttpClientErrorException} and likely an unhandled 500.
     * <p>
     * So this method reuses {@code RestClientAuthorizationCodeTokenResponseClient} itself rather than
     * replacing it, and only layers Canvas's one actual quirk on top via its supported extension
     * points:
     * <ul>
     * <li>{@code addParametersConverter(...)} - confirmed (by reading
     * {@code AbstractRestClientOAuth2AccessTokenResponseClient}'s 7.0.6 source) to <b>compose</b> with
     * the client's existing parameters converter rather than replacing it: the default
     * {@code DefaultOAuth2TokenRequestParametersConverter} already builds {@code grant_type},
     * {@code code}, {@code redirect_uri}, and - since this registration's
     * {@code client-authentication-method} is {@code client_secret_post} - {@code client_id} and
     * {@code client_secret} in the request body, exactly matching what
     * {@code CanvasOAuth2AuthorizationCodeGrantRequestEntityConverter#buildFormParameters} does by
     * hand. The only parameter Canvas needs that Spring doesn't already supply is
     * {@value CanvasOAuth2AuthorizationCodeGrantRequestEntityConverter#REPLACE_TOKENS}, so that's all
     * that gets added here - the vendored converter class itself is untouched and not even
     * instantiated; only its public constants are reused so the two "replace_tokens" quirks can't
     * drift apart.</li>
     * <li>{@code setRestClient(...)} - swaps in the vendored
     * {@link OAuth2AccessTokenResponseHttpMessageConverter} to parse Canvas's non-standard
     * <b>success</b>-path token response JSON, while otherwise mirroring the base class's own default
     * {@link RestClient} exactly (same {@link FormHttpMessageConverter}, same
     * {@link OAuth2ErrorResponseErrorHandler}) so error handling behaves identically to Spring's
     * out-of-the-box behavior. Note that this converter plays no role on the error path: a non-2xx
     * response is handled entirely by {@link OAuth2ErrorResponseErrorHandler}, which reads the body
     * with its own independent, hardcoded {@code OAuth2ErrorHttpMessageConverter} rather than the
     * RestClient's registered message converters - so it doesn't matter (and isn't expected) that
     * Canvas's error responses are standard-shaped JSON while its success responses aren't.</li>
     * </ul>
     * <p>
     * Split into three package-private pieces (rather than one {@code private} method) purely so
     * {@code CanvasOAuth2AccessTokenResponseClientTest} can bind a {@code MockRestServiceServer} to
     * {@link #canvasOAuth2AccessTokenResponseRestClientBuilder()}'s builder - simulating Canvas's
     * token endpoint without a real network call - and then exercise the exact same
     * {@link #canvasOAuth2AccessTokenResponseClient(RestClient)} wiring this bean method uses, with
     * no duplicated parameter-building logic between production code and test.
     */
    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> canvasOAuth2AccessTokenResponseClient() {
        return canvasOAuth2AccessTokenResponseClient(canvasOAuth2AccessTokenResponseRestClientBuilder().build());
    }

    // Hand-copies AbstractRestClientOAuth2AccessTokenResponseClient's own default RestClient field
    // (FormHttpMessageConverter + OAuth2ErrorResponseErrorHandler) so that overriding the RestClient
    // to add Canvas's response converter doesn't silently drop any of Spring's default behavior. If a
    // future Spring Security upgrade changes what that default RestClient includes, re-diff this
    // method against the new AbstractRestClientOAuth2AccessTokenResponseClient source to check for
    // drift.
    static RestClient.Builder canvasOAuth2AccessTokenResponseRestClientBuilder() {
        return RestClient.builder()
                .configureMessageConverters(messageConverters -> {
                    messageConverters.addCustomConverter(new FormHttpMessageConverter());
                    messageConverters.addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter());
                })
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler());
    }

    static OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> canvasOAuth2AccessTokenResponseClient(RestClient restClient) {
        RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();

        client.addParametersConverter(request -> {
            MultiValueMap<String, String> canvasParameters = new LinkedMultiValueMap<>();
            canvasParameters.add(REPLACE_TOKENS, REPLACE_TOKENS_VALUE);
            return canvasParameters;
        });
        client.setRestClient(restClient);

        return client;
    }
}

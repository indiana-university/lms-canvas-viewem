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
import edu.iu.uits.lms.canvasoauth2.security.CanvasOAuth2AuthorizedClientRepository;
import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import edu.iu.uits.lms.viewem.config.SecurityConfig;
import edu.iu.uits.lms.viewem.config.ToolConfig;
import edu.iu.uits.lms.viewem.controller.MainController;
import edu.iu.uits.lms.viewem.model.PendingUpload;
import edu.iu.uits.lms.viewem.model.Sheet;
import edu.iu.uits.lms.viewem.model.SheetColumn;
import edu.iu.uits.lms.viewem.model.SheetUser;
import edu.iu.uits.lms.viewem.model.UserData;
import edu.iu.uits.lms.viewem.repository.SheetRepository;
import edu.iu.uits.lms.viewem.repository.SheetUserRepository;
import edu.iu.uits.lms.viewem.repository.SystemUserRepository;
import edu.iu.uits.lms.viewem.service.SystemUserService;
import edu.iu.uits.lms.viewem.service.ViewemService;
import org.apache.commons.fileupload.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import uk.ac.ox.ctl.lti13.lti.Claims;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.iu.uits.lms.viewem.service.ViewemConstants.DATA_KEY_PENDING_UPLOAD;
import static edu.iu.uits.lms.viewem.service.ViewemConstants.DATA_KEY_ROSTER_USER_IDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Verifies that the upload preview keeps only a gzipped copy of the CSV in the session (never the parsed
 * Sheet), and that Save/Cancel/Back clear it.
 */
@WebMvcTest(value = MainController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@ContextConfiguration(classes = {ToolConfig.class, MainController.class, SecurityConfig.class,
        edu.iu.uits.lms.canvasoauth2.controller.OAuth2ConsentControllerAdvice.class,
        edu.iu.uits.lms.canvasoauth2.controller.CanvasOAuth2ConsentText.class,
        AppLaunchSecurityTest.TestConfig.class})
public class PendingUploadSessionTest {

    private static final String CONTEXT = "1234";
    private static final List<String> ROSTER = List.of("jdoe");
    private static final byte[] CSV = "username,Exam 1\njdoe,95\n".getBytes(StandardCharsets.UTF_8);

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
    @MockitoBean
    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> canvasOAuth2AccessTokenResponseClient;
    @Autowired
    private CanvasOAuth2AuthorizedClientRepository canvasOAuth2AuthorizedClientRepository;

    @BeforeEach
    void setUp() {
        reset(canvasOAuth2AuthorizedClientRepository);
        when(canvasOAuth2AuthorizedClientRepository.hasResolvableCanvasUserId(any())).thenReturn(true);
        when(courseSessionService.getAttributeFromSession(any(), eq(CONTEXT), eq(DATA_KEY_ROSTER_USER_IDS), eq(List.class)))
                .thenReturn(ROSTER);
    }

    @Test
    public void previewStoresGzippedCsvNotTheSheet() throws Exception {
        when(viewemService.validateCsv(any())).thenReturn(true);
        when(viewemService.processCsvFile(any(), anyString(), anyString(), anyString(), anyBoolean(), anyString(),
                eq(true), any(), any())).thenReturn(previewSheet());

        mvc.perform(withInstructor(multipart("/app/{context}/preview", CONTEXT)
                        .file(new MockMultipartFile("file", "grades.csv", "text/csv", CSV))
                        .param("sheetTitle", "My Sheet")
                        .param("action", "save")))
                .andExpect(status().isOk())
                .andExpect(view().name("previewSheet"));

        ArgumentCaptor<PendingUpload> captor = ArgumentCaptor.forClass(PendingUpload.class);
        verify(courseSessionService).addAttributeToSession(any(), eq(CONTEXT), eq(DATA_KEY_PENDING_UPLOAD), captor.capture());
        assertNull(captor.getValue().sheetId());
        assertArrayEquals(CSV, captor.getValue().csvStream().readAllBytes());
        verify(courseSessionService, never()).addAttributeToSession(any(), any(), any(), isA(Sheet.class));
    }

    @Test
    public void previewOfEditRemembersSheetId() throws Exception {
        Sheet existing = previewSheet();
        existing.setSheetId(42L);
        when(sheetRepository.findById(42L)).thenReturn(java.util.Optional.of(existing));
        when(viewemService.validateCsv(any())).thenReturn(true);
        when(viewemService.processCsvFile(any(), anyString(), anyString(), anyString(), anyBoolean(), anyString(),
                eq(true), any(), any())).thenReturn(previewSheet());

        mvc.perform(withInstructor(multipart("/app/{context}/preview", CONTEXT)
                        .file(new MockMultipartFile("file", "grades.csv", "text/csv", CSV))
                        .param("sheetTitle", "My Sheet")
                        .param("sheetId", "42")
                        .param("action", "save")))
                .andExpect(status().isOk())
                .andExpect(view().name("previewSheet"));

        ArgumentCaptor<PendingUpload> captor = ArgumentCaptor.forClass(PendingUpload.class);
        verify(courseSessionService).addAttributeToSession(any(), eq(CONTEXT), eq(DATA_KEY_PENDING_UPLOAD), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(42L, captor.getValue().sheetId());
    }

    @Test
    public void saveParsesPendingUploadAndClearsIt() throws Exception {
        PendingUpload pending = PendingUpload.of(CSV, null);
        givenPendingUpload(pending);

        mvc.perform(withInstructor(post("/app/{context}/upload", CONTEXT)
                        .param("sheetTitle", "My Sheet")
                        .param("action", "save")))
                .andExpect(status().isOk())
                .andExpect(view().name("listSheets"));

        verify(viewemService).savePendingUpload(pending, "My Sheet", CONTEXT, "systemId", "userId", null, ROSTER);
        verifyPendingUploadCleared();
    }

    @Test
    public void saveAndPublishPassesPublishedTrue() throws Exception {
        PendingUpload pending = PendingUpload.of(CSV, null);
        givenPendingUpload(pending);

        mvc.perform(withInstructor(post("/app/{context}/upload", CONTEXT)
                        .param("sheetTitle", "My Sheet")
                        .param("action", "saveAndPublish")))
                .andExpect(status().isOk());

        verify(viewemService).savePendingUpload(pending, "My Sheet", CONTEXT, "systemId", "userId", true, ROSTER);
        verifyPendingUploadCleared();
    }

    @Test
    public void saveAndUnpublishPassesPublishedFalse() throws Exception {
        PendingUpload pending = PendingUpload.of(CSV, null);
        givenPendingUpload(pending);

        mvc.perform(withInstructor(post("/app/{context}/upload", CONTEXT)
                        .param("sheetTitle", "My Sheet")
                        .param("action", "saveAndUnpublish")))
                .andExpect(status().isOk());

        verify(viewemService).savePendingUpload(pending, "My Sheet", CONTEXT, "systemId", "userId", false, ROSTER);
        verifyPendingUploadCleared();
    }

    @Test
    public void saveWithoutPendingUploadSavesNothing() throws Exception {
        mvc.perform(withInstructor(post("/app/{context}/upload", CONTEXT)
                        .param("sheetTitle", "My Sheet")
                        .param("action", "save")))
                .andExpect(status().isOk())
                .andExpect(view().name("listSheets"));

        verify(viewemService, never()).savePendingUpload(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void saveFailureReturnsToUploadPageAndClears() throws Exception {
        PendingUpload pending = PendingUpload.of(CSV, null);
        givenPendingUpload(pending);
        when(viewemService.savePendingUpload(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new FileUploadException());
        when(messageSource.getMessage(eq("upload.sheetFile.invalid.contents.error"), isNull(), any()))
                .thenReturn("bad file");

        mvc.perform(withInstructor(post("/app/{context}/upload", CONTEXT)
                        .param("sheetTitle", "My Sheet")
                        .param("action", "save")))
                .andExpect(status().isOk())
                .andExpect(view().name("uploadSheet"))
                .andExpect(model().attribute("fileError", "bad file"))
                .andExpect(model().attribute("sheetTitle", "My Sheet"));

        verifyPendingUploadCleared();
    }

    @Test
    public void cancelClearsPendingUpload() throws Exception {
        mvc.perform(withInstructor(post("/app/{context}/upload", CONTEXT)
                        .param("sheetTitle", "My Sheet")
                        .param("action", "cancel")))
                .andExpect(status().isOk())
                .andExpect(view().name("listSheets"));

        verifyPendingUploadCleared();
    }

    @Test
    public void backClearsPendingUpload() throws Exception {
        mvc.perform(withInstructor(post("/app/{context}/upload", CONTEXT)
                        .param("sheetTitle", "My Sheet")
                        .param("action", "back")))
                .andExpect(status().isOk())
                .andExpect(view().name("uploadSheet"));

        verifyPendingUploadCleared();
    }

    private void givenPendingUpload(PendingUpload pending) {
        when(courseSessionService.getAttributeFromSession(any(), eq(CONTEXT), eq(DATA_KEY_PENDING_UPLOAD), eq(PendingUpload.class)))
                .thenReturn(pending);
    }

    private void verifyPendingUploadCleared() {
        verify(courseSessionService).removeAttributeFromSession(any(), eq(CONTEXT), eq(DATA_KEY_PENDING_UPLOAD));
    }

    private static Sheet previewSheet() {
        Sheet sheet = new Sheet("My Sheet", CONTEXT, "systemId", false, "userId", "userId");
        sheet.addColumn(new SheetColumn("Exam 1"));
        SheetUser user = new SheetUser("jdoe");
        user.addUserData(new UserData("95"));
        sheet.addSheetUser(user);
        return sheet;
    }

    private static <B extends AbstractMockHttpServletRequestBuilder<B>> B withInstructor(B builder) {
        Map<String, Object> extraAttributes = new HashMap<>();
        Map<String, Object> platformObject = new HashMap<>();
        platformObject.put(LTIConstants.CLAIMS_PLATFORM_GUID_KEY, "systemId");
        extraAttributes.put(Claims.PLATFORM_INSTANCE, platformObject);

        Map<String, Object> customMap = new HashMap<>();
        customMap.put(LTIConstants.CUSTOM_CANVAS_COURSE_ID_KEY, CONTEXT);
        customMap.put(LTIConstants.CUSTOM_CANVAS_USER_LOGIN_ID_KEY, "userId");

        OidcAuthenticationToken token = TestUtils.buildToken("userId", LTIConstants.INSTRUCTOR_AUTHORITY,
                extraAttributes, customMap);
        return builder.with(authentication(token)).with(csrf())
                .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent());
    }
}

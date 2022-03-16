//package edu.iu.uits.lms.viewem;
//
//import edu.iu.uits.lms.canvas.config.CanvasClientTestConfig;
//import edu.iu.uits.lms.canvas.model.Course;
//import edu.iu.uits.lms.canvas.services.CanvasService;
//import edu.iu.uits.lms.canvas.services.CourseService;
//import edu.iu.uits.lms.common.session.CourseSessionService;
//import edu.iu.uits.lms.lti.LTIConstants;
//import edu.iu.uits.lms.lti.config.LtiClientTestConfig;
////import edu.iu.uits.lms.multiclassmessenger.config.ToolConfig;
////import edu.iu.uits.lms.multiclassmessenger.controller.AnnouncementController;
////import edu.iu.uits.lms.multiclassmessenger.service.MultiClassMessengerToolService;
//import edu.iu.uits.lms.viewem.config.ToolConfig;
//import edu.iu.uits.lms.viewem.repository.SheetRepository;
//import edu.iu.uits.lms.viewem.repository.SheetUserRepository;
//import edu.iu.uits.lms.viewem.repository.SystemUserRepository;
//import edu.iu.uits.lms.viewem.service.ViewemService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.MessageSource;
//import org.springframework.context.annotation.Import;
//import org.springframework.context.support.ResourceBundleMessageSource;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.ResultActions;
//import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
//import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;
//
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(value = AnnouncementController.class, properties = {"oauth.tokenprovider.url=http://foo"})
//@Import({ToolConfig.class, CanvasClientTestConfig.class, LtiClientTestConfig.class})
////@ActiveProfiles("ltirest")
//public abstract class BaseAppLaunchSecurityTest {
//
//    @Autowired
//    private MockMvc mvc;
//
//    @MockBean
//    private ViewemService viewemService = null;
//    @MockBean
//    private ResourceBundleMessageSource messageSource = null;
//    @MockBean
//    private SheetRepository sheetRepository = null;
//    @MockBean
//    private SheetUserRepository sheetUserRepository = null;
//    @MockBean
//    private SystemUserRepository systemUserRepository = null;
//    @MockBean
//    private CourseSessionService courseSessionService = null;
//
////   @MockBean
////   @Qualifier("viewemDataSource")
////   public DataSource dataSource;
////
////   @MockBean
////   @Qualifier("viewemEntityMgrFactory")
////   public LocalContainerEntityManagerFactoryBean viewemEntityMgrFactory;
////
////   @MockBean
////   @Qualifier("viewemTransactionMgr")
////   public PlatformTransactionManager viewemTransactionMgr;
//
//    @Test
//    public void appNoAuthnLaunch() throws Exception {
//        //This is a secured endpoint and should not not allow access without authn
//        mvc.perform(get("/app/index/1234")
//                        .header(HttpHeaders.USER_AGENT, edu.iu.uits.lms.viewem.TestUtils.defaultUseragent())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void appAuthnWrongContextLaunch() throws Exception {
//        OidcAuthenticationToken token = TestUtils.buildToken("userId", "asdf", LTIConstants.INSTRUCTOR_AUTHORITY);
////        LtiAuthenticationToken token = new LtiAuthenticationToken("userId",
////                "asdf", "systemId",
////                AuthorityUtils.createAuthorityList(LtiAuthenticationProvider.LTI_USER_ROLE, "authority"),
////                "unit_test");
//
//        SecurityContextHolder.getContext().setAuthentication(token);
//
//        //This is a secured endpoint and should not not allow access without authn
//        ResultActions mockMvcAction = mvc.perform(get("/app/1234/list")
//                .header(HttpHeaders.USER_AGENT, edu.iu.uits.lms.viewem.TestUtils.defaultUseragent())
//                .contentType(MediaType.APPLICATION_JSON));
//
//        mockMvcAction.andExpect(status().isOk());
//        mockMvcAction.andExpect(MockMvcResultMatchers.view().name ("tokenError"));
//        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists("exception"));
//        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists("stackTrace"));
//        mockMvcAction.andExpect(MockMvcResultMatchers.model().attributeExists("timestamp"));
//    }
//
//    @Test
//    public void appAuthnLaunch() throws Exception {
//        LtiAuthenticationToken token = new LtiAuthenticationToken("userId",
//                "1234", "systemId",
//                AuthorityUtils.createAuthorityList(LtiAuthenticationProvider.LTI_USER_ROLE, "authority"),
//                "unit_test");
//
//        SecurityContextHolder.getContext().setAuthentication(token);
//
//        //This is a secured endpoint and should not not allow access without authn
//        mvc.perform(get("/app/1234/list")
//                        .header(HttpHeaders.USER_AGENT, edu.iu.uits.lms.viewem.TestUtils.defaultUseragent())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    public void randomUrlNoAuth() throws Exception {
//        //This is a secured endpoint and should not not allow access without authn
//        mvc.perform(get("/asdf/foobar")
//                        .header(HttpHeaders.USER_AGENT, edu.iu.uits.lms.viewem.TestUtils.defaultUseragent())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    public void randomUrlWithAuth() throws Exception {
//        LtiAuthenticationToken token = new LtiAuthenticationToken("userId",
//                "1234", "systemId",
//                AuthorityUtils.createAuthorityList(LtiAuthenticationProvider.LTI_USER_ROLE, "authority"),
//                "unit_test");
//        SecurityContextHolder.getContext().setAuthentication(token);
//
//        //This is a secured endpoint and should not not allow access without authn
//        mvc.perform(get("/asdf/foobar")
//                        .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isNotFound());
//    }
//}
//

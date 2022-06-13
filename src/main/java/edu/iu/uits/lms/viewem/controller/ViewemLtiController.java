//package edu.iu.uits.lms.viewem.controller;
//
///*-
// * #%L
// * lms-canvas-viewem
// * %%
// * Copyright (C) 2015 - 2022 Indiana University
// * %%
// * Redistribution and use in source and binary forms, with or without modification,
// * are permitted provided that the following conditions are met:
// *
// * 1. Redistributions of source code must retain the above copyright notice, this
// *    list of conditions and the following disclaimer.
// *
// * 2. Redistributions in binary form must reproduce the above copyright notice,
// *    this list of conditions and the following disclaimer in the documentation
// *    and/or other materials provided with the distribution.
// *
// * 3. Neither the name of the Indiana University nor the names of its contributors
// *    may be used to endorse or promote products derived from this software without
// *    specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
// * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
// * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
// * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
// * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// * OF THE POSSIBILITY OF SUCH DAMAGE.
// * #L%
// */
//
//import edu.iu.uits.lms.canvas.model.User;
//import edu.iu.uits.lms.canvas.services.CourseService;
//import edu.iu.uits.lms.common.session.CourseSessionService;
//import edu.iu.uits.lms.lti.LTIConstants;
//import edu.iu.uits.lms.lti.controller.LtiController;
//import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
//import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
//import edu.iu.uits.lms.viewem.config.ToolConfig;
//import edu.iu.uits.lms.viewem.service.SystemUserService;
//import io.jsonwebtoken.Claims;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.authority.AuthorityUtils;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.tsugi.basiclti.BasicLTIConstants;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static edu.iu.uits.lms.viewem.service.ViewemConstants.DATA_KEY_ROSTER_USER_IDS;
//
///**
// * Created by chmaurer on 6/15/15.
// */
//@Controller
//@RequestMapping("/lti")
//@Slf4j
//public class ViewemLtiController extends LtiController {
//
//    @Autowired
//    private ToolConfig toolConfig = null;
//
//    @Autowired
//    private SystemUserService systemUserService = null;
//
//    @Autowired
//    private CourseService courseService = null;
//
//    @Autowired
//    private CourseSessionService courseSessionService = null;
//
//    @Override
//    protected String getLaunchUrl(Map<String, String> launchParams) {
//        String courseId = launchParams.get(CUSTOM_CANVAS_COURSE_ID);
//        return "/app/" + courseId + "/list";
//    }
//
//    @Override
//    protected Map<String, String> getParametersForLaunch(Map<String, String> payload, Claims claims) {
//        Map<String, String> paramMap = new HashMap<String, String>(1);
//
//        paramMap.put(CUSTOM_CANVAS_COURSE_ID, payload.get(CUSTOM_CANVAS_COURSE_ID));
//        paramMap.put(BasicLTIConstants.ROLES, payload.get(BasicLTIConstants.ROLES));
//        paramMap.put(BasicLTIConstants.USER_ID, payload.get(BasicLTIConstants.USER_ID));
//        paramMap.put(CUSTOM_CANVAS_USER_LOGIN_ID, payload.get(CUSTOM_CANVAS_USER_LOGIN_ID));
//        paramMap.put(BasicLTIConstants.TOOL_CONSUMER_INSTANCE_GUID, payload.get(BasicLTIConstants.TOOL_CONSUMER_INSTANCE_GUID));
//
//        return paramMap;
//    }
//
//    @Override
//    protected void preLaunchSetup(Map<String, String> launchParams, HttpServletRequest request, HttpServletResponse response) {
//        String rolesString = launchParams.get(BasicLTIConstants.ROLES);
//        String[] userRoles = rolesString.split(",");
//        String authority = returnEquivalentAuthority(Arrays.asList(userRoles), toolConfig.getInstructorRoles());
//        log.debug("LTI equivalent authority: " + authority);
//
//        String userId = launchParams.get(CUSTOM_CANVAS_USER_LOGIN_ID);
//        String systemId = launchParams.get(BasicLTIConstants.TOOL_CONSUMER_INSTANCE_GUID);
//        String courseId = launchParams.get(CUSTOM_CANVAS_COURSE_ID);
//
//        //Get the canvas roster for this course and make sure that all the users have up-to-date names
//        if (LTIConstants.INSTRUCTOR_AUTHORITY.equals(authority)) {
//            List<User> users = courseService.getRosterForCourseAsUser(courseId, null, null);
//            if (users != null) {
//                systemUserService.createOrUpdateUsers(users, systemId);
//                List<String> userIds = new ArrayList<>();
//                for (User user : users) {
//                    userIds.add(user.getLoginId());
//                }
//                courseSessionService.addAttributeToSession(request.getSession(), courseId, DATA_KEY_ROSTER_USER_IDS, userIds);
//            }
//        }
//
//        LtiAuthenticationToken token = new LtiAuthenticationToken(userId,
//                courseId, systemId, AuthorityUtils.createAuthorityList(LtiAuthenticationProvider.LTI_USER_ROLE, authority), getToolContext());
//        SecurityContextHolder.getContext().setAuthentication(token);
//    }
//
//    @Override
//    protected String getToolContext() {
//        return "lms_viewem";
//    }
//
//    @Override
//    protected LAUNCH_MODE launchMode() {
//        return LAUNCH_MODE.FORWARD;
//    }
//}

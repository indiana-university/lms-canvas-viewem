package edu.iu.uits.lms.viewem.controller;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVWriter;
import edu.iu.uits.lms.canvas.model.User;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.InvalidTokenContextException;
import edu.iu.uits.lms.lti.controller.OidcTokenAwareController;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import edu.iu.uits.lms.viewem.model.Sheet;
import edu.iu.uits.lms.viewem.model.SheetColumn;
import edu.iu.uits.lms.viewem.model.SheetUser;
import edu.iu.uits.lms.viewem.model.SystemUser;
import edu.iu.uits.lms.viewem.model.UserData;
import edu.iu.uits.lms.viewem.repository.SheetRepository;
import edu.iu.uits.lms.viewem.repository.SheetUserRepository;
import edu.iu.uits.lms.viewem.repository.SystemUserRepository;
import edu.iu.uits.lms.viewem.service.SheetExclusionStrategy;
import edu.iu.uits.lms.viewem.service.SystemUserService;
import edu.iu.uits.lms.viewem.service.ViewemConstants;
import edu.iu.uits.lms.viewem.service.ViewemService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static edu.iu.uits.lms.viewem.service.ViewemConstants.DATA_KEY_ROSTER_USER_IDS;

/**
 * Created by chmaurer on 6/4/15.
 */
@Controller
@RequestMapping("/app")
@Slf4j
public class MainController extends OidcTokenAwareController {

    @Autowired
    private ViewemService viewemService = null;
    @Autowired
    private ResourceBundleMessageSource messageSource = null;
    @Autowired
    private SheetRepository sheetRepository = null;
    @Autowired
    private SheetUserRepository sheetUserRepository = null;
    @Autowired
    private SystemUserRepository systemUserRepository = null;
    @Autowired
    private CourseSessionService courseSessionService = null;
    @Autowired
    private CourseService courseService = null;
    @Autowired
    private SystemUserService systemUserService = null;

    @GetMapping("/launch")
    public String launch(Model model, SecurityContextHolderAwareRequestWrapper request) {
        OidcAuthenticationToken token = getTokenWithoutContext();
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        String courseId = oidcTokenUtils.getCourseId();
        String systemId = oidcTokenUtils.getPlatformGuid();

        //Get the canvas roster for this course and make sure that all the users have up-to-date names
        if (request.isUserInRole(LTIConstants.INSTRUCTOR_AUTHORITY)) {
            List<User> users = courseService.getRosterForCourseAsUser(courseId, null, null);
            if (users != null) {
                systemUserService.createOrUpdateUsers(users, systemId);
                List<String> userIds = new ArrayList<>();
                for (User user : users) {
                    userIds.add(user.getLoginId());
                }
                courseSessionService.addAttributeToSession(request.getSession(), courseId, DATA_KEY_ROSTER_USER_IDS, userIds);
            }
        }

        return listSheets(courseId, model, request);
    }

    @RequestMapping("/{context}/list")
    public String listSheets(@PathVariable("context") String context, Model model,
                             SecurityContextHolderAwareRequestWrapper request) {
        OidcAuthenticationToken token = getValidatedToken(context);
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        String currentUser = oidcTokenUtils.getUserLoginId();
        List<Sheet> sheets = null;
        String systemId = oidcTokenUtils.getPlatformGuid();

        if (request.isUserInRole(LTIConstants.INSTRUCTOR_ROLE)) {
            sheets = sheetRepository.findByContextAndSystem(context, systemId);
        } else {
            sheets = sheetRepository.findByContextAndPublishedAndSystemAndUser(context, true, systemId, currentUser);
        }

        model.addAttribute("sheets", sheets);
        model.addAttribute("context", context);
        return "listSheets";
    }

    @RequestMapping("/{context}/edit/{id}")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String editSheet(@PathVariable("context") String context, @PathVariable("id") Long id, Model model,
                            String sheetTitle) {
        OidcAuthenticationToken token = getValidatedToken(context);
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        Sheet sheet = sheetRepository.findById(id).orElse(null);
        List<SheetUser> sheetUsers = sheetUserRepository.findBySheet(id);
        addPreviewDataToModel(sheet, sheetUsers.get(0), model, oidcTokenUtils.getPlatformGuid());

        String title = sheetTitle==null ? sheet.getTitle() : sheetTitle;
        model.addAttribute("sheetTitle", title);
        model.addAttribute("sheet", sheet);
        model.addAttribute("editMode", true);
        return "editSheet";
    }

    @RequestMapping(value = "/{context}/edit/{id}", method = RequestMethod.POST)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String edit(@PathVariable("context") String context, @PathVariable("id") Long id,
                       Model model, @RequestParam("sheetTitle") String sheetTitle, @RequestParam("action") String action,
                       SecurityContextHolderAwareRequestWrapper request) {
        if ("".equals(sheetTitle) || sheetTitle == null) {
            String errorMessage = messageSource.getMessage("upload.sheetTitle.empty.error", null, Locale.getDefault());
            model.addAttribute("titleError", errorMessage);
            return editSheet(context, id, model, sheetTitle);
        } else if (sheetTitle.length()>255) {
            String errorMessage = messageSource.getMessage("upload.sheetTitle.tooLong.error", null, Locale.getDefault());
            model.addAttribute("titleError", errorMessage);
            model.addAttribute("sheetTitle", sheetTitle);
            return editSheet(context, id, model, sheetTitle);
        } else {
            Sheet sheet = sheetRepository.findById(id).orElse(null);

            switch(action) {
                case ViewemConstants.ACTION_SAVE_PUBLISH:
                    sheet.setPublished(true);
                    break;
                case ViewemConstants.ACTION_SAVE_UNPUBLISH:
                    sheet.setPublished(false);
                    break;
                default:
                    break;
            }
            sheet.setTitle(sheetTitle);
            sheetRepository.save(sheet);
            return listSheets(context, model, request);
        }
    }

    @RequestMapping(value = "/{context}/edit/{id}", method = RequestMethod.POST, params="action=" + ViewemConstants.ACTION_UPLOAD)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String editViaUpload(@PathVariable("context") String context, @PathVariable("id") Long id,
                       Model model, @RequestParam("sheetTitle") String sheetTitle, @RequestParam("action") String action,
                       SecurityContextHolderAwareRequestWrapper request) {
        model.addAttribute("sheetTitle", sheetTitle);
        model.addAttribute("sheetId", id);
        return uploadSheet(context, model);
    }


    @RequestMapping("/{context}/view/{id}")
     public String viewSheet(@PathVariable("context") String context, @PathVariable("id") Long id, Model model,
                                 SecurityContextHolderAwareRequestWrapper request) {
        String view = "viewSheet";
        OidcAuthenticationToken token = getValidatedToken(context);
        Sheet sheet = sheetRepository.findById(id).orElse(null);
        if (request.isUserInRole(LTIConstants.INSTRUCTOR_ROLE)) {
            view = viewSheetAsInstructor(sheet, model, token);
        } else {
            view = viewSheetAsStudent(sheet, model, token);
        }

        model.addAttribute("sheet", sheet);
        return view;
    }


    /**
     * Delete a sheet
     * @param context Context that contains the sheet to view
     * @param id Sheet to publish
     * @param model Model containing details necessary to render the page
     * @return The the page with all sheets listed
     */
    @RequestMapping(value = "/{context}/delete/{id}", method = RequestMethod.DELETE)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String deleteSheet(@PathVariable("context") String context, @PathVariable("id") Long id, Model model,
                            SecurityContextHolderAwareRequestWrapper request) {
        getValidatedToken(context);
        Sheet sheet = sheetRepository.findById(id).orElse(null);
        sheet.setDeleted(true);
        sheetRepository.save(sheet);

        return "noOp";
    }


    /**
     * Do all the things necessary to build the student's view page
     * @param sheet Sheet to view
     * @param model Model containing details necessary to render the page
     * @param token Auth token
     * @return The name of the view to render
     */
    private String viewSheetAsStudent(Sheet sheet, Model model, OidcAuthenticationToken token) {
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        String userId = oidcTokenUtils.getUserLoginId();
        populateModelForStudentView(sheet, userId, oidcTokenUtils.getPlatformGuid(), model);

        return "viewSheet";
    }

    /**
     * Do all the things necessary to build the instructor's view page
     * @param sheet Sheet to view
     * @param model Model containing details necessary to render the page
     * @param token Auth token
     * @return The name of the view to render
     */
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    private String viewSheetAsInstructor(Sheet sheet, Model model, OidcAuthenticationToken token) {
        List<String> userIds = sheetUserRepository.getUserIdsBySheet(sheet.getSheetId());
        List<SystemUser> systemUsers = systemUserRepository.findByUsersAndSystem(userIds, sheet.getSystemId());
        List<SheetUser> sheetUsers = sheetUserRepository.findBySheet(sheet.getSheetId());
        model.addAttribute("users", systemUsers);
        model.addAttribute("sheetUsers", sheetUsers);
        model.addAttribute("sheetColumns", sheet.getColumns());
        return "viewSheetAsInstructor";
    }

    /**
     * Load up a specific user's data
     * @param context Context that contains the sheet to view
     * @param sheetId Sheet to view
     * @param userId User id whose data we want to view
     * @param model Model containing details necessary to render the page
     * @return The view (fragment) to render
     */
    @RequestMapping(value = "/{context}/view/{sheetId}/{userId}")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String loadUserDataForSheet(@PathVariable("context") String context, @PathVariable("sheetId") Long sheetId, @PathVariable("userId") String userId, Model model) {
        OidcAuthenticationToken token = getValidatedToken(context);
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        Sheet sheet = sheetRepository.findById(sheetId).orElse(null);
        populateModelForStudentView(sheet, userId, oidcTokenUtils.getPlatformGuid(), model);

        return "fragments/userDataTable :: userData";
    }

    /**
     * Populate the model for a student's view
     * @param sheet Sheet to view
     * @param userId User id whose data we want to view
     * @param systemId System id to use as an identifier for user name lookups
     * @param model Model containing details necessary to render the page
     */
    private void populateModelForStudentView(Sheet sheet, String userId, String systemId, Model model) {
        List<String[]> userData = new ArrayList<>();
        List<SheetColumn> columns = sheet.getColumns();
        SheetUser sheetUser = sheetUserRepository.findByUserAndSheet(userId, sheet.getSheetId());
        SystemUser systemUser = systemUserRepository.findByUserAndSystem(userId, systemId);

        if (sheetUser != null) {
            String userFullName = systemUser!=null? systemUser.getUserFullName() : sheetUser.getUserId();
            model.addAttribute("sheetUser", userFullName);
            model.addAttribute("sheetUserId", sheetUser.getUserId());

            for (UserData ud : sheetUser.getData()) {
                String[] dataRow = {columns.get(ud.getSequence()).getTitle(), ud.getData()};
                userData.add(dataRow);
            }
        }
        model.addAttribute("sheetUserData", userData);
    }

    @RequestMapping(value = {"/{context}/preview", "/{context}/upload"}, method = RequestMethod.POST, params="action=cancel")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String cancelUploadSheet(@PathVariable("context") String context, Model model, SecurityContextHolderAwareRequestWrapper request) {
        return listSheets(context, model, request);
    }

    private void addPreviewDataToModel(Sheet sheet, SheetUser sheetUser, Model model, String systemId) {
        Map<Integer, String> dataMap = new HashMap<>();

        int i = 0;
        for (SheetColumn sc : sheet.getColumns()) {
            dataMap.put(i, sc.getTitle());
            i++;
        }

        List<String[]> dataSheet = new ArrayList<>();

        // see if this user is in the system. If they are, use the full name, otherwise display userId
        SystemUser systemUser = systemUserRepository.findByUserAndSystem(sheetUser.getUserId(), systemId);
        String userFullName = systemUser!=null ? systemUser.getUserFullName() : sheetUser.getUserId();

        model.addAttribute("user", userFullName);

        i = 0;
        for (UserData ud : sheetUser.getData()) {
            String[] dataString = {dataMap.get(i), ud.getData()};
            dataSheet.add(dataString);
            i++;
        }
        model.addAttribute("dataMap", dataSheet);
    }

    @RequestMapping(value = "/{context}/preview", method = RequestMethod.POST, params="action=save")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String previewSheet(@PathVariable("context") String context, Model model, @RequestParam("file") MultipartFile file, @RequestParam("sheetTitle") String sheetTitle,
                               @RequestParam(value = "sheetId", required = false) Long sheetId, SecurityContextHolderAwareRequestWrapper request) {
        OidcAuthenticationToken token = getValidatedToken(context);
        OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
        boolean errors = false;
        String owner = oidcTokenUtils.getUserLoginId();
        String systemId = oidcTokenUtils.getPlatformGuid();
        if ("".equals(sheetTitle) || sheetTitle == null) {
            String errorMessage = messageSource.getMessage("upload.sheetTitle.empty.error", null, Locale.getDefault());
            model.addAttribute("titleError", errorMessage);
            errors = true;
        } else if (sheetTitle.length() > 255) {
            String errorMessage = messageSource.getMessage("upload.sheetTitle.tooLong.error", null, Locale.getDefault());
            model.addAttribute("titleError", errorMessage);
            model.addAttribute("sheetTitle", sheetTitle);
            errors = true;
        }

        // check this to get both errors to potentially display
        if (file.isEmpty()) {
            String errorMessage = messageSource.getMessage("upload.sheetFile.empty.error", null, Locale.getDefault());
            model.addAttribute("sheetTitle", sheetTitle);
            model.addAttribute("fileError", errorMessage);
            errors = true;
        } else if (!errors) {
            // if there's errors already, no need to do any of this
            byte[] fileByteArray = new byte[0];
            try {
                fileByteArray = file.getBytes();
            } catch (IOException e) {
                log.error(e.getMessage());
            }

            boolean validCsv = viewemService.validateCsv(file);

            if (validCsv) {
                InputStream csvFileStream = new ByteArrayInputStream(fileByteArray);
                try {
                    List<String> validUserIds = courseSessionService.getAttributeFromSession(request.getSession(), context, DATA_KEY_ROSTER_USER_IDS, List.class);
                    List<String> skippedUserIds = new ArrayList<>();

                    boolean published = false;

                    //get the published info from sheet
                    if (sheetId != null) {
                        Sheet sheet = sheetRepository.findById(sheetId).orElse(null);
                        published = sheet.isPublished();
                    }
                    Sheet sheet = viewemService.processCsvFile(csvFileStream, sheetTitle, context, systemId, published, owner, true, validUserIds, skippedUserIds);
                    if (sheetId != null) {
                        //We got here by means of an edit instead of a new upload
                        sheet.setSheetId(sheetId);
                    }
                    model.addAttribute("sheetTitle", sheetTitle);
                    model.addAttribute("sheet", sheet);

                    List<SheetUser> sheetUsers = sheet.getSheetUsers();
                    if (sheetUsers != null && sheetUsers.size() > 0) {
                        addPreviewDataToModel(sheet, sheet.getSheetUsers().get(0), model, systemId);
                        GsonBuilder builder = new GsonBuilder().setExclusionStrategies(new SheetExclusionStrategy());
                        Gson gson = builder.create();

                        model.addAttribute("sheetJson", gson.toJson(sheet));

                        if (skippedUserIds.size() > 0) {
                            String delimitedUsers = StringUtils.arrayToDelimitedString(skippedUserIds.toArray(new String[skippedUserIds.size()]), ", ");

                            String errorMessage = messageSource.getMessage("upload.sheetFile.invalid.users.error",
                                    new String[]{delimitedUsers},
                                    Locale.getDefault());
                            model.addAttribute("previewWarnings", errorMessage);
                        }
                    } else {
                        String errorMessage = messageSource.getMessage("upload.sheetFile.missing.users.error", null, Locale.getDefault());
                        model.addAttribute("fileError", errorMessage);
                        errors = true;
                    }


                } catch (FileUploadException e) {
                    String errorMessage = messageSource.getMessage("upload.sheetFile.invalid.contents.error", null, Locale.getDefault());
                    model.addAttribute("fileError", errorMessage);
                    model.addAttribute("sheetTitle", sheetTitle);
                    errors = true;
                } catch (DataTooBigException e) {
                    // get here if some fields are going to be too big for the database
                    String errorMessage = messageSource.getMessage("upload.sheetFile.tooLong.error",
                            new String[]{Integer.toString(SheetColumn.SHEET_COLUMN_TITLE_SIZE),Integer.toString(UserData.USER_DATA_DATA_SIZE)},
                            Locale.getDefault());
                    model.addAttribute("fileError", errorMessage);
                    model.addAttribute("sheetTitle", sheetTitle);
                    errors = true;
                } finally {
                    try {
                        csvFileStream.close();
                    } catch (IOException e) {
                        log.error("Error closing csvFileStream in previewSheet");
                    }
                }
            } else {
                String errorMessage = messageSource.getMessage("upload.sheetFile.invalid.file.error", null, Locale.getDefault());
                model.addAttribute("sheetTitle", sheetTitle);
                model.addAttribute("fileError", errorMessage);
                errors = true;
            }
        }

        if (errors) {
            if (sheetId != null) {
                model.addAttribute("sheetId", sheetId);
            }
            return uploadSheet(context, model);
        }
        else {
            return "previewSheet";
        }
    }

    @RequestMapping(value = "/{context}/upload", method = RequestMethod.POST, params="action=back")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String backToUpload(@PathVariable("context") String context, Model model, @RequestParam("sheetTitle") String sheetTitle, SecurityContextHolderAwareRequestWrapper request) {
        model.addAttribute("sheetTitle", sheetTitle);
        return uploadSheet(context, model);
    }

    @RequestMapping(value = "/{context}/uploadSheet", method = RequestMethod.GET)
    public String uploadSheet(@PathVariable("context") String context, Model model) {
        model.addAttribute("context", context);
        return "uploadSheet";
    }

    @RequestMapping(value = "/{context}/upload", method = RequestMethod.POST, params="action=" + ViewemConstants.ACTION_SAVE_PUBLISH)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String uploadAndPublish(@PathVariable("context") String context, Model model, @RequestParam("sheetTitle") String sheetTitle, @RequestParam("sheetJson") String sheetJson, SecurityContextHolderAwareRequestWrapper request) {
        String result = upload(context, model, sheetTitle, sheetJson, request, true);
        return result;
    }

    /**
     * Upload a new sheet without publish
     * @param sheetTitle title of the sheet
     * @param sheetJson Json info of the Sheet
     * @param model Model containing details necessary to render the page
     * @return The the page with all sheets listed
     */

    @RequestMapping(value = "/{context}/upload", method = RequestMethod.POST, params="action=" + ViewemConstants.ACTION_SAVE_UNPUBLISH)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String uploadAndUnpublish(@PathVariable("context") String context, Model model, @RequestParam("sheetTitle") String sheetTitle, @RequestParam("sheetJson") String sheetJson, SecurityContextHolderAwareRequestWrapper request) {
        String result = upload(context, model, sheetTitle, sheetJson, request, false);
        return result;
    }

    /**
     * Upload a new sheet
     * @param sheetTitle title of the sheet
     * @param sheetJson Json info of the Sheet
     * @param model Model containing details necessary to render the page
     * @return The the page with all sheets listed
     */

    @RequestMapping(value = "/{context}/upload", method = RequestMethod.POST, params="action=" + ViewemConstants.ACTION_SAVE)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String uploadSave(@PathVariable("context") String context, Model model, @RequestParam("sheetTitle") String sheetTitle, @RequestParam("sheetJson") String sheetJson, SecurityContextHolderAwareRequestWrapper request) {
        //this part is a duplicate with upload(), but if I pass "true" or "false" as the placehold, and let upload() deal with published status,
        //it will break SAVE_UNPUBLISH or SAVE_PUBLISH feature.
        Gson gsonSheet = new Gson();
        Sheet sheet = gsonSheet.fromJson(sheetJson, Sheet.class);
        boolean published = sheet.isPublished();

        String result = upload(context, model, sheetTitle, sheetJson, request, published);
        return result;
    }

    private String upload(String context, Model model, @RequestParam("sheetTitle") String sheetTitle, @RequestParam("sheetJson") String sheetJson, SecurityContextHolderAwareRequestWrapper request, boolean published) {
        getValidatedToken(context);

        if (sheetJson != null) {
            Gson gsonSheet = new Gson();
            Sheet sheet = gsonSheet.fromJson(sheetJson, Sheet.class);
            if (sheet != null) {
                sheet.setTitle(sheetTitle);
                sheet.setPublished(published);
                viewemService.reattachChildElements(sheet);
                sheetRepository.save(sheet);
            }
        }
        return listSheets(context, model, request);
    }

//  TODO: Enable the exception handling when appropriate
//    @ExceptionHandler(Exception.class)
//    public ModelAndView handleError(HttpServletRequest req, HttpServletResponse response, Exception exception) {
//        log.error("Request: " + req.getRequestURL() + " raised " + exception);
//
//        ModelAndView mav = new ModelAndView();
//        mav.addObject("errorCode", req.getAttribute("javax.servlet.error.status_code"));
//        mav.addObject("exception", exception);
//        mav.addObject("url", req.getRequestURL());
//        mav.addObject("timestamp", new Date().toString());
//        mav.setViewName("error");
//        return mav;
//    }

    @ExceptionHandler(InvalidTokenContextException.class)
    public ModelAndView handleTokenError(HttpServletRequest req, HttpServletResponse response, Exception exception) {
        log.error("Request: " + req.getRequestURL() + " raised " + exception, exception);

        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        log.error("Error handling token: ", exception);
        String stackTrace = result.toString();

        ModelAndView mav = new ModelAndView();
        mav.addObject("exception", exception);
        mav.addObject("stackTrace", StringEscapeUtils.escapeHtml4(stackTrace)
                .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
                .replace("\n", "<br/>"));

        mav.addObject("timestamp", new Date().toString());
        mav.setViewName("tokenError");
        return mav;
    }

    /**
     * @param context Context that contains the sheet to view
     * @param id Sheet to publish
     * @param model Model containing details necessary to render the page
     * @return A NoOp page
     */
    @RequestMapping(value="/{context}/edit/{id}/publish", method=RequestMethod.POST)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String publishViewSheet(@PathVariable("context") String context, @PathVariable("id") Long id,
                                   Model model, SecurityContextHolderAwareRequestWrapper request) {
        getValidatedToken(context);
        setPublishStatusOnViewSheet(id, true);

        return "noOp";
    }

    /**
     * @param context Context that contains the sheet to view
     * @param id Sheet to unpublish
     * @param model Model containing details necessary to render the page
     * @return A NoOp page
     */
    @RequestMapping(value="/{context}/edit/{id}/unpublish", method=RequestMethod.POST)
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String unpublishViewSheet(@PathVariable("context") String context, @PathVariable("id") Long id,
                                     Model model, SecurityContextHolderAwareRequestWrapper request) {
        getValidatedToken(context);
        setPublishStatusOnViewSheet(id, false);

        return "noOp";
    }

    /**
     * Sets the isPublished property of the sheet identified by sheetId and persists the sheet
     * @param sheetId
     * @param isPublished
     */
    private void setPublishStatusOnViewSheet(Long sheetId, boolean isPublished) {
        if (sheetId == null) {
            return;
        }

        Sheet sheet = sheetRepository.findById(sheetId).orElse(null);

        if (sheet == null) {
            return;
        }

        sheet.setPublished(isPublished);

        sheetRepository.save(sheet);
    }
    
    /**
     * 
     * @param context Context that contains the sheet to view
     * @param id the id of the sheet to download
     * @param model Model containing details necessary to render the page
     * @param request
     * @param response
     * @return user will be prompted to download a csv file representing this sheet
     */
    @RequestMapping("/{context}/download/{id}")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public String downloadSheet(@PathVariable("context") String context, @PathVariable("id") Long id, Model model,
    		SecurityContextHolderAwareRequestWrapper request, HttpServletResponse response) {

    	getValidatedToken(context);
        Sheet sheet = sheetRepository.findById(id).orElse(null);
        List<String[]> csvData = viewemService.convertSheetToArray(sheet);

        PrintWriter out = null;
        try {
            response.setContentType("text/comma-separated-values");
            setEscapedAttachmentHeader(response, sheet.getTitle() + ".csv");
            out = response.getWriter();

            CSVWriter writer = new CSVWriter(out);
            for (String[] row : csvData) {
                writer.writeNext(row);
            }

            writer.close();
            out.flush();
        } catch (IOException ioe) {
            log.error("IOException when attempting to download sheet with id: " + sheet.getSheetId(), ioe);
        } finally {
            out.close();
        }
    	return "noOp";
    }
    
    /**
	 * Convenience method for setting the content-disposition:attachment header with escaping a file name.
	 * @param response
	 * @param fileName unescaped file name of the attachment
	 */
    protected static void setEscapedAttachmentHeader(final HttpServletResponse response, final String fileName) {
    	String escapedFilename;
    	try {
    		escapedFilename = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
    	} catch (UnsupportedEncodingException e) {
    		escapedFilename = fileName;
        }

        response.setHeader("Content-Disposition", "attachment" +
                ((escapedFilename != null && escapedFilename.length() > 0) ? ("; filename*=utf-8''" + escapedFilename) : ""));
    }
}

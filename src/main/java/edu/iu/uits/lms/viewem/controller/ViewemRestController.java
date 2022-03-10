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

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.iu.uits.lms.common.date.DateFormatUtil;
import edu.iu.uits.lms.viewem.config.ToolConfig;
import edu.iu.uits.lms.viewem.model.Sheet;
import edu.iu.uits.lms.viewem.repository.SheetRepository;
//import io.swagger.annotations.Api;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * Created by chmaurer on 8/21/15.
 */
@RestController
@RequestMapping("/rest")
//@LmsSwaggerDocumentation
//@Slf4j
//@Api(tags = "viewem_rest")
public class ViewemRestController {

    @Autowired
    private SheetRepository sheetRepository = null;

    @Autowired
    private ToolConfig toolConfig;

    @GetMapping("/info")
    public Config getInfo() {
        return new Config(toolConfig);
    }

    /**
     * Need this class because I can't return the ToolConfig directly due to beanFactory things also being returned
     */
    @Data
    private static class Config {
        private String version;
        private String env;
        private List<String> instructorRoles;

        public Config(ToolConfig toolConfig) {
            this.version = toolConfig.getVersion();
            this.env = toolConfig.getEnv();
            this.instructorRoles = toolConfig.getInstructorRoles();
        }
    }

    @PostMapping("/undelete/{id}")
    public AlternativeSheet undeleteSheet(@PathVariable Long id) {
        Sheet sheet = sheetRepository.findById(id).orElse(null);
        sheet.setDeleted(false);
        sheet = sheetRepository.save(sheet);
        return new AlternativeSheet(sheet);
    }

    @Data
    private class AlternativeSheet {

        private Long sheetId;
        private String title;
        private String context;
        private String systemId;
        private boolean published;
        private String createdBy;
        private String modifiedBy;
        private boolean deleted;


        @JsonFormat(pattern= DateFormatUtil.JSON_DATE_FORMAT)
        private Date createdOn;

        @JsonFormat(pattern= DateFormatUtil.JSON_DATE_FORMAT)
        private Date modifiedOn;

        public AlternativeSheet(Sheet sheet) {
            this.context = sheet.getContext();
            this.createdBy = sheet.getCreatedBy();
            this.deleted = sheet.isDeleted();
            this.modifiedBy = sheet.getModifiedBy();
            this.published = sheet.isPublished();
            this.sheetId = sheet.getSheetId();
            this.systemId = sheet.getSystemId();
            this.title = sheet.getTitle();
            this.createdOn = sheet.getCreatedOn();
            this.modifiedOn = sheet.getModifiedOn();
        }
    }

}

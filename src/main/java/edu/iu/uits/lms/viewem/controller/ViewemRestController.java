package edu.iu.uits.lms.viewem.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.iu.uits.lms.common.date.DateFormatUtil;
import edu.iu.uits.lms.viewem.config.ToolConfig;
import edu.iu.uits.lms.viewem.model.Sheet;
import edu.iu.uits.lms.viewem.repository.SheetRepository;
import io.swagger.annotations.Api;
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
@Api(tags = "viewem_rest")
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

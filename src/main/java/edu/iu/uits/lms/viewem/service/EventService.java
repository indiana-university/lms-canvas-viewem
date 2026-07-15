package edu.iu.uits.lms.viewem.service;

import edu.iu.uits.lms.common.server.ServerUtils;
import edu.iu.uits.lms.viewem.config.ToolConfig;
import lombok.extern.slf4j.Slf4j;
import org.imsglobal.caliper.CaliperSendable;
import org.imsglobal.caliper.Envelope;
import org.imsglobal.caliper.Sensor;
import org.imsglobal.caliper.actions.Action;
import org.imsglobal.caliper.clients.CaliperClient;
import org.imsglobal.caliper.clients.HttpClient;
import org.imsglobal.caliper.clients.HttpClientOptions;
import org.imsglobal.caliper.config.Config;
import org.imsglobal.caliper.context.JsonldStringContext;
import org.imsglobal.caliper.entities.agent.CourseSection;
import org.imsglobal.caliper.entities.agent.Person;
import org.imsglobal.caliper.entities.agent.SoftwareApplication;
import org.imsglobal.caliper.entities.resource.WebPage;
import org.imsglobal.caliper.events.NavigationEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Profile("caliper")
@Service
@Slf4j
public class EventService {

    @Autowired
    private ToolConfig toolConfig;

    public void caliper(String location, String canvasCourseId, String canvasUserId, String role) {
        JsonldStringContext context = JsonldStringContext.getDefault();
        String id = "urn:uuid:" + UUID.randomUUID();



        SoftwareApplication edApp = SoftwareApplication.builder()
                .id(ServerUtils.getServerHostName())
                .name("viewem").version(toolConfig.getVersion())
                .build();

        Sensor sensor = Sensor.create("viewm.sensor");

        // TODO: provide connection details
        HttpClientOptions hco = HttpClientOptions.builder()
                // Do stuff...
//                .apiKey("443713f55bb27d0bb21fe56ed8d934fdc957dd99ea7bd96247243cc09ff8c75c")
//                .host("https://iu.caliper.dev.cloud.unizin.org")
                .host("https://caliper.iu.cloud.unizin.org")
                .apiKey("Bearer 209d677f20be6afc3158812eeb9766bd271e20b769f3a9cfec4fe69172587c17")
//                .host("iu.caliper.dev.cloud.unizin.org")
                .addExtraHeader("X-DEBUG", "TRUE")
                .build();

        CaliperClient cc = HttpClient.create("viewem.client", hco);
        sensor.registerClient(cc);

        Person actor = Person.builder().id("/users/canvas/".concat(canvasUserId)).build();

        CourseSection group = CourseSection.builder()
                .id("/courses/".concat(canvasCourseId))
//                .courseNumber("CPS 435-01")
//                .academicSession("Fall 2016")
                .build();

//        Membership membership = Membership.builder()
//                .id(BASE_IRI.concat("/terms/201601/courses/7/sections/1/rosters/1"))
//                .member(actor)
//                .organization(CourseSection.builder().id(group.getId()).build())
////                .status(Status.ACTIVE)
//                .role(role)
//                .dateCreated(new DateTime(2016, 8, 1, 6, 0, 0, 0, DateTimeZone.UTC))
//                .build();
//
//        Session session = LtiSession.builder()
//                .id(BASE_IRI.concat("/sessions/1f6442a482de72ea6ad134943812bff564a76259"))
//                .u
//                .startedAtTime(new DateTime(2016, 11, 15, 10, 0, 0, 0, DateTimeZone.UTC))
//                .build();

        WebPage object = WebPage.builder()
                .id(location)
//                .name("Learning Analytics Specifications")
//                .description("Overview of Learning Analytics Specifications with particular emphasis on IMS Caliper.")
//                .dateCreated(new DateTime(2016, 8, 1, 9, 0, 0, 0, DateTimeZone.UTC))
                .build();

        NavigationEvent event = NavigationEvent.builder()
//        ToolUseEvent event = ToolUseEvent.builder()
                .context(context)
                .id(id)
                .actor(actor)
                .action(Action.NAVIGATED_TO)
                .object(object)
                .eventTime(new DateTime(DateTimeZone.UTC))
//                .referrer(referrer)
                .edApp(edApp)
                .group(group)
//                .membership(membership)
//                .session(session)
                .build();

        // Prep envelope
        DateTime sendTime = new DateTime(DateTimeZone.UTC);
        List<CaliperSendable> data = new ArrayList<>();
        data.add(event);

        Envelope env = sensor.create(cc.getId(), sendTime, Config.DATA_VERSION, data);
//        log.debug("{}", env.);

        // Send
        sensor.send(cc, env);


    }
}

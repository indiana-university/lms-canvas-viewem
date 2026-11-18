package edu.iu.uits.lms.viewem.memory;

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

import edu.iu.uits.lms.viewem.model.PendingUpload;
import edu.iu.uits.lms.viewem.model.Sheet;
import edu.iu.uits.lms.viewem.service.ViewemService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.session.Session;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.iu.uits.lms.viewem.service.ViewemConstants.DATA_KEY_PENDING_UPLOAD;
import static edu.iu.uits.lms.viewem.service.ViewemConstants.DATA_KEY_ROSTER_USER_IDS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The new behaviour: Preview stores a gzipped copy of the CSV ({@link PendingUpload}), and Save removes it through
 * CourseSessionService.removeAttributeFromSession (which needs the lms-embedded-services fix to reach Redis).
 */
class PendingUploadInSessionMemoryTest extends AbstractSessionMemoryTest {

    @Override
    protected Object previewValue() {
        return PendingUpload.of(CSV.bytes, null);
    }

    @Override
    protected String scenarioName() {
        return "NEW - gzipped CSV (PendingUpload), removed at Save";
    }

    @Test
    void gzippedCsvInSessionAndRemovedAtSave() throws Exception {
        Result result = runPreviewAndFollowUps(DATA_KEY_PENDING_UPLOAD);

        // The pending upload survives the Redis round trip intact
        PendingUpload stored = courseSessionService.getAttributeFromSession(httpSession(result.sessionId()), CONTEXT,
                DATA_KEY_PENDING_UPLOAD, PendingUpload.class);
        try (InputStream csv = stored.csvStream()) {
            assertArrayEquals(CSV.bytes, csv.readAllBytes());
        }

        // Save: remove the entry, the way MainController.clearPendingUpload does, then persist the session
        Session session = sessionRepository.findById(result.sessionId());
        courseSessionService.removeAttributeFromSession(new SpringSessionHttpSession(session), CONTEXT, DATA_KEY_PENDING_UPLOAD);
        sessionRepository.save(session);

        HttpSession afterSave = httpSession(result.sessionId());
        assertNull(courseSessionService.getAttributeFromSession(afterSave, CONTEXT, DATA_KEY_PENDING_UPLOAD, PendingUpload.class),
                "removeAttributeFromSession must reach Redis (needs the lms-embedded-services fix)");
        assertEquals(CSV.userIds, courseSessionService.getAttributeFromSession(afterSave, CONTEXT, DATA_KEY_ROSTER_USER_IDS, List.class));
        long attributeBytesAfterSave = attributeBytes(result.sessionId());
        assertEquals(result.attributeBytesBeforePreview(), attributeBytesAfterSave,
                "after Save the session should be back to its launch size");

        // Compare with what the old code would have stored, without sending it through Redis/Netty in this JVM
        long oldAttributeBytes = oldSessionAttributeBytes();
        double reduction = (double) oldAttributeBytes / result.attributeBytes();

        report(result,
                String.format("Session attribute in Redis, after Save:                 %s", size(attributeBytesAfterSave)),
                String.format("Old code's session attribute for the same CSV:          %s (%.1fx larger; never removed)",
                        size(oldAttributeBytes), reduction));

        assertTrue(reduction >= 3.0, String.format("expected at least 3x smaller than the old Sheet, was %.1fx", reduction));
    }

    /** Serialized size of the old session attribute: the course map holding the roster and the parsed Sheet */
    private static long oldSessionAttributeBytes() throws Exception {
        Sheet sheet = new ViewemService().processCsvFile(new ByteArrayInputStream(CSV.bytes), "Title", CONTEXT,
                "systemId", false, "owner", true, roster(), new ArrayList<>());
        Map<String, Object> courseData = new HashMap<>();
        courseData.put(DATA_KEY_ROSTER_USER_IDS, CSV.userIds);
        courseData.put("sheet_data", sheet);
        Map<String, Map<String, Object>> courseMap = new HashMap<>();
        courseMap.put(CONTEXT, courseData);
        return new JdkSerializationRedisSerializer().serialize(courseMap).length;
    }
}

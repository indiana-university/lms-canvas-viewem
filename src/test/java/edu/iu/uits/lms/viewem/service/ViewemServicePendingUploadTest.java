package edu.iu.uits.lms.viewem.service;

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
import edu.iu.uits.lms.viewem.model.SheetUser;
import edu.iu.uits.lms.viewem.repository.SheetRepository;
import org.apache.commons.fileupload.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ViewemServicePendingUploadTest {

    private static final String CONTEXT = "1234";
    private static final String SYSTEM_ID = "systemId";
    private static final String OWNER = "teacher";
    private static final List<String> ROSTER = List.of("jdoe", "asmith");
    private static final byte[] CSV = ("username,Exam 1,Feedback\n" +
            "JDoe,95.5,Great work\n" +
            "asmith,88.0,Review section 3\n" +
            "notinroster,70.0,Skipped\n").getBytes(StandardCharsets.UTF_8);

    @Mock
    private SheetRepository sheetRepository;

    @InjectMocks
    private ViewemService viewemService;

    @BeforeEach
    void echoSaves() {
        lenient().when(sheetRepository.save(any(Sheet.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void newUploadIsParsedAndSaved() throws Exception {
        Sheet saved = viewemService.savePendingUpload(PendingUpload.of(CSV, null), "My Sheet", CONTEXT, SYSTEM_ID,
                OWNER, null, ROSTER);

        Sheet sheet = captureSaved();
        assertEquals(saved, sheet);
        assertNull(sheet.getSheetId());
        assertEquals("My Sheet", sheet.getTitle());
        assertEquals(CONTEXT, sheet.getContext());
        assertEquals(SYSTEM_ID, sheet.getSystemId());
        assertEquals(OWNER, sheet.getCreatedBy());
        assertFalse(sheet.isPublished());
        assertEquals(List.of("Exam 1", "Feedback"), sheet.getColumns().stream().map(c -> c.getTitle()).toList());
        assertEquals(List.of("jdoe", "asmith"), sheet.getSheetUsers().stream().map(SheetUser::getUserId).toList());
        assertEquals("95.5", sheet.getSheetUsers().get(0).getData().get(0).getData());
        verify(sheetRepository, never()).findById(any());
    }

    @Test
    public void publishOverrideIsApplied() throws Exception {
        viewemService.savePendingUpload(PendingUpload.of(CSV, null), "My Sheet", CONTEXT, SYSTEM_ID, OWNER, true, ROSTER);
        assertTrue(captureSaved().isPublished());
    }

    @Test
    public void editKeepsSheetIdAndExistingPublishedFlag() throws Exception {
        Sheet existing = new Sheet("Old", CONTEXT, SYSTEM_ID, true, OWNER, OWNER);
        existing.setSheetId(42L);
        when(sheetRepository.findById(42L)).thenReturn(Optional.of(existing));

        viewemService.savePendingUpload(PendingUpload.of(CSV, 42L), "New Title", CONTEXT, SYSTEM_ID, OWNER, null, ROSTER);

        Sheet sheet = captureSaved();
        assertEquals(42L, sheet.getSheetId());
        assertEquals("New Title", sheet.getTitle());
        assertTrue(sheet.isPublished());
    }

    @Test
    public void editCanUnpublish() throws Exception {
        Sheet existing = new Sheet("Old", CONTEXT, SYSTEM_ID, true, OWNER, OWNER);
        existing.setSheetId(42L);
        when(sheetRepository.findById(42L)).thenReturn(Optional.of(existing));

        viewemService.savePendingUpload(PendingUpload.of(CSV, 42L), "New Title", CONTEXT, SYSTEM_ID, OWNER, false, ROSTER);

        assertFalse(captureSaved().isPublished());
    }

    @Test
    public void unparseableUploadIsRejectedAndNothingSaved() {
        PendingUpload headerOnly = PendingUpload.of("username\n".getBytes(StandardCharsets.UTF_8), null);

        assertThrows(FileUploadException.class, () -> viewemService.savePendingUpload(headerOnly, "My Sheet", CONTEXT,
                SYSTEM_ID, OWNER, null, ROSTER));
        verify(sheetRepository, never()).save(any());
    }

    @Test
    public void corruptPendingUploadIsRejectedAndNothingSaved() {
        PendingUpload corrupt = new PendingUpload(new byte[]{1, 2, 3}, null);

        assertThrows(FileUploadException.class, () -> viewemService.savePendingUpload(corrupt, "My Sheet", CONTEXT,
                SYSTEM_ID, OWNER, null, ROSTER));
        verify(sheetRepository, never()).save(any());
    }

    private Sheet captureSaved() {
        ArgumentCaptor<Sheet> captor = ArgumentCaptor.forClass(Sheet.class);
        verify(sheetRepository).save(captor.capture());
        return captor.getValue();
    }
}

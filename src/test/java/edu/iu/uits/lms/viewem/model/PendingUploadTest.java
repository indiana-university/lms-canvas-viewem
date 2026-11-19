package edu.iu.uits.lms.viewem.model;

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

import org.junit.jupiter.api.Test;
import org.springframework.util.SerializationUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PendingUploadTest {

    private static final byte[] CSV = ("username,Exam 1,Feedback\n" +
            "jdoe,95.5,\"Great work, keep it up\"\n" +
            "asmith,88.0,\"Review section 3\"\n").repeat(200).getBytes(StandardCharsets.UTF_8);

    @Test
    public void csvStreamReturnsOriginalBytes() throws Exception {
        PendingUpload pending = PendingUpload.of(CSV, null);
        try (InputStream in = pending.csvStream()) {
            assertArrayEquals(CSV, in.readAllBytes());
        }
    }

    @Test
    public void storesCompressedBytes() {
        PendingUpload pending = PendingUpload.of(CSV, null);
        assertTrue(pending.gzippedCsv().length < CSV.length,
                "expected gzipped size " + pending.gzippedCsv().length + " < raw size " + CSV.length);
    }

    @Test
    public void keepsSheetId() {
        assertNull(PendingUpload.of(CSV, null).sheetId());
        assertEquals(42L, PendingUpload.of(CSV, 42L).sheetId());
    }

    @Test
    public void survivesJavaSerialization() throws Exception {
        PendingUpload pending = PendingUpload.of(CSV, 42L);
        @SuppressWarnings("deprecation")
        PendingUpload copy = (PendingUpload) SerializationUtils.deserialize(SerializationUtils.serialize(pending));

        assertEquals(42L, copy.sheetId());
        try (InputStream in = copy.csvStream()) {
            assertArrayEquals(CSV, in.readAllBytes());
        }
    }
}

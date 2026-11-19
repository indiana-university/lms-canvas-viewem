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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * An uploaded CSV waiting between the Preview and Save steps. It's kept in the HTTP session (Redis in deployed
 * environments), so it holds the gzipped raw CSV rather than the parsed {@link Sheet} graph, which is several
 * times larger. Save parses the CSV again from here.
 *
 * @param gzippedCsv the uploaded file, gzip-compressed
 * @param sheetId    the sheet being replaced when this is an edit-via-upload, or null for a new sheet
 */
public record PendingUpload(byte[] gzippedCsv, Long sheetId) implements Serializable {

    public static PendingUpload of(byte[] csv, Long sheetId) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.max(64, csv.length / 4));
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(csv);
        } catch (IOException e) {
            // Can't happen when writing to a ByteArrayOutputStream
            throw new UncheckedIOException(e);
        }
        return new PendingUpload(bytes.toByteArray(), sheetId);
    }

    /**
     * @return a stream of the original (uncompressed) CSV bytes
     * @throws IOException if the stored bytes aren't valid gzip data
     */
    public InputStream csvStream() throws IOException {
        return new GZIPInputStream(new ByteArrayInputStream(gzippedCsv));
    }
}

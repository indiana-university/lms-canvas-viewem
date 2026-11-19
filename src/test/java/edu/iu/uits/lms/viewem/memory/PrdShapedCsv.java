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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a synthetic CSV shaped like the largest View'em sheet in prd as of Nov 2026: 1073 users x 25 columns,
 * about 2.4 M characters of cell data (10 score columns + 15 feedback columns). Feedback words are drawn from a
 * 1,000-word Zipf-ish vocabulary, which compresses a bit worse than real feedback text, so gzip numbers here are
 * on the pessimistic side.
 */
final class PrdShapedCsv {

    static final int USERS = 1073;
    static final int SCORE_COLUMNS = 10;
    static final int FEEDBACK_COLUMNS = 15;

    final byte[] bytes;
    final List<String> userIds;

    private PrdShapedCsv(byte[] bytes, List<String> userIds) {
        this.bytes = bytes;
        this.userIds = userIds;
    }

    static PrdShapedCsv build() {
        Random vocabRandom = new Random(7);
        String[] vocab = new String[1000];
        for (int i = 0; i < vocab.length; i++) {
            vocab[i] = randomLetters(vocabRandom, 3 + vocabRandom.nextInt(7));
        }

        Random r = new Random(42);
        List<String> userIds = new ArrayList<>(USERS);
        StringBuilder sb = new StringBuilder("username");
        for (int c = 0; c < SCORE_COLUMNS; c++) sb.append(",Assignment ").append(c + 1);
        for (int c = 0; c < FEEDBACK_COLUMNS; c++) sb.append(",Feedback ").append(c + 1);
        sb.append('\n');

        for (int u = 0; u < USERS; u++) {
            // Unique, already lowercase (processCsvFile lowercases ids before checking the roster)
            String userId = randomLetters(r, 5) + u;
            userIds.add(userId);
            sb.append(userId);
            for (int c = 0; c < SCORE_COLUMNS; c++) {
                sb.append(',');
                if (r.nextInt(20) != 0) sb.append(String.format("%.1f", 50 + r.nextDouble() * 50));
            }
            for (int c = 0; c < FEEDBACK_COLUMNS; c++) {
                sb.append(",\"");
                int words = 5 + r.nextInt(35);
                for (int w = 0; w < words; w++) {
                    if (w > 0) sb.append(' ');
                    sb.append(vocab[(int) (Math.pow(r.nextDouble(), 3) * vocab.length)]);
                }
                sb.append('"');
            }
            sb.append('\n');
        }
        return new PrdShapedCsv(sb.toString().getBytes(StandardCharsets.UTF_8), userIds);
    }

    private static String randomLetters(Random r, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append((char) ('a' + r.nextInt(26)));
        return sb.toString();
    }
}

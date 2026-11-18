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

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.session.Session;

import java.time.Duration;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Minimal HttpSession view of a Spring Session {@link Session}, so the real {@code CourseSessionService} can be
 * used against a real Redis-backed session (the same thing Spring Session's filter does in the app).
 */
final class SpringSessionHttpSession implements HttpSession {

    private final Session session;

    SpringSessionHttpSession(Session session) {
        this.session = session;
    }

    @Override public Object getAttribute(String name) { return session.getAttribute(name); }
    @Override public void setAttribute(String name, Object value) { session.setAttribute(name, value); }
    @Override public void removeAttribute(String name) { session.removeAttribute(name); }
    @Override public Enumeration<String> getAttributeNames() { return Collections.enumeration(session.getAttributeNames()); }
    @Override public String getId() { return session.getId(); }
    @Override public long getCreationTime() { return session.getCreationTime().toEpochMilli(); }
    @Override public long getLastAccessedTime() { return session.getLastAccessedTime().toEpochMilli(); }
    @Override public void setMaxInactiveInterval(int interval) { session.setMaxInactiveInterval(Duration.ofSeconds(interval)); }
    @Override public int getMaxInactiveInterval() { return (int) session.getMaxInactiveInterval().toSeconds(); }
    @Override public ServletContext getServletContext() { throw new UnsupportedOperationException(); }
    @Override public void invalidate() { throw new UnsupportedOperationException(); }
    @Override public boolean isNew() { return false; }
}

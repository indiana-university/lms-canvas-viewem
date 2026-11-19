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

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * Fails the test class up front, with a plain explanation, when Docker isn't available. Without this, the first
 * thing you'd see is Testcontainers' "Could not find a valid Docker environment" error from inside container
 * startup, which doesn't say which tests need Docker or why.
 *
 * <p>This deliberately fails instead of skipping: these tests only run when asked for (-Pmemory-tests), so a skip
 * would look like a pass. Register it before {@code @Testcontainers} so it runs before any container starts.
 */
public class RequiresDockerExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            String message = """

                    ************************************************************************
                    Docker is required for %s.
                    These are the View'em Redis session memory tests (-Pmemory-tests); they
                    start Redis with Testcontainers. Start Docker (e.g. Docker Desktop) and
                    run them again. The normal build doesn't run them.
                    ************************************************************************
                    """.formatted(context.getRequiredTestClass().getSimpleName());
            throw new IllegalStateException(message);
        }
    }
}

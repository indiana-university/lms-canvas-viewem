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

import com.sun.management.ThreadMXBean;
import edu.iu.uits.lms.common.session.CourseSessionService;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufAllocatorMetricProvider;
import io.netty.util.internal.PlatformDependent;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static edu.iu.uits.lms.viewem.service.ViewemConstants.DATA_KEY_ROSTER_USER_IDS;

/**
 * Measures what an upload preview costs a Redis-backed session, using real Redis (Testcontainers), real Spring
 * Session ({@link RedisSessionRepository}, which Boot uses by default), real Lettuce/Netty, and the real
 * {@link CourseSessionService}. Each subclass stores a different value at Preview, then simulates the follow-up
 * requests a user makes while that value is still in the session.
 *
 * <p>Tagged "memory": excluded from the normal build. Run with {@code mvn test -Pmemory-tests}. Needs Docker;
 * {@link RequiresDockerExtension} fails the run with a clear message if it isn't available. The profile runs
 * each test class in a fresh JVM, so the Netty direct-memory numbers of one scenario don't leak into the other.
 *
 * <p>What the numbers mean:
 * <ul>
 *   <li><b>Session attribute in Redis</b>: the size of the {@code viewem_course_session} hash field. Spring Session
 *       reads every field of the session hash on every request, so this is roughly what each request pulls
 *       through Lettuce/Netty and deserializes.</li>
 *   <li><b>Netty / JVM direct memory peak</b>: high-water mark above the baseline, sampled every millisecond
 *       during Preview and the follow-up requests. Pooled allocators keep memory once they've grown, so this
 *       shows how far the allocator had to grow, not how much is in use at any instant.</li>
 * </ul>
 */
@Slf4j
@Tag("memory")
// Must come before @Testcontainers so the Docker check runs before any container starts
@ExtendWith(RequiresDockerExtension.class)
@Testcontainers
abstract class AbstractSessionMemoryTest {

    /** Matches {@code @EnableCourseSessionService(sessionKey = ...)} on WebApplication */
    static final String SESSION_KEY = "viewem_course_session";
    static final String CONTEXT = "1234";
    static final int FOLLOW_UP_REQUESTS = 50;
    /** Instructors with a large preview in their session at the same time, all making requests concurrently */
    static final int CONCURRENT_USERS = 10;
    static final int CONCURRENT_REQUESTS_EACH = 10;

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    static final PrdShapedCsv CSV = PrdShapedCsv.build();

    protected final CourseSessionService courseSessionService = new CourseSessionService(SESSION_KEY);
    protected RedisTemplate<String, Object> redisTemplate;
    /** Typed as the public interface: RedisSessionRepository.RedisSession isn't public in Spring Session 4 */
    protected SessionRepository<Session> sessionRepository;
    private LettuceConnectionFactory connectionFactory;

    @BeforeEach
    void connect() {
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();

        // Same serializers Spring Session's RedisHttpSessionConfiguration uses
        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setHashValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.afterPropertiesSet();

        @SuppressWarnings({"unchecked", "rawtypes"})
        SessionRepository<Session> repository = (SessionRepository) new RedisSessionRepository(redisTemplate);
        sessionRepository = repository;
    }

    @AfterEach
    void disconnect() {
        connectionFactory.destroy();
    }

    /** What the Preview step stores under {@link #DATA_KEY_PENDING_UPLOAD} (or the old key) in this scenario */
    protected abstract Object previewValue();

    protected abstract String scenarioName();

    /**
     * Phase 1 (one user): Launch (roster into the session) → Preview (store {@link #previewValue()}) → N follow-up
     * requests, each a full session read plus a lastAccessedTime write, the way SessionRepositoryFilter does it.
     * <p>Phase 2 (concurrent): {@link #CONCURRENT_USERS} more sessions each launch and preview, then all of them make
     * requests at the same time over the shared Lettuce connection (Boot shares one native connection by default).
     * <p>Direct-memory high-water marks are cumulative from before Phase 1.
     */
    protected Result runPreviewAndFollowUps(String dataKey) throws InterruptedException {
        String sessionId = launch();
        long attributeBytesBeforePreview = attributeBytes(sessionId);

        DirectMemorySampler sampler = DirectMemorySampler.start();

        preview(sessionId, dataKey);
        long attributeBytes = attributeBytes(sessionId);
        long sessionMemoryUsage = redisMemoryUsage(sessionId);

        // Deserialization happens on the calling thread, so its allocation counter shows the heap cost per request
        ThreadMXBean threads = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        long allocatedBefore = threads.getCurrentThreadAllocatedBytes();
        long start = System.nanoTime();
        for (int i = 0; i < FOLLOW_UP_REQUESTS; i++) {
            followUpRequest(sessionId);
        }
        long followUpMillis = (System.nanoTime() - start) / 1_000_000;
        long heapPerRequest = (threads.getCurrentThreadAllocatedBytes() - allocatedBefore) / FOLLOW_UP_REQUESTS;
        DirectMemorySampler.Peaks singleUserPeaks = sampler.snapshot();

        List<String> concurrentIds = new ArrayList<>();
        for (int u = 0; u < CONCURRENT_USERS; u++) {
            String id = launch();
            preview(id, dataKey);
            concurrentIds.add(id);
        }
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (String id : concurrentIds) {
            futures.add(pool.submit(() -> {
                go.await();
                for (int i = 0; i < CONCURRENT_REQUESTS_EACH; i++) {
                    followUpRequest(id);
                }
                return null;
            }));
        }
        start = System.nanoTime();
        go.countDown();
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw new IllegalStateException(e.getCause());
            }
        }
        long concurrentMillis = (System.nanoTime() - start) / 1_000_000;
        pool.shutdown();

        DirectMemorySampler.Peaks allPeaks = sampler.stop();
        return new Result(sessionId, attributeBytesBeforePreview, attributeBytes, sessionMemoryUsage, followUpMillis,
                heapPerRequest, singleUserPeaks, concurrentMillis, allPeaks);
    }

    private String launch() {
        Session session = sessionRepository.createSession();
        courseSessionService.addAttributeToSession(new SpringSessionHttpSession(session), CONTEXT,
                DATA_KEY_ROSTER_USER_IDS, CSV.userIds);
        sessionRepository.save(session);
        return session.getId();
    }

    private void preview(String sessionId, String dataKey) {
        Session session = sessionRepository.findById(sessionId);
        courseSessionService.addAttributeToSession(new SpringSessionHttpSession(session), CONTEXT, dataKey, previewValue());
        sessionRepository.save(session);
    }

    private void followUpRequest(String sessionId) {
        Session session = sessionRepository.findById(sessionId);
        session.setLastAccessedTime(Instant.now());
        sessionRepository.save(session);
    }

    protected HttpSession httpSession(String sessionId) {
        return new SpringSessionHttpSession(sessionRepository.findById(sessionId));
    }

    protected long attributeBytes(String sessionId) {
        byte[] key = sessionKey(sessionId);
        byte[] field = ("sessionAttr:" + SESSION_KEY).getBytes(StandardCharsets.UTF_8);
        Long length = redisTemplate.execute((RedisCallback<Long>) c -> c.hashCommands().hStrLen(key, field));
        return length == null ? 0 : length;
    }

    protected long redisMemoryUsage(String sessionId) {
        // Spring's generic execute() can't read an integer reply, so ask redis-cli inside the container
        try {
            var result = REDIS.execInContainer("redis-cli", "MEMORY", "USAGE",
                    new String(sessionKey(sessionId), StandardCharsets.UTF_8), "SAMPLES", "0");
            return Long.parseLong(result.getStdout().trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private static byte[] sessionKey(String sessionId) {
        // RedisSessionRepository's default key layout: <namespace>:sessions:<id>
        return ("spring:session:sessions:" + sessionId).getBytes(StandardCharsets.UTF_8);
    }

    protected void report(Result result, String... extraLines) {
        StringBuilder sb = new StringBuilder()
                .append("\n==== View'em session memory: ").append(scenarioName()).append(" ====\n")
                .append(String.format("CSV: %d users x %d columns, %s raw%n", PrdShapedCsv.USERS,
                        PrdShapedCsv.SCORE_COLUMNS + PrdShapedCsv.FEEDBACK_COLUMNS, size(CSV.bytes.length)))
                .append(String.format("Session attribute in Redis, after launch (roster only): %s%n", size(result.attributeBytesBeforePreview())))
                .append(String.format("Session attribute in Redis, after Preview:              %s%n", size(result.attributeBytes())))
                .append(String.format("Redis MEMORY USAGE for the session hash:                %s%n", size(result.sessionMemoryUsage())))
                .append(String.format("Read back over %d follow-up requests:                   %s (%d ms)%n", FOLLOW_UP_REQUESTS,
                        size(result.attributeBytes() * FOLLOW_UP_REQUESTS), result.followUpMillis()))
                .append(String.format("Heap allocated per follow-up request:                   %s%n", size(result.heapPerRequest())))
                .append(String.format("Netty allocator in use:                                 %s%n", ByteBufAllocator.DEFAULT.getClass().getSimpleName()))
                .append(String.format("Direct memory high-water, one user (allocator / Netty total / JVM pool): %s / %s / %s%n",
                        size(result.singleUserPeaks().nettyAllocator()), size(result.singleUserPeaks().nettyPlatform()),
                        size(result.singleUserPeaks().jvmDirectPool())))
                .append(String.format("%d users previewing, then %d requests each at once:      %d ms%n", CONCURRENT_USERS,
                        CONCURRENT_REQUESTS_EACH, result.concurrentMillis()))
                .append(String.format("Direct memory high-water, after concurrent phase:       %s / %s / %s%n",
                        size(result.allPeaks().nettyAllocator()), size(result.allPeaks().nettyPlatform()),
                        size(result.allPeaks().jvmDirectPool())));
        for (String line : extraLines) {
            sb.append(line).append('\n');
        }
        String text = sb.toString();
        log.info("{} memory report:\n{}", getClass().getSimpleName(), text);
        try {
            Path out = Path.of("target", "memory-report-" + getClass().getSimpleName() + ".txt");
            Files.createDirectories(out.getParent());
            Files.writeString(out, text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String size(long bytes) {
        if (bytes < 0) return "n/a";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }

    record Result(String sessionId, long attributeBytesBeforePreview, long attributeBytes, long sessionMemoryUsage,
                  long followUpMillis, long heapPerRequest, DirectMemorySampler.Peaks singleUserPeaks, long concurrentMillis,
                  DirectMemorySampler.Peaks allPeaks) {}

    /** Polls direct-memory counters on a background thread and keeps the high-water mark above the starting value */
    static final class DirectMemorySampler {
        record Peaks(long nettyAllocator, long nettyPlatform, long jvmDirectPool) {}

        private final AtomicLong nettyAllocatorPeak = new AtomicLong();
        private final AtomicLong nettyPlatformPeak = new AtomicLong();
        private final AtomicLong jvmPoolPeak = new AtomicLong();
        private final long nettyAllocatorBase = nettyAllocatorUsed();
        private final long nettyPlatformBase = PlatformDependent.usedDirectMemory();
        private final long jvmPoolBase = jvmDirectPoolUsed();
        private volatile boolean running = true;
        private final Thread thread = new Thread(this::poll, "direct-memory-sampler");

        static DirectMemorySampler start() {
            DirectMemorySampler sampler = new DirectMemorySampler();
            sampler.thread.setDaemon(true);
            sampler.thread.start();
            return sampler;
        }

        /** High-water marks so far, without stopping */
        Peaks snapshot() {
            sample();
            return peaks();
        }

        Peaks stop() {
            running = false;
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            sample();
            return peaks();
        }

        private Peaks peaks() {
            return new Peaks(nettyAllocatorBase < 0 ? -1 : nettyAllocatorPeak.get(),
                    nettyPlatformBase < 0 ? -1 : nettyPlatformPeak.get(), jvmPoolPeak.get());
        }

        private void poll() {
            while (running) {
                sample();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        private void sample() {
            nettyAllocatorPeak.accumulateAndGet(nettyAllocatorUsed() - nettyAllocatorBase, Math::max);
            nettyPlatformPeak.accumulateAndGet(PlatformDependent.usedDirectMemory() - nettyPlatformBase, Math::max);
            jvmPoolPeak.accumulateAndGet(jvmDirectPoolUsed() - jvmPoolBase, Math::max);
        }

        private static long nettyAllocatorUsed() {
            return ByteBufAllocator.DEFAULT instanceof ByteBufAllocatorMetricProvider provider
                    ? provider.metric().usedDirectMemory() : -1;
        }

        private static long jvmDirectPoolUsed() {
            return ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class).stream()
                    .filter(pool -> "direct".equals(pool.getName()))
                    .mapToLong(BufferPoolMXBean::getMemoryUsed)
                    .findFirst().orElse(-1);
        }
    }

    static List<String> roster() {
        return CSV.userIds;
    }
}

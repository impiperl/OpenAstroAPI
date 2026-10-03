package astroapi.service;

import swisseph.SwissEph;

import java.util.concurrent.*;

/**
 * Runs Swiss Ephemeris calculations on a bounded worker pool.
 *
 * <p>The pure-Java {@link SwissEph} object holds per-instance mutable state
 * (SwissData, open file pointers, topocentric settings set via
 * {@code swe_set_topo}) and is therefore <b>not</b> safe to share across
 * threads. Instead of serialising every calculation onto a single thread, each
 * worker thread owns its own {@code SwissEph} instance via a {@link ThreadLocal}.
 * The fixed pool size thus doubles as the instance-pool size: at most
 * {@code N} instances exist, each only ever touched by one thread at a time.
 *
 * <p>The only shared static state in the library ({@code SweDate}'s Delta-T
 * table and tidal acceleration) is guarded by {@code synchronized} methods and
 * is effectively read-only after a one-time lazy init, so parallel instances
 * are safe.
 */
public class SwissEphemerisExecutor {
    private final ThreadPoolExecutor executor;
    private final ThreadLocal<SwissEph> localEph;

    public SwissEphemerisExecutor(String ephePath) {
        this(ephePath, Math.max(2, Runtime.getRuntime().availableProcessors()), null);
    }

    /**
     * Pool mit eigener Größe und einem Initialisierer, der <b>einmal je Worker-Thread</b> auf der
     * frisch gebauten {@link SwissEph}-Instanz läuft.
     *
     * <p>Genau dafür ist der Initialisierer da: die siderische Betriebsart
     * ({@code swe_set_sid_mode}) ist Zustand <i>der Instanz</i> ({@code swed.sidd.sid_mode}). Wer
     * siderisch rechnen will, braucht deshalb einen eigenen Instanzpool — sonst trüge dieselbe
     * {@code SwissEph}, die gleich darauf eine tropische Radix rechnet, eine gesetzte
     * Ayanamsha mit sich. Mit einem zweiten Pool ist eine Vermischung technisch unmöglich statt
     * nur unwahrscheinlich.
     *
     * @param ephePath    Verzeichnis der Ephemeridendateien
     * @param threads     Poolgröße = Höchstzahl gleichzeitiger {@code SwissEph}-Instanzen
     * @param initializer läuft je Thread genau einmal, direkt nach {@code new SwissEph(...)};
     *                    {@code null} für keine Initialisierung
     */
    public SwissEphemerisExecutor(String ephePath, int threads,
                                  java.util.function.Consumer<SwissEph> initializer) {
        threads = Math.max(1, threads);
        // Bounded task queue so a burst of requests can't grow the backlog (and
        // heap) without limit. When the queue is full the submitting (request)
        // thread blocks until a slot frees up — natural backpressure instead of
        // OOM or unbounded latency. The queue is generously sized because tasks
        // are millisecond-level and heavily cached.
        int queueCapacity = Math.max(128, threads * 64);
        this.executor = new ThreadPoolExecutor(
                threads, threads, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new BoundedWaitPolicy(OFFER_TIMEOUT_MS));
        // Each worker thread lazily builds (and then reuses) its own SwissEph.
        this.localEph = ThreadLocal.withInitial(() -> {
            SwissEph eph = new SwissEph(ephePath);
            if (initializer != null) initializer.accept(eph);
            return eph;
        });
    }

    /** How long a submitting thread waits for a free queue slot before the
     *  request is shed with {@link RejectedExecutionException} (→ HTTP 503). */
    private static final long OFFER_TIMEOUT_MS = 500;

    /**
     * When the bounded queue is full, wait a short bounded time for a slot; if
     * none frees up, reject so the caller can return 503 instead of pinning a
     * request thread indefinitely. Deliberately not CallerRunsPolicy: running
     * the task on the caller (request) thread would allocate a SwissEph outside
     * the pool and break the bounded instance count.
     */
    private static final class BoundedWaitPolicy implements RejectedExecutionHandler {
        private final long offerTimeoutMs;

        BoundedWaitPolicy(long offerTimeoutMs) {
            this.offerTimeoutMs = offerTimeoutMs;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (e.isShutdown()) {
                throw new RejectedExecutionException("Swiss Ephemeris executor is shutting down");
            }
            try {
                if (!e.getQueue().offer(r, offerTimeoutMs, TimeUnit.MILLISECONDS)) {
                    throw new RejectedExecutionException("Swiss Ephemeris executor overloaded");
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Interrupted while queueing Swiss Ephemeris task", ie);
            }
        }
    }

    /**
     * The {@link SwissEph} instance owned by the current worker thread.
     * Only valid when called from inside a task submitted via
     * {@link #runWithTimeout} — i.e. every Swiss Ephemeris access must happen
     * inside such a task so it runs on a pool thread (bounded instance count).
     */
    public SwissEph current() {
        return localEph.get();
    }

    public <T> T runWithTimeout(Callable<T> task, long timeout, TimeUnit unit) throws TimeoutException {
        Future<T> future = executor.submit(task);
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            // Task did not answer in time
            future.cancel(true);
            throw e;
        } catch (InterruptedException | ExecutionException e) {
            // Error during execution
            throw new RuntimeException("Error executing Swiss Ephemeris task: " + e.getMessage(), e);
        }
    }

    /** Graceful shutdown: stop accepting work, let in-flight calcs finish
     *  briefly, then force-stop. Invoked from the owning bean's {@code @PreDestroy}. */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

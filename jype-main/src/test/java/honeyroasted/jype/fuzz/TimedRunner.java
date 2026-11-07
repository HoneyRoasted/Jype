package honeyroasted.jype.fuzz;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class TimedRunner implements AutoCloseable {
    private final long timeoutMillis;
    private final ClassLoader contextLoader;
    private ExecutorService executor;

    public TimedRunner(long timeoutMillis, ClassLoader contextLoader) {
        this.timeoutMillis = timeoutMillis;
        this.contextLoader = contextLoader;
        this.executor = this.newExecutor();
    }

    private ExecutorService newExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "fuzz-worker");
            t.setDaemon(true);
            t.setContextClassLoader(this.contextLoader);
            return t;
        });
    }

    public <T> Outcome<T> run(Callable<T> callable) {
        Future<T> future = this.executor.submit(callable);
        try {
            return Outcome.value(future.get(this.timeoutMillis, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            future.cancel(true);
            this.executor.shutdownNow();
            this.executor = this.newExecutor();
            return Outcome.timeout();
        } catch (ExecutionException e) {
            return Outcome.error(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Outcome.error(e);
        }
    }

    @Override
    public void close() {
        this.executor.shutdownNow();
    }
}

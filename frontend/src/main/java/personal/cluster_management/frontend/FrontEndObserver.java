package personal.cluster_management.frontend;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;

/**
 * Observer that handles the callback from a Backend server.
 * It adds the result to the collector and counts down the latch so the main thread knows when to proceed.
 * @param <T> The response type.
 */
public class FrontEndObserver<T> implements StreamObserver<T> {

    private final ResponseCollector<T> collector;
    private final CountDownLatch latch;

    public FrontEndObserver(ResponseCollector<T> collector, CountDownLatch latch) {
        this.collector = collector;
        this.latch = latch;
    }

    @Override
    public void onNext(T value) {
        // We received a valid response from a backend
        collector.addResponse(value);
    }

    @Override
    public void onError(Throwable t) {
        System.err.println("Error receiving response from backend: " + t.getMessage());
        // Count down even on error so the frontend doesn't hang forever
        latch.countDown();
    }

    @Override
    public void onCompleted() {
        latch.countDown();
    }
}
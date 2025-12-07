package personal.cluster_management.frontend;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import personal.cluster_management.proto.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests covering interactions between Frontend and (Mocked) Backends.
 * Focuses on partial failures, specific method forwarding, and error handling.
 */
class FrontEndIntegrationTest {

    private Server healthyBackendServer;
    private Server failingBackendServer;
    private ManagedChannel healthyChannel;
    private ManagedChannel failingChannel;
    private FrontEndImpl frontend;

    private final String healthyServerName = "in-process-healthy";
    private final String failingServerName = "in-process-failing";

    /**
     * A Backend Service implementation that behaves normally.
     */
    static class HealthyBackendService extends DistributedJobServiceGrpc.DistributedJobServiceImplBase {
        @Override
        public void createTmuxSession(TmuxCreateRequest request, StreamObserver<JobStatusResponse> responseObserver) {
            responseObserver.onNext(JobStatusResponse.newBuilder()
                    .setJobId("tmux-" + request.getSessionName())
                    .setStatus(JobStatusResponse.Status.RUNNING)
                    .setMessage("Session created on Healthy Backend")
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void getStatus(ServerStatusRequest request, StreamObserver<ServerStatusResponse> responseObserver) {
            responseObserver.onNext(ServerStatusResponse.newBuilder()
                    .addServers(ServerInfo.newBuilder().setHostName("HealthyHost").setStatus(ServerInfo.Availability.AVAILABLE).build())
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * A Backend Service implementation that throws errors.
     */
    static class FailingBackendService extends DistributedJobServiceGrpc.DistributedJobServiceImplBase {
        @Override
        public void getStatus(ServerStatusRequest request, StreamObserver<ServerStatusResponse> responseObserver) {
            // Simulate a runtime error (e.g., network failure or internal error)
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Simulated Internal Error")
                    .asRuntimeException());
        }

        @Override
        public void submitJob(JobRequest request, StreamObserver<JobStatusResponse> responseObserver) {
             responseObserver.onError(Status.UNAVAILABLE
                    .withDescription("Simulated Unavailable")
                    .asRuntimeException());
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        // 1. Start a Healthy Backend
        healthyBackendServer = InProcessServerBuilder.forName(healthyServerName)
                .addService(new HealthyBackendService())
                .directExecutor()
                .build()
                .start();

        // 2. Start a Failing Backend
        failingBackendServer = InProcessServerBuilder.forName(failingServerName)
                .addService(new FailingBackendService())
                .directExecutor()
                .build()
                .start();

        // 3. Create Channels
        healthyChannel = InProcessChannelBuilder.forName(healthyServerName).directExecutor().build();
        failingChannel = InProcessChannelBuilder.forName(failingServerName).directExecutor().build();

        // 4. Initialize Frontend with both channels
        List<ManagedChannel> channels = new ArrayList<>();
        channels.add(healthyChannel);
        channels.add(failingChannel);
        frontend = new FrontEndImpl(channels);
    }

    @AfterEach
    void tearDown() {
        if (healthyChannel != null) healthyChannel.shutdownNow();
        if (failingChannel != null) failingChannel.shutdownNow();
        if (healthyBackendServer != null) healthyBackendServer.shutdownNow();
        if (failingBackendServer != null) failingBackendServer.shutdownNow();
    }

    @Test
    void testCreateTmuxSessionForwardsSuccessfully() throws InterruptedException {
        // Arrange
        TmuxCreateRequest request = TmuxCreateRequest.newBuilder()
                .setSessionName("demo-session")
                .setInitialCommand("top")
                .build();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<JobStatusResponse> responseRef = new AtomicReference<>();

        // Act
        // Frontend picks the first channel (healthyChannel) by default logic (index 0)
        frontend.createTmuxSession(request, new StreamObserver<JobStatusResponse>() {
            @Override
            public void onNext(JobStatusResponse value) {
                responseRef.set(value);
            }
            @Override
            public void onError(Throwable t) { latch.countDown(); }
            @Override
            public void onCompleted() { latch.countDown(); }
        });

        // Assert
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(responseRef.get());
        assertEquals("tmux-demo-session", responseRef.get().getJobId());
        assertTrue(responseRef.get().getMessage().contains("Healthy Backend"));
    }

    @Test
    void testGetStatusPartialFailureReturnsAvailableResults() throws InterruptedException {
        // Arrange
        ServerStatusRequest request = ServerStatusRequest.newBuilder().build();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ServerStatusResponse> responseRef = new AtomicReference<>();

        // Act
        // This should broadcast to BOTH healthy and failing backends.
        // The failing backend will trigger onError in the FrontEndObserver, which should just countdown the latch.
        frontend.getStatus(request, new StreamObserver<ServerStatusResponse>() {
            @Override
            public void onNext(ServerStatusResponse value) {
                responseRef.set(value);
            }
            @Override
            public void onError(Throwable t) { latch.countDown(); }
            @Override
            public void onCompleted() { latch.countDown(); }
        });

        // Assert
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "Frontend should complete even if one backend fails");
        
        assertNotNull(responseRef.get(), "Should receive a partial response");
        assertEquals(1, responseRef.get().getServersCount(), "Should contain exactly one server status (from the healthy node)");
        assertEquals("HealthyHost", responseRef.get().getServers(0).getHostName());
    }

    @Test
    void testSubmitJobBackendRuntimeErrorPropagatesError() throws InterruptedException {
        // Arrange
        // We need to force the frontend to use the failing channel. 
        // Since FrontEndImpl currently picks index 0, we create a specific frontend instance for this test.
        List<ManagedChannel> failOnlyChannels = new ArrayList<>();
        failOnlyChannels.add(failingChannel);
        FrontEndImpl failFrontend = new FrontEndImpl(failOnlyChannels);

        JobRequest request = JobRequest.newBuilder().setJobId("doomed-job").build();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        // Act
        failFrontend.submitJob(request, new StreamObserver<JobStatusResponse>() {
            @Override
            public void onNext(JobStatusResponse value) {}
            @Override
            public void onError(Throwable t) {
                errorRef.set(t);
                latch.countDown();
            }
            @Override
            public void onCompleted() { latch.countDown(); }
        });

        // Assert
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(errorRef.get());
        // Verify it is the specific gRPC exception we threw
        assertTrue(errorRef.get().getMessage().contains("Simulated Unavailable"));
    }
}
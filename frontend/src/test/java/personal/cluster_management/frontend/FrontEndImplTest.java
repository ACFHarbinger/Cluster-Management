package personal.cluster_management.frontend;

import io.grpc.ManagedChannel;
import io.grpc.Server;
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

class FrontEndImplTest {

    private Server fakeBackendServer1;
    private Server fakeBackendServer2;
    private ManagedChannel channel1;
    private ManagedChannel channel2;
    private FrontEndImpl frontend;

    // Names for the in-process servers
    private final String serverName1 = "in-process-server-1";
    private final String serverName2 = "in-process-server-2";

    /**
     * A mock implementation of the Backend Service to handle calls during tests.
     */
    private static class MockBackendService extends DistributedJobServiceGrpc.DistributedJobServiceImplBase {
        private final String serverId;

        MockBackendService(String serverId) {
            this.serverId = serverId;
        }

        @Override
        public void submitJob(JobRequest request, StreamObserver<JobStatusResponse> responseObserver) {
            // Echo back a success with the server ID in the message
            responseObserver.onNext(JobStatusResponse.newBuilder()
                    .setJobId(request.getJobId())
                    .setStatus(JobStatusResponse.Status.RUNNING)
                    .setMessage("Processed by " + serverId)
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void getStatus(ServerStatusRequest request, StreamObserver<ServerStatusResponse> responseObserver) {
            // Return status for this specific fake server
            ServerInfo info = ServerInfo.newBuilder()
                    .setHostName(serverId)
                    .setStatus(ServerInfo.Availability.AVAILABLE)
                    .build();
            responseObserver.onNext(ServerStatusResponse.newBuilder()
                    .addServers(info)
                    .build());
            responseObserver.onCompleted();
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        // 1. Start two fake in-process backend servers
        fakeBackendServer1 = InProcessServerBuilder.forName(serverName1)
                .addService(new MockBackendService("Backend-1"))
                .directExecutor()
                .build()
                .start();

        fakeBackendServer2 = InProcessServerBuilder.forName(serverName2)
                .addService(new MockBackendService("Backend-2"))
                .directExecutor()
                .build()
                .start();

        // 2. Create channels connecting to them
        channel1 = InProcessChannelBuilder.forName(serverName1).directExecutor().build();
        channel2 = InProcessChannelBuilder.forName(serverName2).directExecutor().build();

        // 3. Initialize Frontend with these channels
        List<ManagedChannel> channels = new ArrayList<>();
        channels.add(channel1);
        channels.add(channel2);
        frontend = new FrontEndImpl(channels);
    }

    @AfterEach
    void tearDown() {
        // Shutdown channels
        if (channel1 != null) channel1.shutdownNow();
        if (channel2 != null) channel2.shutdownNow();
        
        // Shutdown servers
        if (fakeBackendServer1 != null) fakeBackendServer1.shutdownNow();
        if (fakeBackendServer2 != null) fakeBackendServer2.shutdownNow();
    }

    @Test
    void testSubmitJobForwardsToBackend() throws InterruptedException {
        // Arrange
        JobRequest request = JobRequest.newBuilder().setJobId("job-123").build();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<JobStatusResponse> responseRef = new AtomicReference<>();

        // Act
        frontend.submitJob(request, new StreamObserver<JobStatusResponse>() {
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
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "Call should complete within timeout");
        assertNotNull(responseRef.get());
        assertEquals("job-123", responseRef.get().getJobId());
        // Frontend logic picks index 0 (Backend-1) first
        assertTrue(responseRef.get().getMessage().contains("Backend-1"));
    }

    @Test
    void testGetStatusAggregatesResponses() throws InterruptedException {
        // Arrange
        ServerStatusRequest request = ServerStatusRequest.newBuilder().build();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ServerStatusResponse> responseRef = new AtomicReference<>();

        // Act
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
        assertTrue(completed, "Call should complete within timeout");
        assertNotNull(responseRef.get(), "Response should not be null");
        
        // Should contain info from both Backend-1 and Backend-2
        assertEquals(2, responseRef.get().getServersList().size());
        
        List<String> hostNames = new ArrayList<>();
        responseRef.get().getServersList().forEach(s -> hostNames.add(s.getHostName()));
        
        assertTrue(hostNames.contains("Backend-1"));
        assertTrue(hostNames.contains("Backend-2"));
    }

    @Test
    void testSubmitJobNoBackendsAvailableReturnsError() throws InterruptedException {
        // Arrange: Create a frontend with NO channels
        FrontEndImpl emptyFrontend = new FrontEndImpl(new ArrayList<>());
        JobRequest request = JobRequest.newBuilder().setJobId("job-empty").build();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        // Act
        emptyFrontend.submitJob(request, new StreamObserver<JobStatusResponse>() {
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
        latch.await(2, TimeUnit.SECONDS);
        assertNotNull(errorRef.get());
        assertTrue(errorRef.get().getMessage().contains("No backend servers available"));
    }
}
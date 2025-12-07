package personal.cluster_management.frontend;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import personal.cluster_management.proto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the Frontend Service.
 * Acts as a middle-man/load-balancer between Client and Backend Servers.
 *
 * ASSUMPTION: The DistributedJob.proto defines a service named 'DistributedJobService'.
 */
public class FrontEndImpl extends DistributedJobServiceGrpc.DistributedJobServiceImplBase {

    private final List<ManagedChannel> backendChannels;

    public FrontEndImpl(String[] backendAddresses) {
        this.backendChannels = new ArrayList<>();
        for (String address : backendAddresses) {
            backendChannels.add(ManagedChannelBuilder.forTarget(address)
                    .usePlaintext()
                    .build());
        }
    }

    @Override
    public void submitJob(JobRequest request, StreamObserver<JobStatusResponse> responseObserver) {
        System.out.println("Frontend: Received Job Request " + request.getJobId());

        // Strategy: Simple Round Robin or Random pick could be used here.
        // For demonstration, we pick the first backend server available.
        if (backendChannels.isEmpty()) {
            responseObserver.onError(new RuntimeException("No backend servers available"));
            return;
        }

        ManagedChannel channel = backendChannels.get(0); // Pick first for now
        DistributedJobServiceGrpc.DistributedJobServiceStub stub = DistributedJobServiceGrpc.newStub(channel);

        // Forward the request to the backend
        stub.submitJob(request, new StreamObserver<JobStatusResponse>() {
            @Override
            public void onNext(JobStatusResponse value) {
                responseObserver.onNext(value);
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        });
    }

    @Override
    public void getStatus(ServerStatusRequest request, StreamObserver<ServerStatusResponse> responseObserver) {
        System.out.println("Frontend: Received Status Request. Broadcasting to " + backendChannels.size() + " backends.");

        ResponseCollector<ServerStatusResponse> collector = new ResponseCollector<>();
        CountDownLatch latch = new CountDownLatch(backendChannels.size());

        // Broadcast request to ALL backends
        for (ManagedChannel channel : backendChannels) {
            DistributedJobServiceGrpc.DistributedJobServiceStub stub = DistributedJobServiceGrpc.newStub(channel);
            
            // Use our custom Observer to collect responses
            stub.getStatus(request, new FrontEndObserver<>(collector, latch));
        }

        try {
            // Wait for all backends to reply (or timeout after 5 seconds)
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            if (!completed) {
                System.err.println("Frontend: Timed out waiting for some backend statuses.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseObserver.onError(e);
            return;
        }

        // Aggregate results
        ServerStatusResponse.Builder aggregatedResponse = ServerStatusResponse.newBuilder();
        for (ServerStatusResponse response : collector.getResponses()) {
            aggregatedResponse.addAllServers(response.getServersList());
        }

        responseObserver.onNext(aggregatedResponse.build());
        responseObserver.onCompleted();
    }
    
    // Helper for tmux creation
    @Override
    public void createTmuxSession(TmuxCreateRequest request, StreamObserver<JobStatusResponse> responseObserver) {
        // Similar forwarding logic as submitJob
        if (backendChannels.isEmpty()) {
            responseObserver.onError(new RuntimeException("No backend servers available"));
            return;
        }
        
        // Forwarding to first backend as an example
        DistributedJobServiceGrpc.DistributedJobServiceStub stub = DistributedJobServiceGrpc.newStub(backendChannels.get(0));
        stub.createTmuxSession(request, new StreamObserver<JobStatusResponse>() {
            @Override
            public void onNext(JobStatusResponse value) {
                responseObserver.onNext(value);
            }
            @Override
            public void onError(Throwable t) { responseObserver.onError(t); }
            @Override
            public void onCompleted() { responseObserver.onCompleted(); }
        });
    }
}
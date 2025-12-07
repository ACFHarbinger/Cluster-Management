package personal.cluster_management.frontend;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class FrontEndMain {

    private static final int PORT = 9090;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Starting Frontend Server on port " + PORT + "...");

        // In a real scenario, these addresses might come from a config file or service discovery
        String[] backendServers = {
            "localhost:50051",
            "localhost:50052"
        };

        Server server = ServerBuilder.forPort(PORT)
                .intercept(new HeaderFrontEndInterceptor())
                .addService(new FrontEndImpl(backendServers))
                .build();

        server.start();
        System.out.println("Frontend Server started successfully.");
        
        server.awaitTermination();
    }
}
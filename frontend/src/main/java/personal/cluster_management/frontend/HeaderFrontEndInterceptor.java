package personal.cluster_management.frontend;

import io.grpc.*;

/**
 * Interceptor to log incoming requests or handle metadata headers.
 */
public class HeaderFrontEndInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        System.out.println("[Frontend Interceptor] Received call for: " + methodName);

        // You can inspect headers here if needed (e.g., auth tokens)
        // Metadata.Key<String> authKey = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        // String token = headers.get(authKey);

        return next.startCall(call, headers);
    }
}
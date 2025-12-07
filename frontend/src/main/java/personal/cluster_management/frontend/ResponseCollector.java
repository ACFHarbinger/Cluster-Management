package personal.cluster_management.frontend;

import java.util.ArrayList;
import java.util.List;

/**
 * A synchronized helper class to collect responses from multiple asynchronous backend calls.
 * @param <T> The type of response object (e.g., JobStatusResponse).
 */
public class ResponseCollector<T> {
    private final List<T> responses = new ArrayList<>();

    public synchronized void addResponse(T response) {
        responses.add(response);
    }

    public synchronized List<T> getResponses() {
        return new ArrayList<>(responses);
    }
}
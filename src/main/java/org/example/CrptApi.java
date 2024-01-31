package org.example;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Semaphore requestSemaphore;
    private final HttpClient httpClient;
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestSemaphore = new Semaphore(requestLimit);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::resetSemaphore, 0, timeUnit.toSeconds(1), TimeUnit.SECONDS);
    }

    private void resetSemaphore() {
        requestSemaphore.release(requestSemaphore.availablePermits());
    }

    public void createDocument(String document, String signature)
    {
        try
        {
            if (requestSemaphore.tryAcquire())
            {
                sendPostRequest(document, signature);
            } else {
                System.out.println("Request limit exceeded. Try again later.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void sendPostRequest(String document, String signature) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(document, signature)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Response Code: " + response.statusCode());
        System.out.println("Response Body: " + response.body());
    }

    private String buildRequestBody(String document, String signature) throws IOException
    {
        return "{\"document\":" + document + ", \"signature\":\"" + signature + "\"}";
    }

    public static void main(String[] args)
    {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);
        String document = "{\"description\":{\"participantInn\":\"1234567890\"},\"doc_id\":\"123\",\"doc_status\":\"draft\"," +
                "\"doc_type\":\"LP_INTRODUCE_GOODS\",\"importRequest\":true,\"owner_inn\":\"1234567890\",\"participant_inn\":\"1234567890\"," +
                "\"producer_inn\":\"1234567890\",\"production_date\":\"2020-01-23\",\"production_type\":\"type\"," +
                "\"products\":[{\"certificate_document\":\"cert\",\"certificate_document_date\":\"2020-01-23\",\"certificate_document_number\":\"cert123\"," +
                "\"owner_inn\":\"1234567890\",\"producer_inn\":\"1234567890\",\"production_date\":\"2020-01-23\",\"tnved_code\":\"code\"," +
                "\"uit_code\":\"code\",\"uitu_code\":\"code\"}],\"reg_date\":\"2020-01-23\",\"reg_number\":\"number\"}";

        String signature = "sampleSignature";

        crptApi.createDocument(document, signature);
    }
}

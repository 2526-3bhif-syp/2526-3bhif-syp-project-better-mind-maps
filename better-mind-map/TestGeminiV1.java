import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestGeminiV1 {
    public static void main(String[] args) throws Exception {
        String[] models = {"gemini-1.5-flash", "gemini-1.5-pro", "gemini-1.0-pro"};
        String apiKey = System.getenv("MINDMAP_AI_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("No API key");
            return;
        }
        
        HttpClient client = HttpClient.newHttpClient();
        String jsonPayload = "{\"contents\": [{\"parts\": [{\"text\": \"hello\"}]}]}";

        for (String model : models) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1/models/" + model + ":generateContent?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(model + " (v1) -> " + response.statusCode());
            if(response.statusCode() != 200) {
                 System.out.println(response.body());
            }
        }
    }
}

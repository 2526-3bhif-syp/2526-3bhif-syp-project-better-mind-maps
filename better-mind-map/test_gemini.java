import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class test_gemini {
    public static void main(String[] args) throws Exception {
        String apiKey = "AIzaSyANWxRFMXA1OF6BGD_rhzqE4qjXimTpnyE";
        String aiPrompt = "Du bist ein Mindmap-Experte. Erstelle zum Thema 'Fussball' eine extrem detaillierte und logisch strukturierte Mindmap mit den wichtigsten Begriffen.";
        String jsonPayload = "{\"contents\": [{\"parts\": [{\"text\": \"" + aiPrompt.replace("\"", "\\\"") + "\"}]}]}";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.statusCode());
        System.out.println(response.body().substring(0, Math.min(response.body().length(), 100)));
    }
}

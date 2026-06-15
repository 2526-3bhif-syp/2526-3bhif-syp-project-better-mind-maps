import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestGemini {
    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("MINDMAP_AI_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("No API key");
            return;
        }
        
        String aiPrompt = "Du bist ein Mindmap-Experte. Das ist die AKTUELLE Mindmap (in Textform):\nAI: test\nInfo\n- Beispiel\n\nÄndere diese Mindmap basierend auf dem neuen Nutzer-Wunsch.\nNeuer Nutzer-Wunsch: 'mehr'\nWICHTIG: Antworte AUSSCHLIESSLICH mit der KOMPLETTEN neuen/aktualisierten Mindmap im folgenden Format, OHNE Markdown, OHNE Text davor oder danach. Zeile 1 MUSS das Hauptthema sein.\nZeile 2 und weiter für Kategorien und Unterkategorien:\nKategorie 1\n- Unterkategorie 1.1\n- Unterkategorie 1.2\nKategorie 2\n- Unterkategorie 2.1";
        
        String safePrompt = aiPrompt.replace("\"", "'").replace("\n", "\\n").replace("\r", "");
        String jsonPayload = "{\"contents\": [{\"parts\": [{\"text\": \"" + safePrompt + "\"}]}]}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }
}

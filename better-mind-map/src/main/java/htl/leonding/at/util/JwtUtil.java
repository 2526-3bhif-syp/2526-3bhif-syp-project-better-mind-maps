package htl.leonding.at.util;
import htl.leonding.at.model.*;
import htl.leonding.at.controller.*;
import htl.leonding.at.service.*;
import htl.leonding.at.repository.*;
import htl.leonding.at.util.*;
import htl.leonding.at.App;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class JwtUtil {

    private static final String SECRET = "better-mind-maps-super-secret-key-12345";
    private static final Gson gson = new Gson();

    private static String base64UrlEncode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64UrlDecode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public static String generateToken(String userId, String username) {
        // Create Header
        JsonObject header = new JsonObject();
        header.addProperty("alg", "HS256");
        header.addProperty("typ", "JWT");
        String encodedHeader = base64UrlEncode(gson.toJson(header));

        // Create Payload
        JsonObject payload = new JsonObject();
        payload.addProperty("userId", userId);
        payload.addProperty("username", username);
        payload.addProperty("exp", System.currentTimeMillis() + (1000 * 60 * 60 * 24)); // 24 hours expiry
        String encodedPayload = base64UrlEncode(gson.toJson(payload));

        // Create Signature
        String data = encodedHeader + "." + encodedPayload;
        String signature = sign(data, SECRET);

        return data + "." + signature;
    }

    public static boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            String encodedHeader = parts[0];
            String encodedPayload = parts[1];
            String signature = parts[2];

            String expectedSignature = sign(encodedHeader + "." + encodedPayload, SECRET);
            if (!expectedSignature.equals(signature)) {
                return false;
            }

            // Check Expiry
            String payloadJson = base64UrlDecode(encodedPayload);
            JsonObject payload = gson.fromJson(payloadJson, JsonObject.class);
            if (payload.has("exp")) {
                long exp = payload.get("exp").getAsLong();
                return System.currentTimeMillis() < exp;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String payloadJson = base64UrlDecode(parts[1]);
            JsonObject payload = gson.fromJson(payloadJson, JsonObject.class);
            return payload.get("userId").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getUsernameFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String payloadJson = base64UrlDecode(parts[1]);
            JsonObject payload = gson.fromJson(payloadJson, JsonObject.class);
            return payload.get("username").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error signing JWT", e);
        }
    }
}

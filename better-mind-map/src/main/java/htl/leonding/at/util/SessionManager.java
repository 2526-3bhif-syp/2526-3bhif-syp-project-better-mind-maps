package htl.leonding.at;

public class SessionManager {

    private static String token;
    private static User currentUser;

    public static class User {
        private final String id;
        private final String username;

        public User(String id, String username) {
            this.id = id;
            this.username = username;
        }

        public String getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }
    }

    public static void login(String jwtToken) {
        if (JwtUtil.validateToken(jwtToken)) {
            token = jwtToken;
            String userId = JwtUtil.getUserIdFromToken(jwtToken);
            String username = JwtUtil.getUsernameFromToken(jwtToken);
            currentUser = new User(userId, username);
        } else {
            throw new IllegalArgumentException("Invalid JWT token");
        }
    }

    public static void logout() {
        token = null;
        currentUser = null;
    }

    public static String getToken() {
        return token;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}

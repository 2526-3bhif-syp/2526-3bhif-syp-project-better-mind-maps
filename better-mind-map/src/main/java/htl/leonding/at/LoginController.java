package htl.leonding.at;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            messageLabel.setText("Please enter username and password.");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        String sql = "SELECT id, password_hash, salt FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String userId = rs.getString("id");
                    String hash = rs.getString("password_hash");
                    String salt = rs.getString("salt");

                    if (PasswordHasher.verifyPassword(password, hash, salt)) {
                        String token = JwtUtil.generateToken(userId, username.trim());
                        SessionManager.login(token);
                        
                        // Load Overview View
                        loadOverview();
                    } else {
                        messageLabel.setText("Invalid username or password.");
                        messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                    }
                } else {
                    messageLabel.setText("User does not exist.");
                    messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                }
            }
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    private void onRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            messageLabel.setText("Please enter username and password.");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        username = username.trim();
        if (username.length() < 3 || password.length() < 4) {
            messageLabel.setText("Username >= 3 and Password >= 4 chars.");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        // Check if username exists
        String checkSql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    messageLabel.setText("Username already exists.");
                    messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                    return;
                }
            }
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        // Insert new user
        String insertSql = "INSERT INTO users (id, username, password_hash, salt) VALUES (?, ?, ?, ?)";
        String userId = UUID.randomUUID().toString();
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(password, salt);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setString(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, hash);
            stmt.setString(4, salt);
            stmt.executeUpdate();

            messageLabel.setText("Registration successful! Please login.");
            messageLabel.setStyle("-fx-text-fill: #2ecc71;");
            passwordField.clear();
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    private void loadOverview() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("overview-view.fxml"));
            Scene scene = new Scene(loader.load(), 1024, 768);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (IOException e) {
            messageLabel.setText("Failed to load overview: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }
}

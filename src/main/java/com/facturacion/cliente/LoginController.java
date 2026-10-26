package com.facturacion.cliente;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button loginButton;

    private final ApiClient api = new ApiClient();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Enter username and password");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setText("");

        Task<ApiClient.LoginResult> task = new Task<>() {
            @Override
            protected ApiClient.LoginResult call() {
                return api.login(username, password);
            }
        };

        task.setOnSucceeded(event -> {
            ApiClient.LoginResult result = task.getValue();
            Session.start(result.token(), result.username());
            Navigator.show("clients.fxml", 800, 500);
        });

        task.setOnFailed(event -> {
            loginButton.setDisable(false);
            errorLabel.setText(task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
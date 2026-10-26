package com.facturacion.cliente;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField companyNameField;
    @FXML
    private TextField taxIdField;
    @FXML
    private TextField addressField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button registerButton;

    private final ApiClient api = new ApiClient();

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String companyName = companyNameField.getText().trim();
        String taxId = taxIdField.getText().trim();
        String address = addressField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || companyName.isEmpty() || taxId.isEmpty()) {
            errorLabel.setText("Username, password, company name and tax ID are required");
            return;
        }

        registerButton.setDisable(true);
        errorLabel.setText("");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                api.register(username, password, companyName, taxId, address);
                return null;
            }
        };

        task.setOnSucceeded(event -> Navigator.show("login.fxml", 400, 300));

        task.setOnFailed(event -> {
            registerButton.setDisable(false);
            errorLabel.setText(task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleBack() {
        Navigator.show("login.fxml", 400, 300);
    }
}
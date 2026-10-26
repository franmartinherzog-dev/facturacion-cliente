package com.facturacion.cliente;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ClientFormController {

    @FXML
    private Label formTitle;
    @FXML
    private TextField nameField;
    @FXML
    private TextField taxIdField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveButton;

    private final ApiClient api = new ApiClient();
    private ApiClient.ClientData editing;
    private boolean saved;

    public void setClient(ApiClient.ClientData client) {
        this.editing = client;
        if (client == null) {
            formTitle.setText("New client");
            return;
        }
        formTitle.setText("Edit client");
        nameField.setText(client.name());
        taxIdField.setText(client.taxId());
        addressField.setText(client.address());
        emailField.setText(client.email());
        phoneField.setText(client.phone());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        String taxId = taxIdField.getText().trim();
        String address = addressField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (name.isEmpty() || taxId.isEmpty()) {
            errorLabel.setText("Name and tax ID are required");
            return;
        }

        saveButton.setDisable(true);
        errorLabel.setText("");
        ApiClient.ClientData target = editing;

        Task<ApiClient.ClientData> task = new Task<>() {
            @Override
            protected ApiClient.ClientData call() {
                if (target == null) {
                    return api.createClient(name, taxId, address, email, phone);
                }
                return api.updateClient(target.id(), name, taxId, address, email, phone);
            }
        };

        task.setOnSucceeded(event -> {
            saved = true;
            close();
        });

        task.setOnFailed(event -> {
            saveButton.setDisable(false);
            errorLabel.setText(task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
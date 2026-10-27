package com.facturacion.cliente;

import java.io.IOException;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ClientsController {

    @FXML
    private Label userLabel;
    @FXML
    private TextField searchField;
    @FXML
    private TableView<ApiClient.ClientData> clientsTable;
    @FXML
    private TableColumn<ApiClient.ClientData, String> nameColumn;
    @FXML
    private TableColumn<ApiClient.ClientData, String> taxIdColumn;
    @FXML
    private TableColumn<ApiClient.ClientData, String> emailColumn;
    @FXML
    private TableColumn<ApiClient.ClientData, String> phoneColumn;
    @FXML
    private Label statusLabel;

    private final ApiClient api = new ApiClient();

    @FXML
    private void initialize() {
        userLabel.setText("Sesión iniciada como " + Session.getUsername());
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().name()));
        taxIdColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().taxId()));
        emailColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().email()));
        phoneColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().phone()));
        loadClients();
    }

    @FXML
    private void handleSearch() {
        loadClients();
    }

    @FXML
    private void handleLogout() {
        Session.end();
        Navigator.show("login.fxml", 400, 300);
    }

    @FXML
    private void handleInvoices() {
        Navigator.show("invoice-history.fxml", 800, 500);
    }

    @FXML
    private void handleNew() {
        openForm(null);
    }

    @FXML
    private void handleEdit() {
        ApiClient.ClientData selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Selecciona primero un cliente");
            return;
        }
        openForm(selected);
    }

    @FXML
    private void handleDelete() {
        ApiClient.ClientData selected = clientsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Selecciona primero un cliente");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar al cliente " + selected.name() + "?", ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        alert.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .ifPresent(button -> deleteClient(selected));
    }

    private void openForm(ApiClient.ClientData client) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("client-form.fxml"));
            Scene scene = new Scene(loader.load(), 400, 450);
            ClientFormController form = loader.getController();
            form.setClient(client);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(clientsTable.getScene().getWindow());
            dialog.setTitle(client == null ? "Nuevo cliente" : "Editar cliente");
            dialog.setScene(scene);
            dialog.showAndWait();

            if (form.isSaved()) {
                loadClients();
            }
        } catch (IOException e) {
            statusLabel.setText("No se puede abrir el formulario de cliente");
        }
    }

    private void deleteClient(ApiClient.ClientData client) {
        statusLabel.setText("");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                api.deleteClient(client.id());
                return null;
            }
        };

        task.setOnSucceeded(event -> loadClients());
        task.setOnFailed(event -> handleError(task.getException()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadClients() {
        String search = searchField.getText();
        statusLabel.setText("");

        Task<List<ApiClient.ClientData>> task = new Task<>() {
            @Override
            protected List<ApiClient.ClientData> call() {
                return api.getClients(search);
            }
        };

        task.setOnSucceeded(event -> clientsTable.getItems().setAll(task.getValue()));
        task.setOnFailed(event -> handleError(task.getException()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void handleError(Throwable error) {
        if (error instanceof ApiException apiError && apiError.getStatus() == 401) {
            handleLogout();
            return;
        }
        statusLabel.setText(error.getMessage());
    }
}
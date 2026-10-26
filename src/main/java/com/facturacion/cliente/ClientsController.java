package com.facturacion.cliente;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

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
        userLabel.setText("Signed in as " + Session.getUsername());
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
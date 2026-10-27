package com.facturacion.cliente;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class InvoiceHistoryController {

    @FXML
    private TableView<ApiClient.InvoiceData> invoicesTable;
    @FXML
    private TableColumn<ApiClient.InvoiceData, String> numberColumn;
    @FXML
    private TableColumn<ApiClient.InvoiceData, String> dateColumn;
    @FXML
    private TableColumn<ApiClient.InvoiceData, String> clientColumn;
    @FXML
    private TableColumn<ApiClient.InvoiceData, String> totalColumn;
    @FXML
    private Label statusLabel;

    private final ApiClient api = new ApiClient();

    @FXML
    private void initialize() {
        numberColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().number()));
        dateColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(Formats.date(cell.getValue().issueDate())));
        clientColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().clientName()));
        totalColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(Formats.money(cell.getValue().total())));
        totalColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        invoicesTable.setPlaceholder(new Label("Todavía no hay facturas"));
        loadInvoices();
    }

    @FXML
    private void handleBack() {
        Navigator.show("clients.fxml", 800, 500);
    }

    @FXML
    private void handleNew() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("invoice-form.fxml"));
            Scene scene = new Scene(loader.load(), 720, 640);
            InvoiceFormController form = loader.getController();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(invoicesTable.getScene().getWindow());
            dialog.setTitle("Nueva factura");
            dialog.setScene(scene);
            dialog.showAndWait();

            ApiClient.InvoiceData created = form.getCreated();
            if (created != null) {
                loadInvoices();
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Factura " + created.number() + " creada. ¿Quieres guardarla en PDF ahora?",
                        ButtonType.YES, ButtonType.NO);
                alert.setHeaderText(null);
                alert.showAndWait()
                        .filter(button -> button == ButtonType.YES)
                        .ifPresent(button -> savePdf(created));
            }
        } catch (IOException e) {
            statusLabel.setText("No se puede abrir el formulario de factura");
        }
    }

    @FXML
    private void handleDownload() {
        ApiClient.InvoiceData selected = invoicesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Selecciona primero una factura");
            return;
        }
        savePdf(selected);
    }

    private void savePdf(ApiClient.InvoiceData invoice) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar factura en PDF");
        chooser.setInitialFileName("factura-" + invoice.number() + ".pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        File file = chooser.showSaveDialog(invoicesTable.getScene().getWindow());
        if (file == null) {
            return; // the user cancelled: nothing is requested
        }
        statusLabel.setText("");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                byte[] pdf = api.downloadInvoicePdf(invoice.id());
                try {
                    Files.write(file.toPath(), pdf);
                } catch (IOException e) {
                    throw new IllegalStateException("No se pudo guardar el archivo", e);
                }
                return null;
            }
        };

        task.setOnSucceeded(event -> statusLabel.setText("PDF guardado en " + file.getAbsolutePath()));
        task.setOnFailed(event -> handleError(task.getException()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadInvoices() {
        statusLabel.setText("");

        Task<List<ApiClient.InvoiceData>> task = new Task<>() {
            @Override
            protected List<ApiClient.InvoiceData> call() {
                return api.getInvoices();
            }
        };

        task.setOnSucceeded(event -> invoicesTable.getItems().setAll(task.getValue()));
        task.setOnFailed(event -> handleError(task.getException()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void handleError(Throwable error) {
        if (error instanceof ApiException apiError && apiError.getStatus() == 401) {
            Session.end();
            Navigator.show("login.fxml", 400, 300);
            return;
        }
        statusLabel.setText(error.getMessage());
    }
}
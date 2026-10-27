package com.facturacion.cliente;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class InvoiceFormController {

    @FXML
    private ComboBox<ApiClient.ClientData> clientCombo;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<String> vatCombo;
    @FXML
    private ComboBox<String> irpfCombo;
    @FXML
    private VBox linesBox;
    @FXML
    private Label baseLabel;
    @FXML
    private Label vatLabel;
    @FXML
    private Label irpfLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button saveButton;

    private final ApiClient api = new ApiClient();
    private final List<LineRow> rows = new ArrayList<>();
    private ApiClient.InvoiceData created;

    // One editable line of the invoice
    private class LineRow {
        final TextField description = new TextField();
        final TextField quantity = new TextField("1");
        final TextField unitPrice = new TextField();
        final Label subtotal = new Label();
        final HBox box = new HBox(8);

        LineRow() {
            description.setPromptText("Descripción");
            HBox.setHgrow(description, Priority.ALWAYS);
            quantity.setPrefWidth(80);
            unitPrice.setPrefWidth(90);
            subtotal.setPrefWidth(100);
            subtotal.setAlignment(Pos.CENTER_RIGHT);
            Button remove = new Button("Quitar");
            remove.setPrefWidth(60);
            remove.setOnAction(event -> removeRow(this));

            quantity.textProperty().addListener((obs, oldValue, newValue) -> updateTotals());
            unitPrice.textProperty().addListener((obs, oldValue, newValue) -> updateTotals());

            box.getChildren().addAll(description, quantity, unitPrice, subtotal, remove);
        }

        // A row without description and price is ignored when saving
        boolean isEmpty() {
            return description.getText().isBlank() && unitPrice.getText().isBlank();
        }
    }

    @FXML
    private void initialize() {
        clientCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(ApiClient.ClientData client) {
                return client == null ? "" : client.name() + " (" + client.taxId() + ")";
            }

            @Override
            public ApiClient.ClientData fromString(String text) {
                return null;
            }
        });

        vatCombo.getItems().addAll("21", "10", "4", "0");
        vatCombo.setValue("21");
        irpfCombo.getItems().addAll("15", "7", "0");
        irpfCombo.setValue("15");
        datePicker.setValue(LocalDate.now());

        vatCombo.getEditor().textProperty().addListener((obs, oldValue, newValue) -> updateTotals());
        irpfCombo.getEditor().textProperty().addListener((obs, oldValue, newValue) -> updateTotals());

        addRow();
        updateTotals();
        loadClients();
    }

    public ApiClient.InvoiceData getCreated() {
        return created;
    }

    @FXML
    private void handleAddLine() {
        addRow();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    @FXML
    private void handleSave() {
        statusLabel.setText("");

        ApiClient.ClientData client = clientCombo.getValue();
        if (client == null) {
            statusLabel.setText("Selecciona un cliente");
            return;
        }
        LocalDate date = datePicker.getValue();
        if (date == null) {
            statusLabel.setText("Indica la fecha de la factura");
            return;
        }
        BigDecimal vatRate = parse(vatCombo.getEditor().getText());
        BigDecimal irpfRate = parse(irpfCombo.getEditor().getText());
        if (vatRate == null || irpfRate == null) {
            statusLabel.setText("Revisa los tipos de IVA e IRPF");
            return;
        }

        List<ApiClient.InvoiceLineInput> lines = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            LineRow row = rows.get(i);
            if (row.isEmpty()) {
                continue;
            }
            BigDecimal quantity = parse(row.quantity.getText());
            BigDecimal unitPrice = parse(row.unitPrice.getText());
            if (row.description.getText().isBlank()
                    || quantity == null || quantity.signum() <= 0
                    || unitPrice == null || unitPrice.signum() < 0) {
                statusLabel.setText("Revisa la línea " + (i + 1)
                        + ": descripción, cantidad mayor que 0 y precio");
                return;
            }
            lines.add(new ApiClient.InvoiceLineInput(
                    row.description.getText().trim(), quantity, unitPrice));
        }
        if (lines.isEmpty()) {
            statusLabel.setText("Añade al menos una línea");
            return;
        }

        saveButton.setDisable(true);

        Task<ApiClient.InvoiceData> task = new Task<>() {
            @Override
            protected ApiClient.InvoiceData call() {
                return api.createInvoice(client.id(), date, vatRate, irpfRate, lines);
            }
        };

        task.setOnSucceeded(event -> {
            created = task.getValue();
            close();
        });
        task.setOnFailed(event -> {
            saveButton.setDisable(false);
            statusLabel.setText(task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadClients() {
        Task<List<ApiClient.ClientData>> task = new Task<>() {
            @Override
            protected List<ApiClient.ClientData> call() {
                return api.getClients(null);
            }
        };

        task.setOnSucceeded(event -> {
            clientCombo.getItems().setAll(task.getValue());
            if (task.getValue().isEmpty()) {
                statusLabel.setText("Antes de facturar hay que crear al menos un cliente");
            }
        });
        task.setOnFailed(event -> statusLabel.setText(task.getException().getMessage()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void addRow() {
        LineRow row = new LineRow();
        rows.add(row);
        linesBox.getChildren().add(row.box);
    }

    private void removeRow(LineRow row) {
        if (rows.size() == 1) {
            // Always keep one row: just clear it
            row.description.clear();
            row.quantity.setText("1");
            row.unitPrice.clear();
            return;
        }
        rows.remove(row);
        linesBox.getChildren().remove(row.box);
        updateTotals();
    }

    // Preview only: the final amounts are always calculated by the API
    private void updateTotals() {
        List<ApiClient.InvoiceLineInput> lines = new ArrayList<>();
        for (LineRow row : rows) {
            BigDecimal quantity = parse(row.quantity.getText());
            BigDecimal unitPrice = parse(row.unitPrice.getText());
            if (quantity != null && unitPrice != null) {
                lines.add(new ApiClient.InvoiceLineInput(row.description.getText(), quantity, unitPrice));
                row.subtotal.setText(Formats.money(
                        quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)));
            } else {
                row.subtotal.setText("");
            }
        }

        BigDecimal vatRate = parse(vatCombo.getEditor().getText());
        BigDecimal irpfRate = parse(irpfCombo.getEditor().getText());
        InvoiceCalculator.Totals totals = InvoiceCalculator.calculate(lines,
                vatRate == null ? BigDecimal.ZERO : vatRate,
                irpfRate == null ? BigDecimal.ZERO : irpfRate);

        baseLabel.setText(Formats.money(totals.taxableBase()));
        vatLabel.setText(Formats.money(totals.vatAmount()));
        irpfLabel.setText(Formats.money(totals.irpfAmount().negate()));
        totalLabel.setText(Formats.money(totals.total()));
    }

    // Accepts "12.5" and "12,5"; returns null when empty or not a number
    private static BigDecimal parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
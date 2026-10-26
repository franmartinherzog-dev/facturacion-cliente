package com.facturacion.cliente;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Facturación");
        Navigator.init(stage);
        Navigator.show("login.fxml", 400, 300);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
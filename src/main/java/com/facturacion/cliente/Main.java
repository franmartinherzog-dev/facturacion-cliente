package com.facturacion.cliente;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("login.fxml"));
        stage.setTitle("Facturación");
        stage.setScene(new Scene(loader.load(), 400, 300));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
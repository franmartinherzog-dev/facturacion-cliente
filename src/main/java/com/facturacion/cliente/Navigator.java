package com.facturacion.cliente;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class Navigator {

    private static Stage stage;

    private Navigator() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static void show(String fxmlName, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(fxmlName));
            stage.setScene(new Scene(loader.load(), width, height));
            stage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load screen " + fxmlName, e);
        }
    }
}
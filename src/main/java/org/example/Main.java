package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        Pane root = new Pane();

        Scene scene = new Scene(root, 800, 600);

        new GameLoop(root, scene);

        stage.setTitle("projeto-omega");
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }

}
package org.example;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import org.example.render.Renderer;

public final class Main extends Application {
  private GameLoop game;

  @Override
  public void start(Stage stage) {
    Canvas canvas = new Canvas(Renderer.WIDTH, Renderer.HEIGHT);
    Group scaled = new Group(canvas);
    Pane root = new Pane(scaled);
    root.setStyle("-fx-background-color: #050d14;");
    Scene scene = new Scene(root, Renderer.WIDTH, Renderer.HEIGHT, Color.web("#050d14"));
    Runnable resize =
        () -> {
          double scale =
              Math.min(scene.getWidth() / Renderer.WIDTH, scene.getHeight() / Renderer.HEIGHT);
          // Affine scale avoids Group's center-based scale pivot during resize.
          scaled.getTransforms().setAll(new javafx.scene.transform.Scale(scale, scale));
          scaled.setLayoutX((scene.getWidth() - Renderer.WIDTH * scale) / 2);
          scaled.setLayoutY((scene.getHeight() - Renderer.HEIGHT * scale) / 2);
        };
    scene.widthProperty().addListener((o, a, b) -> resize.run());
    scene.heightProperty().addListener((o, a, b) -> resize.run());
    stage.setTitle("Projeto Ômega · Protocolo de evacuação");
    stage.setScene(scene);
    stage.setMinWidth(960);
    stage.setMinHeight(650);
    stage.setFullScreenExitHint("");
    stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
    game = new GameLoop(canvas, scene, stage);
    stage.setOnCloseRequest(e -> game.close());
    stage.show();
    resize.run();
    if (getParameters().getRaw().contains("--smoke")) smoke(canvas);
  }

  /** Explicit local smoke mode captures only our own canvas and then exits. */
  private void smoke(Canvas canvas) {
    PauseTransition menuShot = new PauseTransition(Duration.seconds(1));
    menuShot.setOnFinished(
        e -> {
          try {
            save(canvas, "menu.png");
            game.startMission();
            game.draw();
          } catch (Exception failure) {
            throw new RuntimeException(failure);
          }
        });
    menuShot.play();
    PauseTransition gameShot = new PauseTransition(Duration.seconds(2));
    gameShot.setOnFinished(
        e -> {
          try {
            save(canvas, "game.png");
            game.session()
                .update(
                    .04,
                    new org.example.game.GameSession.Input(0, 0, false, false, false, false, true));
            for (int i = 0; i < 6; i++)
              game.session().update(.04, org.example.game.GameSession.Input.idle());
            game.draw();
            save(canvas, "effects.png");
            game.session().showMap = true;
            game.draw();
            save(canvas, "map.png");
            System.out.println("OMEGA_SMOKE_OK: target/screenshots/{menu,game}.png");
          } catch (Exception failure) {
            throw new RuntimeException(failure);
          } finally {
            game.close();
            Platform.exit();
          }
        });
    gameShot.play();
  }

  private static void save(Canvas canvas, String name) throws Exception {
    WritableImage shot = canvas.snapshot(null, null);
    BufferedImage png =
        new BufferedImage(
            (int) shot.getWidth(), (int) shot.getHeight(), BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < png.getHeight(); y++)
      for (int x = 0; x < png.getWidth(); x++)
        png.setRGB(x, y, shot.getPixelReader().getArgb(x, y));
    Path path = Path.of("target/screenshots", name);
    Files.createDirectories(path.getParent());
    ImageIO.write(png, "png", path.toFile());
  }

  @Override
  public void stop() {
    if (game != null) game.close();
  }

  public static void main(String[] args) {
    launch(args);
  }
}

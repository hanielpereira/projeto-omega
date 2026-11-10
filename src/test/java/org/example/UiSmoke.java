package org.example;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

/** Optional real JavaFX integration check. Requires a display; not part of headless mvn test. */
public final class UiSmoke {
  private static final CountDownLatch finished = new CountDownLatch(1);
  private static final AtomicReference<Throwable> failure = new AtomicReference<>();

  public static void main(String[] args) throws Exception {
    Platform.startup(UiSmoke::run);
    if (!finished.await(12, TimeUnit.SECONDS)) throw new AssertionError("UI smoke timed out");
    Platform.exit();
    if (failure.get() != null) throw new AssertionError("UI smoke failed", failure.get());
    System.out.println(
        "OMEGA_UI_OK: enter, held movement + G/Q/F, pause/resume, terminal hold, mouse menu,"
            + " resize");
  }

  private static void run() {
    Canvas canvas = new Canvas(1280, 800);
    Scene scene = new Scene(new Pane(canvas), 1280, 800);
    Stage stage = new Stage();
    stage.setScene(scene);
    GameLoop game = new GameLoop(canvas, scene, stage);
    stage.show();
    stage.requestFocus();
    double[] values = new double[2];
    Timeline checks = new Timeline();
    add(checks, 200, () -> tap(scene, KeyCode.ENTER), game, stage);
    add(
        checks,
        400,
        () -> {
          check(game.screen().equals("PLAY"), "Enter did not start mission");
          values[0] = game.session().player.y;
          press(scene, KeyCode.W);
        },
        game,
        stage);
    add(
        checks,
        650,
        () -> {
          tap(scene, KeyCode.G);
          tap(scene, KeyCode.Q);
          press(scene, KeyCode.F);
        },
        game,
        stage);
    add(
        checks,
        850,
        () -> {
          release(scene, KeyCode.W);
          release(scene, KeyCode.F);
          check(game.session().combat.ammo == 7, "F did not fire the sidearm");
          check(game.session().player.y < values[0] - 15, "movement was overwritten by action key");
          check(game.session().lures == 2, "Q did not throw lure");
          tap(scene, KeyCode.ESCAPE);
        },
        game,
        stage);
    add(
        checks,
        1050,
        () -> {
          check(game.screen().equals("PAUSED"), "Escape did not pause");
          values[1] = game.session().elapsed;
        },
        game,
        stage);
    add(
        checks,
        1250,
        () -> {
          check(game.session().elapsed == values[1], "simulation continued during pause");
          tap(scene, KeyCode.ESCAPE);
        },
        game,
        stage);
    add(
        checks,
        1450,
        () -> {
          check(game.screen().equals("PLAY"), "Escape did not resume");
          game.session().agents.clear();
          var d = game.session().devices.get(0);
          game.session().player.x = d.x();
          game.session().player.y = d.y();
          press(scene, KeyCode.E);
          stage.setWidth(1100);
          stage.setHeight(740);
        },
        game,
        stage);
    add(
        checks,
        3400,
        () -> {
          release(scene, KeyCode.E);
          check(game.session().cores == 1, "holding E did not activate terminal");
          game.menu();
          game.draw();
          Event.fireEvent(
              canvas,
              new javafx.scene.input.MouseEvent(
                  javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                  100,
                  560,
                  100,
                  560,
                  javafx.scene.input.MouseButton.PRIMARY,
                  1,
                  false,
                  false,
                  false,
                  false,
                  true,
                  false,
                  false,
                  true,
                  false,
                  false,
                  null));
          check(game.screen().equals("PLAY"), "mouse start button did not work");
          game.close();
          stage.close();
          finished.countDown();
        },
        game,
        stage);
    checks.play();
  }

  private static void add(
      Timeline timeline, int milliseconds, Runnable action, GameLoop game, Stage stage) {
    timeline
        .getKeyFrames()
        .add(
            new KeyFrame(
                Duration.millis(milliseconds),
                e -> {
                  if (failure.get() != null) return;
                  try {
                    action.run();
                  } catch (Throwable problem) {
                    failure.set(problem);
                    game.close();
                    stage.close();
                    finished.countDown();
                  }
                }));
  }

  private static void check(boolean passed, String message) {
    if (!passed) throw new AssertionError(message);
  }

  private static void tap(Scene scene, KeyCode code) {
    press(scene, code);
    release(scene, code);
  }

  private static void press(Scene scene, KeyCode code) {
    Event.fireEvent(
        scene, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
  }

  private static void release(Scene scene, KeyCode code) {
    Event.fireEvent(
        scene, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, false, false, false, false));
  }
}

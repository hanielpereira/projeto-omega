package org.example;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.example.audio.AudioEngine;
import org.example.game.GameSession;
import org.example.input.Gamepad;
import org.example.input.Keyboard;
import org.example.render.Renderer;

public final class GameLoop implements AutoCloseable {
  private final Renderer renderer;
  private final Keyboard keys;
  private final Gamepad gamepad = new Gamepad();
  private final AudioEngine audio = new AudioEngine();
  private final Stage stage;
  private final AnimationTimer timer;
  private GameSession session = new GameSession();
  private String screen = "MENU";
  private boolean debug, padAWasDown, mouseFiring;
  private long last;
  private double clock, mouseX = -1, mouseY = -1;

  public GameLoop(Canvas canvas, Scene scene, Stage stage) {
    this.stage = stage;
    renderer = new Renderer(canvas.getGraphicsContext2D());
    keys = new Keyboard(scene);
    canvas.setOnMouseMoved(
        e -> {
          mouseX = e.getX();
          mouseY = e.getY();
        });
    canvas.setOnMouseExited(
        e -> {
          mouseX = -1;
          mouseY = -1;
        });
    canvas.setOnScroll(
        e -> {
          if (screen.equals("PAUSED"))
            session.journalOffset =
                Math.max(
                    0,
                    Math.min(
                        Math.max(0, session.logs.size() - 2),
                        session.journalOffset + (e.getDeltaY() < 0 ? 1 : -1)));
        });
    canvas.setOnMousePressed(
        e -> {
          if (screen.equals("PLAY") && e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
            mouseFiring = true;
            mouseX = e.getX();
            mouseY = e.getY();
          }
        });
    canvas.setOnMouseReleased(e -> mouseFiring = false);
    canvas.setOnMouseClicked(e -> click(e.getX(), e.getY()));
    stage
        .focusedProperty()
        .addListener(
            (o, was, is) -> {
              if (!is) {
                keys.clear();
                mouseFiring = false;
                gamepad.clear();
                if (screen.equals("PLAY") && session.result == GameSession.Result.PLAYING)
                  screen = "PAUSED";
              }
            });
    timer =
        new AnimationTimer() {
          @Override
          public void handle(long now) {
            double dt = last == 0 ? 0 : Math.min(.04, (now - last) / 1_000_000_000.0);
            last = now;
            clock += dt;
            update(dt);
            draw();
            keys.endFrame();
          }
        };
    timer.start();
  }

  private void update(double dt) {
    Gamepad.Input pad = gamepad.poll(dt);
    session.controller = gamepad.remapPrompt().isEmpty() ? gamepad.label() : gamepad.remapPrompt();
    session.audioStatus = audio.status();
    session.musicVolume = audio.ambienceVolume();
    session.sfxVolume = audio.effectsVolume();
    audio.atmosphere(
        screen.equals("PLAY") ? (session.alert ? .85 : session.player.health < 35 ? .55 : 0) : 0,
        screen.equals("PLAY")
            ? Math.max(session.player.health < 25 ? .6 : 0, (65 - session.player.stamina) / 65)
            : 0);
    if (keys.tapped(KeyCode.F2)) {
      gamepad.beginRemap();
      screen = "PAUSED";
    }
    if (keys.tapped(KeyCode.TAB) || pad.map()) session.showMap = !session.showMap;
    if ((keys.tapped(KeyCode.R) || (pad.interact() && !padAWasDown && session.nearby == null))
        && screen.equals("PLAY")) session.toggleDoor();
    padAWasDown = pad.interact();
    if (screen.equals("PAUSED")) {
      if (pad.lure()) menu();
      if (keys.tapped(KeyCode.PAGE_DOWN))
        session.journalOffset =
            Math.min(Math.max(0, session.logs.size() - 2), session.journalOffset + 1);
      if (keys.tapped(KeyCode.PAGE_UP))
        session.journalOffset = Math.max(0, session.journalOffset - 1);
    }
    if (keys.tapped(KeyCode.M)) audio.toggleMute();
    if (keys.tapped(KeyCode.F11)) stage.setFullScreen(!stage.isFullScreen());
    if (keys.tapped(KeyCode.G)) debug = !debug;
    if (keys.tapped(KeyCode.ESCAPE) || pad.pause()) {
      if (screen.equals("PLAY") && session.result == GameSession.Result.PLAYING) {
        screen = "PAUSED";
        keys.clear();
      } else if (screen.equals("PAUSED")) {
        screen = "PLAY";
        keys.clear();
      } else if (session.result != GameSession.Result.PLAYING) menu();
    }
    if (keys.tapped(KeyCode.ENTER)
        || ((screen.equals("MENU") || session.result != GameSession.Result.PLAYING)
            && pad.interact())) {
      if (screen.equals("MENU") || session.result != GameSession.Result.PLAYING) startMission();
      else if (screen.equals("PAUSED")) screen = "PLAY";
    }
    if (screen.equals("PLAY")) {
      if (keys.tapped(KeyCode.ALT) || keys.tapped(KeyCode.V) || pad.dodge()) {
        if (session.player.dodge(
            (keys.isRight() ? 1 : 0) - (keys.isLeft() ? 1 : 0) + pad.dx(),
            (keys.isDown() ? 1 : 0) - (keys.isUp() ? 1 : 0) + pad.dy())) audio.play("sneak");
      }
      session.update(
          dt,
          new GameSession.Input(
              Math.max(
                  -1, Math.min(1, (keys.isRight() ? 1 : 0) - (keys.isLeft() ? 1 : 0) + pad.dx())),
              Math.max(-1, Math.min(1, (keys.isDown() ? 1 : 0) - (keys.isUp() ? 1 : 0) + pad.dy())),
              keys.held(KeyCode.SHIFT) || pad.sprint(),
              keys.held(KeyCode.CONTROL) || keys.held(KeyCode.C) || pad.sneak(),
              keys.held(KeyCode.E) || pad.interact(),
              keys.tapped(KeyCode.Q) || pad.lure(),
              keys.tapped(KeyCode.SPACE) || pad.pulse()));
      if (mouseFiring || keys.held(KeyCode.F) || pad.fire()) {
        double angle =
            mouseFiring ? renderer.aimAngle(mouseX, mouseY, session.player) : session.player.facing;
        if (mouseFiring) session.player.facing = angle;
        session.combat.fire(session, angle);
      }
      while (!session.sounds.isEmpty()) audio.play(session.sounds.remove());
      while (!session.spatialSounds.isEmpty()) {
        var event = session.spatialSounds.remove();
        double d = Math.hypot(event.x() - session.player.x, event.y() - session.player.y);
        if (d < 450)
          audio.play(
              event.name(),
              Math.max(.15, 1 - d / 500),
              Math.max(-.85, Math.min(.85, (event.x() - session.player.x) / 300)));
      }
    }
  }

  GameSession session() {
    return session;
  }

  String screen() {
    return screen;
  }

  public void startMission() {
    session = new GameSession();
    screen = "PLAY";
    keys.clear();
    mouseFiring = false;
    audio.play("terminal");
  }

  public void menu() {
    session = new GameSession();
    screen = "MENU";
    keys.clear();
    mouseFiring = false;
  }

  public void draw() {
    renderer.render(
        session, screen, audio.muted(), audio.available(), debug, clock, mouseX, mouseY);
  }

  private void click(double x, double y) {
    if (screen.equals("MENU") && inside(x, y, 76, 535, 320, 57)) startMission();
    else if (screen.equals("PAUSED")) {
      if (inside(x, y, 432, 508, 38, 33)) audio.adjustAmbience(-.1);
      else if (inside(x, y, 525, 508, 38, 33)) audio.adjustAmbience(.1);
      else if (inside(x, y, 750, 508, 38, 33)) audio.adjustEffects(-.1);
      else if (inside(x, y, 847, 508, 38, 33)) audio.adjustEffects(.1);
      else if (inside(x, y, 300, 580, 310, 48)) {
        screen = "PLAY";
        keys.clear();
      } else if (inside(x, y, 630, 580, 350, 48)) menu();
    } else if (session.result != GameSession.Result.PLAYING) {
      if (inside(x, y, 360, 500, 300, 52)) startMission();
      else if (inside(x, y, 680, 500, 240, 52)) menu();
    }
  }

  private static boolean inside(double x, double y, double bx, double by, double w, double h) {
    return x >= bx && x <= bx + w && y >= by && y <= by + h;
  }

  @Override
  public void close() {
    timer.stop();
    audio.close();
    gamepad.close();
  }
}

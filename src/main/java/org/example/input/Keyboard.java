package org.example.input;

import java.util.EnumSet;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

/** Filters coexist with UI handlers; taps are edge-triggered, movement is held. */
public final class Keyboard {
  private final EnumSet<KeyCode> down = EnumSet.noneOf(KeyCode.class);
  private final EnumSet<KeyCode> pressed = EnumSet.noneOf(KeyCode.class);

  public Keyboard(Scene scene) {
    scene.addEventFilter(
        javafx.scene.input.KeyEvent.KEY_PRESSED,
        e -> {
          if (down.add(e.getCode())) pressed.add(e.getCode());
          e.consume();
        });
    scene.addEventFilter(
        javafx.scene.input.KeyEvent.KEY_RELEASED,
        e -> {
          down.remove(e.getCode());
          e.consume();
        });
  }

  public boolean held(KeyCode code) {
    return down.contains(code);
  }

  public boolean tapped(KeyCode code) {
    return pressed.remove(code);
  }

  public void endFrame() {
    pressed.clear();
  }

  public void clear() {
    down.clear();
    pressed.clear();
  }

  public boolean isUp() {
    return held(KeyCode.W) || held(KeyCode.UP);
  }

  public boolean isDown() {
    return held(KeyCode.S) || held(KeyCode.DOWN);
  }

  public boolean isLeft() {
    return held(KeyCode.A) || held(KeyCode.LEFT);
  }

  public boolean isRight() {
    return held(KeyCode.D) || held(KeyCode.RIGHT);
  }
}

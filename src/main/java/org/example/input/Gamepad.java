package org.example.input;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Linux USB joystick support, including NES adapters. Keyboard remains entirely independent. */
public final class Gamepad implements AutoCloseable {
  public record Input(
      double dx,
      double dy,
      boolean interact,
      boolean sprint,
      boolean sneak,
      boolean pulse,
      boolean lure,
      boolean pause,
      boolean map,
      boolean fire,
      boolean dodge) {}

  private final boolean[] buttons = new boolean[64],
      pressed = new boolean[64],
      released = new boolean[64];
  private final double[] axes = new double[16];
  private final int[] mapping = {0, 1, 2, 3}; // A, B, Select, Start; F2 learns arbitrary adapters.
  private int remap = -1;
  private double bTime;
  private boolean mapChord, combatChord;
  private volatile boolean connected, closed;
  private volatile InputStream stream;
  private final Thread reader;

  public Gamepad() {
    this(true);
  }

  public Gamepad(boolean discover) {
    reader = new Thread(this::listen, "omega-usb-gamepad");
    reader.setDaemon(true);
    if (discover) reader.start();
  }

  private void listen() {
    if (!System.getProperty("os.name").toLowerCase().contains("linux")) return;
    while (!closed) {
      try {
        Path chosen = null;
        try (var devices = Files.newDirectoryStream(Path.of("/dev/input"), "js*")) {
          for (Path device : devices)
            if (Files.isReadable(device)) {
              chosen = device;
              break;
            }
        }
        if (chosen == null) {
          Thread.sleep(1000);
          continue;
        }
        try (InputStream input = Files.newInputStream(chosen)) {
          stream = input;
          connected = true;
          while (!closed) {
            byte[] event = input.readNBytes(8);
            if (event.length != 8) break;
            var data = ByteBuffer.wrap(event).order(ByteOrder.LITTLE_ENDIAN);
            accept(data.getShort(4), event[6] & 255, event[7] & 255);
          }
        }
      } catch (IOException ignored) {
        /* Missing/denied device never affects keyboard play. */
      } catch (InterruptedException stop) {
        Thread.currentThread().interrupt();
        break;
      } finally {
        connected = false;
        clear();
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException stop) {
        break;
      }
    }
  }

  /** The public decoder permits deterministic hardware-independent event tests. */
  public synchronized void accept(int value, int type, int number) {
    boolean initial = (type & 128) != 0;
    boolean learning = remap >= 0;
    type &= 127;
    if (type == 2 && number < axes.length)
      axes[number] = Math.abs(value) < 9000 ? 0 : Math.signum(value);
    if (type == 1 && number < buttons.length) {
      boolean down = value != 0;
      if (!initial && down && !buttons[number]) {
        if (remap >= 0) {
          mapping[remap++] = number;
          if (remap == 4) remap = -1;
        } else pressed[number] = true;
      }
      if (!initial && !learning && !down && buttons[number]) released[number] = true;
      buttons[number] = down;
    }
  }

  public synchronized Input poll(double dt) {
    int a = mapping[0], b = mapping[1], select = mapping[2], start = mapping[3];
    bTime = buttons[b] ? bTime + dt : bTime;
    boolean pulse = released[b] && bTime < .28 && !combatChord;
    if (!buttons[b]) combatChord = false;
    if (!buttons[b]) bTime = 0;
    boolean selectHeld = buttons[select] || buttons[6] || buttons[8];
    boolean startTap = pressed[start] || pressed[7] || pressed[9];
    boolean fire = buttons[a] && pressed[b] || buttons[b] && pressed[a];
    boolean dodge = selectHeld && pressed[a];
    if (fire) {
      combatChord = true;
      pulse = false;
    }
    if (dodge) mapChord = true;
    boolean map = startTap && selectHeld;
    if (map) mapChord = true;
    boolean lure = (released[select] || released[6] || released[8]) && !mapChord;
    if (!selectHeld) mapChord = false;
    Input input =
        new Input(
            axis(0, 6),
            axis(1, 7),
            buttons[a] && !fire && !dodge && !combatChord && !(mapChord && selectHeld),
            (buttons[b] && bTime >= .28) || (buttons[4] && !mapped(4)),
            buttons[a] || (buttons[5] && !mapped(5)),
            pulse,
            lure,
            startTap && !map,
            map,
            fire,
            dodge);
    Arrays.fill(pressed, false);
    Arrays.fill(released, false);
    return remap >= 0
        ? new Input(0, 0, false, false, false, false, false, false, false, false, false)
        : input;
  }

  private boolean mapped(int button) {
    for (int value : mapping) if (value == button) return true;
    return false;
  }

  private double axis(int stick, int hat) {
    return axes[hat] != 0 ? axes[hat] : axes[stick];
  }

  public synchronized void beginRemap() {
    remap = 0;
    clear();
  }

  public synchronized String remapPrompt() {
    return remap < 0
        ? ""
        : "Pressione "
            + new String[] {"A · interagir", "B · pulso / correr", "Select · isca", "Start · pausa"}
                [remap];
  }

  public synchronized void clear() {
    Arrays.fill(buttons, false);
    Arrays.fill(pressed, false);
    Arrays.fill(released, false);
    Arrays.fill(axes, 0);
    bTime = 0;
    mapChord = false;
    combatChord = false;
  }

  public boolean connected() {
    return connected;
  }

  public String label() {
    return connected ? "USB conectado · F2 configurar" : "Teclado · controle USB opcional";
  }

  @Override
  public void close() {
    closed = true;
    reader.interrupt();
    try {
      if (stream != null) stream.close();
    } catch (IOException ignored) {
    }
  }
}

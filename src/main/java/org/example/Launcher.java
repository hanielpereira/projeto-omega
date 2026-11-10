package org.example;

/** Separate launcher permits JavaFX startup from the executable shaded jar. */
public final class Launcher {
  private Launcher() {}

  public static void main(String[] args) {
    Main.main(args);
  }
}

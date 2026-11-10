package org.example;

import org.example.audio.AudioEngine;

/**
 * Writes real mixed audio to the local device; invoke explicitly, outside the headless test suite.
 */
public final class AudioSmoke {
  public static void main(String[] args) throws Exception {
    try (AudioEngine engine = new AudioEngine()) {
      long deadline = System.nanoTime() + 5_000_000_000L;
      // Events deliberately arrive before asset loading has completed.
      engine.play("terminal");
      engine.play("radio", .8, -.5);
      while (!engine.available() && System.nanoTime() < deadline) Thread.sleep(20);
      if (!engine.available()) throw new AssertionError(engine.status());
      engine.play("pulse", .8, .5);
      engine.atmosphere(.65, .3);
      Thread.sleep(1600);
      if (engine.framesWritten() < 44100) throw new AssertionError("audio stream did not advance");
      System.out.println(
          "OMEGA_AUDIO_OK: " + engine.status() + "; frames=" + engine.framesWritten());
    }
  }
}

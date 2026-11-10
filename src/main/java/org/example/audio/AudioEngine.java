package org.example.audio;

import java.io.*;
import java.util.concurrent.*;
import javax.sound.sampled.*;

/** One low-latency stereo stream. PulseAudio/PipeWire on Linux; Java Sound elsewhere. */
public final class AudioEngine implements AutoCloseable {
  private record Sound(String name, double gain, double pan) {}

  private final ConcurrentLinkedQueue<Sound> queued = new ConcurrentLinkedQueue<>();
  private volatile boolean closed, muted, available;
  private volatile double ambient = .72, effects = .85, threat, exhaustion;
  private volatile String status = "Inicializando áudio";
  private volatile Output output;
  private volatile AudioMixer mixer;
  private final Thread thread;
  private int step;

  private interface Output extends AutoCloseable {
    void write(byte[] pcm) throws Exception;

    void close();
  }

  public AudioEngine() {
    thread = new Thread(this::run, "omega-stereo-mixer");
    thread.setDaemon(true);
    thread.start();
  }

  private void run() {
    try {
      mixer = new AudioMixer();
      if (closed) return;
      output = openOutput();
      available = true;
      while (!closed) {
        Sound event;
        while ((event = queued.poll()) != null) mixer.play(event.name, event.gain, event.pan);
        mixer.muted = muted;
        mixer.ambienceVolume = ambient;
        mixer.effectsVolume = effects;
        mixer.threat = threat;
        mixer.exhaustion = exhaustion;
        output.write(mixer.mix(512));
      }
    } catch (Exception failure) {
      if (!closed) {
        available = false;
        status = "Áudio indisponível: " + failure.getMessage();
        System.err.println("[Ômega] " + status);
      }
    } finally {
      if (output != null) output.close();
    }
  }

  private Output openOutput() throws Exception {
    if (System.getProperty("os.name").toLowerCase().contains("linux")) {
      try {
        Process process =
            new ProcessBuilder(
                    "pacat",
                    "--playback",
                    "--raw",
                    "--format=s16le",
                    "--rate=44100",
                    "--channels=2",
                    "--latency-msec=40",
                    "--client-name=Projeto Omega",
                    "--stream-name=Laboratorio")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        if (process.waitFor(150, TimeUnit.MILLISECONDS))
          throw new IOException("Servidor PulseAudio sem resposta");
        status = "PulseAudio / PipeWire · estéreo 44,1 kHz";
        return new Output() {
          public void write(byte[] b) throws IOException {
            process.getOutputStream().write(b);
          }

          public void close() {
            try {
              process.getOutputStream().close();
            } catch (IOException ignored) {
            }
            process.destroy();
          }
        };
      } catch (IOException ignored) {
        /* Native Java Sound remains available on non-Pulse systems. */
      }
    }
    SourceDataLine line = AudioSystem.getSourceDataLine(AudioMixer.FORMAT);
    line.open(AudioMixer.FORMAT, 8192);
    line.start();
    status = "Java Sound · estéreo 44,1 kHz";
    return new Output() {
      public void write(byte[] b) {
        line.write(b, 0, b.length);
      }

      public void close() {
        line.stop();
        line.close();
      }
    };
  }

  public void play(String name) {
    double gain = name.equals("step") ? .55 : name.equals("sneak") ? .27 : 1;
    if (name.equals("step")) {
      step = (step + 1) % 3;
      name = step == 0 ? "step" : step == 1 ? "step2" : "step3";
    }
    play(name, gain, 0);
  }

  public void play(String name, double gain, double pan) {
    if (!closed) {
      if (queued.size() >= 128) queued.poll();
      queued.add(new Sound(name, gain, pan));
    }
  }

  public void atmosphere(double danger, double tired) {
    threat = Math.max(0, Math.min(1, danger));
    exhaustion = Math.max(0, Math.min(1, tired));
  }

  public void toggleMute() {
    muted = !muted;
  }

  public boolean muted() {
    return muted;
  }

  public boolean available() {
    return available;
  }

  public String status() {
    return status;
  }

  public void adjustAmbience(double amount) {
    ambient = Math.max(0, Math.min(1, ambient + amount));
  }

  public void adjustEffects(double amount) {
    effects = Math.max(0, Math.min(1, effects + amount));
    play("terminal");
  }

  public double ambienceVolume() {
    return ambient;
  }

  public double effectsVolume() {
    return effects;
  }

  public long framesWritten() {
    return mixer == null ? 0 : mixer.framesMixed();
  }

  @Override
  public void close() {
    if (closed) return;
    closed = true;
    if (output != null) output.close();
    thread.interrupt();
  }
}

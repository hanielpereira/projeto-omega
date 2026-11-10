package org.example.audio;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.sound.sampled.*;

/** Software mixer: all voices share one stereo output, so sounds never compete for device lines. */
public final class AudioMixer {
  public static final int RATE = 44100;
  public static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 2, true, false);
  public static final List<String> NAMES =
      List.of(
          "shot",
          "breach",
          "ricochet",
          "ambient",
          "machinery",
          "heartbeat",
          "breath",
          "terminal",
          "pulse",
          "lure",
          "hit",
          "step",
          "step2",
          "step3",
          "sneak",
          "win",
          "lose",
          "door",
          "radio",
          "alert",
          "roar",
          "interact",
          "drip");

  private record Request(String name, float gain, float pan) {}

  private static final class Voice {
    final float[] data;
    final float left, right;
    int position;

    Voice(float[] data, float gain, float pan) {
      this.data = data;
      left = gain * (pan > 0 ? 1 - pan : 1);
      right = gain * (pan < 0 ? 1 + pan : 1);
    }
  }

  private final Map<String, float[]> samples = new HashMap<>();
  private final List<Voice> voices = new ArrayList<>();
  private final Queue<Request> pending = new ConcurrentLinkedQueue<>();
  private final Map<String, Integer> loops = new HashMap<>();
  public volatile double ambienceVolume = .72, effectsVolume = .85, threat, exhaustion;
  public volatile boolean muted;
  private long framesMixed;
  private double duck;

  public AudioMixer() throws Exception {
    for (String name : NAMES) {
      var resource = AudioMixer.class.getResourceAsStream("/audio/" + name + ".wav");
      if (resource == null) throw new FileNotFoundException("audio/" + name + ".wav");
      try (var source = AudioSystem.getAudioInputStream(new BufferedInputStream(resource));
          var converted = AudioSystem.getAudioInputStream(FORMAT, source)) {
        byte[] pcm = converted.readAllBytes();
        float[] values = new float[pcm.length / 2];
        for (int i = 0; i < values.length; i++)
          values[i] = (short) ((pcm[i * 2] & 255) | (pcm[i * 2 + 1] << 8)) / 32768f;
        samples.put(name, values);
      }
    }
  }

  public void play(String name, double gain, double pan) {
    pending.add(
        new Request(
            name, (float) Math.max(0, Math.min(1, gain)), (float) Math.max(-1, Math.min(1, pan))));
  }

  public byte[] mix(int frames) {
    Request request;
    while ((request = pending.poll()) != null) {
      float[] sample = samples.get(request.name);
      if (sample == null) continue;
      if (voices.size() >= 32) voices.remove(0);
      voices.add(new Voice(sample, request.gain, request.pan));
      if (request.name.equals("radio") || request.name.equals("alert")) duck = .9;
    }
    float[] buffer = new float[frames * 2];
    loop(buffer, "ambient", .64 * ambienceVolume * (duck > 0 ? .55 : 1));
    loop(buffer, "machinery", .28 * ambienceVolume);
    loop(buffer, "heartbeat", threat * .45 * effectsVolume);
    loop(buffer, "breath", exhaustion * .45 * effectsVolume);
    Iterator<Voice> it = voices.iterator();
    while (it.hasNext()) {
      Voice v = it.next();
      for (int i = 0; i < buffer.length && v.position < v.data.length; i += 2) {
        buffer[i] += v.data[v.position++] * v.left * effectsVolume;
        buffer[i + 1] += v.data[v.position++] * v.right * effectsVolume;
      }
      if (v.position >= v.data.length) it.remove();
    }
    duck = Math.max(0, duck - (double) frames / RATE);
    framesMixed += frames;
    byte[] result = new byte[frames * 4];
    for (int i = 0; i < buffer.length; i++) {
      double v = muted ? 0 : Math.tanh(buffer[i] * 1.1) * .92;
      int n = (int) (v * 32767);
      result[i * 2] = (byte) n;
      result[i * 2 + 1] = (byte) (n >> 8);
    }
    return result;
  }

  private void loop(float[] out, String name, double gain) {
    float[] data = samples.get(name);
    int p = loops.getOrDefault(name, 0);
    for (int i = 0; i < out.length; i++) {
      out[i] += data[p] * gain;
      if (++p >= data.length) p = 0;
    }
    loops.put(name, p);
  }

  public long framesMixed() {
    return framesMixed;
  }
}

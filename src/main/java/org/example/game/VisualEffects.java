package org.example.game;

import java.util.*;

/** Bounded world-space particles, advanced by simulation time (therefore paused with the game). */
public final class VisualEffects {
  public enum Kind {
    DUST,
    SPARK,
    GLASS,
    BLOOD,
    EMP,
    STEAM,
    DATA,
    AFTERIMAGE
  }

  public static final class Particle {
    public double x, y, vx, vy, life;
    public final double total, size;
    public final Kind kind;

    Particle(double x, double y, double vx, double vy, double life, double size, Kind kind) {
      this.x = x;
      this.y = y;
      this.vx = vx;
      this.vy = vy;
      this.life = life;
      total = life;
      this.size = size;
      this.kind = kind;
    }
  }

  private final Random random = new Random(707);
  public final List<Particle> particles = new ArrayList<>();
  public double shake, flash;

  public void burst(double x, double y, Kind kind, int count) {
    for (int i = 0; i < count; i++) {
      if (particles.size() >= 550) particles.remove(0);
      double angle = random.nextDouble() * Math.PI * 2,
          speed = kind == Kind.DUST ? 6 : kind == Kind.STEAM ? 18 : 25 + random.nextDouble() * 75;
      particles.add(
          new Particle(
              x,
              y,
              Math.cos(angle) * speed,
              Math.sin(angle) * speed,
              .25 + random.nextDouble() * (kind == Kind.DUST ? 3 : 1),
              kind == Kind.DUST ? 1 : 1 + random.nextInt(2),
              kind));
    }
  }

  public void ghost(double x, double y, double facing) {
    particles.add(new Particle(x, y, 0, 0, .2, facing, Kind.AFTERIMAGE));
  }

  public void update(double dt) {
    shake = Math.max(0, shake - dt * 18);
    flash = Math.max(0, flash - dt * 3);
    particles.removeIf(
        p -> {
          p.life -= dt;
          p.x += p.vx * dt;
          p.y += p.vy * dt;
          p.vx *= Math.exp(-dt * 1.7);
          p.vy *= Math.exp(-dt * 1.7);
          if (p.kind == Kind.SPARK || p.kind == Kind.GLASS) p.vy += dt * 70;
          return p.life <= 0;
        });
  }
}

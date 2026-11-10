package org.example.game;

import java.util.*;
import org.example.entities.Agent;

/** Finite-ammunition sidearm. Substeps stop fast rounds from passing through walls or enemies. */
public final class Combat {
  public static final class Round {
    public double x, y, life = .8;
    public final double vx, vy;

    Round(double x, double y, double angle) {
      this.x = x;
      this.y = y;
      vx = Math.cos(angle) * 460;
      vy = Math.sin(angle) * 460;
    }
  }

  public record Fallen(Agent.Kind kind, double x, double y) {}

  public final List<Round> rounds = new ArrayList<>();
  public final List<Fallen> fallen = new ArrayList<>();
  public int ammo = 8, kills;
  public double cooldown, muzzle;

  public boolean fire(GameSession s, double angle) {
    if (s.result != GameSession.Result.PLAYING || cooldown > 0) return false;
    cooldown = .3;
    if (ammo <= 0) {
      s.sounds.add("interact");
      s.message("Sem munição. Procure as caixas de suprimentos.");
      return false;
    }
    ammo--;
    muzzle = .09;
    s.player.facing = angle;
    rounds.add(
        new Round(s.player.x + Math.cos(angle) * 9, s.player.y + Math.sin(angle) * 9, angle));
    s.fx.shake = 1.8;
    s.sounds.add("shot");
    s.noiseX = s.player.x;
    s.noiseY = s.player.y;
    s.noiseTime = 1.1;
    s.noiseRadius = 300;
    s.fx.burst(
        s.player.x + Math.cos(angle) * 14,
        s.player.y + Math.sin(angle) * 14,
        VisualEffects.Kind.SPARK,
        5);
    return true;
  }

  public void update(GameSession s, double dt) {
    cooldown = Math.max(0, cooldown - dt);
    muzzle = Math.max(0, muzzle - dt);
    Iterator<Round> it = rounds.iterator();
    while (it.hasNext()) {
      Round r = it.next();
      r.life -= dt;
      boolean stop = r.life <= 0;
      int steps = Math.max(1, (int) Math.ceil(460 * dt / 3));
      for (int i = 0; i < steps && !stop; i++) {
        r.x += r.vx * dt / steps;
        r.y += r.vy * dt / steps;
        if (!s.map.walkable((int) Math.floor(r.y / 32), (int) Math.floor(r.x / 32))) {
          s.fx.burst(r.x, r.y, VisualEffects.Kind.SPARK, 8);
          s.spatialSounds.add(new GameSession.SpatialSound("ricochet", r.x, r.y));
          stop = true;
          break;
        }
        for (Agent a : s.agents)
          if (a.health > 0 && Math.hypot(a.x - r.x, a.y - r.y) < 12) {
            a.health--;
            a.hurtTime = 2;
            a.stun(.38);
            stop = true;
            s.fx.burst(
                a.x,
                a.y,
                a.kind == Agent.Kind.SPECIMEN ? VisualEffects.Kind.BLOOD : VisualEffects.Kind.SPARK,
                14);
            s.sounds.add("hit");
            if (a.health <= 0) {
              kills++;
              fallen.add(new Fallen(a.kind, a.x, a.y));
              s.radio.release(a);
              s.fx.shake = 2.4;
            }
            break;
          }
      }
      if (stop) it.remove();
    }
    s.agents.removeIf(a -> a.health <= 0);
  }
}

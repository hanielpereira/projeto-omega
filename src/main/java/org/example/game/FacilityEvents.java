package org.example.game;

import org.example.entities.Agent;
import org.example.world.TileMap;

/**
 * Authored encounter beats: objectives change the facility instead of only incrementing a counter.
 */
public final class FacilityEvents {
  public double blackout, arrival = -1;
  public boolean liftReady, breachPending;
  public int breaches;
  public final int[][] arcs = {{12, 18}, {13, 18}, {20, 29}};
  private final boolean[] powered = new boolean[3];

  public void recovered(GameSession s, int count) {
    s.combat.ammo = Math.min(14, s.combat.ammo + 3);
    blackout = 2.4;
    s.fx.shake = 2;
    s.sounds.add("breach");
    s.noiseX = s.player.x;
    s.noiseY = s.player.y;
    s.noiseTime = 2.5;
    s.noiseRadius = 240;
    if (count == 1) {
      breachPending = true;
      s.message("CONTENÇÃO ROMPIDA · Movimento no biotério. O disparo denuncia sua posição.");
    }
    if (count == 2) {
      breachPending = true;
      s.message("REDE DE SEGURANÇA ATIVA · A equipe está rastreando os acessos aos arquivos.");
    }
    if (count == 3) s.message("Acesso recuperado. Restabeleça a energia no reator auxiliar.");
  }

  public double phase(GameSession s, int i) {
    return (s.elapsed + i * 2.7) % 8;
  }

  public boolean live(int i) {
    return powered[i];
  }

  public void callLift(GameSession s) {
    if (arrival < 0) {
      arrival = 12;
      s.noiseX = s.player.x;
      s.noiseY = s.player.y;
      s.noiseTime = 4;
      s.noiseRadius = 400;
      s.sounds.add("door");
      s.message("Elevador acionado. Doze segundos até a chegada. Encontre cobertura.");
      breachPending = true;
    }
  }

  public void update(GameSession s, double dt) {
    blackout = Math.max(0, blackout - dt);
    if (arrival > 0) {
      arrival = Math.max(0, arrival - dt);
      if (arrival == 0) {
        liftReady = true;
        s.sounds.add("terminal");
        s.message("Elevador no andar. Volte à porta e entre.");
      }
    }
    for (int i = 0; i < arcs.length; i++) {
      boolean live = s.reactor && phase(s, i) >= 3 && phase(s, i) < 5;
      if (powered[i] != live) {
        powered[i] = live;
        s.map.extraCost(arcs[i][0], arcs[i][1], live ? 8 : 0);
      }
      if (live) {
        double x = TileMap.center(arcs[i][1]), y = TileMap.center(arcs[i][0]);
        if ((int) (s.elapsed * 30) % 3 == 0) s.fx.burst(x, y, VisualEffects.Kind.SPARK, 1);
        if (Math.hypot(s.player.x - x, s.player.y - y) < 22 && s.player.invulnerable <= 0)
          s.player.damage(16);
      }
    }
    if (breachPending && blackout <= 0 && breaches < 3) {
      int[][] vents = {{23, 3}, {8, 30}, {23, 26}};
      java.util.Arrays.sort(
          vents,
          java.util.Comparator.comparingDouble(
              v ->
                  Math.hypot(
                      TileMap.center(v[1]) - s.player.x, TileMap.center(v[0]) - s.player.y)));
      for (int[] vent : vents) {
        double x = TileMap.center(vent[1]), y = TileMap.center(vent[0]);
        if (Math.hypot(x - s.player.x, y - s.player.y) < 240
            || s.map.lineOfSight(s.player.x, s.player.y, x, y)) continue;
        Agent a =
            new Agent(
                Agent.Kind.SPECIMEN,
                s.map,
                new int[][] {{vent[0], vent[1]}, {13, 18}, {20, 18}},
                s.radio);
        a.stun(1.5);
        s.agents.add(a);
        s.spatialSounds.add(new GameSession.SpatialSound("roar", x, y));
        s.fx.burst(x, y, VisualEffects.Kind.STEAM, 22);
        breaches++;
        breachPending = false;
        break;
      }
    }
  }
}

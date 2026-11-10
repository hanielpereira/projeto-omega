package org.example;

import java.util.*;
import org.example.ai.AStarPathfinder;
import org.example.entities.Agent;
import org.example.game.*;
import org.example.world.TileMap;

/**
 * Optional model playthrough: uses movement, interactions and ammunition; never teleports or
 * changes the player health directly.
 */
public final class PlaythroughSmoke {
  public static void main(String[] args) {
    var s = new GameSession();
    var finder = new AStarPathfinder(s.map);
    GameSession.Device target = null;
    List<int[]> route = List.of();
    int waypoint = 0;
    double replan = 0;
    for (int frame = 0; frame < 20000 && s.result == GameSession.Result.PLAYING; frame++) {
      Agent threat =
          s.agents.stream()
              .filter(a -> s.map.lineOfSight(s.player.x, s.player.y, a.x, a.y))
              .min(Comparator.comparingDouble(a -> Math.hypot(a.x - s.player.x, a.y - s.player.y)))
              .orElse(null);
      double danger =
          threat == null ? 999 : Math.hypot(threat.x - s.player.x, threat.y - s.player.y);
      if (threat != null && danger < 200)
        s.combat.fire(s, Math.atan2(threat.y - s.player.y, threat.x - s.player.x));
      GameSession.Device wanted = choose(s, finder);
      replan -= .04;
      if (wanted != target || replan <= 0) {
        target = wanted;
        if (target == null) break;
        route = finder.findPath(s.player.row(), s.player.col(), target.row, target.col);
        waypoint = 0;
        replan = .8;
      }
      double dx = 0, dy = 0;
      boolean interact =
          target != null
              && Math.hypot(target.x() - s.player.x, target.y() - s.player.y) < 38
              && s.map.lineOfSight(s.player.x, s.player.y, target.x(), target.y());
      if (!interact && waypoint < route.size()) {
        int[] node = route.get(waypoint);
        double tx = TileMap.center(node[1]), ty = TileMap.center(node[0]);
        if (Math.hypot(tx - s.player.x, ty - s.player.y) < 4) {
          waypoint++;
        } else {
          dx = tx - s.player.x;
          dy = ty - s.player.y;
        }
      }
      if (danger < 32 && s.player.dodgeCooldown <= 0)
        s.player.dodge(s.player.x - threat.x, s.player.y - threat.y);
      s.update(
          .04,
          new GameSession.Input(
              dx, dy, danger < 140, false, interact, false, danger < 70 && s.pulseCooldown <= 0));
    }
    System.out.println(
        "PLAYTHROUGH: "
            + s.result
            + "; time="
            + s.time()
            + "; hp="
            + (int) s.player.health
            + "; cores="
            + s.cores
            + "; kills="
            + s.combat.kills
            + "; ammo="
            + s.combat.ammo);
    if (s.result != GameSession.Result.ESCAPED)
      throw new AssertionError(
          "Mission did not complete: "
              + s.objective()
              + " @ "
              + s.player.col()
              + ","
              + s.player.row());
  }

  private static GameSession.Device choose(GameSession s, AStarPathfinder finder) {
    List<GameSession.Device> possible = new ArrayList<>();
    for (var d : s.devices)
      if (!d.active && s.usable(d)) {
        if (d.type == GameSession.DeviceType.MEDKIT && s.player.health < 62
            || d.type == GameSession.DeviceType.SUPPLY && s.combat.ammo < 3) possible.add(d);
      }
    if (possible.isEmpty())
      for (var d : s.devices)
        if (!d.active) {
          if (s.cores < 3 && d.type == GameSession.DeviceType.CORE
              || s.cores == 3 && !s.reactor && d.type == GameSession.DeviceType.REACTOR
              || s.reactor && !s.centralAccess && d.type == GameSession.DeviceType.CONTROL
              || s.centralAccess && d.type == GameSession.DeviceType.EXIT) possible.add(d);
        }
    return possible.stream()
        .min(
            Comparator.comparingInt(
                d -> {
                  var p = finder.findPath(s.player.row(), s.player.col(), d.row, d.col);
                  return p.isEmpty() ? 99999 : p.size();
                }))
        .orElse(null);
  }
}

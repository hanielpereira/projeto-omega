package org.example.ai;

import java.util.*;
import org.example.entities.Agent;
import org.example.world.TileMap;

/** Scoped blackboard. Android radios carry old observations, with delay and a finite range. */
public final class RadioNetwork {
  private record Transmission(
      Agent sender, AgentMemory.Evidence clue, double deliverAt, double x, double y) {}

  private record Reservation(Agent owner, double until) {}

  private final List<Transmission> pending = new ArrayList<>();
  private final Map<String, Reservation> reservations = new HashMap<>();
  private long sequence;
  public int delivered;

  public long nextEvent() {
    return ++sequence;
  }

  public void transmit(Agent sender, AgentMemory.Evidence clue, double now) {
    if (!sender.radioEnabled || sender.kind == Agent.Kind.SPECIMEN) return;
    pending.add(new Transmission(sender, clue, now + .8, sender.x, sender.y));
    sender.radioVisual = 1;
  }

  public void update(double now, List<Agent> agents) {
    reservations
        .entrySet()
        .removeIf(
            e ->
                e.getValue().until < now
                    || !agents.contains(e.getValue().owner)
                    || e.getValue().owner.state == Agent.State.STUNNED);
    Iterator<Transmission> it = pending.iterator();
    while (it.hasNext()) {
      Transmission t = it.next();
      if (now < t.deliverAt) continue;
      it.remove();
      if (!agents.contains(t.sender)
          || !t.sender.radioEnabled
          || t.sender.state == Agent.State.STUNNED) continue;
      for (Agent a : agents)
        if (a != t.sender
            && a.radioEnabled
            && a.kind != Agent.Kind.SPECIMEN
            && Math.hypot(a.x - t.x, a.y - t.y) <= 340) {
          var c = t.clue;
          var report =
              new AgentMemory.Evidence(
                  c.id(),
                  c.x(),
                  c.y(),
                  c.time(),
                  c.confidence() * .72,
                  AgentMemory.Source.RADIO,
                  c.origin(),
                  c.expires());
          if (a.receive(report, now)) {
            delivered++;
            a.radioVisual = 1;
          }
        }
    }
  }

  /** Each searcher reserves a distinct reachable sector near its actual clue. */
  public int[] reserveSearch(
      Agent agent, TileMap map, double x, double y, double now, int attempt) {
    int row = (int) (y / 32), col = (int) (x / 32);
    int[][] offsets = {{0, 0}, {-2, 0}, {0, 2}, {2, 0}, {0, -2}, {-2, 2}, {2, -2}};
    for (int k = 0; k < offsets.length; k++) {
      int[] d = offsets[(k + attempt) % offsets.length];
      int r = row + d[0], c = col + d[1];
      if (!map.walkable(r, c)) continue;
      String key = r + "," + c;
      Reservation old = reservations.get(key);
      if (old != null && old.owner != agent && old.until > now) continue;
      if (new AStarPathfinder(map)
          .findPath((int) (agent.y / 32), (int) (agent.x / 32), r, c)
          .isEmpty()) continue;
      release(agent);
      reservations.put(key, new Reservation(agent, now + 4));
      return new int[] {r, c};
    }
    return null;
  }

  public void release(Agent a) {
    reservations.entrySet().removeIf(e -> e.getValue().owner == a);
  }

  public int reservations() {
    return reservations.size();
  }
}

package org.example.ai;

import java.util.*;

/** Knowledge is evidence, never a reference to the live player position. */
public final class AgentMemory {
  public enum Source {
    VISION,
    SOUND,
    RADIO
  }

  public record Evidence(
      long id,
      double x,
      double y,
      double time,
      double confidence,
      Source source,
      String origin,
      double expires) {
    public double confidenceAt(double now) {
      return confidence * Math.exp(-.22 * Math.max(0, now - time));
    }

    public boolean valid(double now) {
      return now < expires && confidenceAt(now) > .1;
    }
  }

  private final LinkedHashMap<Long, Evidence> evidence = new LinkedHashMap<>();
  private Evidence current;

  public boolean remember(Evidence clue, double now) {
    if (!clue.valid(now) || evidence.containsKey(clue.id())) return false;
    evidence.put(clue.id(), clue);
    while (evidence.size() > 12) evidence.remove(evidence.keySet().iterator().next());
    if (current == null
        || !current.valid(now)
        || clue.confidenceAt(now) >= current.confidenceAt(now)) current = clue;
    return true;
  }

  public Evidence best(double now) {
    return current != null && current.valid(now) ? current : null;
  }

  public void inspected() {
    current = null;
  }

  public List<Evidence> history() {
    return List.copyOf(evidence.values());
  }
}

package org.example.ai;

import java.util.List;
import org.example.world.TileMap;

/** Hybrid event + minimum interval replanning; never runs A* for every frame. */
public final class DynamicNavigator {
  private final TileMap map;
  private final AStarPathfinder finder;
  private List<int[]> path = List.of();
  private int index, goalRow = -1, goalCol = -1, plannedRow = -1, plannedCol = -1, revision = -1;
  private double cooldown;
  public int searches;
  public String reason = "inicial";

  public DynamicNavigator(TileMap map) {
    this.map = map;
    finder = new AStarPathfinder(map);
  }

  public void target(int row, int col) {
    goalRow = row;
    goalCol = col;
  }

  public void clear() {
    path = List.of();
    index = 0;
    plannedRow = -1;
    plannedCol = -1;
  }

  public void tick(double dt, double x, double y) {
    cooldown = Math.max(0, cooldown - dt);
    boolean blocked = index < path.size() && !map.walkable(path.get(index)[0], path.get(index)[1]);
    boolean changed = goalRow != plannedRow || goalCol != plannedCol;
    boolean worldChanged = revision != map.revision();
    if (blocked) {
      path = List.of();
      index = 0;
      reason = "rota bloqueada";
    }
    if (goalRow < 0
        || cooldown > 0
        || (!changed
            && !worldChanged
            && !blocked
            && (!path.isEmpty() || revision == map.revision()))) return;
    if (!map.takeNavigationSlot()) return;
    reason =
        blocked
            ? "obstáculo"
            : worldChanged ? "mapa alterado" : changed ? "alvo mudou de tile" : "sem rota";
    path = finder.findPath((int) (y / 32), (int) (x / 32), goalRow, goalCol);
    searches++;
    plannedRow = goalRow;
    plannedCol = goalCol;
    revision = map.revision();
    cooldown = path.isEmpty() ? 1.2 : .55;
    index = 0;
    if (!path.isEmpty()
        && Math.hypot(TileMap.center(path.get(0)[1]) - x, TileMap.center(path.get(0)[0]) - y) < 1)
      index = 1;
    if (path.size() > 1
        && (Math.abs(TileMap.center(path.get(1)[1]) - x) < .01
            || Math.abs(TileMap.center(path.get(1)[0]) - y) < .01)) index = 1;
  }

  public double[] move(double x, double y, double speed, double dt) {
    double remaining = speed * dt, facing = Double.NaN;
    while (index < path.size() && remaining > 0) {
      int[] p = path.get(index);
      if (!map.walkable(p[0], p[1])) break;
      double tx = TileMap.center(p[1]),
          ty = TileMap.center(p[0]),
          dx = tx - x,
          dy = ty - y,
          d = Math.hypot(dx, dy);
      if (d < .01) {
        index++;
        continue;
      }
      double step = Math.min(d, remaining), nx = x + dx / d * step, ny = y + dy / d * step;
      if (!map.canOccupy(nx, ny, 7)) break;
      x = nx;
      y = ny;
      facing = Math.atan2(dy, dx);
      remaining -= step;
      if (step >= d) index++;
    }
    return new double[] {x, y, facing};
  }

  public List<int[]> path() {
    return path;
  }

  public boolean failed() {
    return path.isEmpty() && plannedRow == goalRow && plannedCol == goalCol;
  }
}

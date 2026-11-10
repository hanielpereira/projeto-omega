package org.example.ai;

import java.util.*;
import org.example.world.TileMap;

/** Weighted A*: Manhattan is admissible because all traversable costs are >= 1. */
public final class AStarPathfinder {
  private final TileMap map;
  private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

  public AStarPathfinder(TileMap map) {
    this.map = map;
  }

  public List<int[]> findPath(int sr, int sc, int gr, int gc) {
    if (!map.walkable(sr, sc) || !map.walkable(gr, gc)) return List.of();
    double[][] best = new double[TileMap.HEIGHT][TileMap.WIDTH];
    boolean[][] closed = new boolean[TileMap.HEIGHT][TileMap.WIDTH];
    for (double[] row : best) Arrays.fill(row, Double.POSITIVE_INFINITY);
    PriorityQueue<Node> open =
        new PriorityQueue<>(Comparator.comparingDouble(Node::f).thenComparingDouble(Node::h));
    best[sr][sc] = 0;
    open.add(new Node(sr, sc, 0, heuristic(sr, sc, gr, gc), null));
    while (!open.isEmpty()) {
      Node n = open.poll();
      if (n.g > best[n.r][n.c] || closed[n.r][n.c]) continue;
      closed[n.r][n.c] = true;
      if (n.r == gr && n.c == gc) {
        LinkedList<int[]> route = new LinkedList<>();
        for (Node p = n; p != null; p = p.parent) route.addFirst(new int[] {p.r, p.c});
        return route;
      }
      for (int[] d : DIRECTIONS) {
        int r = n.r + d[0], c = n.c + d[1];
        if (!map.walkable(r, c) || closed[r][c]) continue;
        double cost = n.g + map.getTile(r, c).getMovementCost();
        if (cost >= best[r][c]) continue;
        best[r][c] = cost;
        open.add(new Node(r, c, cost, heuristic(r, c, gr, gc), n));
      }
    }
    return List.of();
  }

  private static double heuristic(int r, int c, int gr, int gc) {
    return Math.abs(r - gr) + Math.abs(c - gc);
  }

  private record Node(int r, int c, double g, double h, Node parent) {
    double f() {
      return g + h;
    }
  }
}

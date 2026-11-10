package org.example.world;

/** Fixed, connected facility. All coordinates are in world pixels, not screen pixels. */
public final class TileMap {
  public static final int WIDTH = 36, HEIGHT = 26, TILE_SIZE = 32;
  private int revision, navigationSlots = Integer.MAX_VALUE;
  private final double[][] costs = new double[HEIGHT][WIDTH];

  public void extraCost(int row, int col, double value) {
    if (inside(row, col) && costs[row][col] != value) {
      costs[row][col] = Math.max(0, value);
      revision++;
    }
  }

  private final boolean[][] blocked = new boolean[HEIGHT][WIDTH];

  public int revision() {
    return revision;
  }

  public void beginNavigationFrame() {
    navigationSlots = 2;
  }

  public boolean takeNavigationSlot() {
    if (navigationSlots <= 0) return false;
    navigationSlots--;
    return true;
  }

  public void block(int row, int col, boolean value) {
    if (inside(row, col) && blocked[row][col] != value) {
      blocked[row][col] = value;
      revision++;
    }
  }

  private final Tile[][] tiles = new Tile[HEIGHT][WIDTH];

  public TileMap() {
    for (int row = 0; row < HEIGHT; row++) {
      for (int col = 0; col < WIDTH; col++) {
        set(
            row,
            col,
            row == 0 || col == 0 || row == HEIGHT - 1 || col == WIDTH - 1
                ? Tile.Type.WALL
                : Tile.Type.FLOOR);
      }
    }
    // Four wings connected by two central circulation corridors.
    for (int row = 1; row < HEIGHT - 1; row++) {
      if (row != 5 && row != 6 && row != 12 && row != 13 && row != 20 && row != 21) {
        set(row, 11, Tile.Type.WALL);
        set(row, 24, Tile.Type.WALL);
      }
    }
    for (int col = 1; col < WIDTH - 1; col++) {
      if (col != 4 && col != 5 && col != 17 && col != 18 && col != 29 && col != 30) {
        set(10, col, Tile.Type.WALL);
        set(16, col, Tile.Type.WALL);
      }
    }
    // Island benches, storage and containment tanks (solid obstacles).
    for (int[] bench :
        new int[][] {
          {3, 4}, {3, 7}, {7, 4}, {3, 16}, {3, 20}, {7, 16}, {3, 28}, {7, 31}, {19, 4}, {22, 7},
          {19, 15}, {22, 19}, {19, 28}, {22, 31}
        }) {
      set(bench[0], bench[1], Tile.Type.WALL);
      set(bench[0], bench[1] + 1, Tile.Type.WALL);
    }
    for (int row = 18; row <= 22; row++)
      for (int col = 26; col <= 29; col++) {
        if (tiles[row][col].isWalkable()) set(row, col, Tile.Type.CONTAMINATED);
      }
    for (int row = 7; row <= 8; row++)
      for (int col = 18; col <= 21; col++) {
        set(row, col, Tile.Type.MUD);
      }
  }

  /** Test/custom-map constructor: 0 floor, 1 wall, 2 mud, 3 contamination. */
  public TileMap(int[][] layout) {
    for (int row = 0; row < HEIGHT; row++)
      for (int col = 0; col < WIDTH; col++) {
        int value = row < layout.length && col < layout[row].length ? layout[row][col] : 1;
        set(
            row,
            col,
            switch (value) {
              case 0 -> Tile.Type.FLOOR;
              case 2 -> Tile.Type.MUD;
              case 3 -> Tile.Type.CONTAMINATED;
              default -> Tile.Type.WALL;
            });
      }
  }

  private void set(int row, int col, Tile.Type type) {
    tiles[row][col] =
        new Tile(
            type,
            type != Tile.Type.WALL,
            switch (type) {
              case WALL -> 0;
              case MUD -> 2;
              case CONTAMINATED -> 5;
              default -> 1;
            });
  }

  public boolean inside(int row, int col) {
    return row >= 0 && col >= 0 && row < HEIGHT && col < WIDTH;
  }

  public boolean walkable(int row, int col) {
    return inside(row, col) && !blocked[row][col] && tiles[row][col].isWalkable();
  }

  public Tile getTile(int row, int col) {
    return costs[row][col] == 0
        ? tiles[row][col]
        : new Tile(
            tiles[row][col].getType(),
            tiles[row][col].isWalkable(),
            tiles[row][col].getMovementCost() + costs[row][col]);
  }

  public boolean canOccupy(double x, double y, double radius) {
    int left = (int) Math.floor((x - radius) / TILE_SIZE),
        right = (int) Math.floor((x + radius) / TILE_SIZE);
    int top = (int) Math.floor((y - radius) / TILE_SIZE),
        bottom = (int) Math.floor((y + radius) / TILE_SIZE);
    for (int r = top; r <= bottom; r++)
      for (int c = left; c <= right; c++) if (!walkable(r, c)) return false;
    return true;
  }

  public boolean lineOfSight(double ax, double ay, double bx, double by) {
    double distance = Math.hypot(bx - ax, by - ay);
    int steps = Math.max(1, (int) Math.ceil(distance / 4));
    for (int i = 0; i <= steps; i++) {
      double t = (double) i / steps;
      if (!walkable(
          (int) Math.floor((ay + (by - ay) * t) / TILE_SIZE),
          (int) Math.floor((ax + (bx - ax) * t) / TILE_SIZE))) return false;
    }
    return true;
  }

  public static double center(int tile) {
    return tile * TILE_SIZE + TILE_SIZE / 2.0;
  }
}

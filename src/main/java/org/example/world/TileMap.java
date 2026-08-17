package org.example.world;

public class TileMap {

    public static final int WIDTH = 15;
    public static final int HEIGHT = 15;
    public static final int TILE_SIZE = 40;

    private final Tile[][] tiles;

    public TileMap() {

        tiles = new Tile[HEIGHT][WIDTH];

        createMap();

    }

    private void createMap() {

        int[][] map = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1},
                {1, 0, 0, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 1},
                {1, 0, 0, 1, 2, 2, 2, 0, 1, 0, 1, 0, 0, 0, 1},
                {1, 0, 0, 1, 2, 2, 2, 0, 1, 0, 0, 3, 3, 0, 1},
                {1, 0, 0, 1, 1, 1, 1, 1, 1, 0, 1, 3, 3, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };

        for (int row = 0; row < HEIGHT; row++) {

            for (int col = 0; col < WIDTH; col++) {

                if (map[row][col] == 1) {

                    tiles[row][col] =
                            new Tile(Tile.Type.WALL, false, 0);

                } else if (map[row][col] == 2) {

                    tiles[row][col] =
                            new Tile(Tile.Type.MUD, true, 2);

                } else if (map[row][col] == 3) {

                    tiles[row][col] =
                            new Tile(Tile.Type.CONTAMINATED, true, 3);

                } else {

                    tiles[row][col] =
                            new Tile(Tile.Type.FLOOR, true, 1);

                }

            }

        }

    }

    public Tile getTile(int row, int col) {

        return tiles[row][col];

    }

}
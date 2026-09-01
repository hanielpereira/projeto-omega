package org.example.ai;

import org.example.world.Tile;
import org.example.world.TileMap;

import java.util.*;

public class AStarPathfinder {

    private final TileMap tileMap;

    public AStarPathfinder(TileMap tileMap) {
        this.tileMap = tileMap;
    }

    public List<int[]> findPath(
            int startRow,
            int startCol,
            int goalRow,
            int goalCol) {

        PriorityQueue<Node> openList =
                new PriorityQueue<>(Comparator.comparingDouble(Node::getF));

        Set<String> closedList = new HashSet<>();

        Node start = new Node(
                startRow,
                startCol,
                0,
                heuristic(startRow, startCol, goalRow, goalCol),
                null
        );

        openList.add(start);

        while (!openList.isEmpty()) {

            Node current = openList.poll();

            String currentKey =
                    current.row + "," + current.col;

            if (closedList.contains(currentKey)) {
                continue;
            }

            closedList.add(currentKey);

            if (current.row == goalRow &&
                    current.col == goalCol) {

                return reconstructPath(current);
            }

            for (int[] direction : getDirections()) {

                int nextRow = current.row + direction[0];
                int nextCol = current.col + direction[1];

                if (!isValidTile(nextRow, nextCol)) {
                    continue;
                }

                String nextKey =
                        nextRow + "," + nextCol;

                if (closedList.contains(nextKey)) {
                    continue;
                }

                double g =
                        current.g +
                                tileMap.getTile(nextRow, nextCol)
                                        .getMovementCost();

                double h =
                        heuristic(
                                nextRow,
                                nextCol,
                                goalRow,
                                goalCol
                        );

                Node next = new Node(
                        nextRow,
                        nextCol,
                        g,
                        h,
                        current
                );

                openList.add(next);
            }
        }

        return Collections.emptyList();
    }

    private boolean isValidTile(int row, int col) {

        if (row < 0 || row >= TileMap.HEIGHT ||
                col < 0 || col >= TileMap.WIDTH) {

            return false;
        }

        Tile tile = tileMap.getTile(row, col);

        return tile.isWalkable();
    }

    private double heuristic(
            int row,
            int col,
            int goalRow,
            int goalCol) {

        return Math.abs(row - goalRow)
                + Math.abs(col - goalCol);
    }

    private int[][] getDirections() {

        return new int[][]{
                {-1, 0},
                {1, 0},
                {0, -1},
                {0, 1}
        };
    }

    private List<int[]> reconstructPath(Node node) {

        List<int[]> path = new ArrayList<>();

        while (node != null) {

            path.add(new int[]{
                    node.row,
                    node.col
            });

            node = node.parent;
        }

        Collections.reverse(path);

        return path;
    }

    private static class Node {

        private final int row;
        private final int col;

        private final double g;
        private final double h;

        private final Node parent;

        public Node(
                int row,
                int col,
                double g,
                double h,
                Node parent) {

            this.row = row;
            this.col = col;

            this.g = g;
            this.h = h;

            this.parent = parent;
        }

        public double getF() {
            return g + h;
        }
    }
}
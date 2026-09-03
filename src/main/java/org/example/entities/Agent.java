package org.example.entities;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.example.ai.AStarPathfinder;
import org.example.world.TileMap;

import java.util.List;

public class Agent {

    private double x;
    private double y;

    private final double size;
    private final double speed;

    private final TileMap tileMap;
    private final AStarPathfinder pathfinder;

    private List<int[]> path;

    private int currentPathIndex;

    public Agent(
            int startRow,
            int startCol,
            int goalRow,
            int goalCol,
            TileMap tileMap) {

        this.tileMap = tileMap;
        this.pathfinder = new AStarPathfinder(tileMap);

        this.size = 24;
        this.speed = 100;

        this.x = startCol * TileMap.TILE_SIZE
                + (TileMap.TILE_SIZE - size) / 2.0;

        this.y = startRow * TileMap.TILE_SIZE
                + (TileMap.TILE_SIZE - size) / 2.0;

        calculatePath(
                startRow,
                startCol,
                goalRow,
                goalCol
        );
    }

    public void calculatePath(
            int startRow,
            int startCol,
            int goalRow,
            int goalCol) {

        path = pathfinder.findPath(
                startRow,
                startCol,
                goalRow,
                goalCol
        );

        currentPathIndex = 1;
    }

    public void setDestination(int goalRow, int goalCol) {

        int currentRow =
                (int) (y / TileMap.TILE_SIZE);

        int currentCol =
                (int) (x / TileMap.TILE_SIZE);

        calculatePath(
                currentRow,
                currentCol,
                goalRow,
                goalCol
        );
    }

    public void update(double deltaTime) {

        if (path.isEmpty() ||
                currentPathIndex >= path.size()) {
            return;
        }

        int[] targetTile =
                path.get(currentPathIndex);

        double targetX =
                targetTile[1] * TileMap.TILE_SIZE
                        + (TileMap.TILE_SIZE - size) / 2.0;

        double targetY =
                targetTile[0] * TileMap.TILE_SIZE
                        + (TileMap.TILE_SIZE - size) / 2.0;

        double dx = targetX - x;
        double dy = targetY - y;

        double distance =
                Math.sqrt(dx * dx + dy * dy);

        if (distance < 2) {

            x = targetX;
            y = targetY;

            currentPathIndex++;

            return;
        }

        x += (dx / distance) * speed * deltaTime;
        y += (dy / distance) * speed * deltaTime;
    }

    public void render(GraphicsContext gc) {

        gc.setFill(Color.RED);

        gc.fillRect(
                x,
                y,
                size,
                size
        );
    }

    public List<int[]> getPath() {
        return path;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
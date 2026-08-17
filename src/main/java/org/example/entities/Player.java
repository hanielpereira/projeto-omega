package org.example.entities;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.example.input.Keyboard;
import org.example.world.Tile;
import org.example.world.TileMap;

public class Player {

    private double x;
    private double y;

    private final double width;
    private final double height;

    private final Keyboard keyboard;
    private final TileMap tileMap;

    public Player(double x, double y, double width, double height,
                  Keyboard keyboard, TileMap tileMap) {

        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;

        this.keyboard = keyboard;
        this.tileMap = tileMap;
    }

    public void update(double deltaTime) {

        double speed = 250;

        double movementX = 0;
        double movementY = 0;

        if (keyboard.isUp()) {
            movementY -= speed * deltaTime;
        }

        if (keyboard.isDown()) {
            movementY += speed * deltaTime;
        }

        if (keyboard.isLeft()) {
            movementX -= speed * deltaTime;
        }

        if (keyboard.isRight()) {
            movementX += speed * deltaTime;
        }

        if (canMoveTo(x + movementX, y)) {
            x += movementX;
        }

        if (canMoveTo(x, y + movementY)) {
            y += movementY;
        }
    }

    private boolean canMoveTo(double nextX, double nextY) {

        if (nextX < 0 || nextY < 0) {
            return false;
        }

        if (nextX + width > TileMap.WIDTH * TileMap.TILE_SIZE) {
            return false;
        }

        if (nextY + height > TileMap.HEIGHT * TileMap.TILE_SIZE) {
            return false;
        }

        int leftTile = (int) (nextX / TileMap.TILE_SIZE);
        int rightTile =
                (int) ((nextX + width - 1) / TileMap.TILE_SIZE);

        int topTile = (int) (nextY / TileMap.TILE_SIZE);
        int bottomTile =
                (int) ((nextY + height - 1) / TileMap.TILE_SIZE);

        for (int row = topTile; row <= bottomTile; row++) {

            for (int col = leftTile; col <= rightTile; col++) {

                Tile tile = tileMap.getTile(row, col);

                if (!tile.isWalkable()) {
                    return false;
                }
            }
        }

        return true;
    }

    public void render(GraphicsContext gc) {

        gc.setFill(Color.BLUE);
        gc.fillRect(x, y, width, height);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
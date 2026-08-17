package org.example;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import org.example.entities.Player;
import org.example.input.Keyboard;
import javafx.scene.paint.Color;
import org.example.world.Tile;
import org.example.world.TileMap;

public class GameLoop {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Player player;
    private final Keyboard keyboard;
    private final TileMap tileMap;

    private long lastTime = 0;

    public GameLoop(Pane root, Scene scene) {

        canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();

        root.getChildren().add(canvas);

        tileMap = new TileMap();

        keyboard = new Keyboard(scene);

        player = new Player(100, 100, 30, 30, keyboard, tileMap);

        startGameLoop();

    }

    private void startGameLoop() {

        AnimationTimer timer = new AnimationTimer() {

            @Override
            public void handle(long now) {

                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                update(deltaTime);
                render();

            }

        };

        timer.start();

    }

    private void update(double deltaTime) {

        player.update(deltaTime);

    }

    private void render() {

        gc.clearRect(0, 0, 800, 600);

        renderMap();

        player.render(gc);

        gc.fillText("X: " + (int) player.getX(), 10, 20);
        gc.fillText("Y: " + (int) player.getY(), 10, 40);

    }

    private void renderMap() {

        for (int row = 0; row < TileMap.HEIGHT; row++) {

            for (int col = 0; col < TileMap.WIDTH; col++) {

                Tile tile = tileMap.getTile(row, col);

                if (tile.getType() == Tile.Type.WALL) {

                    gc.setFill(Color.DARKGRAY);

                } else if (tile.getType() == Tile.Type.MUD) {

                    gc.setFill(Color.BURLYWOOD);

                } else if (tile.getType() == Tile.Type.CONTAMINATED) {

                    gc.setFill(Color.LIGHTGREEN);

                } else {

                    gc.setFill(Color.LIGHTGRAY);

                }

                gc.fillRect(
                        col * TileMap.TILE_SIZE,
                        row * TileMap.TILE_SIZE,
                        TileMap.TILE_SIZE,
                        TileMap.TILE_SIZE
                );

            }

        }

    }

}
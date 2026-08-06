package org.example;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import org.example.entities.Player;
import org.example.input.Keyboard;

public class GameLoop {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Player player;
    private final Keyboard keyboard;

    private long lastTime = 0;

    public GameLoop(Pane root, Scene scene) {

        canvas = new Canvas(800, 600);
        gc = canvas.getGraphicsContext2D();

        root.getChildren().add(canvas);

        keyboard = new Keyboard(scene);

        player = new Player(100, 100, 100, 100, keyboard);

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

        player.render(gc);

        gc.fillText("X: " + (int) player.getX(), 10, 20);
        gc.fillText("Y: " + (int) player.getY(), 10, 40);

    }

}
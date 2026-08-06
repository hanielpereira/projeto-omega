package org.example.entities;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.example.input.Keyboard;

public class Player {

    private double x;
    private double y;

    private final double width;
    private final double height;

    private final Keyboard keyboard;

    public Player(double x, double y, double width, double height, Keyboard keyboard) {

        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;

        this.keyboard = keyboard;

    }

    public void update(double deltaTime) {

        double speed = 250;

        if (keyboard.isUp()) {
            y -= speed * deltaTime;
        }

        if (keyboard.isDown()) {
            y += speed * deltaTime;
        }

        if (keyboard.isLeft()) {
            x -= speed * deltaTime;
        }

        if (keyboard.isRight()) {
            x += speed * deltaTime;
        }

        if (x < 0) {
            x = 0;
        }

        if (y < 0) {
            y = 0;
        }

        if (x > 800 - width) {
            x = 800 - width;
        }

        if (y > 600 - height) {
            y = 600 - height;
        }

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
package org.example.input;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

public class Keyboard {

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;

    public Keyboard(Scene scene) {

        scene.setOnKeyPressed(event -> {

            if (event.getCode() == KeyCode.W) up = true;
            if (event.getCode() == KeyCode.S) down = true;
            if (event.getCode() == KeyCode.A) left = true;
            if (event.getCode() == KeyCode.D) right = true;

        });

        scene.setOnKeyReleased(event -> {

            if (event.getCode() == KeyCode.W) up = false;
            if (event.getCode() == KeyCode.S) down = false;
            if (event.getCode() == KeyCode.A) left = false;
            if (event.getCode() == KeyCode.D) right = false;

        });

    }

    public boolean isUp() {
        return up;
    }

    public boolean isDown() {
        return down;
    }

    public boolean isLeft() {
        return left;
    }

    public boolean isRight() {
        return right;
    }

}
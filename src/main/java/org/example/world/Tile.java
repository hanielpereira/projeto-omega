package org.example.world;

public class Tile {

    public enum Type {
        FLOOR,
        WALL,
        MUD,
        CONTAMINATED
    }

    private final Type type;
    private final boolean walkable;
    private final double movementCost;

    public Tile(Type type, boolean walkable, double movementCost) {

        this.type = type;
        this.walkable = walkable;
        this.movementCost = movementCost;

    }

    public Type getType() {
        return type;
    }

    public boolean isWalkable() {
        return walkable;
    }

    public double getMovementCost() {
        return movementCost;
    }

}
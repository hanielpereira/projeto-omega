package org.example.entities;

import org.example.world.Tile;
import org.example.world.TileMap;

public final class Player {
  public double x = TileMap.center(4), y = TileMap.center(13);
  public double health = 100, stamina = 100, facing = Math.PI / 2, animation, invulnerable;
  public boolean moving, sprinting, sneaking;
  private boolean exhausted;
  public double dodgeTime, dodgeCooldown;
  private double dodgeX, dodgeY;
  public static final double RADIUS = 8;

  public void update(double dt, double dx, double dy, boolean sprint, boolean sneak, TileMap map) {
    invulnerable = Math.max(0, invulnerable - dt);
    dodgeCooldown = Math.max(0, dodgeCooldown - dt);
    if (dodgeTime > 0) {
      dodgeTime = Math.max(0, dodgeTime - dt);
      moving = true;
      sprinting = false;
      for (int i = 0; i < 3; i++) {
        double nx = x + dodgeX * 290 * dt / 3, ny = y + dodgeY * 290 * dt / 3;
        if (map.canOccupy(nx, y, RADIUS)) x = nx;
        if (map.canOccupy(x, ny, RADIUS)) y = ny;
      }
      animation += dt * 20;
      return;
    }
    double length = Math.hypot(dx, dy);
    moving = length > 0;
    sneaking = sneak;
    if (stamina <= 2) exhausted = true;
    if (stamina >= 22) exhausted = false;
    sprinting = moving && sprint && !sneak && !exhausted;
    double speed = sneak ? 55 : sprinting ? 150 : 95;
    Tile.Type ground = map.getTile((int) (y / 32), (int) (x / 32)).getType();
    if (ground == Tile.Type.MUD) speed *= .65;
    if (ground == Tile.Type.CONTAMINATED) {
      health = Math.max(0, health - 8 * dt);
      speed *= .8;
    }
    stamina = Math.max(0, Math.min(100, stamina + (sprinting ? -25 : 16) * dt));
    if (!moving) return;
    dx /= length;
    dy /= length;
    facing = Math.atan2(dy, dx);
    double nx = x + dx * speed * dt, ny = y + dy * speed * dt;
    if (map.canOccupy(nx, y, RADIUS)) x = nx;
    if (map.canOccupy(x, ny, RADIUS)) y = ny;
    animation += dt * (sprinting ? 13 : 8);
  }

  public boolean dodge(double dx, double dy) {
    if (dodgeCooldown > 0 || stamina < 24) return false;
    double length = Math.hypot(dx, dy);
    if (length == 0) {
      dx = Math.cos(facing);
      dy = Math.sin(facing);
      length = 1;
    }
    dodgeX = dx / length;
    dodgeY = dy / length;
    facing = Math.atan2(dodgeY, dodgeX);
    dodgeTime = .22;
    dodgeCooldown = 1.05;
    invulnerable = .27;
    stamina -= 24;
    return true;
  }

  public void damage(double value) {
    if (invulnerable > 0) return;
    health = Math.max(0, health - value);
    invulnerable = 1.2;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public int row() {
    return (int) (y / 32);
  }

  public int col() {
    return (int) (x / 32);
  }
}

package org.example.entities;

import java.util.*;
import org.example.ai.*;
import org.example.world.TileMap;

/** Sensors -> expiring evidence -> FSM -> dynamic A*. Each state owns its lifecycle. */
public final class Agent {
  public enum Kind {
    SENTINEL,
    DRONE,
    SPECIMEN
  }

  public enum State {
    PATROL,
    INVESTIGATE,
    CHASE,
    ATTACK,
    SEARCH,
    RETURN,
    STUNNED
  }

  public final Kind kind;
  public int health;
  public double hurtTime;
  public State state = State.PATROL;
  public final AgentMemory knowledge = new AgentMemory();
  public final DynamicNavigator navigation;
  public boolean radioEnabled = true;
  public double x, y, facing, animation, suspicion, radioVisual, attackVisual;
  public String intent = "patrulha";
  private final TileMap map;
  private final int[][] patrol;
  private final RadioNetwork radio;
  private final EnumMap<State, Behavior> behaviors = new EnumMap<>(State.class);
  private int waypoint = 1, searchAttempt;
  private double searchStarted;
  private double time,
      stun,
      searchDeadline,
      inspectTimer,
      radioCooldown,
      attackTimer,
      attackCooldown;
  private double lastX, lastY, searchX, searchY;
  private boolean visible;
  private long localSequence;
  private AgentMemory.Evidence direct;

  private interface Behavior {
    default void enter() {}

    void update(double dt, Player p);

    default void exit() {}
  }

  public Agent(Kind kind, TileMap map, int[][] patrol) {
    this(kind, map, patrol, new RadioNetwork());
  }

  public Agent(Kind kind, TileMap map, int[][] patrol, RadioNetwork radio) {
    this.kind = kind;
    health = kind == Kind.DRONE ? 2 : kind == Kind.SPECIMEN ? 4 : 3;
    this.map = map;
    this.patrol = patrol;
    this.radio = radio;
    navigation = new DynamicNavigator(map);
    x = TileMap.center(patrol[0][1]);
    y = TileMap.center(patrol[0][0]);
    waypoint %= patrol.length;
    facing = kind == Kind.SPECIMEN ? Math.PI : 0;
    radioEnabled = kind != Kind.SPECIMEN;
    behaviors.put(State.PATROL, new Patrol());
    behaviors.put(State.INVESTIGATE, new Investigate());
    behaviors.put(State.CHASE, new Chase());
    behaviors.put(State.ATTACK, new Attack());
    behaviors.put(State.SEARCH, new Search());
    behaviors.put(State.RETURN, new Return());
    behaviors.put(State.STUNNED, new Stunned());
    behaviors.get(state).enter();
  }

  public void update(double dt, Player p, double nx, double ny, double radius, boolean noise) {
    time += dt;
    hurtTime = Math.max(0, hurtTime - dt);
    radioCooldown -= dt;
    attackCooldown = Math.max(0, attackCooldown - dt);
    radioVisual = Math.max(0, radioVisual - dt);
    attackVisual = Math.max(0, attackVisual - dt);
    if (state != State.STUNNED) perceive(dt, p, nx, ny, radius, noise);
    behaviors.get(state).update(dt, p);
    if (state != State.STUNNED && state != State.ATTACK) {
      navigation.tick(dt, x, y);
      double speed =
          switch (state) {
            case CHASE -> kind == Kind.SPECIMEN ? 118 : kind == Kind.DRONE ? 110 : 99;
            case RETURN, PATROL -> kind == Kind.DRONE ? 65 : kind == Kind.SPECIMEN ? 47 : 48;
            default -> kind == Kind.DRONE ? 78 : 62;
          };
      double oldX = x, oldY = y;
      double[] moved = navigation.move(x, y, speed, dt);
      x = moved[0];
      y = moved[1];
      if (!Double.isNaN(moved[2])) facing = moved[2];
      if (Math.hypot(x - oldX, y - oldY) > .01) animation += dt * 8;
    }
  }

  private void perceive(double dt, Player p, double nx, double ny, double radius, boolean noise) {
    visible = canSee(p);
    suspicion = Math.max(0, Math.min(1, suspicion + (visible ? dt * 2.4 : -dt * .2)));
    if (visible) {
      lastX = p.x;
      lastY = p.y;
      direct =
          new AgentMemory.Evidence(
              radio.nextEvent(),
              lastX,
              lastY,
              time,
              .95,
              AgentMemory.Source.VISION,
              kind.name(),
              time + 9);
      knowledge.remember(direct, time);
      if (suspicion >= .52 && radioCooldown <= 0 && radioEnabled) {
        radio.transmit(this, direct, time);
        radioCooldown = 3.5;
      }
    } else {
      double distance = Math.hypot(p.x - x, p.y - y),
          hearing = kind == Kind.SPECIMEN ? 1.65 : kind == Kind.DRONE ? .8 : 1;
      boolean step =
          p.moving
              && !p.sneaking
              && distance < (p.sprinting ? 165 : 48) * hearing
              && (distance < 50 || map.lineOfSight(x, y, p.x, p.y));
      boolean lure = noise && Math.hypot(nx - x, ny - y) < radius * hearing;
      if ((step || lure) && state != State.CHASE && state != State.ATTACK) {
        // One sound per local interval, not sixty independent clues per second.
        var old = knowledge.best(time);
        if (old == null || time - old.time() > .8) {
          lastX = lure ? nx : p.x;
          lastY = lure ? ny : p.y;
          knowledge.remember(
              new AgentMemory.Evidence(
                  -(++localSequence),
                  lastX,
                  lastY,
                  time,
                  .55,
                  AgentMemory.Source.SOUND,
                  "ruído",
                  time + 6),
              time);
        }
      }
    }
  }

  /** Common priorities: current sight > remembered evidence > normal state completion. */
  private boolean react(Player p) {
    if (visible && suspicion >= .52) {
      if (Math.hypot(p.x - x, p.y - y) < 29 && attackCooldown <= 0) {
        change(State.ATTACK);
        return true;
      }
      if (state != State.CHASE && state != State.ATTACK) {
        change(State.CHASE);
        return true;
      }
    }
    if (state == State.PATROL || state == State.RETURN || state == State.SEARCH) {
      var clue = knowledge.best(time);
      if (clue != null && (state != State.SEARCH || clue.time() > searchStarted + .15)) {
        lastX = clue.x();
        lastY = clue.y();
        change(State.INVESTIGATE);
        return true;
      }
    }
    return false;
  }

  private void change(State next) {
    if (state == next) return;
    behaviors.get(state).exit();
    radio.release(this);
    navigation.clear();
    state = next;
    behaviors.get(next).enter();
  }

  private void go(double tx, double ty) {
    navigation.target((int) (ty / 32), (int) (tx / 32));
  }

  private boolean arrived(double tx, double ty) {
    return Math.hypot(tx - x, ty - y) < 7;
  }

  private final class Patrol implements Behavior {
    private double blockedSince;

    public void enter() {
      blockedSince = time;
      intent = "patrulha";
      go(TileMap.center(patrol[waypoint][1]), TileMap.center(patrol[waypoint][0]));
    }

    public void update(double dt, Player p) {
      if (react(p)) return;
      if (arrived(TileMap.center(patrol[waypoint][1]), TileMap.center(patrol[waypoint][0]))
          || (navigation.failed() && time - blockedSince > 2)) {
        waypoint =
            (waypoint + (kind == Kind.SPECIMEN && (int) time % 3 == 0 ? 2 : 1)) % patrol.length;
        enter();
      }
    }
  }

  private final class Investigate implements Behavior {
    private double deadline;

    public void enter() {
      deadline = time + 8;
      intent = "investigar pista";
      go(lastX, lastY);
    }

    public void update(double dt, Player p) {
      if (react(p)) return;
      var clue = knowledge.best(time);
      if (clue != null) {
        lastX = clue.x();
        lastY = clue.y();
        go(lastX, lastY);
      }
      if (arrived(lastX, lastY) || time > deadline || navigation.failed()) change(State.SEARCH);
    }
  }

  private final class Chase implements Behavior {
    private double lostAt;

    public void enter() {
      lostAt = time;
      intent = "contato visual";
      go(lastX, lastY);
    }

    public void update(double dt, Player p) {
      if (react(p)) return;
      if (visible) {
        lostAt = time;
        go(lastX, lastY);
      } else if (arrived(lastX, lastY) || time - lostAt > (kind == Kind.DRONE ? .9 : 1.8))
        change(State.SEARCH);
    }
  }

  private final class Attack implements Behavior {
    public void enter() {
      attackTimer = kind == Kind.SPECIMEN ? .48 : .65;
      attackVisual = attackTimer;
      intent = "ataque preparado";
    }

    public void update(double dt, Player p) {
      attackTimer -= dt;
      facing = Math.atan2(lastY - y, lastX - x);
      if (attackTimer > 0) return;
      if (Math.hypot(p.x - x, p.y - y) < 36 && map.lineOfSight(x, y, p.x, p.y))
        p.damage(kind == Kind.SPECIMEN ? 24 : 16);
      attackCooldown = 1.6;
      change(visible ? State.CHASE : State.SEARCH);
    }
  }

  private final class Search implements Behavior {
    public void enter() {
      searchStarted = time;
      searchDeadline = time + (kind == Kind.DRONE ? 4 : 7);
      searchAttempt = 0;
      inspectTimer = .8;
      intent = "varrer setor";
      pick();
    }

    private void pick() {
      int[] point = radio.reserveSearch(Agent.this, map, lastX, lastY, time, searchAttempt++);
      if (point == null) {
        searchX = x;
        searchY = y;
      } else {
        searchX = TileMap.center(point[1]);
        searchY = TileMap.center(point[0]);
      }
      go(searchX, searchY);
    }

    public void update(double dt, Player p) {
      if (react(p)) return;
      if (time > searchDeadline) {
        knowledge.inspected();
        change(State.RETURN);
        return;
      }
      if (arrived(searchX, searchY) || navigation.failed()) {
        inspectTimer -= dt;
        facing += dt * 1.7;
        if (inspectTimer <= 0) {
          radio.release(Agent.this);
          pick();
          inspectTimer = 1;
        }
      }
    }

    public void exit() {
      radio.release(Agent.this);
    }
  }

  private final class Return implements Behavior {
    public void enter() {
      intent = "retomar posto";
      go(TileMap.center(patrol[waypoint][1]), TileMap.center(patrol[waypoint][0]));
    }

    public void update(double dt, Player p) {
      if (react(p)) return;
      if (arrived(TileMap.center(patrol[waypoint][1]), TileMap.center(patrol[waypoint][0]))
          || navigation.failed()) change(State.PATROL);
    }
  }

  private final class Stunned implements Behavior {
    public void enter() {
      intent = "interrompido";
      suspicion = 0;
    }

    public void update(double dt, Player p) {
      stun -= dt;
      if (stun <= 0) {
        lastX = x;
        lastY = y;
        change(State.SEARCH);
      }
    }
  }

  public boolean receive(AgentMemory.Evidence clue, double now) {
    if (!radioEnabled || kind == Kind.SPECIMEN || state == State.STUNNED) return false;
    return knowledge.remember(clue, now);
  }

  public boolean canSee(Player p) {
    double distance = Math.hypot(p.x - x, p.y - y);
    double angle =
        Math.atan2(
            Math.sin(Math.atan2(p.y - y, p.x - x) - facing),
            Math.cos(Math.atan2(p.y - y, p.x - x) - facing));
    return distance < visionRange() * (p.sneaking ? .58 : 1)
        && (Math.abs(angle) < visionAngle() || distance < 26)
        && map.lineOfSight(x, y, p.x, p.y);
  }

  public double visionRange() {
    return kind == Kind.SPECIMEN ? 130 : kind == Kind.DRONE ? 180 : 215;
  }

  public double visionAngle() {
    return kind == Kind.SPECIMEN ? 1.05 : kind == Kind.DRONE ? .85 : .56;
  }

  public void setDestination(int row, int col) {
    navigation.target(row, col);
  }

  public void stun(double seconds) {
    stun = seconds;
    change(State.STUNNED);
  }

  public List<int[]> getPath() {
    return navigation.path();
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }
}

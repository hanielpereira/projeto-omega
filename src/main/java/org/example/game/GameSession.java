package org.example.game;

import java.util.*;
import org.example.ai.RadioNetwork;
import org.example.entities.Agent;
import org.example.entities.Player;
import org.example.world.TileMap;

/** Simulation has no JavaFX dependency so rules and AI can be tested headlessly. */
public final class GameSession {
  public enum Result {
    PLAYING,
    ESCAPED,
    DEAD
  }

  public enum DeviceType {
    CORE,
    REACTOR,
    EXIT,
    MEDKIT,
    CONTROL,
    SUPPLY
  }

  public static final class Device {
    public final int row, col;
    public final String name;
    public final DeviceType type;
    public boolean active;

    public Device(int row, int col, String name, DeviceType type) {
      this.row = row;
      this.col = col;
      this.name = name;
      this.type = type;
    }

    public double x() {
      return TileMap.center(col);
    }

    public double y() {
      return TileMap.center(row);
    }
  }

  public record Input(
      double dx,
      double dy,
      boolean sprint,
      boolean sneak,
      boolean interact,
      boolean lure,
      boolean pulse) {
    public static Input idle() {
      return new Input(0, 0, false, false, false, false, false);
    }
  }

  public final TileMap map = new TileMap();
  public final RadioNetwork radio = new RadioNetwork();
  public final VisualEffects fx = new VisualEffects();
  public final Combat combat = new Combat();
  public final FacilityEvents events = new FacilityEvents();

  public static final class Door {
    public final int col;
    public boolean closed;

    Door(int col) {
      this.col = col;
    }
  }

  public final List<Door> doors = List.of(new Door(11), new Door(24));
  public boolean showMap, centralAccess;
  public int journalOffset;
  public String controller = "";
  public String audioStatus = "";
  public double musicVolume = .72, sfxVolume = .85;
  private double ambienceTimer, interactionSound;
  private int radioCount;
  public final Player player = new Player();
  public final List<Agent> agents = new ArrayList<>();
  public final List<Device> devices = new ArrayList<>();
  public final List<String> logs = new ArrayList<>();

  public record SpatialSound(String name, double x, double y) {}

  public final Queue<SpatialSound> spatialSounds = new ArrayDeque<>();
  public final Queue<String> sounds = new ArrayDeque<>();
  public Result result = Result.PLAYING;
  public double elapsed, progress, pulseCooldown, pulseVisual, noiseTime, noiseX, noiseY, toastTime;
  public String toast =
      "INVESTIGAÇÃO · Recolha os registros da equipe e restabeleça o acesso ao controle central.";
  public int cores, lures = 3, hits;
  public boolean reactor, alert;
  public Device nearby;
  private Device interacting;
  private double footstep;
  public double noiseRadius = 230;

  public GameSession() {
    devices.add(new Device(5, 6, "Arquivo neural", DeviceType.CORE));
    devices.add(new Device(5, 30, "Controle de contenção", DeviceType.CORE));
    devices.add(new Device(21, 32, "Banco de amostras", DeviceType.CORE));
    devices.add(new Device(21, 17, "Reator auxiliar", DeviceType.REACTOR));
    devices.add(new Device(13, 33, "Elevador de evacuação", DeviceType.EXIT));
    devices.add(new Device(8, 9, "Kit médico", DeviceType.MEDKIT));
    devices.add(new Device(18, 21, "Kit médico", DeviceType.MEDKIT));
    agents.add(
        new Agent(
            Agent.Kind.SENTINEL, map, new int[][] {{6, 14}, {6, 22}, {12, 22}, {12, 14}}, radio));
    agents.add(
        new Agent(Agent.Kind.DRONE, map, new int[][] {{2, 27}, {2, 33}, {8, 33}, {8, 27}}, radio));
    agents.add(
        new Agent(
            Agent.Kind.SPECIMEN, map, new int[][] {{23, 3}, {18, 3}, {18, 9}, {23, 9}}, radio));
    agents.add(
        new Agent(
            Agent.Kind.SPECIMEN, map, new int[][] {{23, 26}, {23, 33}, {18, 33}, {18, 26}}, radio));
    agents.add(
        new Agent(
            Agent.Kind.DRONE, map, new int[][] {{12, 28}, {14, 32}, {14, 26}, {12, 32}}, radio));
    devices.add(new Device(5, 18, "Controle central", DeviceType.CONTROL));
    devices.add(new Device(13, 6, "Suprimentos de campo", DeviceType.SUPPLY));
    devices.add(new Device(8, 27, "Munição de segurança", DeviceType.SUPPLY));
    devices.add(new Device(23, 16, "Depósito de munição", DeviceType.SUPPLY));
    agents.add(
        new Agent(
            Agent.Kind.SENTINEL, map, new int[][] {{7, 8}, {5, 9}, {2, 9}, {2, 2}, {8, 2}}, radio));
    logs.add("23:17 / Agente 07. Entrada pela ala médica. Sem contato com a equipe de pesquisa.");
    toastTime = 7;
  }

  public void update(double dt, Input input) {
    if (result != Result.PLAYING) return;
    dt = Math.min(.04, Math.max(0, dt));
    elapsed += dt;
    fx.update(dt);
    combat.update(this, dt);
    events.update(this, dt);
    map.beginNavigationFrame();
    radio.update(elapsed, agents);
    interactionSound = Math.max(0, interactionSound - dt);
    ambienceTimer -= dt;
    if (ambienceTimer <= 0) {
      ambienceTimer = 5.5 + Math.sin(elapsed) * 1.2;
      fx.burst(player.x + Math.cos(elapsed) * 170, player.y - 100, VisualEffects.Kind.DUST, 12);
      spatialSounds.add(new SpatialSound("drip", player.x + 140, player.y - 100));
    }
    toastTime = Math.max(0, toastTime - dt);
    noiseTime = Math.max(0, noiseTime - dt);
    pulseCooldown = Math.max(0, pulseCooldown - dt);
    pulseVisual = Math.max(0, pulseVisual - dt);
    player.update(dt, input.dx, input.dy, input.sprint, input.sneak, map);
    if (player.dodgeTime > 0) {
      fx.burst(player.x, player.y + 6, VisualEffects.Kind.DUST, 2);
      fx.ghost(player.x, player.y, player.facing);
    }
    if (input.lure) throwLure();
    if (input.pulse && pulseCooldown <= 0) {
      pulseCooldown = 9;
      pulseVisual = .55;
      sounds.add("pulse");
      fx.shake = 3;
      fx.flash = .2;
      fx.burst(player.x, player.y, VisualEffects.Kind.EMP, 65);
      for (Agent a : agents)
        if (Math.hypot(a.x - player.x, a.y - player.y) < 115
            && map.lineOfSight(a.x, a.y, player.x, player.y)) a.stun(3.5);
      message("PULSO EMP · Unidades próximas desativadas por alguns segundos.");
    }
    footstep -= dt;
    if (player.moving && footstep <= 0) {
      if (player.sprinting) fx.burst(player.x, player.y + 6, VisualEffects.Kind.DUST, 4);
      footstep = player.sprinting ? .25 : player.sneaking ? .65 : .4;
      sounds.add(player.sneaking ? "sneak" : "step");
    }
    alert = false;
    double previous = player.health;
    for (Agent a : agents) {
      var prior = a.state;
      a.update(dt, player, noiseX, noiseY, noiseRadius, noiseTime > 0);
      if (a.state != prior) {
        if (a.state == Agent.State.CHASE)
          spatialSounds.add(
              new SpatialSound(a.kind == Agent.Kind.SPECIMEN ? "roar" : "alert", a.x, a.y));
        if (a.state == Agent.State.ATTACK)
          fx.burst(
              a.x,
              a.y,
              a.kind == Agent.Kind.SPECIMEN ? VisualEffects.Kind.BLOOD : VisualEffects.Kind.SPARK,
              6);
      }
      if (a.state == Agent.State.STUNNED && ((int) (elapsed * 12) % 3 == 0))
        fx.burst(a.x, a.y, VisualEffects.Kind.SPARK, 1);
      alert |= a.state == Agent.State.CHASE || a.state == Agent.State.ATTACK;
    }
    if (player.health < previous) {
      sounds.add("hit");
      hits++;
      fx.shake = 5;
      fx.burst(player.x, player.y, VisualEffects.Kind.BLOOD, 20);
    }
    if (radio.delivered > radioCount) {
      sounds.add("radio");
      radioCount = radio.delivered;
    }
    nearby = null;
    double closest = 48;
    for (Device d : devices) {
      if (d.active) continue;
      double dist = Math.hypot(d.x() - player.x, d.y() - player.y);
      if (dist < closest && map.lineOfSight(player.x, player.y, d.x(), d.y())) {
        nearby = d;
        closest = dist;
      }
    }
    if (input.interact && nearby != null && usable(nearby)) {
      if (interacting != nearby) {
        interacting = nearby;
        progress = 0;
      }
      if (interactionSound <= 0) {
        sounds.add("interact");
        interactionSound = .45;
        fx.burst(nearby.x(), nearby.y() - 8, VisualEffects.Kind.DATA, 4);
      }
      progress += dt / (nearby.type == DeviceType.MEDKIT ? .45 : 1.5);
      if (progress >= 1) {
        activate(nearby);
        progress = 0;
        interacting = null;
      }
    } else {
      progress = 0;
      interacting = null;
    }
    if (player.health <= 0) {
      result = Result.DEAD;
      sounds.add("lose");
    }
  }

  public boolean usable(Device d) {
    return switch (d.type) {
      case REACTOR -> cores == 3;
      case EXIT -> reactor && centralAccess && (events.arrival < 0 || events.liftReady);
      case CONTROL -> reactor && cores == 3;
      case MEDKIT -> player.health < 100;
      case SUPPLY -> combat.ammo < 14;
      default -> true;
    };
  }

  public String prompt() {
    if (nearby == null) return "";
    if (!usable(nearby))
      return switch (nearby.type) {
        case REACTOR -> "Acesso bloqueado · recupere os 3 núcleos";
        case EXIT ->
            events.arrival > 0
                ? "Elevador chegando · " + (int) Math.ceil(events.arrival) + "s"
                : !reactor
                    ? "Sem energia · ative o reator auxiliar"
                    : "Investigue o controle central antes de sair";
        case CONTROL -> "Sem acesso · reúna os registros e ative o reator";
        case MEDKIT -> "Sua integridade já está completa";
        case SUPPLY -> "Munição completa";
        default -> "";
      };
    if (nearby.type == DeviceType.EXIT && events.arrival > 0)
      return "ELEVADOR EM TRÂNSITO · " + (int) Math.ceil(events.arrival) + "s";
    return "SEGURE E  /  " + nearby.name;
  }

  private void activate(Device d) {
    if (d.type == DeviceType.EXIT && !events.liftReady) {
      events.callLift(this);
      return;
    }
    d.active = true;
    sounds.add("terminal");
    fx.burst(d.x(), d.y(), VisualEffects.Kind.DATA, 35);
    switch (d.type) {
      case CORE -> {
        cores++;
        lures = Math.min(3, lures + 1);
        String log =
            switch (d.col) {
              case 6 ->
                  "ARQUIVO 01 / Os pesquisadores ligaram a rede neural ao sistema de contenção. O"
                      + " último teste começou às 03:30.";
              case 30 ->
                  "ARQUIVO 02 / As câmaras perderam energia às 03:41. Os sentinelas receberam ordem"
                      + " para impedir qualquer saída.";
              default ->
                  "ARQUIVO 03 / O reator auxiliar não está na rede. Ligue-o manualmente. Ainda há"
                      + " uma saída.";
            };
        logs.add(log);
        message(log);
        player.health = Math.min(100, player.health + 12);
        events.recovered(this, cores);
      }
      case REACTOR -> {
        reactor = true;
        message("Energia auxiliar restaurada. Investigue o controle central na ala norte.");
        logs.add(
            "SISTEMA / Controle central acessível. O registro do incidente ainda está na rede.");
      }
      case CONTROL -> {
        centralAccess = true;
        message(
            "INCIDENTE 03:41 · A rede bloqueou a evacuação para preservar o experimento. Leve os"
                + " registros ao elevador.");
        logs.add(
            "03:41 / A equipe tentou desligar a rede neural. Ômega interpretou a evacuação como"
                + " quebra de contenção e selou os setores de pesquisa.");
      }
      case EXIT -> {
        result = Result.ESCAPED;
        sounds.add("win");
      }
      case SUPPLY -> {
        combat.ammo = Math.min(14, combat.ammo + 6);
        lures = Math.min(3, lures + 1);
        message("Suprimentos recolhidos. Seis cartuchos e uma isca.");
      }
      case MEDKIT -> {
        player.health = Math.min(100, player.health + 45);
        message("KIT MÉDICO · Integridade recuperada.");
      }
    }
  }

  private void throwLure() {
    if (lures <= 0) {
      message("Sem iscas · recupere um núcleo para reabastecer.");
      return;
    }
    if (noiseTime > 0) {
      message("A isca anterior ainda está emitindo sinal.");
      return;
    }
    noiseX = player.x;
    noiseY = player.y;
    for (int d = 8; d <= 120; d += 8) {
      double nx = player.x + Math.cos(player.facing) * d,
          ny = player.y + Math.sin(player.facing) * d;
      if (!map.canOccupy(nx, ny, 4) || !map.lineOfSight(player.x, player.y, nx, ny)) break;
      noiseX = nx;
      noiseY = ny;
    }
    noiseTime = 4;
    noiseRadius = 230;
    lures--;
    sounds.add("lure");
    message("ISCA LANÇADA · Aproveite o ruído para passar despercebido.");
  }

  public Door nearbyDoor() {
    for (Door d : doors)
      if (Math.abs(player.x - TileMap.center(d.col)) < 54 && Math.abs(player.y - 416) < 66)
        return d;
    return null;
  }

  public void toggleDoor() {
    Door d = nearbyDoor();
    if (d == null) return;
    if (!d.closed) {
      boolean occupied =
          Math.abs(player.x - TileMap.center(d.col)) < 24 && player.y > 376 && player.y < 456;
      for (Agent a : agents)
        occupied |= Math.abs(a.x - TileMap.center(d.col)) < 24 && a.y > 376 && a.y < 456;
      if (occupied) {
        message("Passagem ocupada. Afaste-se da porta para fechá-la.");
        return;
      }
    }
    d.closed = !d.closed;
    map.block(12, d.col, d.closed);
    map.block(13, d.col, d.closed);
    sounds.add("door");
    fx.burst(TileMap.center(d.col), 416, VisualEffects.Kind.STEAM, 20);
    message(d.closed ? "Porta selada. Ruídos próximos ainda podem ser ouvidos." : "Porta aberta.");
  }

  public void message(String message) {
    toast = message;
    toastTime = 7;
  }

  public String objective() {
    if (events.arrival > 0)
      return "Sobreviva até o elevador chegar · " + (int) Math.ceil(events.arrival) + "s";
    return cores < 3
        ? "Investigue os três arquivos da equipe"
        : !reactor
            ? "Restabeleça a energia auxiliar"
            : !centralAccess
                ? "Descubra o incidente no controle central"
                : "Saia pelo elevador de evacuação";
  }

  public String time() {
    return String.format(Locale.ROOT, "%02d:%02d", (int) elapsed / 60, (int) elapsed % 60);
  }

  public int score() {
    return Math.max(
        0,
        3000
            + cores * 500
            + (reactor ? 1000 : 0)
            + (result == Result.ESCAPED ? 2000 : 0)
            - (int) elapsed * 3
            - hits * 100);
  }
}

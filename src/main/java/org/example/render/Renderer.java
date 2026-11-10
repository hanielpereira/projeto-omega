package org.example.render;

import java.util.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import org.example.entities.Agent;
import org.example.entities.Player;
import org.example.game.GameSession;
import org.example.game.GameSession.Device;
import org.example.game.GameSession.DeviceType;
import org.example.game.VisualEffects;
import org.example.world.Tile;
import org.example.world.TileMap;

/** All visuals are rendered at a fixed logical resolution then letterboxed by Main. */
public final class Renderer {
  public static final int WIDTH = 1280, HEIGHT = 800;
  private static final Color BG = Color.web("#101517"),
      PANEL = Color.web("#171f21"),
      LINE = Color.web("#3e4947"),
      TEXT = Color.web("#f0e9dc"),
      MUTED = Color.web("#a0aab4"),
      GREEN = Color.web("#a7c3b4"),
      AMBER = Color.web("#bea16c"),
      RED = Color.web("#d66a60");
  private final GraphicsContext g;
  private final Image portrait = load("portrait"), icons = load("icons");
  private final Image tiles = load("facility"),
      playerImage = load("player"),
      sentinel = load("sentinel"),
      drone = load("drone"),
      specimen = load("specimen");
  private double cameraX, cameraY, clock;
  private boolean debug;

  public Renderer(GraphicsContext context) {
    g = context;
    g.setImageSmoothing(false);
  }

  private static Image load(String name) {
    var url = Renderer.class.getResource("/sprites/" + name + ".png");
    if (url == null) throw new IllegalStateException("Sprite ausente: " + name);
    return new Image(url.toExternalForm());
  }

  public void render(
      GameSession s,
      String screen,
      boolean muted,
      boolean audioAvailable,
      boolean debug,
      double clock,
      double mouseX,
      double mouseY) {
    this.clock = clock;
    this.debug = debug;
    box(0, 0, WIDTH, HEIGHT, BG);
    if (screen.equals("MENU")) {
      menu(s, muted, audioAvailable, mouseX, mouseY);
      return;
    }
    world(s, 0, 0, 1280, 800, false);
    hud(s, muted, audioAvailable);
    if (s.showMap) mapOverlay(s);
    if (debug) diagnostics(s);
    if (screen.equals("PAUSED")) pause(s, mouseX, mouseY);
    if (s.result != GameSession.Result.PLAYING) result(s, mouseX, mouseY);
  }

  private void hud(GameSession s, boolean muted, boolean available) {
    g.setFill(
        new LinearGradient(
            0,
            0,
            0,
            1,
            true,
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#080d14", .85)),
            new Stop(1, Color.TRANSPARENT)));
    g.fillRect(0, 0, 1280, 115);
    g.setFill(
        new LinearGradient(
            0,
            0,
            0,
            1,
            true,
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.TRANSPARENT),
            new Stop(1, Color.web("#080d14", .95))));
    g.fillRect(0, 660, 1280, 140);
    box(30, 30, 3, 39, RED);
    String stage =
        s.cores < 3
            ? "ARQUIVOS  " + s.cores + " / 3"
            : !s.reactor ? "ENERGIA" : !s.centralAccess ? "CONTROLE CENTRAL" : "EXTRAÇÃO";
    text(stage, 45, 43, 11, RED, true);
    text(s.objective(), 45, 66, 15, TEXT, false);
    g.setTextAlign(TextAlignment.RIGHT);
    g.setFont(Font.font("DejaVu Sans", 11));
    g.setFill(MUTED);
    g.fillText(sector(s.player.x, s.player.y), 1248, 39);
    text("TAB  mapa   ESC  pausa", 1095, 61, 11, MUTED, false);
    if (s.alert) {
      circle(1220, 87, 3, RED);
      text("DETECTADO", 1125, 91, 11, RED, true);
    }
    if (s.events.arrival > 0) {
      box(506, 80, 268, 38, Color.web("#101922", .92));
      text("ELEVADOR  " + (int) Math.ceil(s.events.arrival) + "s", 530, 105, 18, AMBER, true);
    }
    // A field ID and an actual inventory, kept out of the play area.
    g.drawImage(portrait, 30, 714, 58, 58);
    box(30, 770, 58, 2, RED);
    icon(0, 104, 717, 24);
    text("07", 136, 734, 20, TEXT, true);
    text((int) s.player.health + "", 295, 734, 17, s.player.health < 35 ? RED : TEXT, true);
    box(106, 744, 220, 7, Color.web("#303b47"));
    box(
        106,
        744,
        220 * s.player.health / 100,
        7,
        s.player.health < 35 ? RED : Color.web("#e4d7bf"));
    for (int i = 1; i < 5; i++) box(106 + i * 44, 744, 2, 7, Color.web("#111a24"));
    box(106, 759, 220, 3, Color.web("#303b47"));
    box(106, 759, 220 * s.player.stamina / 100, 3, Color.web("#8ba8b9"));
    if (s.player.dodgeCooldown > 0)
      text(
          "ESQUIVA " + String.format(java.util.Locale.ROOT, "%.1fs", s.player.dodgeCooldown),
          106,
          781,
          10,
          MUTED,
          false);
    if (s.elapsed < 17)
      text(
          "WASD mover   F / clique disparar   ALT esquivar   E interagir",
          365,
          774,
          11,
          MUTED,
          false);
    icon(1, 876, 717, 32);
    text(
        String.format(java.util.Locale.ROOT, "%02d", s.combat.ammo),
        919,
        743,
        27,
        s.combat.ammo == 0 ? RED : TEXT,
        true);
    text("F / DISPARAR", 875, 768, 10, MUTED, false);
    icon(2, 1003, 716, 30);
    text(
        s.pulseCooldown <= 0 ? "PRONTO" : (int) Math.ceil(s.pulseCooldown) + "s",
        1041,
        741,
        15,
        s.pulseCooldown <= 0 ? TEXT : MUTED,
        true);
    text("ESPAÇO / EMP", 1003, 768, 10, MUTED, false);
    for (int i = 0; i < 3; i++) {
      g.setStroke(i < s.lures ? AMBER : LINE);
      g.strokeOval(1165 + i * 25, 723, 13, 13);
      if (i < s.lures) circle(1171 + i * 25, 729, 2, AMBER);
    }
    text("Q / ISCAS", 1163, 768, 10, MUTED, false);
    if (s.nearbyDoor() != null && s.nearby == null)
      text(
          "R  " + (s.nearbyDoor().closed ? "Abrir porta" : "Fechar porta"),
          548,
          666,
          13,
          TEXT,
          true);
  }

  private void icon(int index, double x, double y, double size) {
    g.drawImage(icons, index * 16, 0, 16, 16, x, y, size, size);
  }

  public double aimAngle(double x, double y, Player p) {
    return Math.atan2(cameraY + y / 1.5 - p.y, cameraX + x / 1.5 - p.x);
  }

  private void bar(double x, double y, double width, double value, Color color) {
    box(x, y, width, 4, LINE);
    box(x, y, width * Math.max(0, Math.min(100, value)) / 100, 4, color);
    for (int i = 1; i < 5; i++) box(x + width * i / 5, y, 2, 4, Color.web("#0b1214"));
  }

  private void mapOverlay(GameSession s) {
    box(844, 106, 404, 370, Color.web("#111b1d", .97));
    border(844, 106, 404, 370);
    text("PLANTA DA INSTALAÇÃO", 866, 139, 12, TEXT, true);
    miniMap(s, 866, 163, 10);
    text("● agente    ◆ investigação", 866, 446, 11, AMBER, false);
    text("TAB / fechar", 1115, 446, 10, MUTED, false);
  }

  private void diagnostics(GameSession s) {
    box(20, 120, 460, 22 + s.agents.size() * 31, Color.web("#0a1316", .94));
    for (int i = 0; i < s.agents.size(); i++) {
      Agent a = s.agents.get(i);
      text(
          a.kind + "  " + a.state + "  A*:" + a.navigation.searches,
          32,
          140 + i * 31,
          10,
          TEXT,
          false);
      var clue = a.knowledge.best(s.elapsed);
      text(
          a.intent
              + (clue == null
                  ? ""
                  : " / " + clue.source() + " idade " + (int) (s.elapsed - clue.time()) + "s"),
          32,
          153 + i * 31,
          9,
          MUTED,
          false);
    }
  }

  private String sector(double x, double y) {
    if (y < 320) return x < 352 ? "01 · ARQUIVO" : x < 768 ? "02 · PESQUISA" : "03 · CONTENÇÃO";
    if (y > 512) return x < 352 ? "04 · DEPÓSITO" : x < 768 ? "05 · REATOR" : "06 · BIOTÉRIO";
    return "00 · CIRCULAÇÃO";
  }

  private void world(GameSession s, double vx, double vy, double vw, double vh, boolean showcase) {
    g.save();
    g.beginPath();
    g.rect(vx, vy, vw, vh);
    g.clip();
    double scale = showcase ? 1 : 1.5, worldW = vw / scale, worldH = vh / scale;
    cameraX =
        Math.floor(Math.max(0, Math.min(TileMap.WIDTH * 32 - worldW, s.player.x - worldW * .5)) / 2)
            * 2;
    cameraY =
        Math.floor(
                Math.max(0, Math.min(TileMap.HEIGHT * 32 - worldH, s.player.y - worldH * .5)) / 2)
            * 2;
    if (showcase) {
      cameraX = 0;
      cameraY = 0;
    }
    double shake = showcase ? 0 : s.fx.shake;
    g.translate(
        vx - cameraX * scale + Math.sin(clock * 71) * shake,
        vy - cameraY * scale + Math.cos(clock * 93) * shake);
    g.scale(scale, scale);
    box(cameraX, cameraY, worldW, worldH, Color.web("#10232d"));
    for (int row = 0; row < TileMap.HEIGHT; row++)
      for (int col = 0; col < TileMap.WIDTH; col++) {
        double x = col * 32, y = row * 32;
        if (x < cameraX - 32 || y < cameraY - 32 || x > cameraX + vw || y > cameraY + vh) continue;
        Tile.Type type = s.map.getTile(row, col).getType();
        int hash = (row * 137 + col * 67) % 41;
        tile(
            type == Tile.Type.MUD ? 2 : type == Tile.Type.CONTAMINATED ? 3 : hash % 5 == 0 ? 1 : 0,
            x,
            y);
        if (type == Tile.Type.WALL) {
          boolean bench =
              row != 0 && row != 25 && row != 10 && row != 16 && col != 0 && col != 35 && col != 11
                  && col != 24;
          box(x, y + 24, 32, 13, Color.color(0, 0, 0, .27));
          tile(bench ? ((col > 25 && row < 10) ? 12 : 5) : 4, x, y);
          if (!bench && row % 6 == 0 && col % 3 == 0) {
            box(x + 10, y + 11, 12, 2, GREEN.deriveColor(0, 1, 1, .65));
            glow(x + 16, y + 16, 42, GREEN, .08);
          }
        } else {
          if (hash == 9) tile(13, x, y);
          if (hash == 18) {
            box(x + 7, y + 16, 17, 1, Color.web("#31434a"));
            box(x + 13, y + 12, 1, 9, Color.web("#31434a"));
          }
          if (row == 12 || row == 14) {
            box(x + 13, y + 15, 6, 2, Color.web("#3b514d"));
          }
        }
      }
    for (int[] fixture : new int[][] {{2, 11}, {10, 8}, {16, 20}, {8, 24}, {23, 24}}) {
      double lx = TileMap.center(fixture[1]), ly = TileMap.center(fixture[0]);
      double intensity =
          .07 + .045 * Math.max(0, Math.sin(clock * 13 + fixture[0]) * Math.sin(clock * 3.7));
      glow(lx, ly, 85, RED, intensity);
      box(lx - 4, ly - 7, 8, 2, RED.deriveColor(0, .7, 1, .6));
    }
    roomLabel("01  /  ARQUIVO", 45, 53);
    roomLabel("02  /  PESQUISA", 398, 53);
    roomLabel("03  /  CONTENÇÃO", 817, 53);
    roomLabel("04  /  DEPÓSITO", 45, 565);
    roomLabel("05  /  REATOR", 398, 565);
    roomLabel("06  /  BIOTÉRIO", 817, 565);
    // Door thresholds.
    for (int c : new int[] {4, 5, 17, 18, 29, 30}) {
      tile(15, c * 32, 10 * 32);
      tile(15, c * 32, 16 * 32);
    }
    for (int[] mark : new int[][] {{8, 7}, {18, 8}, {9, 22}, {22, 30}, {2, 9}}) {
      tile(20, mark[1] * 32, mark[0] * 32);
    }
    for (int[] crack : new int[][] {{6, 8}, {9, 19}, {14, 26}, {20, 5}, {23, 17}, {4, 27}})
      tile(21, crack[1] * 32, crack[0] * 32);
    for (var door : s.doors)
      for (int r = 12; r <= 13; r++) tile(door.closed ? 16 : 17, door.col * 32, r * 32);
    for (Device d : s.devices) {
      Color color =
          d.type == DeviceType.MEDKIT
              ? TEXT
              : d.active ? MUTED : d.type == DeviceType.EXIT && !s.reactor ? RED : GREEN;
      if (!d.active) glow(d.x(), d.y(), 58, color, .14 + .025 * Math.sin(clock * 2));
      int sprite =
          switch (d.type) {
            case CORE -> d.active ? 7 : 6;
            case REACTOR -> 8;
            case EXIT -> 9;
            case MEDKIT -> 10;
            case CONTROL -> 18;
            case SUPPLY -> 11;
          };
      if ((d.type == DeviceType.MEDKIT || d.type == DeviceType.SUPPLY) && d.active) continue;
      tile(sprite, d.col * 32, d.row * 32);
      if (!d.active) {
        box(d.x() - 2, d.row * 32 - 6, 4, 4, color);
        if (d.type != DeviceType.MEDKIT && d.type != DeviceType.SUPPLY)
          text(
              d.type == DeviceType.CORE
                  ? "NÚCLEO"
                  : d.type == DeviceType.REACTOR
                      ? "REATOR"
                      : d.type == DeviceType.CONTROL ? "CONTROLE" : "SAÍDA",
              d.col * 32 - 4,
              d.row * 32 - 12,
              8,
              color,
              true);
      }
    }
    if (s.noiseTime > 0) {
      circle(s.noiseX, s.noiseY, 5, AMBER);
      for (int i = 0; i < 3; i++) {
        double r = ((clock * 35 + i * 28) % 90);
        g.setStroke(Color.color(.93, .71, .47, (1 - r / 90) * .4));
        g.setLineWidth(1);
        g.strokeOval(s.noiseX - r, s.noiseY - r, r * 2, r * 2);
      }
    }
    for (Agent a : s.agents) {
      if (a.state != Agent.State.STUNNED && (debug || a.kind == Agent.Kind.DRONE)) cone(s.map, a);
      if (debug) {
        g.setStroke(a.state == Agent.State.CHASE ? RED : AMBER.deriveColor(0, 1, 1, .6));
        g.setLineWidth(1);
        List<int[]> path = a.getPath();
        for (int i = 1; i < path.size(); i++)
          g.strokeLine(
              TileMap.center(path.get(i - 1)[1]),
              TileMap.center(path.get(i - 1)[0]),
              TileMap.center(path.get(i)[1]),
              TileMap.center(path.get(i)[0]));
      }
    }
    for (var body : s.combat.fallen) {
      g.save();
      g.translate(body.x(), body.y());
      g.rotate(83);
      g.setGlobalAlpha(.55);
      character(
          body.kind() == Agent.Kind.SPECIMEN
              ? specimen
              : body.kind() == Agent.Kind.DRONE ? drone : sentinel,
          0,
          0,
          0,
          0);
      g.restore();
    }
    for (int i = 0; i < s.events.arcs.length; i++)
      if (s.reactor) {
        double phase = s.events.phase(s, i),
            x = TileMap.center(s.events.arcs[i][1]),
            y = TileMap.center(s.events.arcs[i][0]);
        if (phase >= 2 && phase < 5) {
          boolean live = s.events.live(i);
          Color c = live ? Color.web("#c9e5ef") : AMBER;
          box(x - 16, y - 16, 32, 32, c.deriveColor(0, 1, 1, live ? .13 : .07));
          g.setStroke(c.deriveColor(0, 1, 1, .65));
          g.strokeRect(x - 14, y - 14, 28, 28);
          if (live) {
            glow(x, y, 65, c, .13);
            g.setLineWidth(1.5);
            g.strokePolyline(
                new double[] {x - 14, x - 5, x + 3, x + 14},
                new double[] {y + 5, y - 9 * Math.sin(clock * 30), y + 7, y - 5},
                4);
          }
        }
      }
    // Sort by feet position so nearby sprites overlap naturally.
    List<Agent> ordered = new ArrayList<>(s.agents);
    ordered.sort(Comparator.comparingDouble(a -> a.y));
    boolean drawn = false;
    for (Agent a : ordered) {
      if (!debug && !showcase && !s.map.lineOfSight(s.player.x, s.player.y, a.x, a.y)) continue;
      if (!drawn && a.y > s.player.y) {
        drawPlayer(s.player);
        drawn = true;
      }
      shadow(a.x, a.y, 11);
      if (a.hurtTime > 1.8) g.setGlobalAlpha(.55 + .45 * Math.sin(clock * 50));
      character(
          a.kind == Agent.Kind.SENTINEL ? sentinel : a.kind == Agent.Kind.DRONE ? drone : specimen,
          a.x,
          a.y,
          a.facing,
          (int) a.animation % 4);
      g.setGlobalAlpha(1);
      if (a.hurtTime > 0) {
        box(a.x - 10, a.y - 29, 20, 2, LINE);
        box(
            a.x - 10,
            a.y - 29,
            20.0
                * a.health
                / (a.kind == Agent.Kind.DRONE ? 2 : a.kind == Agent.Kind.SPECIMEN ? 4 : 3),
            2,
            RED);
      }
      if (a.state == Agent.State.STUNNED) text("· · ·", a.x - 9, a.y - 23, 14, GREEN, true);
      else if (a.state == Agent.State.CHASE) text("!", a.x - 3, a.y - 25, 17, RED, true);
      else if (a.state == Agent.State.INVESTIGATE || a.state == Agent.State.SEARCH)
        text("?", a.x - 3, a.y - 25, 14, AMBER, true);
      if (a.radioVisual > 0) {
        g.setStroke(AMBER.deriveColor(0, 1, 1, a.radioVisual * .5));
        g.strokeOval(a.x - 18, a.y - 30, 36, 36);
        text("···", a.x + 12, a.y - 16, 10, AMBER, true);
      }
      if (a.attackVisual > 0) {
        g.setStroke(RED);
        g.setLineWidth(1);
        double r = 9 + a.attackVisual * 12;
        g.strokeOval(a.x - r, a.y - r, r * 2, r * 2);
      }
      if (a.kind == Agent.Kind.SPECIMEN) glow(a.x, a.y - 8, 15, RED, .13);
      if (debug) text(a.state.name(), a.x - 20, a.y + 21, 7, AMBER, true);
      if (a.suspicion > 0 && a.state != Agent.State.CHASE) {
        box(a.x - 10, a.y - 18, 20, 2, LINE);
        box(a.x - 10, a.y - 18, 20 * a.suspicion, 2, AMBER);
      }
    }
    if (!drawn) drawPlayer(s.player);
    if (s.pulseVisual > 0) {
      double r = (1 - s.pulseVisual / .55) * 120;
      g.setStroke(GREEN.deriveColor(0, 1, 1, s.pulseVisual / .55));
      g.setLineWidth(3);
      g.strokeOval(s.player.x - r, s.player.y - r, r * 2, r * 2);
    }
    for (var round : s.combat.rounds) {
      g.setStroke(Color.web("#f8d8a2"));
      g.setLineWidth(1.5);
      g.strokeLine(round.x, round.y, round.x - round.vx * .022, round.y - round.vy * .022);
      glow(round.x, round.y, 13, AMBER, .24);
    }
    if (s.combat.muzzle > 0) {
      double x = s.player.x + Math.cos(s.player.facing) * 18,
          y = s.player.y + Math.sin(s.player.facing) * 18;
      glow(x, y, 80, Color.web("#f8cf8f"), .35);
      circle(x, y, 3, TEXT);
    }
    particles(s);
    // Seek-safe fixture flicker and the player's weak torch stay in world coordinates.
    double flicker = .91 + .09 * Math.sin(clock * 17) * Math.sin(clock * 7);
    glow(
        s.player.x + Math.cos(s.player.facing) * 65,
        s.player.y + Math.sin(s.player.facing) * 65,
        120,
        TEXT,
        .055 * flicker);
    if (s.events.blackout > 0 && !showcase)
      box(cameraX, cameraY, worldW, worldH, Color.web("#050c19", .24 + .12 * Math.sin(clock * 25)));
    // Darkness is a visual vignette only; collision and vision use the actual tile map.
    g.setFill(
        new RadialGradient(
            0,
            0,
            s.player.x,
            s.player.y,
            350,
            false,
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.TRANSPARENT),
            new Stop(.5, Color.color(.01, .04, .07, .08)),
            new Stop(1, Color.color(.015, .025, .03, .84))));
    if (!showcase) g.fillRect(cameraX, cameraY, worldW, worldH);
    if (s.player.invulnerable > .8)
      box(cameraX, cameraY, worldW, worldH, Color.color(.8, .15, .1, .08));
    g.restore();
    if (!showcase) {
      if (s.player.health < 35) {
        g.setFill(
            new RadialGradient(
                0,
                0,
                640,
                400,
                710,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(.35, Color.TRANSPARENT),
                new Stop(1, Color.web("#8f241c", .2 + .04 * Math.sin(clock * 4)))));
        g.fillRect(0, 0, 1280, 800);
      }
      if (s.fx.flash > 0) box(0, 0, 1280, 800, Color.web("#b8d6c2", s.fx.flash * .4));
      if (s.nearby != null) {
        double x = 370, y = 654, w = 540;
        box(x, y, w, 37, Color.web("#0b1214", .94));
        text(s.prompt(), x + 14, y + 22, 12, s.usable(s.nearby) ? TEXT : AMBER, false);
        if (s.progress > 0) box(x, y + 35, w * s.progress, 2, GREEN);
      }
      if (s.toastTime > 0) {
        box(280, 590, 720, 43, Color.web("#0b1214", .91));
        box(280, 590, 2, 43, AMBER);
        wrap(s.toast, 295, 608, 685, 11, TEXT, 15);
      }
      // Sparse dust on the lens, never an opaque scanline filter.
      for (int i = 0; i < 24; i++) {
        double px = (i * 193 + Math.sin(i) * 49) % 1280, py = (i * 73 + clock * (2 + i % 3)) % 800;
        box(px, py, 1, 1, Color.web("#c2be9d", .12));
      }
    }
  }

  private void particles(GameSession s) {
    for (var p : s.fx.particles) {
      if (p.kind == VisualEffects.Kind.AFTERIMAGE) {
        g.setGlobalAlpha(p.life / p.total * .28);
        character(playerImage, p.x, p.y, p.size, 0);
        g.setGlobalAlpha(1);
        continue;
      }
      Color color =
          switch (p.kind) {
            case BLOOD -> Color.web("#a75b45");
            case SPARK -> Color.web("#e8bc77");
            case EMP -> Color.web("#a7c6bd");
            case DATA -> AMBER;
            case GLASS -> TEXT;
            case STEAM -> Color.web("#acb1a4");
            default -> Color.web("#a19e82");
          };
      double alpha = p.life / p.total;
      g.setGlobalAlpha(alpha * (p.kind == VisualEffects.Kind.DUST ? .35 : .85));
      box(Math.rint(p.x), Math.rint(p.y), p.size, p.size, color);
      if (p.kind == VisualEffects.Kind.SPARK || p.kind == VisualEffects.Kind.EMP) {
        g.setStroke(color);
        g.strokeLine(p.x, p.y, p.x - p.vx * .035, p.y - p.vy * .035);
      }
    }
    g.setGlobalAlpha(1);
  }

  private void roomLabel(String label, double x, double y) {
    text(label, x, y, 10, Color.web("#4c6a71"), true);
  }

  private void cone(TileMap map, Agent a) {
    double range = a.visionRange(), angle = a.visionAngle();
    int rays = 32;
    double[] xs = new double[rays + 2], ys = new double[rays + 2];
    xs[0] = a.x;
    ys[0] = a.y;
    for (int i = 0; i <= rays; i++) {
      double theta = a.facing - angle + 2 * angle * i / rays, d = 0;
      for (double r = 8; r <= range; r += 8) {
        if (!map.walkable(
            (int) ((a.y + Math.sin(theta) * r) / 32), (int) ((a.x + Math.cos(theta) * r) / 32)))
          break;
        d = r;
      }
      xs[i + 1] = a.x + Math.cos(theta) * d;
      ys[i + 1] = a.y + Math.sin(theta) * d;
    }
    Color tint = a.state == Agent.State.CHASE ? RED : AMBER;
    g.setFill(
        new RadialGradient(
            0,
            0,
            a.x,
            a.y,
            range,
            false,
            CycleMethod.NO_CYCLE,
            new Stop(0, tint.deriveColor(0, 1, 1, .14)),
            new Stop(1, tint.deriveColor(0, 1, 1, .012))));
    g.fillPolygon(xs, ys, xs.length);
  }

  private void drawPlayer(Player p) {
    glow(p.x, p.y, 76, GREEN, .055);
    shadow(p.x, p.y, 10);
    if (p.invulnerable > 0 && ((int) (clock * 15) % 2 == 0)) g.setGlobalAlpha(.4);
    character(playerImage, p.x, p.y, p.facing, p.moving ? (int) p.animation % 4 : 0);
    g.setGlobalAlpha(1);
    if (p.dodgeTime <= 0) {
      g.save();
      g.translate(p.x, p.y - 4);
      g.rotate(Math.toDegrees(p.facing));
      box(8, -1, 9, 2, Color.web("#b4b5b0"));
      box(8, 1, 3, 3, Color.web("#574539"));
      g.restore();
    }
    double dx = Math.cos(p.facing), dy = Math.sin(p.facing);
    circle(p.x + dx * 20, p.y + dy * 20, 1.5, GREEN.deriveColor(0, 1, 1, .7));
  }

  private void character(Image image, double x, double y, double facing, int frame) {
    int dir =
        Math.abs(facing) > Math.PI * .75
            ? 1
            : facing < -Math.PI * .25 ? 3 : facing > Math.PI * .25 ? 0 : 2;
    g.drawImage(image, frame * 16, dir * 16, 16, 16, Math.rint(x - 16), Math.rint(y - 24), 32, 32);
  }

  private void tile(int index, double x, double y) {
    g.drawImage(tiles, (index % 8) * 16, (index / 8) * 16, 16, 16, x, y, 32, 32);
  }

  private void miniMap(GameSession s, double x, double y, double scale) {
    box(x - 4, y - 4, TileMap.WIDTH * scale + 8, TileMap.HEIGHT * scale + 8, Color.web("#091820"));
    for (int r = 0; r < TileMap.HEIGHT; r++)
      for (int c = 0; c < TileMap.WIDTH; c++) {
        if (s.map.walkable(r, c))
          box(x + c * scale, y + r * scale, scale - 1, scale - 1, Color.web("#2b434b"));
      }
    for (Device d : s.devices)
      if (!d.active && d.type != DeviceType.MEDKIT && d.type != DeviceType.SUPPLY) {
        boolean goal =
            s.cores < 3
                ? d.type == DeviceType.CORE
                : !s.reactor
                    ? d.type == DeviceType.REACTOR
                    : !s.centralAccess ? d.type == DeviceType.CONTROL : d.type == DeviceType.EXIT;
        if (goal)
          box(
              x + d.col * scale,
              y + d.row * scale,
              5,
              5,
              AMBER.deriveColor(0, 1, 1, .8 + .2 * Math.sin(clock * 3)));
      }
    if (debug) for (Agent a : s.agents) circle(x + a.x / 32 * scale, y + a.y / 32 * scale, 2, RED);
    circle(x + s.player.x / 32 * scale, y + s.player.y / 32 * scale, 3, GREEN);
  }

  private void menu(GameSession s, boolean muted, boolean available, double mx, double my) {
    world(s, 0, 0, 1280, 800, true);
    g.setFill(
        new LinearGradient(
            0,
            0,
            1,
            0,
            true,
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#0b111b")),
            new Stop(.4, Color.web("#0b111b", .97)),
            new Stop(1, Color.web("#0b111b", .18))));
    g.fillRect(0, 0, 1280, 800);
    text("PROJETO", 76, 240, 16, RED, true);
    g.setFill(TEXT);
    g.setFont(Font.font("DejaVu Sans", FontWeight.EXTRA_BOLD, 100));
    g.fillText("ÔMEGA", 68, 346);
    text("INSTALAÇÃO DE PESQUISA / ACESSO RESTRITO", 76, 389, 11, MUTED, false);
    wrap(
        "Seis dias sem contato com a equipe. Entre pela ala médica, recupere os registros e"
            + " encontre uma saída.",
        76,
        442,
        410,
        16,
        TEXT,
        25);
    button(76, 535, 320, 57, "INICIAR INVESTIGAÇÃO", mx, my, true);
    text("ENTER para começar", 96, 617, 12, MUTED, false);
    g.drawImage(portrait, 76, 690, 48, 48);
    text("AGENTE 07", 140, 710, 13, TEXT, true);
    text(s.controller, 140, 732, 11, MUTED, false);
    text(
        "M  " + (muted ? "Som desligado" : available ? "Som ligado" : "Verificar áudio"),
        76,
        777,
        11,
        MUTED,
        false);
    // The hero is a native 16px sprite, enlarged with hard pixel edges.
    glow(984, 467, 170, RED, .04);
    box(836, 595, 354, 2, Color.web("#bf6760", .45));
    text("03:41 / ARQUIVO DE CONTENÇÃO", 836, 625, 12, RED, true);
    wrap("“A energia voltou. As portas não abriram.”", 836, 658, 348, 17, TEXT, 25);
  }

  private void pause(GameSession s, double mx, double my) {
    scrim();
    box(260, 110, 760, 580, PANEL);
    border(260, 110, 760, 580);
    text("AGENTE 07 / REGISTRO DE CAMPO", 300, 158, 11, GREEN, true);
    text("Pausa", 300, 203, 30, TEXT, true);
    text("WASD / SETAS", 300, 249, 12, GREEN, true);
    text("Mova-se pelo laboratório.", 455, 249, 12, TEXT, false);
    text("SHIFT / CTRL", 300, 280, 12, GREEN, true);
    text("Corra para escapar; ande devagar para fazer menos ruído.", 455, 280, 12, TEXT, false);
    text("E / Q / ESPAÇO", 300, 311, 12, GREEN, true);
    text(
        "E: interagir · Q: isca · Espaço: EMP · F: disparar · Alt: esquiva",
        455,
        311,
        12,
        TEXT,
        false);
    line(300, 335, 980, 335, LINE);
    text("REGISTROS RECUPERADOS", 300, 364, 10, MUTED, true);
    double y = 391;
    for (String log :
        s.logs.subList(
            Math.min(s.journalOffset, s.logs.size()),
            Math.min(s.logs.size(), s.journalOffset + 2))) {
      y = wrap(log, 300, y, 680, 12, TEXT, 18) + 13;
    }
    text("PgUp / PgDn ou roda do mouse: registros", 300, 492, 10, MUTED, false);
    text("AMBIENTE", 300, 530, 11, MUTED, false);
    button(432, 508, 38, 33, "−", mx, my, false);
    text((int) (s.musicVolume * 100) + "%", 480, 530, 11, TEXT, false);
    button(525, 508, 38, 33, "+", mx, my, false);
    text("EFEITOS", 640, 530, 11, MUTED, false);
    button(750, 508, 38, 33, "−", mx, my, false);
    text((int) (s.sfxVolume * 100) + "%", 798, 530, 11, TEXT, false);
    button(847, 508, 38, 33, "+", mx, my, false);
    text(s.audioStatus, 300, 563, 10, MUTED, false);
    button(300, 580, 310, 48, "CONTINUAR  /  ESC", mx, my, true);
    button(630, 580, 350, 48, "VOLTAR AO MENU", mx, my, false);
    text(
        "TAB: mapa · R: porta · G: IA · M: som · F2: configurar controle · F11: tela cheia",
        300,
        658,
        11,
        MUTED,
        false);
  }

  private void result(GameSession s, double mx, double my) {
    boolean won = s.result == GameSession.Result.ESCAPED;
    scrim();
    box(320, 175, 640, 450, PANEL);
    border(320, 175, 640, 450);
    text(won ? "PROTOCOLO CONCLUÍDO" : "SINAL PERDIDO", 360, 222, 11, won ? GREEN : RED, true);
    text(won ? "Extração concluída" : "Agente perdido", 360, 275, 34, TEXT, true);
    wrap(
        won
            ? "Os registros do incidente foram recuperados. A instalação permanece isolada. A"
                + " equipe não foi encontrada."
            : "Ômega continua em quarentena. Use os corredores, evite os cones de visão e guarde um"
                + " pulso para a fuga.",
        360,
        316,
        550,
        16,
        MUTED,
        25);
    line(360, 390, 920, 390, LINE);
    text("TEMPO", 360, 421, 10, MUTED, true);
    text(s.time(), 360, 456, 27, TEXT, true);
    text("NÚCLEOS", 552, 421, 10, MUTED, true);
    text(s.cores + " / 3", 552, 456, 27, GREEN, true);
    text("PONTUAÇÃO", 740, 421, 10, MUTED, true);
    text("" + s.score(), 740, 456, 27, AMBER, true);
    button(360, 500, 300, 52, won ? "JOGAR NOVAMENTE  →" : "TENTAR NOVAMENTE  →", mx, my, true);
    button(680, 500, 240, 52, "MENU", mx, my, false);
    text("ENTER  /  reiniciar a missão", 360, 588, 11, MUTED, false);
  }

  private void scrim() {
    box(0, 0, 1280, 800, Color.color(.01, .03, .05, .85));
  }

  private void button(
      double x, double y, double w, double h, String label, double mx, double my, boolean primary) {
    boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
    box(
        x,
        y,
        w,
        h,
        primary
            ? (hover ? Color.web("#b35450") : Color.web("#8e4144"))
            : (hover ? Color.web("#303b47") : Color.web("#18222e")));
    if (primary) box(x, y, 3, h, Color.web("#e99b80"));
    text(label, x + 20, y + h / 2 + 5, 12, primary ? Color.web("#fff0df") : TEXT, true);
  }

  private void border(double x, double y, double w, double h) {
    g.setStroke(LINE);
    g.setLineWidth(1);
    g.strokeRect(x + .5, y + .5, w - 1, h - 1);
  }

  private void glow(double x, double y, double radius, Color color, double opacity) {
    g.setFill(
        new RadialGradient(
            0,
            0,
            x,
            y,
            radius,
            false,
            CycleMethod.NO_CYCLE,
            new Stop(0, color.deriveColor(0, 1, 1, opacity)),
            new Stop(1, Color.TRANSPARENT)));
    g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
  }

  private void shadow(double x, double y, double r) {
    g.setFill(Color.color(0, 0, 0, .33));
    g.fillOval(x - r, y + 3, r * 2, 8);
  }

  private void circle(double x, double y, double radius, Color color) {
    g.setFill(color);
    g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
  }

  private void box(double x, double y, double w, double h, Color color) {
    g.setFill(color);
    g.fillRect(x, y, w, h);
  }

  private void line(double x, double y, double bx, double by, Color color) {
    g.setStroke(color);
    g.setLineWidth(1);
    g.strokeLine(x, y, bx, by);
  }

  private void text(String text, double x, double y, double size, Color color, boolean bold) {
    g.setTextAlign(TextAlignment.LEFT);
    g.setFont(Font.font("DejaVu Sans", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
    g.setFill(color);
    g.fillText(text, x, y);
  }

  private double wrap(
      String value, double x, double y, double width, double size, Color color, double leading) {
    // Conservative measurement using JavaFX's font metrics for accents and platform fonts.
    StringBuilder row = new StringBuilder();
    javafx.scene.text.Text measure = new javafx.scene.text.Text();
    measure.setFont(Font.font("DejaVu Sans", size));
    for (String word : value.split(" ")) {
      String next = row.isEmpty() ? word : row + " " + word;
      measure.setText(next);
      if (measure.getLayoutBounds().getWidth() > width && !row.isEmpty()) {
        text(row.toString(), x, y, size, color, false);
        y += leading;
        row.setLength(0);
        row.append(word);
      } else {
        row.setLength(0);
        row.append(next);
      }
    }
    if (!row.isEmpty()) text(row.toString(), x, y, size, color, false);
    return y;
  }
}

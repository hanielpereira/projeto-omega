package org.example;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import org.example.ai.AStarPathfinder;
import org.example.entities.Agent;
import org.example.entities.Player;
import org.example.game.GameSession;
import org.example.world.TileMap;
import org.junit.jupiter.api.Test;

class GameRulesTest {
  @Test
  void everyObjectiveAndPatrolIsReachable() {
    GameSession s = new GameSession();
    AStarPathfinder a = new AStarPathfinder(s.map);
    assertTrue(s.map.canOccupy(s.player.x, s.player.y, Player.RADIUS));
    for (var d : s.devices)
      assertFalse(a.findPath(s.player.row(), s.player.col(), d.row, d.col).isEmpty(), d.name);
    for (var enemy : s.agents)
      assertFalse(
          a.findPath(s.player.row(), s.player.col(), (int) enemy.y / 32, (int) enemy.x / 32)
              .isEmpty());
  }

  @Test
  void invalidAndBlockedAStarEndpointsReturnNoPath() {
    AStarPathfinder a = new AStarPathfinder(new TileMap());
    assertTrue(a.findPath(-1, 1, 5, 6).isEmpty());
    assertTrue(a.findPath(1, 1, 0, 0).isEmpty());
    assertTrue(a.findPath(0, 0, 1, 1).isEmpty());
    assertEquals(1, a.findPath(1, 1, 1, 1).size());
  }

  @Test
  void weightedAStarChoosesCheapDetourAndHandlesDisconnectedMap() {
    int[][] data = {
      {1, 1, 1, 1, 1, 1, 1}, {1, 0, 3, 3, 3, 0, 1}, {1, 0, 0, 0, 0, 0, 1}, {1, 1, 1, 1, 1, 1, 1}
    };
    TileMap map = new TileMap(data);
    var path = new AStarPathfinder(map).findPath(1, 1, 1, 5);
    assertEquals(7, path.size());
    assertTrue(path.stream().anyMatch(p -> p[0] == 2));
    for (int i = 1; i < path.size(); i++)
      assertEquals(
          1,
          Math.abs(path.get(i)[0] - path.get(i - 1)[0])
              + Math.abs(path.get(i)[1] - path.get(i - 1)[1]));
    assertTrue(
        new AStarPathfinder(new TileMap(new int[][] {{0, 1, 0}})).findPath(0, 0, 0, 2).isEmpty());
  }

  @Test
  void normalizedMovementCannotPassThroughWalls() {
    Player p = new Player();
    TileMap map = new TileMap();
    double x = p.x, y = p.y;
    p.update(.04, 1, 1, false, false, map);
    assertEquals(95 * .04, Math.hypot(p.x - x, p.y - y), .0001);
    for (int i = 0; i < 400; i++) p.update(.04, -1, 0, true, false, map);
    assertTrue(p.x >= 32 + Player.RADIUS);
    assertTrue(map.canOccupy(p.x, p.y, Player.RADIUS));
    assertFalse(map.canOccupy(-1, 50, 8));
    assertFalse(map.canOccupy(50, -1, 8));
  }

  @Test
  void wallsOccludeSightAndSneakingReducesDetectionRange() {
    TileMap map = new TileMap();
    Player p = new Player();
    Agent a = new Agent(Agent.Kind.SENTINEL, map, new int[][] {{12, 14}, {14, 14}});
    a.x = TileMap.center(14);
    a.y = TileMap.center(12);
    a.facing = 0;
    p.x = a.x + 150;
    p.y = a.y;
    assertTrue(a.canSee(p));
    p.sneaking = true;
    assertFalse(a.canSee(p));
    p.sneaking = false;
    p.x = TileMap.center(26);
    a.x = TileMap.center(23);
    a.y = p.y = TileMap.center(9);
    assertFalse(a.canSee(p));
    assertFalse(map.lineOfSight(a.x, a.y, p.x, p.y));
  }

  @Test
  void perceptionTransitionsAndStunRecover() {
    TileMap map = new TileMap();
    Player p = new Player();
    Agent a = new Agent(Agent.Kind.SENTINEL, map, new int[][] {{12, 14}, {14, 14}});
    p.x = a.x;
    p.y = a.y + 70;
    a.facing = Math.PI / 2;
    for (int i = 0; i < 8; i++) a.update(.04, p, 0, 0, 0, false);
    assertEquals(Agent.State.CHASE, a.state);
    a.stun(.2);
    a.update(.1, p, 0, 0, 0, false);
    assertEquals(Agent.State.STUNNED, a.state);
    p.x = 100;
    p.y = 100;
    for (int i = 0; i < 70; i++) a.update(.04, p, 0, 0, 0, false);
    assertNotEquals(Agent.State.STUNNED, a.state);
    a.update(.04, p, a.x + 50, a.y, 200, true);
    assertEquals(Agent.State.INVESTIGATE, a.state);
  }

  @Test
  void agentNeverCutsSolidCornersWhileReplanning() {
    TileMap map = new TileMap();
    Player p = new Player();
    p.x = 80;
    p.y = 80;
    Agent a =
        new Agent(Agent.Kind.SENTINEL, map, new int[][] {{6, 14}, {6, 22}, {12, 22}, {12, 14}});
    for (int i = 0; i < 3000; i++) {
      if (i % 13 == 0) a.setDestination(i % 26 == 0 ? 12 : 6, 22);
      a.update(.04, p, 0, 0, 0, false);
      assertTrue(map.canOccupy(a.x, a.y, 8), "collision at " + a.x + ", " + a.y);
    }
  }

  @Test
  void completeMissionRequiresCoresThenReactorThenExit() {
    GameSession s = new GameSession();
    s.agents.clear();
    var reactor = s.devices.get(3);
    var exit = s.devices.get(4);
    use(s, exit);
    assertEquals(GameSession.Result.PLAYING, s.result);
    assertFalse(exit.active);
    use(s, reactor);
    assertFalse(s.reactor);
    for (int i = 0; i < 3; i++) use(s, s.devices.get(i));
    assertEquals(3, s.cores);
    assertEquals(4, s.logs.size());
    use(s, reactor);
    assertTrue(s.reactor);
    use(s, s.devices.get(7));
    assertTrue(s.centralAccess);
    use(s, exit);
    assertEquals(GameSession.Result.PLAYING, s.result);
    s.agents.clear();
    s.events.breachPending = false;
    for (int i = 0; i < 310; i++) s.update(.04, GameSession.Input.idle());
    assertTrue(s.events.liftReady);
    use(s, exit);
    assertEquals(GameSession.Result.ESCAPED, s.result);
    assertTrue(s.score() > 0);
    double t = s.elapsed;
    s.update(.04, GameSession.Input.idle());
    assertEquals(t, s.elapsed);
  }

  @Test
  void interactingRequiresHoldAndCancelsOnRelease() {
    GameSession s = new GameSession();
    s.agents.clear();
    var d = s.devices.get(0);
    s.player.x = d.x();
    s.player.y = d.y();
    for (int i = 0; i < 15; i++)
      s.update(.04, new GameSession.Input(0, 0, false, false, true, false, false));
    assertTrue(s.progress > 0);
    assertFalse(d.active);
    s.update(.04, GameSession.Input.idle());
    assertEquals(0, s.progress);
    use(s, d);
    assertTrue(d.active);
    use(s, d);
    assertEquals(1, s.cores);
  }

  @Test
  void decoysAreLimitedAndPulseHasCooldown() {
    GameSession s = new GameSession();
    s.agents.clear();
    s.update(.04, new GameSession.Input(0, 0, false, false, false, true, true));
    assertEquals(2, s.lures);
    assertTrue(s.noiseTime > 0);
    assertEquals(9, s.pulseCooldown);
    s.update(.04, new GameSession.Input(0, 0, false, false, false, true, true));
    assertEquals(2, s.lures);
    assertTrue(s.pulseCooldown < 9);
    assertTrue(s.map.canOccupy(s.noiseX, s.noiseY, 4));
  }

  @Test
  void contaminationKillsAndMedicalKitsHealOnlyOnce() {
    GameSession s = new GameSession();
    s.agents.clear();
    s.player.health = 20;
    use(s, s.devices.get(5));
    assertEquals(65, s.player.health, .01);
    use(s, s.devices.get(5));
    assertEquals(65, s.player.health, .01);
    s.player.x = TileMap.center(27);
    s.player.y = TileMap.center(20);
    s.player.health = .1;
    s.update(.04, GameSession.Input.idle());
    assertEquals(GameSession.Result.DEAD, s.result);
  }

  @Test
  void allShippedSpritesAndAudioDecode() throws Exception {
    for (String name : List.of("player", "sentinel", "specimen", "drone", "facility")) {
      var image = ImageIO.read(getClass().getResourceAsStream("/sprites/" + name + ".png"));
      assertNotNull(image);
      assertEquals(name.equals("facility") ? 128 : 64, image.getWidth());
      assertEquals(name.equals("facility") ? 48 : 64, image.getHeight());
    }
    for (String name :
        List.of("ambient", "terminal", "pulse", "lure", "hit", "step", "sneak", "win", "lose")) {
      try (var stream =
          AudioSystem.getAudioInputStream(getClass().getResource("/audio/" + name + ".wav"))) {
        assertTrue(stream.getFrameLength() > 0);
        assertEquals(44100, stream.getFormat().getSampleRate());
        assertEquals(2, stream.getFormat().getChannels());
      }
    }
  }

  private static void use(GameSession s, GameSession.Device d) {
    s.player.x = d.x();
    s.player.y = d.y();
    for (int i = 0; i < 42; i++)
      s.update(.04, new GameSession.Input(0, 0, false, false, true, false, false));
  }
}

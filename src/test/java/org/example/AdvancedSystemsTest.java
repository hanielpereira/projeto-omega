package org.example;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.example.ai.*;
import org.example.audio.AudioMixer;
import org.example.entities.*;
import org.example.game.GameSession;
import org.example.input.Gamepad;
import org.example.world.TileMap;
import org.junit.jupiter.api.Test;

class AdvancedSystemsTest {
  @Test
  void radioHasDelayRangeExpiryAndNoCreatureOmniscience() {
    TileMap map = new TileMap();
    RadioNetwork network = new RadioNetwork();
    Agent sender = agent(Agent.Kind.SENTINEL, map, network, 12, 14);
    Agent receiver = agent(Agent.Kind.DRONE, map, network, 12, 18);
    Agent far = agent(Agent.Kind.SENTINEL, map, network, 23, 33);
    Agent creature = agent(Agent.Kind.SPECIMEN, map, network, 12, 19);
    var evidence =
        new AgentMemory.Evidence(
            network.nextEvent(), 400, 400, 0, .9, AgentMemory.Source.VISION, "S1", 8);
    network.transmit(sender, evidence, 0);
    List<Agent> all = List.of(sender, receiver, far, creature);
    network.update(.7, all);
    assertNull(receiver.knowledge.best(.7));
    network.update(.9, all);
    assertNotNull(receiver.knowledge.best(.9));
    assertEquals(400, receiver.knowledge.best(.9).x());
    assertEquals(0, receiver.knowledge.best(.9).time());
    assertEquals(AgentMemory.Source.RADIO, receiver.knowledge.best(.9).source());
    assertNull(far.knowledge.best(.9));
    assertNull(creature.knowledge.best(.9));
    assertNull(receiver.knowledge.best(9));
    assertFalse(receiver.receive(evidence, .95), "same event must not accumulate suspicion");
  }

  @Test
  void interruptedOrDisabledTransmissionsNeverArrive() {
    TileMap map = new TileMap();
    RadioNetwork n = new RadioNetwork();
    Agent a = agent(Agent.Kind.SENTINEL, map, n, 12, 14),
        b = agent(Agent.Kind.DRONE, map, n, 12, 18);
    var e =
        new AgentMemory.Evidence(
            n.nextEvent(), 400, 400, 0, .9, AgentMemory.Source.VISION, "S1", 8);
    n.transmit(a, e, 0);
    a.stun(2);
    n.update(1, List.of(a, b));
    assertNull(b.knowledge.best(1));
    a.radioEnabled = false;
    n.transmit(a, e, 2);
    n.update(3, List.of(a, b));
    assertNull(b.knowledge.best(3));
  }

  @Test
  void sharedSearchReservationsAreUniqueAndRecoverWhenOwnerDisappears() {
    TileMap map = new TileMap();
    RadioNetwork n = new RadioNetwork();
    Agent a = agent(Agent.Kind.SENTINEL, map, n, 12, 14),
        b = agent(Agent.Kind.DRONE, map, n, 12, 18);
    int[] p = n.reserveSearch(a, map, TileMap.center(18), TileMap.center(12), 0, 0);
    int[] q = n.reserveSearch(b, map, TileMap.center(18), TileMap.center(12), 0, 0);
    assertNotNull(p);
    assertNotNull(q);
    assertFalse(Arrays.equals(p, q));
    assertEquals(2, n.reservations());
    n.update(1, List.of(b));
    assertEquals(1, n.reservations());
    n.update(5, List.of(b));
    assertEquals(0, n.reservations());
  }

  @Test
  void losingSightFreezesTheObservedPosition() {
    TileMap map = new TileMap();
    RadioNetwork n = new RadioNetwork();
    Agent a = agent(Agent.Kind.SENTINEL, map, n, 12, 14);
    Player p = new Player();
    p.x = a.x + 100;
    p.y = a.y;
    a.facing = 0;
    for (int i = 0; i < 10; i++) a.update(.04, p, 0, 0, 0, false);
    var seen = a.knowledge.best(.4);
    assertNotNull(seen);
    double rememberedX = seen.x();
    p.x = TileMap.center(3);
    p.y = TileMap.center(3);
    p.moving = false;
    for (int i = 0; i < 20; i++) a.update(.04, p, 0, 0, 0, false);
    assertEquals(rememberedX, a.knowledge.best(1.2).x());
  }

  @Test
  void dynamicAStarReplansForMovingTargetsAndNewObstaclesWithRateLimit() {
    TileMap map =
        new TileMap(
            new int[][] {
              {1, 1, 1, 1, 1, 1, 1},
              {1, 0, 0, 0, 0, 0, 1},
              {1, 0, 0, 0, 0, 0, 1},
              {1, 1, 1, 1, 1, 1, 1}
            });
    DynamicNavigator nav = new DynamicNavigator(map);
    double x = 48, y = 48;
    nav.target(1, 5);
    nav.tick(.01, x, y);
    assertEquals(1, nav.searches);
    assertTrue(nav.path().stream().allMatch(p -> p[0] == 1));
    map.block(1, 3, true);
    nav.tick(.1, x, y);
    assertEquals(1, nav.searches);
    nav.tick(.5, x, y);
    assertEquals(2, nav.searches);
    assertFalse(nav.path().stream().anyMatch(p -> p[0] == 1 && p[1] == 3));
    assertTrue(nav.path().stream().anyMatch(p -> p[0] == 2));
    nav.target(2, 5);
    nav.tick(.56, x, y);
    assertEquals(3, nav.searches);
    assertArrayEquals(new int[] {2, 5}, nav.path().get(nav.path().size() - 1));
    for (int i = 0; i < 100; i++) nav.tick(.01, x, y);
    assertEquals(3, nav.searches, "valid route must be reused");
  }

  @Test
  void globalNavigationBudgetAndNoRouteAreBounded() {
    TileMap map = new TileMap(new int[][] {{0, 1, 0}});
    map.beginNavigationFrame();
    var a = new DynamicNavigator(map);
    var b = new DynamicNavigator(map);
    var c = new DynamicNavigator(map);
    for (var n : List.of(a, b, c)) {
      n.target(0, 2);
      n.tick(.04, 16, 16);
    }
    assertEquals(2, a.searches + b.searches + c.searches);
    assertTrue(a.failed());
    map.beginNavigationFrame();
    c.tick(.04, 16, 16);
    assertEquals(1, c.searches);
  }

  @Test
  void doorsCannotCrushEntitiesAndChangeTheNavigationGraph() {
    GameSession s = new GameSession();
    s.agents.clear();
    s.player.x = TileMap.center(10);
    s.player.y = TileMap.center(12);
    s.toggleDoor();
    assertTrue(s.doors.get(0).closed);
    assertFalse(s.map.walkable(12, 11));
    assertFalse(s.map.walkable(13, 11));
    s.toggleDoor();
    assertTrue(s.map.walkable(12, 11));
    s.player.x = TileMap.center(11);
    s.toggleDoor();
    assertFalse(s.doors.get(0).closed);
  }

  @Test
  void attacksAreTelegraphedAndCanBeDodged() {
    TileMap map = new TileMap();
    Agent a = new Agent(Agent.Kind.SPECIMEN, map, new int[][] {{12, 14}, {12, 18}});
    Player p = new Player();
    p.x = a.x + 24;
    p.y = a.y;
    a.facing = 0;
    for (int i = 0; i < 8; i++) a.update(.04, p, 0, 0, 0, false);
    assertEquals(Agent.State.ATTACK, a.state);
    assertEquals(100, p.health);
    p.x += 85;
    for (int i = 0; i < 12; i++) a.update(.04, p, 0, 0, 0, false);
    assertEquals(100, p.health);
  }

  @Test
  void gamepadAndKeyboardAreOptionalAndUsbEventsAreMapped() {
    try (Gamepad pad = new Gamepad(false)) {
      assertEquals(0, pad.poll(.04).dx());
      pad.accept(-32767, 2, 0);
      assertEquals(-1, pad.poll(.04).dx());
      pad.accept(1, 1, 0);
      assertTrue(pad.poll(.04).interact());
      pad.accept(0, 1, 0);
      pad.accept(1, 1, 1);
      pad.poll(.1);
      pad.accept(0, 1, 1);
      assertTrue(pad.poll(.04).pulse());
      pad.accept(1, 1, 1);
      assertTrue(pad.poll(.4).sprint());
      pad.accept(0, 1, 1);
      assertFalse(pad.poll(.04).pulse());
      pad.accept(1, 1, 3);
      assertTrue(pad.poll(.04).pause());
      pad.clear();
      assertEquals(0, pad.poll(.04).dx());
      pad.accept(1, 1, 2);
      assertFalse(pad.poll(.04).lure());
      pad.accept(0, 1, 2);
      assertTrue(pad.poll(.04).lure());
      pad.accept(1, 1, 2);
      pad.poll(.04);
      pad.accept(1, 1, 3);
      assertTrue(pad.poll(.04).map());
      pad.accept(0, 1, 2);
      assertFalse(pad.poll(.04).lure());
      pad.clear();
      pad.beginRemap();
      for (int button : new int[] {4, 5, 10, 11}) {
        pad.accept(1, 1, button);
        pad.accept(0, 1, button);
      }
      assertEquals("", pad.remapPrompt());
      pad.accept(1, 1, 4);
      assertTrue(pad.poll(.04).interact());
    }
  }

  @Test
  void stereoMixerPreservesQueuedEventsOverlapsAndMute() throws Exception {
    AudioMixer mixer = new AudioMixer();
    mixer.ambienceVolume = 0;
    mixer.threat = 0;
    mixer.exhaustion = 0;
    mixer.play("pulse", 1, -.7);
    mixer.play("radio", 1, .7);
    byte[] pcm = mixer.mix(4410);
    long energy = 0;
    for (int i = 0; i < pcm.length; i += 2)
      energy += Math.abs((short) ((pcm[i] & 255) | (pcm[i + 1] << 8)));
    assertTrue(energy > 100000, "effects must produce audible PCM output");
    mixer.muted = true;
    assertArrayEquals(new byte[2048], mixer.mix(512));
    assertEquals(4922, mixer.framesMixed());
  }

  private static Agent agent(Agent.Kind kind, TileMap map, RadioNetwork n, int r, int c) {
    return new Agent(kind, map, new int[][] {{r, c}, {r + 1, c}}, n);
  }
}

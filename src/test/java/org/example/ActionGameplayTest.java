package org.example;

import static org.junit.jupiter.api.Assertions.*;

import org.example.entities.*;
import org.example.game.*;
import org.example.input.Gamepad;
import org.example.world.TileMap;
import org.junit.jupiter.api.Test;

class ActionGameplayTest {
  @Test
  void sidearmConsumesAmmoHasCooldownAndCannotFireEmpty() {
    var s = new GameSession();
    s.agents.clear();
    assertTrue(s.combat.fire(s, 0));
    assertEquals(7, s.combat.ammo);
    assertFalse(s.combat.fire(s, 0));
    s.update(.04, GameSession.Input.idle());
    assertEquals(7, s.combat.ammo);
    s.combat.cooldown = 0;
    s.combat.ammo = 0;
    assertFalse(s.combat.fire(s, 0));
    assertEquals(0, s.combat.ammo);
  }

  @Test
  void projectileDamagesAndRemovesEnemyAndReleasesRadioOwnership() {
    var s = new GameSession();
    s.agents.clear();
    var enemy = new Agent(Agent.Kind.DRONE, s.map, new int[][] {{13, 7}, {14, 7}}, s.radio);
    enemy.health = 1;
    enemy.stun(5);
    s.agents.add(enemy);
    s.player.x = TileMap.center(4);
    s.player.y = TileMap.center(13);
    s.combat.fire(s, 0);
    for (int i = 0; i < 15; i++) s.update(.04, GameSession.Input.idle());
    assertEquals(1, s.combat.kills);
    assertEquals(1, s.combat.fallen.size());
    assertTrue(s.agents.isEmpty());
  }

  @Test
  void projectileStopsAtClosedSecurityDoor() {
    var s = new GameSession();
    s.agents.clear();
    s.map.block(13, 11, true);
    s.player.x = TileMap.center(9);
    s.player.y = TileMap.center(13);
    var enemy = new Agent(Agent.Kind.SENTINEL, s.map, new int[][] {{13, 13}, {14, 13}}, s.radio);
    enemy.stun(5);
    s.agents.add(enemy);
    s.combat.fire(s, 0);
    for (int i = 0; i < 20; i++) s.update(.04, GameSession.Input.idle());
    assertEquals(3, enemy.health);
    assertTrue(s.combat.rounds.isEmpty());
  }

  @Test
  void dodgeRequiresStaminaPreventsImmediateDamageAndDoesNotCrossWalls() {
    Player p = new Player();
    TileMap map = new TileMap();
    p.x = 45;
    p.y = TileMap.center(13);
    assertTrue(p.dodge(-1, 0));
    assertEquals(76, p.stamina);
    p.damage(30);
    assertEquals(100, p.health);
    assertFalse(p.dodge(-1, 0));
    for (int i = 0; i < 8; i++) p.update(.04, 0, 0, false, false, map);
    assertTrue(map.canOccupy(p.x, p.y, Player.RADIUS));
    assertTrue(p.x >= 40);
    p.dodgeCooldown = 0;
    p.stamina = 10;
    assertFalse(p.dodge(1, 0));
  }

  @Test
  void terminalsTriggerContainmentEventsAndRestockAmmo() {
    var s = new GameSession();
    s.agents.clear();
    s.combat.ammo = 2;
    var d = s.devices.get(0);
    s.player.x = d.x();
    s.player.y = d.y();
    for (int i = 0; i < 40; i++)
      s.update(.04, new GameSession.Input(0, 0, false, false, true, false, false));
    assertEquals(1, s.cores);
    assertEquals(5, s.combat.ammo);
    assertTrue(s.events.blackout > 0);
    assertTrue(s.events.breachPending);
    assertFalse(s.usable(s.devices.get(3))); // reactor still requires all evidence
  }

  @Test
  void liveArcChangesRouteCostsAndCanBeDodged() {
    var s = new GameSession();
    s.agents.clear();
    s.reactor = true;
    s.elapsed = 3.1;
    s.player.x = TileMap.center(18);
    s.player.y = TileMap.center(12);
    int revision = s.map.revision();
    s.update(.04, GameSession.Input.idle());
    assertTrue(s.events.live(0));
    assertTrue(s.player.health < 100);
    assertTrue(s.map.getTile(12, 18).getMovementCost() > 1);
    assertTrue(s.map.revision() > revision);
    s.player.invulnerable = 0;
    s.player.health = 100;
    s.player.dodge(1, 0);
    s.update(.04, GameSession.Input.idle());
    assertEquals(100, s.player.health);
  }

  @Test
  void controllerCombosDoNotSpendAnEmpOrThrowAnUnwantedLure() {
    try (var pad = new Gamepad(false)) {
      pad.accept(1, 1, 0);
      pad.accept(1, 1, 1);
      var shot = pad.poll(.04);
      assertTrue(shot.fire());
      assertFalse(shot.pulse());
      pad.accept(0, 1, 1);
      assertFalse(pad.poll(.04).pulse());
      pad.clear();
      pad.accept(1, 1, 2);
      pad.poll(.04);
      pad.accept(1, 1, 0);
      assertTrue(pad.poll(.04).dodge());
      pad.accept(0, 1, 2);
      assertFalse(pad.poll(.04).lure());
    }
  }
}

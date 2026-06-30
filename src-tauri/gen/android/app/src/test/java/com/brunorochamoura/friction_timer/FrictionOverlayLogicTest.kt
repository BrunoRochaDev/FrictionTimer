package com.brunorochamoura.friction_timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrictionOverlayLogicTest {
  @Test
  fun cooldownIsActiveOnlyBeforeExpiry() {
    assertTrue(FrictionOverlayLogic.isCooldownActive(cooldownUntilMs = 11_000L, nowMs = 10_000L))
    assertFalse(FrictionOverlayLogic.isCooldownActive(cooldownUntilMs = 10_000L, nowMs = 10_000L))
  }

  @Test
  fun cooldownExpiryAddsDurationSeconds() {
    assertEquals(28_000L, FrictionOverlayLogic.cooldownExpiry(nowMs = 8_000L, durationSeconds = 20L))
  }

  @Test
  fun selectMessageRotatesThroughTrimmedMessages() {
    val first = FrictionOverlayLogic.selectMessage(
      messages = listOf(" Pause. Reflect. ", "", "Try again later"),
      nextIndex = 0,
      fallback = "Fallback",
    )
    val second = FrictionOverlayLogic.selectMessage(
      messages = listOf(" Pause. Reflect. ", "", "Try again later"),
      nextIndex = first.nextIndex,
      fallback = "Fallback",
    )

    assertEquals("Pause. Reflect.", first.message)
    assertEquals("Try again later", second.message)
    assertEquals(0, second.nextIndex)
  }

  @Test
  fun selectMessageFallsBackWhenListIsEmpty() {
    val selection = FrictionOverlayLogic.selectMessage(
      messages = listOf(" ", ""),
      nextIndex = 7,
      fallback = "Take a breath.",
    )

    assertEquals("Take a breath.", selection.message)
    assertEquals(0, selection.nextIndex)
  }

  @Test
  fun secondsRemainingRoundsUp() {
    assertEquals(3L, FrictionOverlayLogic.secondsRemaining(2_001L))
    assertEquals(1L, FrictionOverlayLogic.secondsRemaining(1L))
  }
}

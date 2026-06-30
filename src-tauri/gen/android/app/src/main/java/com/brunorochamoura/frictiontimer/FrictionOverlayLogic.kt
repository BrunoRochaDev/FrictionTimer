package com.brunorochamoura.frictiontimer

data class FrictionMessageSelection(
  val message: String,
  val nextIndex: Int,
)

object FrictionOverlayLogic {
  fun isCooldownActive(cooldownUntilMs: Long, nowMs: Long): Boolean = cooldownUntilMs > nowMs

  fun cooldownExpiry(nowMs: Long, durationSeconds: Long): Long =
    nowMs + durationSeconds.coerceAtLeast(0) * 1000L

  fun selectMessage(
    messages: List<String>,
    nextIndex: Int,
    fallback: String,
  ): FrictionMessageSelection {
    val sanitized = sanitizeMessages(messages)
    if (sanitized.isEmpty()) {
      return FrictionMessageSelection(fallback, 0)
    }

    val currentIndex = nextIndex.coerceAtLeast(0) % sanitized.size
    return FrictionMessageSelection(
      message = sanitized[currentIndex],
      nextIndex = (currentIndex + 1) % sanitized.size,
    )
  }

  fun sanitizeMessages(messages: List<String>): List<String> =
    messages.map(String::trim).filter(String::isNotEmpty)

  fun secondsRemaining(millisUntilFinished: Long): Long =
    maxOf(1L, (millisUntilFinished + 999L) / 1000L)
}

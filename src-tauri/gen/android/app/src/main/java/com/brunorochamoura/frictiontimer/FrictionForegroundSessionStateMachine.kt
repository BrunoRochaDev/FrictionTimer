package com.brunorochamoura.frictiontimer

data class FrictionForegroundTransition(
  val dismissActiveOverlay: Boolean = false,
  val packageToEvaluateForOverlay: String? = null,
  val shouldRecheckForeground: Boolean = false,
)

class FrictionForegroundSessionStateMachine(
  private val exitConfirmationObservations: Int = DEFAULT_EXIT_CONFIRMATION_OBSERVATIONS,
) {
  var activeOverlayPackage: String? = null
    private set

  private var pendingExitPackage: String? = null
  private var pendingExitObservations: Int = 0

  fun onOverlayShown(packageName: String) {
    activeOverlayPackage = packageName
    clearPendingExit()
  }

  fun onOverlayDismissed() {
    activeOverlayPackage = null
    clearPendingExit()
  }

  fun onForegroundObserved(packageName: String?): FrictionForegroundTransition {
    val activePackage = activeOverlayPackage
    if (activePackage == null) {
      clearPendingExit()
      return FrictionForegroundTransition(packageToEvaluateForOverlay = packageName)
    }

    if (packageName == null || packageName == activePackage) {
      clearPendingExit()
      return FrictionForegroundTransition()
    }

    if (packageName == pendingExitPackage) {
      pendingExitObservations += 1
    } else {
      pendingExitPackage = packageName
      pendingExitObservations = 1
    }

    if (pendingExitObservations < exitConfirmationObservations) {
      return FrictionForegroundTransition(shouldRecheckForeground = true)
    }

    clearPendingExit()
    return FrictionForegroundTransition(
      dismissActiveOverlay = true,
      packageToEvaluateForOverlay = packageName,
    )
  }

  fun debugState(): String =
    "activeOverlayPackage=$activeOverlayPackage, pendingExitPackage=$pendingExitPackage, pendingExitObservations=$pendingExitObservations"

  private fun clearPendingExit() {
    pendingExitPackage = null
    pendingExitObservations = 0
  }

  companion object {
    private const val DEFAULT_EXIT_CONFIRMATION_OBSERVATIONS = 2
  }
}

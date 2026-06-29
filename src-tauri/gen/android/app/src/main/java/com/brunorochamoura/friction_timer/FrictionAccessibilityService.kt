package com.brunorochamoura.friction_timer

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class FrictionAccessibilityService : AccessibilityService() {
  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
  }

  override fun onInterrupt() {
  }
}

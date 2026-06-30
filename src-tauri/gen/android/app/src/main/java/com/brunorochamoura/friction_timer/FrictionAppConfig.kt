package com.brunorochamoura.friction_timer

data class FrictionAppConfig(
  val appId: String,
  val name: String,
  val waitSeconds: Long,
  val durationSeconds: Long,
  val messages: List<String>,
)

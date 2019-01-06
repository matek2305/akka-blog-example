package com.github.matek2305.djamoe.domain

import java.time.LocalDateTime

trait TimeProvider {
  def getCurrentTime: LocalDateTime
}

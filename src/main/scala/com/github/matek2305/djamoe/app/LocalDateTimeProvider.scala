package com.github.matek2305.djamoe.app

import java.time.LocalDateTime

import com.github.matek2305.djamoe.domain.TimeProvider

class LocalDateTimeProvider extends TimeProvider {
  override def getCurrentTime: LocalDateTime = LocalDateTime.now()
}

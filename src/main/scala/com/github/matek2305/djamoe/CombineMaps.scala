package com.github.matek2305.djamoe

object CombineMaps {
  type Counts = Map[String,Int]
  def combine(x: Counts, y: Counts): Counts = {
    val x0 = x.withDefaultValue(0)
    val y0 = y.withDefaultValue(0)
    val keys = x.keys.toSet.union(y.keys.toSet)
    keys.map{ k => k -> (x0(k) + y0(k)) }.toMap
  }
}

package com.github.matek2305.djamoe

object CombinePointMaps {

  type PointsMap = Map[String, Int]

  def combine(x: PointsMap, y: PointsMap): PointsMap = {
    val keys = x.keys.toSet.union(y.keys.toSet)

    val xDef = x.withDefaultValue(0)
    val yDef = y.withDefaultValue(0)

    keys
      .map { k => k -> (xDef(k) + yDef(k)) }
      .toMap
  }
}

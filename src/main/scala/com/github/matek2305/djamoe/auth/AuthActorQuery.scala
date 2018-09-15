package com.github.matek2305.djamoe.auth

sealed trait AuthActorQuery

object AuthActorQuery {
  final case class GetAccessToken(username: String, password: String) extends AuthActorQuery
  final case class ValidateAccessToken(jwt: String) extends AuthActorQuery
}

package com.github.matek2305.djamoe.auth

sealed trait ValidateAccessTokenResponse

object ValidateAccessTokenResponse {
  final case class TokenIsValid(claims: Map[String, Any]) extends ValidateAccessTokenResponse
  final case object TokenExpired extends ValidateAccessTokenResponse
  final case object ValidationFailed extends ValidateAccessTokenResponse
}

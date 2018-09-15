package com.github.matek2305.djamoe.auth

sealed trait GetAccessTokenResponse

object GetAccessTokenResponse {
  final case class AccessToken(token: String) extends GetAccessTokenResponse
  final case object InvalidCredentials extends GetAccessTokenResponse
}
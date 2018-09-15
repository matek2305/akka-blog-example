package com.github.matek2305.djamoe.auth

sealed trait RegisterResponse

object RegisterResponse {
  final case class UserRegistered(username: String, password: String) extends RegisterResponse
  final case class UsernameTaken(username: String) extends RegisterResponse
}
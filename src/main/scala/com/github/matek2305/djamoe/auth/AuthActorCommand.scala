package com.github.matek2305.djamoe.auth

sealed trait AuthActorCommand

object AuthActorCommand {
  final case class Register(username: String, password: String) extends AuthActorCommand
}

package com.github.matek2305.djamoe.restapi

sealed trait RestApiRequest

object RestApiRequest {

  final case class LoginRequest(username: String, password: String)
    extends RestApiRequest

}



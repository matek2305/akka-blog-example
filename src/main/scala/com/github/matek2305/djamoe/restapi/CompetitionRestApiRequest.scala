package com.github.matek2305.djamoe.restapi

sealed trait CompetitionRestApiRequest

object CompetitionRestApiRequest {

  final case class LoginRequest(username: String, password: String)
    extends CompetitionRestApiRequest

}



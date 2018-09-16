# Djamoe Betting Engine [![Build Status](https://travis-ci.org/matek2305/djamoe-betting-engine.svg?branch=master)](https://travis-ci.org/matek2305/djamoe-betting-engine) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/9d32782adf374748b2ad452257321b42)](https://www.codacy.com/app/matek2305/djamoe-betting-engine?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=matek2305/djamoe-betting-engine&amp;utm_campaign=Badge_Grade) [![Codacy Badge](https://api.codacy.com/project/badge/Coverage/9d32782adf374748b2ad452257321b42)](https://www.codacy.com/app/matek2305/djamoe-betting-engine?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=matek2305/djamoe-betting-engine&amp;utm_campaign=Badge_Coverage)

## Register user
POST to `/register` endpoint
```json
{
  "username": "username",
  "password": "password",
}
```

## Login
POST to `/login` endpoint
```json
{
  "username": "username",
  "password": "password",
}
```

## Make a bet
POST to `/matches/{matchId}/bets` endpoint
```json
{
  "homeTeam": 3,
  "awayTeam": 1,
}
```

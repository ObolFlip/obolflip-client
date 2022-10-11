# ObolFlip Whitepaper

## Synopsis

Idea of ObolFlip is to have a decentralized CoinFlip application on Ergo where users can bet on head or tail.

A single-box SC will handle the state that transforms through betting - cooldown - payout.
Only one ObolFlip can run in state betting.
Payout will share all amounts betted between all winners.

For obvious reasons, there will be no central server handling the SC transitions/off chain code. Here comes the
connection to the current hackathon theme: We will provide an open-source application with which every user can
run the off chain code. Users will earn a share for each transition, this is some kind of mining. Additionally,
for every new round of flip new participation tokens will be minted.

## Concept bullet points

* ObolFlip is a single-box smart contract cycling through the states preparation - betting
* Only one flip in state betting is active at a time. It can be found on the chain by searching for its marker token
* Betting is restricted by a time stamp when the evaluation happens. The last 30 minutes before evaluation is a cooldown period where no bets are accepted any more
* Each active betting period have dedicated tokens for heads or tails which can be purchased by users
* Payout SC holds the funds to be payed out after a cooldown period

## Concept details

### Transitions

ObolFlip will transition through the following states:

```
        
 ---------------------
 |                   | Init 1
 |                   | new_value >= value * 0.01
 |                   | new_R7.0 = R7.0 + 1
 |                   | new_R7.2-4 = constants
 |                   | new_R4 = "ObolFlip #<Round> Heads"
 |                   | new_R6 = 0
 |                   |
 |       --------------------------
 |       | Preparation            |
 |       |------------------------|
 |       | Marker Token           |
 |       | Heads token*           |
 |       |------------------------|
 |       | R4-R6 EIP4 properties  |
 |       |         heads token    |
 |       | R7 Coll[Long]          |
 |       |    0 round number      |
 |       |    1 state = 0         |
 |       |    2 max amount        |
 |       |    3 max time          |
 |       |    4 nERG per ticket   |
 |       |    5 0L                |
 |       |    6 0L                |
 |       |------------------------|
 |                   |     
 |                   | Init 2
 |                   | new_value >= min
 |                   | new_R4 = "ObolFlip #<Round> Tails"
 |                   | new_R6 = 0
 |                   |
 |       --------------------------
 |       | Betting                |
 |       |------------------------|
 |       | Marker Token           |
 |       | Heads token            |------------------|
 |       | Tails token*           |                  | Token purchase (when timestamp < max time or no ticket sold)
 |       |------------------------|                  | new_value >= value + nERG per ticket
 |       | R4-R6 EIP4 properties  |                  | new_heads_num = heads_num - 1 || new_tails_num = tails_num - 1
 |       |          tails token   |                  | new_R7.5 = R7.5 + 1 || new_R7.6 = R7.6 + 1      
 |       | R7 Coll[Long]          |                  |
 |       |    0 round number      |                  | everything else stays the same
 |       |    1 state = 1         | <-----------------
 |       |    2 max amount        |
 |       |    3 max time          |
 |       |    4 nERG per ticket   |
 |       |    5 heads sold        |
 |       |    6 tails sold        |
 |       --------------------------
 |                  | |
 |                  | | Transition to cooldown and preparation
 |                  | | when (value >= R7.2 || timestamp >= R7.3) && tickets sold
 |                  | |
 -------------------  |
                      | new_value >= value * .98 (miner gets 1%)
                      | new_R5    >= R7.3 + 30 minutes
        --------------------------                    ---------------------------
        | Cooldown SC            |                    | Payout SC               |
        |------------------------|                    |-------------------------|
        | Heads token            |                    | Heads token/tails token |------
        | Tails token            |                    |    (winning one)        |     | Payout
        |------------------------|                    |-------------------------|     | new_value = value - (R6 / R5)
        | R4 round number        | Transition when    | R4 round number         |     | token_num = token_num + 1
        | R5 time to transition  | timestamp >= R5    | R5 number winners       |<-----
        | R6 Coll[Long]          |------------------->|     (heads/tails sold)  |       
        |    0 heads sold        | new_value >=       | R6 original value       | 
        |    1 tails sold        |     value * .99    | R7 winning token (0/1)  |
        -------------------------- R6 = new_value     ---------------------------
                                   winning one is from AgeUSD oracle box ID
                                        
```

### Winner

Decision if heads or tails wins a flip is done by checking the current oracle box id and calculating 
a random number. This approach is following how ergo-raffle does the randomization 
([raffle doc](https://github.com/ErgoRaffle/raffle-documentation#pseudo-random-number)).
To prevent last-minute bets while the oracle is already in mempool, we introduced a cooldown time after
last bets take place.

## Contracts

Find the contract sources [here](tree/main/src/test/resources)
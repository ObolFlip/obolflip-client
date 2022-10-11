# obolflip

## Concept 
See [Whitepaper](whitepaper.md)

## Usage

ObolFlip client can be used for taking bets on a flip or for managing flips and earning revenue,
depending on what's legal in your location. You can even deploy it on a server and let other users
do bets, earning extra revenue for withdrawals.

### Prerequisites

* Install Java 11 or higher.
* Download obolflip client from releases page.

### Configuration

After extracting the zip file, take a look at application.properties file. If you just want to make 
a bet, you don't need to change anything here. If you want to take part of decentralized management
of the obol flips, activate the manageFlip option and set a revenue address.

### Running obol flip client

Run obolflip client the following way from a command line:

    java -jar obolflip-version.jar

The server is starting and will present you the address it accepts requests on like this:

    INFO 47269 --- [           main] o.e.obolflip.ObolFlipApplication         : No public url set, auto detected address is
    INFO 47269 --- [           main] o.e.obolflip.ObolFlipApplication         :    ******************************
    INFO 47269 --- [           main] o.e.obolflip.ObolFlipApplication         :    * http://192.168.178.60:8080 *
    INFO 47269 --- [           main] o.e.obolflip.ObolFlipApplication         :    ******************************

The address printed can be visited with a web browser or a Mosaik capable wallet application to place
bets or redeem won bets.

You can stop the server process with Ctrl-C.

### Revenue

If you are using this client client without managing flips, it charges a 1% software usage fee 
when redeeming won bets that is used for maintaining this client.

In case you are using it to manage flips, you will get revenue when you are the first to submit 
transactions for the following actions:

* Closing a flip - 1% revenue
* Completing flip initialisation - 1% revenue from former flip
* Transitioning from cooldown to payout - 1% revenue

From revenues you earn, 30% software usage fee will be withdrawn that is used for maintaining this 
client.


## Build from source

In case you want to build or run from source, clone this repository and run

    ./gradlew bootJar

or

    ./gradlew bootRun

To use on testnet: Change `isMainNet` from in `Constants.kt` and recompile. On Testnet, the 
random box token is always on the same box, thus always winning the same side.
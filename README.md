# Description
A simple server-side configurable economy system with mob rewarding, trading and bounty components for those running public/private Forge servers without plugin capability. This project was made for that reason, a lack of standalone quality economy mods for forge MC 1.20.1, where components aren't present.

# Commands
## Currency component:
The main component of the mod, allowing a player to see their currency, see player's currency and send currency to another player - OP player being able to set currency and remove currency from a player.
### Member commands (Level 0):
- **/currency** - Shows the commanding player's currency.
- **/currency _<target>_** - Shows the targetting player's currency.
- **/currency send _<target> <amouunt>_** - Send currency to a player.

### OP commands (Level 4):
- **/currency remove _<target> <amount>_** - Removes a player's currency by an amount.
- **/currency set _<target> <amount>_** - Sets a player's currency by an amount.

## Trade component:
A secondary component of the mod, allowing a player to initialise a trade with another player from an item held in their hand, accepting, denying and cancelling trades is handle in-chat so player's don't have to type out a small command just to cancel a trade.
### Member commands (Level 0):
- **/trade <amount> _<target> <price>_** - Initialise a trade with a player from a item held in hand.

![Trade request sent example](https://github.com/Mogrul/MogrulEconomy/blob/main/images/trade_sent_example.png?raw=true)
![trade request example](https://github.com/Mogrul/MogrulEconomy/blob/main/images/trade_request_example.png?raw=true)

## Mob rewards component:
A secondary component of the mod, allowing player's to receive a reward for killing a mob - OP's can set a reward for a mob and when a player kills that mob type the player is rewarded by a set amount.
### OP commands (Level 4):
- **/mobrewards set _<mob> <amount>_** - Sets a reward for a mob.
- **/mobrewards remove _<mob>_** - Removes a reward for a mob.

## Bounty component:
A secondary component of the mod, allowing a player to set or add onto a bounty of a player, when the player is killed by another player, the killing player gets the reward of the bounty and the bounty is removed. OP's having the ability to remove a bounty from a player.
### Member commands (Level 0):
- **/bounty set _<target> <price>_** - Sets/adds a bounty to a player.

### OP commands (Level 4):
- **/bounty remove _<target>_** - Removes a player's bounty.

# Configuration
The configuration file of this mod can be found in your server's configuration folder, named **"mogruleconomy-common.toml"** - here you can set the command names, currency name, symbols ect as well as disabling secondary components of the mod.
```
[Currency]
	#Name of the currency.
	currencyName = "Pounds"
	#Command used to interact with the mod. (lowercase, no spaces)
	currencyCommandName = "currency"
	#Symbol of the currency.
	currencySymbol = "£"
	#Starting currency for players.
	#Range: > 0
	startingCurrency = 500

[MobRewards]
	#Enable/Disable mob rewards.
	mobRewardsEnabled = true
	#Command used to interact with the mob rewards component. (lowercase, no spaces)
	mobRewardsCommandName = "mobrewards"

[Trade]
	#Enable/Disable trading system.
	tradeEnabled = true
	#Command used to interact with the trade component. (lowercase, no spaces)
	tradeCommandName = "trade"

[Bounty]
	#Enable/Disable bounty system.
	bountyEnabled = true
	#Command used to interact with the bounty component. (lowercase, no spaces)
	bountyCommandName = "bounty"
```

# Data Handling
The data stored and accessed by this mod can be accessed from a sqlite database file in your server's configuration folder, named **"MogrulEconomy.db"**. Future updates will possibly include the addition of an external networked database for those using PostgreSQL, MySQL ect.


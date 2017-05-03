Checkpoints for faster sync

https://bitcoin.stackexchange.com/questions/22532/how-to-build-a-bitcoinj-checkpoint-file
 -> answer with the least points is the way to go

1. Checkout the bitcoinj project from github:
git clone git@github.com:bitcoinj/bitcoinj.git
2. Go to ./bitcoinj/tools
3. Build checkpoints for mainnet and testnet:
./build-checkpoints --net=MAIN --peer=bitcoin4-fullnode.csg.uzh.ch
./build-checkpoints --net=TEST --peer=bitcoin4-fullnode.csg.uzh.ch
4. Add checkpoints.txt and checkpoints-testnet.txt to the assets folder
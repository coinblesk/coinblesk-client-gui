1. Checkout the bitcoinj project from github
2. Go to ./bitcoinj/tools
3. Build checkpoints for mainnet and testnet:
./build-checkpoints --net=MAIN --peer=bitcoin.sipa.be
./build-checkpoints --net=TEST --peer=testnet-seed.bitcoin.petertodd.org
4. Add checkpoints.txt and checkpoints-testnet.txt to the assets folder
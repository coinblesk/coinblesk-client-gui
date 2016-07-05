package com.coinblesk.client.config;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

import retrofit2.Retrofit;

/**
 * @author Andreas Albrecht
 */
public abstract class AppConfig {

    protected NetworkParameters networkParameters;
    protected String walletFilesPrefix;
    protected String checkpointsFileName;

    protected String coinbleskServerUrl;
    protected String blockchainExplorerUrl;

    private String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
            .append(getName())
            .append("[")
                .append("networkParameters: ").append(getNetworkParameters().getId()).append(", ")
                .append("walletFilesPrefx: ").append(getWalletFilesPrefix()).append(", ")
                .append("coinbleskServerUrl: ").append(getCoinbleskServerUrl()).append(", ")
                .append("blockchainExplorerUrl: ").append(getBlockchainExplorerUrl())
            .append("]");
        return sb.toString();
    }

    public NetworkParameters getNetworkParameters() {
        return networkParameters;
    }

    public String getWalletFilesPrefix() {
        return walletFilesPrefix;
    }

    public String getCheckpointsFileName() {
        return checkpointsFileName;
    }

    public String getCoinbleskServerUrl() {
        return coinbleskServerUrl;
    }

    public String getBlockchainExplorerUrl() {
        return blockchainExplorerUrl;
    }

    private Retrofit getRetrofit() {
        // TODO: return default retrofit instance
        return null;
    }

    public void getCoinbleskService() {
        // TODO: return coinblesk web service
        return;
    }

    /**
     * MAINNET CONFIGURATION
     */
    public static class MainNetConfig extends AppConfig {
        private static AppConfig instance;

        private MainNetConfig() {
            super();
            networkParameters = MainNetParams.get();
            walletFilesPrefix = Constants.WALLET_FILES_PREFIX_MAINNET;
            checkpointsFileName = Constants.CHECKPOINTS_FILE_NAME_MAINNET;
            coinbleskServerUrl = Constants.COINBLESK_SERVER_BASE_URL_PROD;
            blockchainExplorerUrl = Constants.URL_BLOCKTRAIL_EXPLORER_MAINNET;
        }

        public static synchronized AppConfig get() {
            if (instance == null) {
                instance = new MainNetConfig();
            }
            return instance;
        }
    }

    /**
     * TESTNET CONFIGURATION
     */
    public static class TestNetConfig extends AppConfig {
        private static AppConfig instance;

        private TestNetConfig() {
            super();
            networkParameters = TestNet3Params.get();
            walletFilesPrefix = Constants.WALLET_FILES_PREFIX_TESTNET;
            checkpointsFileName = Constants.CHECKPOINTS_FILE_NAME_TESTNET;

            coinbleskServerUrl = Constants.COINBLESK_SERVER_BASE_URL_TEST;
            blockchainExplorerUrl = Constants.URL_BLOCKTRAIL_EXPLORER_TESTNET;
        }

        public static synchronized AppConfig get() {
            if (instance == null) {
                instance = new TestNetConfig();
            }
            return instance;
        }
    }
}

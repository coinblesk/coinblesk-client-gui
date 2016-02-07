package ch.papers.payments;

import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 21/01/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class Constants {
    public final static NetworkParameters PARAMS = TestNet3Params.get();
    public final static BitcoinSerializer BITCOIN_SERIALIZER = new BitcoinSerializer(Constants.PARAMS);
    public final static long UNIX_TIME_MONTH = 60*60*24*30;
    public final static int LOCK_TIME_MONTHS = 3;
    public final static int LOCK_THRESHOLD = 60*4;//https://bitcoin.org/en/developer-reference#block-headers

}

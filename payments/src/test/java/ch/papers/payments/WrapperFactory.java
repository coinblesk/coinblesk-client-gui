package ch.papers.payments;

import com.coinblesk.json.KeyTO;

import org.apache.commons.codec.binary.Base64;
import org.bitcoinj.core.ECKey;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 22/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class WrapperFactory {
    public static ECKey toECKey(KeyTO keyTO){
        return ECKey.fromPublicOnly(Base64.decodeBase64(keyTO.publicKey()));
    }
}

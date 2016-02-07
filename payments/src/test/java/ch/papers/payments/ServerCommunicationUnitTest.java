package ch.papers.payments;

import org.bitcoinj.core.ECKey;
import org.junit.Test;

import java.io.IOException;

import ch.papers.payments.communications.http.MockServerInterface;
import ch.papers.payments.communications.http.ServerInterface;
import ch.papers.payments.models.User;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 06/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ServerCommunicationUnitTest {

    @Test
    public void multisigBootstrap() throws IOException {
        User testUser = new User();
        ServerInterface service = new MockServerInterface();
        User serverUser = service.createUser(new User(testUser.getUuid(), ECKey.fromPublicOnly(testUser.getEcKey().getPubKey()))).execute().body();


    }

}

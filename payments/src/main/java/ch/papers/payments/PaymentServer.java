package ch.papers.payments;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 05/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentServer {
    private final Address address;
    private final Coin amount;
    private final ECKey ecKey;


    private ServerSocket serverSocket;

    public PaymentServer(Coin amount, Address address) {
        this.amount = amount;
        this.address = address;
        this.ecKey = new ECKey();
    }

    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PaymentServer.this.serverSocket = new ServerSocket(8999);
                    Socket clientSocket = null;
                    while ((clientSocket = PaymentServer.this.serverSocket.accept()) != null) {
                        final InputStream inputStream = clientSocket.getInputStream();
                        final OutputStream outputStream = clientSocket.getOutputStream();

                        byte[] buffer = new byte[1024];

                        int readBytes = inputStream.read(buffer);
                        if (readBytes > 0) {

                            UUID id = UUID.randomUUID();
                            ECKey clientPublicKey = ECKey.fromPublicOnly(buffer);
                            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
                            byteBuffer.putLong(amount.getValue());
                            byteBuffer.put(address.getHash160());
                            byteBuffer.put(ecKey.getPubKey());

                            outputStream.write(byteBuffer.array());
                            outputStream.flush();

                            readBytes = inputStream.read(buffer);


                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
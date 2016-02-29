package ch.papers.payments.communications.peers.steps;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public interface Step {
    public int expectedInputLength();
    public byte[] process(byte[] input);
}

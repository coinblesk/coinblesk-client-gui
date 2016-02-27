package ch.papers.communications;

import android.content.Context;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 26/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public abstract class AbstractPeer implements Peer {
    private final Context context;
    private boolean isRunning = false;

    private Thread thread = new Thread();

    protected AbstractPeer(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    protected void stopThread(){
        this.thread.interrupt();
    }

    protected void startThread(Runnable runnable){
        this.thread = new Thread(runnable);
        this.thread.start();
    }
}

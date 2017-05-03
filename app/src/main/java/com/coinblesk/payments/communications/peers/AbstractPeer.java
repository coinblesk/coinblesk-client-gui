package com.coinblesk.payments.communications.peers;

import android.content.Context;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 26/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public abstract class AbstractPeer implements Peer {
    private final Context context;
    private boolean isRunning = false;
    private static final AtomicInteger threadId = new AtomicInteger(0);
    private Thread thread;

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
        String threadName = getClass().getSimpleName()+"-"+threadId.getAndIncrement();
        this.thread = new Thread(runnable, threadName);
        this.thread.start();
    }

    @Override
    public final void start() {
        if(isSupported() && !this.isRunning()) {
            this.setRunning(true);
            this.onStart();
        }
    }

    protected abstract void onStart();

    @Override
    public final void stop() {
        if(this.isRunning()) {
            this.setRunning(false);
            this.onStop();
        }
    }

    protected abstract void onStop();
}

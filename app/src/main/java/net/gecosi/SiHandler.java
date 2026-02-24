/**
 * Copyright (c) 2013 Simon Denier
 * Modified for Android: removed RXTX dependency, accepts SiPort directly.
 */
package net.gecosi;

import java.io.IOException;
import java.util.TooManyListenersException;
import java.util.concurrent.ArrayBlockingQueue;

import net.gecosi.dataframe.SiDataFrame;
import net.gecosi.internal.GecoSILogger;
import net.gecosi.internal.SiDriver;
import net.gecosi.internal.SiPort;

public class SiHandler implements Runnable {

    private ArrayBlockingQueue<SiDataFrame> dataQueue;
    private SiListener siListener;
    private long zerohour;
    private SiDriver driver;
    private Thread thread;

    public SiHandler(SiListener siListener) {
        this.dataQueue = new ArrayBlockingQueue<SiDataFrame>(5);
        this.siListener = siListener;
    }

    public void setZeroHour(long zerohour) {
        this.zerohour = zerohour;
    }

    /**
     * Connect using a pre-configured SiPort (Android USB adapter).
     */
    public void connect(SiPort siPort) throws IOException, TooManyListenersException {
        GecoSILogger.open("######");
        GecoSILogger.logTime("Start USB");
        start();
        driver = new SiDriver(siPort, this).start();
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public Thread stop() {
        if (driver != null) {
            driver.interrupt();
        }
        if (thread != null) {
            thread.interrupt();
        }
        return thread;
    }

    public boolean isAlive() {
        return thread != null && thread.isAlive();
    }

    public void notify(SiDataFrame data) {
        data.startingAt(zerohour);
        dataQueue.offer(data);
    }

    public void notify(CommStatus status) {
        GecoSILogger.log("!", status.name());
        siListener.notify(status);
    }

    public void notifyError(CommStatus errorStatus, String errorMessage) {
        GecoSILogger.error(errorMessage);
        siListener.notify(errorStatus, errorMessage);
    }

    public void run() {
        try {
            SiDataFrame dataFrame;
            while ((dataFrame = dataQueue.take()) != null) {
                siListener.handleEcard(dataFrame);
            }
        } catch (InterruptedException e) {
            dataQueue.clear();
        }
    }
}

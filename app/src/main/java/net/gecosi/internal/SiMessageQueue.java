/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi.internal;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SiMessageQueue extends ArrayBlockingQueue<SiMessage> {

    private long defaultTimeout;

    public SiMessageQueue(int capacity) {
        this(capacity, 2000);
    }

    public SiMessageQueue(int capacity, long defaultTimeout) {
        super(capacity);
        this.defaultTimeout = defaultTimeout;
    }

    public SiMessage timeoutPoll() throws InterruptedException, TimeoutException {
        SiMessage message = poll(defaultTimeout, TimeUnit.MILLISECONDS);
        if (message != null) {
            return message;
        } else {
            throw new TimeoutException();
        }
    }
}

/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi.internal;

public class InvalidMessage extends Exception {

    private SiMessage receivedMessage;

    public InvalidMessage(SiMessage receivedMessage) {
        this.receivedMessage = receivedMessage;
    }

    public SiMessage receivedMessage() {
        return receivedMessage;
    }
}

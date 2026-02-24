/**
 * Copyright (c) 2013 Simon Denier
 * Modified for Android: removed RXTX UnsupportedCommOperationException.
 */
package net.gecosi.internal;

import java.io.IOException;
import java.util.TooManyListenersException;

public interface SiPort {
    public SiMessageQueue createMessageQueue() throws TooManyListenersException, IOException;
    public CommWriter createWriter() throws IOException;
    public void setupHighSpeed() throws IOException;
    public void setupLowSpeed() throws IOException;
    public void close();
}

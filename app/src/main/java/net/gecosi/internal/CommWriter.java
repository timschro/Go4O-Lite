/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi.internal;

import java.io.IOException;

public interface CommWriter {
    public void write(SiMessage message) throws IOException;
}

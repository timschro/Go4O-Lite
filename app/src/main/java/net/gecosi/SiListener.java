/**
 * Copyright (c) 2013 Simon Denier
 */
package net.gecosi;

import net.gecosi.dataframe.SiDataFrame;

public interface SiListener {
    public void handleEcard(SiDataFrame dataFrame);
    public void notify(CommStatus status);
    public void notify(CommStatus errorStatus, String errorMessage);
}

/*
 * $Id$
 * 
 * Copyright (c) 2007, Dmitri Trounine.
 * All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation.  
 */

package org.dtrounine.obid;

/**
 *
 * @author Dmitri Trounine
 */
public class OBIDServiceException extends Exception {
    
    public enum Reason {
        SERIAL_PORT_FAILURE,
        INVALID_DATA_LENGTH,
        CRC_ERROR,
        FRAME_LENGTH_ERROR,
        INVALID_DATA
    }
    
    /** Creates a new instance of OBIDServiceException */
    OBIDServiceException(Reason reason) {
        this.reason = reason;
    }
    
    OBIDServiceException(Throwable cause, Reason reason) {
        super(cause);
        this.reason = reason;
    }
    
    public Reason getReason() {
        return reason;
    }
    
    public String toString() {
        return getClass().getName() + "(" + reason.toString() + ")";
    }
    
    private Reason reason;
}

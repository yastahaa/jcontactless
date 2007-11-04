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
public class SerialPortServiceException extends Exception {
    
    public enum Reason {
        PORT_INITIALIZATION_FAILURE,
        INVALID_PORT_TYPE,
        READ_NOT_ALLOWED,
        WRITE_NOT_ALLOWED,
        SEND_FAILURE,
        RECEIVE_FAILURE,
        TIMEOUT_NOT_SUPPORTED,
        NO_SUCH_PORT,
        PORT_IN_USE,
        RECEIVE_TIMEOUT,
        BUFFER_OVERFLOW
    }

    /** Creates a new instance of SerialPortServiceException */
    SerialPortServiceException(Reason reason) {
        this.reason = reason;
    }
    
    SerialPortServiceException(Throwable cause, Reason reason) {
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

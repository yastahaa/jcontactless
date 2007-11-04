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

package org.dtrounine.contactless;

/**
 *
 * @author Dmitri Trounine
 */
public class Transponder {
    
    public static enum Type {
        ISO_15693,
        ISO_14443A,
        ISO_14443B,
        I_CODE_EPC,
        JEWEL
    }
    
    /** Creates a new instance of Transponder */
    public Transponder(Type type, byte[] uid, int off, int len) {
        this.type = type;
        this.uid = new byte[len];
        System.arraycopy(uid, off, this.uid, 0, len);
        hash = toString().hashCode();
    }
    
    public byte[] getUID() {
        return uid;
    }
    
    public int getUIDLength() {
        return uid.length;
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Transponder)) {
            return false;
        }
        Transponder t = (Transponder) o;
        if (uid.length != t.uid.length) {
            return false;
        }
        for (int i = 0; i < uid.length; i++) {
            if (uid[i] != t.uid[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return hash;
    }
    
    public String toString() {
        StringBuffer res = new StringBuffer(type.toString());
        res.append(':');
        for (int i = 0; i < uid.length; i++) {
            if ((uid[i] & 0xf0) == 0) {
                res.append('0');
            }
            res.append(Integer.toHexString(0x000000ff & (int) uid[i]));
        }
        return res.toString();
    }
    
    private byte[] uid;
    private int hash;
    private Type type;
}

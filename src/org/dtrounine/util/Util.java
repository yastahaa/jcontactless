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

package org.dtrounine.util;

/**
 *
 * @author Dmitri Trounine
 */
public class Util {
    
    private Util() {}
    
    public static String toHex(byte[] data, int off, int len) {
        StringBuffer s = new StringBuffer();
        for (int i = off; i < off + len; i++) {
            if (i > off) {
                s.append(' ');
            }
            s.append(toHex(0x000000ff & data[i]));
        }
        return s.toString();
    }
    
    public static String toHex(int x) {
        StringBuffer s = new StringBuffer();
        s.append("0x");
        if (x < 16) {
            s.append('0');
        }
        s.append(Integer.toHexString(0x000000ff & x));
        return s.toString();
    }
    
    private static final int CRC_POLYNOM = 0x8408;
    private static final int CRC_PRESET = 0xFFFF;
    
    public static int updateCRC(int crc, byte[] data, int off, int len) {
        for (int i = off; i < off + len; i++) {
            crc ^= 0x000000FF & (int) data[i];
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >> 1) ^ CRC_POLYNOM;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc;
    }
    
    public static int updateCRC(int crc, byte value) {
        crc ^= 0x000000FF & (int) value;
        for (int j = 0; j < 8; j++) {
            if ((crc & 1) != 0) {
                crc = (crc >> 1) ^ CRC_POLYNOM;
            } else {
                crc >>= 1;
            }
        }
        return crc;
    }
    
    public static int getCRC(byte value) {
        return updateCRC(CRC_PRESET, value);
    }
    
    public static int getCRC(byte[] data, int off, int len) {
        return updateCRC(CRC_PRESET, data, off, len);
    }
    
}

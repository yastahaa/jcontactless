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

import java.util.ArrayList;
import java.util.List;
import javax.comm.SerialPort;
import org.dtrounine.contactless.Transponder;
import org.dtrounine.util.Util;

/**
 *
 * @author Dmitri Trounine
 */
public class OBIDService implements Constants {
    
    /**
     * Creates a new instance of OBIDService for communicating
     * to OBID device on specified port.
     *
     * @param   portName    name of the port in the system to
     *      which the OBID device is attached.
     */
    public OBIDService(String portName) throws OBIDServiceException {
        try {
            this.portService = new SerialPortService(portName);
        } catch (SerialPortServiceException e) {
            throw new OBIDServiceException(e,
                    OBIDServiceException.Reason.SERIAL_PORT_FAILURE);
        }
        this.buffer = portService.getBuffer();
        negociatePortParams();
    }
    
    public byte[] getBuffer() {
        return buffer;
    }
    
    public int sendOBIDCommand(int len) 
            throws OBIDServiceException {
        int responseLen;
        int crc;
        
        if (len + 4 > MAX_OUTGOING_FRAME_LENGTH) {
            throw new OBIDServiceException(
                    OBIDServiceException.Reason.INVALID_DATA_LENGTH);
        }
        buffer[FRAME_LENGTH_OFFSET] = (byte) (len + 4);
        buffer[FRAME_ADDRESS_OFFSET] = DEFAULT_DEVICE_ADDRESS;
        crc = Util.getCRC((byte) (len + 4));
        crc = Util.updateCRC(crc, DEFAULT_DEVICE_ADDRESS);
        crc = Util.updateCRC(crc, buffer, FRAME_OUTGOING_DATA_OFFSET, len);
        buffer[FRAME_OUTGOING_DATA_OFFSET + len] = (byte) (0x000000ff & crc);
        crc >>= 8;
        buffer[FRAME_OUTGOING_DATA_OFFSET + len + 1] = 
                (byte) (0x000000ff & crc);
        try {
            portService.sendFrame(len + 4);
            responseLen = portService.receiveFrame();
        } catch (SerialPortServiceException e) {
            throw new OBIDServiceException(e,
                    OBIDServiceException.Reason.SERIAL_PORT_FAILURE);
        }
        if (responseLen < 3) {
            throw new OBIDServiceException(
                    OBIDServiceException.Reason.FRAME_LENGTH_ERROR);
        }
        checkCRC(responseLen);
        return responseLen;
    }
    
    public void sendISOHostCommand(int len) throws OBIDServiceException {
        buffer[FRAME_OUTGOING_DATA_OFFSET] = (byte) 0xb0;
        sendOBIDCommand(len + 1);
    }
    
    public void cpuReset() throws OBIDServiceException {
        buffer[FRAME_OUTGOING_DATA_OFFSET] = (byte) 0x63;
        sendOBIDCommand(1);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
    }
    
    public List<Transponder> inventory() throws OBIDServiceException {
        buffer[ISO_HOST_OUTGOING_DATA_OFFSET] = (byte) 0x01;
        buffer[ISO_HOST_OUTGOING_DATA_OFFSET + 1] = (byte) 0x00;
        sendISOHostCommand(2);
        int off = ISO_HOST_INCOMING_DATA_OFFSET;
        int dataSets = 0x000000ff & (int) buffer[off++];
        List<Transponder> res = new ArrayList<Transponder>(dataSets);
        if (buffer[ISO_HOST_STATUS_OFFSET] == (byte) 0x01) {
            // No transponders in range
            return res;
        }
        while (dataSets-- > 0) {
            Transponder.Type type;
            Transponder tr;
            switch (buffer[off++]) {
            case (byte) 0x03:
                type = Transponder.Type.ISO_15693;
                tr = new Transponder(type, buffer, off + 1, 8);
                off += 9;
                break;
            case (byte) 0x04:
                type = Transponder.Type.ISO_14443A;
                if ((buffer[off++] & (byte) 0x40) != 0) {
                    tr = new Transponder(type, buffer, off + 1, 10);
                    off += 11;
                } else {
                    tr = new Transponder(type, buffer, off + 1, 7);
                    off += 8;
                }
                break;
            case (byte) 0x05:
                type = Transponder.Type.ISO_14443B;
                tr = new Transponder(type, buffer, off + 1, 4);
                off += 9;
                break;
            case (byte) 0x06:
                type = Transponder.Type.I_CODE_EPC;
                tr = new Transponder(type, buffer, off + 1, 8);
                off += 8;
                break;
            case (byte) 0x08:
                type = Transponder.Type.JEWEL;
                tr = new Transponder(type, buffer, off + 2, 6);
                off += 8;
                break;
            default:
                throw new OBIDServiceException(
                        OBIDServiceException.Reason.INVALID_DATA);
            }
            res.add(tr);
        }
        return res;
    }
    
    public void baudRateDetection() throws OBIDServiceException {
        buffer[FRAME_OUTGOING_DATA_OFFSET] = (byte) 0x52;
        buffer[FRAME_OUTGOING_DATA_OFFSET + 1] = (byte) 0;
        sendOBIDCommand(2);
    }
    
    public void getSoftwareVersion() throws OBIDServiceException {
        buffer[FRAME_OUTGOING_DATA_OFFSET] = (byte) 0x65;
        sendOBIDCommand(1);
    }
    
    public void dispose() {
        if (portService != null) {
            portService.dispose();
        }
    }

    protected void finalize() throws Throwable {
        dispose();
    }
    
    protected void negociatePortParams() throws OBIDServiceException {
        boolean ok = false;
        
        System.out.println("OBIDService: negociating port speed...");
negociate:
        for (int parity : standardParities) {
            for (int baudRate : standardBaudRates) {
                try {
                    System.out.println("Trying baudRate=" + baudRate +
                            " parity=" + parity);
                    portService.setPortParams(baudRate, parity);
                    baudRateDetection();
                    ok = true;
                    break negociate;
                } catch (SerialPortServiceException e) {// Ignore and try again
                } catch (OBIDServiceException e) {} //Ignore and try again
            }
        }
        if (!ok) {
            throw new OBIDServiceException(
                    OBIDServiceException.Reason.SERIAL_PORT_FAILURE);
        }
    }
    
    protected void checkCRC(int receivedLength) throws OBIDServiceException {
        int crc = Util.getCRC(buffer, 0, receivedLength - 2);
        if ((byte) (0x000000ff & crc) != buffer[receivedLength - 2]) {
            throw new OBIDServiceException(
                    OBIDServiceException.Reason.CRC_ERROR);
        }
        crc >>= 8;
        if ((byte) (0x000000ff & crc) != buffer[receivedLength - 1]) {
            throw new OBIDServiceException(
                    OBIDServiceException.Reason.CRC_ERROR);
        }
        
    }
    
    protected static final byte DEFAULT_DEVICE_ADDRESS = (byte) 0xff;

    static final int[] standardBaudRates = {
        115200, 57600, 38400, 19200, 9600, 4800, 2400, 1200
    };
    
    static final int[] standardParities = {
        SerialPort.PARITY_EVEN, SerialPort.PARITY_ODD, SerialPort.PARITY_NONE
    };

    
    protected static final int MAX_OUTGOING_FRAME_LENGTH = 255;
    protected byte[] buffer;
    protected SerialPortService portService;
}

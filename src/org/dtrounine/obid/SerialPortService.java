/*
 * SerialPortService.java
 *
 * Created on November 1, 2007, 8:24 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.dtrounine.obid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.comm.CommPortIdentifier;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.UnsupportedCommOperationException;
import org.dtrounine.util.Util;

/**
 *
 * @author Dmitri Trounine
 */
public class SerialPortService implements Constants {

    public SerialPortService(String portName)
            throws SerialPortServiceException {
        this(portName, DEFAULT_BUFFER_LENGTH);
    }
    
    /** Creates a new instance of SerialPortService */
    public SerialPortService(String portName, int bufferLength) 
            throws SerialPortServiceException {
        openPort(portName);
        try {
            in = port.getInputStream();
        } catch (IOException e) {
            throw new SerialPortServiceException(e,
                    SerialPortServiceException.Reason.READ_NOT_ALLOWED);
        }
        try {
            out = port.getOutputStream();
        } catch (IOException e) {
            throw new SerialPortServiceException(e,
                    SerialPortServiceException.Reason.WRITE_NOT_ALLOWED);
        }
        try {
            port.enableReceiveTimeout(RECEIVE_TIMEOUT);
            if (!port.isReceiveTimeoutEnabled()) {
                throw new UnsupportedCommOperationException();
            }
            System.out.println("Set receive timeout: " + RECEIVE_TIMEOUT 
                    + "ms");
        } catch (UnsupportedCommOperationException e) {
            throw new SerialPortServiceException(e,
                    SerialPortServiceException.Reason.TIMEOUT_NOT_SUPPORTED);
        }
        buffer = new byte[bufferLength];
    }
    
    public void sendFrame(int len) throws SerialPortServiceException {
        if (state == STATE_RECEIVING) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {}
            state = STATE_SENDING;
        }
        try {
            System.out.println(System.currentTimeMillis() + " >>> " + Util.toHex(buffer, 0, len));
            out.write(buffer, 0, len);
            out.flush();
        } catch (IOException e) {
            throw new SerialPortServiceException(e,
                    SerialPortServiceException.Reason.SEND_FAILURE);
        }
    }
    
    public int receiveFrame() throws SerialPortServiceException {
        int bytesRead;
        int off;

        state = STATE_RECEIVING;
        try {
            port.enableReceiveTimeout(RECEIVE_TIMEOUT);
        } catch (UnsupportedCommOperationException e) {}

        try {
            off = 0;
            while (true) {
                bytesRead = in.read(buffer, off, buffer.length - off);
                try {
                    port.enableReceiveTimeout(20);
                } catch (UnsupportedCommOperationException e) {}
                if (bytesRead <= 0) {
                    break;
                }
                off += bytesRead;
                if (off >= buffer.length) {
                    throw new SerialPortServiceException(
                            SerialPortServiceException.Reason.BUFFER_OVERFLOW);
                }
            }
/*            
            responseLen = in.read();
            if (responseLen == -1) {
                System.out.println("TIMEOUT");
                throw new SerialPortServiceException(
                        SerialPortServiceException.Reason.RECEIVE_TIMEOUT);
            }
            buffer[FRAME_LENGTH_OFFSET] = (byte) (0x000000ff & responseLen);
            System.out.print(System.currentTimeMillis() + "<<< " 
                    + Util.toHex(responseLen));
            if (responseLen < 3) {
                throw new SerialPortServiceException(
                        SerialPortServiceException.Reason.FRAME_LENGTH_ERROR);
            }
            

            off = FRAME_LENGTH_OFFSET + 1;
            while (off != responseLen) {
                try {
                    port.enableReceiveTimeout(12);
                    if (!port.isReceiveTimeoutEnabled()) {
                        throw new UnsupportedCommOperationException();
                    }
                } catch (UnsupportedCommOperationException e) {
                    throw new SerialPortServiceException(
                            SerialPortServiceException.Reason.TIMEOUT_NOT_SUPPORTED);
                }
                int res = in.read(buffer, off, buffer.length - off);
                if (res == -1) {
                    System.out.println("TIMEOUT");
                    throw new SerialPortServiceException(
                            SerialPortServiceException.Reason.RECEIVE_TIMEOUT);
                }
                off += res;
            }
 */
            System.out.print(System.currentTimeMillis() + " <<< " 
                    + Util.toHex(buffer, 0, off));
        } catch (IOException e) {
            throw new SerialPortServiceException(e,
                    SerialPortServiceException.Reason.RECEIVE_FAILURE);
        } finally {
            System.out.println();
        }
        return off;
    }

    public int exchangeFrame(int len) 
            throws SerialPortServiceException {
        sendFrame(len);
        return receiveFrame();
    }
    
    public int getBufferSize() {
        return buffer.length;
    }
    
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * Closes communication port. This method should be invoked when
     * there are no more need in OBIDService, and the system resources
     * (serial port) should be freed.
     */ 
    public void dispose() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
        if (port != null) {
            port.close();
        }
    }
    
    protected void finalize() throws Throwable {
        dispose();
    }
    
    protected static final int MAX_PORT_INIT_ATTEMPTS = 5;
    
    protected static final int PORT_OPEN_TIMEOUT = 1000;
    
    protected static final int RECEIVE_TIMEOUT = 1000;
    
    protected static final int DEFAULT_BUFFER_LENGTH = 256;

    
    
    protected SerialPort port;
    protected InputStream in;
    protected OutputStream out;
    protected byte[] buffer;

    protected void openPort(String portName)
            throws SerialPortServiceException {
        CommPortIdentifier portId;
        
        try {
            portId = CommPortIdentifier.getPortIdentifier(portName);
        } catch (NoSuchPortException e) {
            throw new SerialPortServiceException(e,
                    SerialPortServiceException.Reason.NO_SUCH_PORT);
        }
        if (portId.getPortType() != CommPortIdentifier.PORT_SERIAL) {
            throw new SerialPortServiceException(
                    SerialPortServiceException.Reason.INVALID_PORT_TYPE);
        }
        try {
            port = (SerialPort) portId.open(this.getClass().getCanonicalName(),
                    PORT_OPEN_TIMEOUT);
        } catch (PortInUseException e) {
            throw new SerialPortServiceException(e,
                    SerialPortServiceException.Reason.PORT_IN_USE);
        }
    }
    
    protected void setPortParams(int baudRate, int parity) 
            throws SerialPortServiceException {
        boolean paramsSet = false;
        int attempts = MAX_PORT_INIT_ATTEMPTS;
        while (attempts-- > 0) {
            try {
                port.setSerialPortParams(baudRate, SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
                // success
                paramsSet = true;
                break;
            } catch (Exception e) {} // Ignore and try again
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {}
        }
        if (!paramsSet) {
            port.close();
            throw new SerialPortServiceException(
                    SerialPortServiceException.Reason
                            .PORT_INITIALIZATION_FAILURE);
        }
    }
    
    private int state = STATE_IDLE;
    
    private static final int STATE_SENDING = 1;
    private static final int STATE_RECEIVING = 2;
    private static final int STATE_IDLE = 3;
}

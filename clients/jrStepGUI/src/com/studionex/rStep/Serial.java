package com.studionex.rStep;

/*
 * Copyright 2010 Jean-Louis Paquelin
 * 
 * This file is part of jrStepGUI.
 * 
 * jrStepGUI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * 
 * jrStepGUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with jrStepGUI.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact info: jrstepgui@studionex.com
 */

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import java.util.Vector;

public class Serial implements SerialPortEventListener {
	// TODO: queuing data to send?
	private CommPortIdentifier portId;
	private SerialPort serialPort;

	private BufferedReader bufferedInputStream;
	private OutputStream outputStream;
	
	public Serial(String portName)
	throws PortInUseException, IOException, TooManyListenersException, UnsupportedCommOperationException {
		this(
				portName,
				"JavaSerial", 5000,
				9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
	}
	
	public Serial(
			String portName,
			String applicationName, int portOpenTimeout,
			int communicationSpeed, int dataBits, int stopBits, int parity)
	throws PortInUseException, IOException, TooManyListenersException, UnsupportedCommOperationException {
		
		portId = getPortIdFromPortName(portName);
		
		if(portId != null) {
			try {
				serialPort = (SerialPort) portId.open(applicationName, portOpenTimeout);
				bufferedInputStream = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
				outputStream = serialPort.getOutputStream();

				serialPort.addEventListener(this);
				serialPort.notifyOnDataAvailable(true);

				serialPort.setSerialPortParams(communicationSpeed, dataBits, stopBits, parity);
			} catch (IOException e) {
				// something went wrong
				close();
				throw e;
			} catch (TooManyListenersException e) {
				// something went wrong
				close();
				throw e;
			} catch (UnsupportedCommOperationException e) {
				// something went wrong
				close();
				throw e;
			}
		}
	}
	
	public boolean isOpen() {
		return (bufferedInputStream != null) && (outputStream != null);
	}
	
	public String getPortName() {
		if(isOpen())
			return portId.getName();
		return null;
	}
	
	public void send(String s) throws IOException {
		synchronized(outputStream) {
			outputStream.write(s.getBytes());
		}
	}
	
	public String waitForInput() throws IOException {
		synchronized(bufferedInputStream) {
			while(!bufferedInputStream.ready())
				try {
					bufferedInputStream.wait();
				} catch (InterruptedException e) {
					// TODO: log this?
					e.printStackTrace();
				}
			return bufferedInputStream.readLine();
		}
	}

	public String waitForInput(long timeout) throws IOException {
		synchronized(bufferedInputStream) {
			if(!bufferedInputStream.ready())
				try {
					bufferedInputStream.wait(timeout);
				} catch (InterruptedException e) {
					// TODO: log this?
					e.printStackTrace();
				}
			if(bufferedInputStream.ready())
				return bufferedInputStream.readLine();
			else
				return null;
		}
	}
	
	public void setDTR(boolean dtr) {
		serialPort.setDTR(dtr);
	}

	public boolean isDTR() {
		return serialPort.isDTR();
	}

	public void close() {
		if(serialPort != null) {
			if(bufferedInputStream != null) {
				try {
					bufferedInputStream.close();
					bufferedInputStream = null;
				} catch (IOException e) {
					// TODO: log this?
					e.printStackTrace();
				}
			}
			if (outputStream != null) {
				try {
					outputStream.close();
					outputStream = null;
				} catch (IOException e) {
					// TODO: log this?
					e.printStackTrace();
				}
			}
			serialPort.close();
			serialPort = null;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	public void serialEvent(SerialPortEvent serialPortEvent) {
		// it isn't necessary to check serialPortEvent.getEventType() against SerialPortEvent.DATA_AVAILABLE
		// as it is only registered to notify on DATA_AVAILABLE
		synchronized(bufferedInputStream) {
			bufferedInputStream.notify();
		}
	}

	/**
	 * 
	 * @param portName
	 * @return the port id related to the portName
	 */
	private static CommPortIdentifier getPortIdFromPortName(String portName) {
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
		while(portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
			if((portId.getPortType() == CommPortIdentifier.PORT_SERIAL) && portId.getName().equals(portName)) {
				return portId;
			}
		}
		
		return null;
	}
	
	public static String[] getAvailablePorts() {
		Vector<String> availablePorts = new Vector<String>();
		
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
		while(portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
			availablePorts.add(portId.getName());
		}

		return availablePorts.toArray(new String[0]);
	}
}

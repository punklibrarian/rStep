package com.studionex.misc.ui;

/*
 * Copyright 2010 Jean-Louis Paquelin
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact info: jlp@studionex.com
 */

import java.awt.GridLayout;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

/**
 * @see http://unserializableone.blogspot.com/2009/01/redirecting-systemout-and-systemerr-to.html
 */
@SuppressWarnings("serial")
public class Console extends JPanel {
	public static final int DEFAULT_BUFFER_SIZE = 10000;
	
	public enum SystemStreams {OUT, ERR, OUTERR};
	private SystemStreams stream;

	private JTextArea jTextArea;
	private int bufferSize;

	public Console(SystemStreams stream) throws IOException {
		this(stream, DEFAULT_BUFFER_SIZE);
    }
	
	public Console(SystemStreams stream, int bufferSize) throws IOException {
		// Add a scrolling text area
		jTextArea = new JTextArea();
		jTextArea.setEditable(false);
		this.setLayout(new GridLayout(1, 1));
		this.add(new JScrollPane(jTextArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS));
		setVisible(true);

		this.stream = stream;
		this.bufferSize = bufferSize;
		redirectSystemStreams();
    }
	
	private void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				updateTextArea(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateTextArea(new String(b, off, len));
			}

			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};

		if(stream != SystemStreams.ERR) {
			// Set up System.out
			System.setOut(new PrintStream(out, true));
		}
		
		if(stream != SystemStreams.OUT) {
			// Set up System.err
			System.setErr(new PrintStream(out, true));
		}
	}

	private void updateTextArea(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				jTextArea.append(text);

				try {
					int documentLength = jTextArea.getDocument().getLength();
					if(documentLength >= bufferSize) {
						// search the end position of the first line
						int p = 0;
						while((p < documentLength) && !jTextArea.getDocument().getText(p, 1).equals("\n"))
							p++;
						if(p < documentLength) {
							// remove the first line
							jTextArea.getDocument().remove(0, p + 1);
						}
					}
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
	}

	/**
	 * This method was written for test purposes.
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame jFrame = new JFrame();
				jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				
				try {
					Console outConsole = new Console(Console.SystemStreams.OUT, 500);
					jFrame.getContentPane().add(outConsole);
				} catch (IOException e) {
					e.printStackTrace();
				}
				jFrame.setSize(640, 480);
				jFrame.setVisible(true);
				
				(new Thread() {

					public void run() {
						int i = 0;
						while(true) {
							System.out.print("i = " + (i++));
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							System.out.println(" 0123456789");
						}
					}
				}).start();
			}
		});
	}
}

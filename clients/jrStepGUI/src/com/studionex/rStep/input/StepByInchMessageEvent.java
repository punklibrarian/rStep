package com.studionex.rStep.input;

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

@SuppressWarnings("serial")
public class StepByInchMessageEvent extends InputEvent {
	private final int x;
	private final int y;
	private final int z;

	public StepByInchMessageEvent(Object source, int x, int y, int z) {
		super(source);
		
		this.x = x;
		this.y = y;
		this.z = z;
	}

	protected int getX() { return x; }
	protected int getY() { return y; }
	protected int getZ() { return z; }

	@Override
	public String toString() {
		return "Step by inch: " + getX() + ", " + getY() + ", " + getZ();
	}
}

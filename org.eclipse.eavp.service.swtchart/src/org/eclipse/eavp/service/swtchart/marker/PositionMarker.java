/*******************************************************************************
 * Copyright (c) 2017 Lablicate GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Dr. Philip Wenig - initial API and implementation
 *******************************************************************************/
package org.eclipse.eavp.service.swtchart.marker;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.swtchart.ICustomPaintListener;

public class PositionMarker implements ICustomPaintListener {

	private int x;
	private int y;
	private Color foregroundColor;
	private boolean drawInfo;

	public PositionMarker() {
		foregroundColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
		drawInfo = true;
	}

	public void setActualPosition(int x, int y) {

		this.x = x;
		this.y = y;
	}

	public boolean isDrawInfo() {

		return drawInfo;
	}

	public void setDrawInfo(boolean drawInfo) {

		this.drawInfo = drawInfo;
	}

	@Override
	public void paintControl(PaintEvent e) {

		/*
		 * Plots a vertical/horizontal line in the plot area at the x position of the mouse.
		 */
		if(drawInfo) {
			e.gc.setForeground(foregroundColor);
			if(x > 0 && x < e.width && y > 0 && y < e.height) {
				e.gc.drawLine(x, 0, x, e.height);
				e.gc.drawLine(0, y, e.width, y);
			}
		}
	}

	@Override
	public boolean drawBehindSeries() {

		return false;
	}
}

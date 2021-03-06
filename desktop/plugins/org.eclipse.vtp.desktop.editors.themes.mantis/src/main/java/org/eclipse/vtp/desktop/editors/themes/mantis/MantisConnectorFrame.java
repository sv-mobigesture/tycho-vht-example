/*--------------------------------------------------------------------------
 * Copyright (c) 2004, 2006-2009 OpenMethods, LLC
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Trip Gilman (OpenMethods), Lonnie G. Pryor (OpenMethods)
 *    - initial API and implementation
 -------------------------------------------------------------------------*/
package org.eclipse.vtp.desktop.editors.themes.mantis;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.vtp.desktop.core.util.Geom;
import org.eclipse.vtp.desktop.editors.themes.core.ConnectorFrame;
import org.eclipse.vtp.desktop.editors.themes.core.ElementFrame;
import org.eclipse.vtp.desktop.editors.themes.core.commands.CommandListener;
import org.eclipse.vtp.desktop.editors.themes.core.commands.StartMove;
import org.eclipse.vtp.desktop.model.core.design.IDesignConnector;
import org.eclipse.vtp.desktop.model.core.design.IDesignConnectorLabel;
import org.eclipse.vtp.desktop.model.core.design.IDesignConnectorMidpoint;
import org.eclipse.vtp.desktop.model.core.design.IDesignElementConnectionPoint;

/**
 * This is the Mantis theme's implementation of the connector frame interface.
 * The Mantis connector supports mid-points and is drawn with solid lines and
 * hard angles at line joints.  The arrow head is stylized similar to an
 * antenna and when the line has multiple segments, each segment except the
 * one with the arrow head has tick marks indicating the direction of the line
 * midway down that line segment.
 * 
 * @author trip
 */
public class MantisConnectorFrame extends MantisComponentFrame implements ConnectorFrame
{
	/** The ui connector being represented */
	private IDesignConnector uiConnector;
	/**	Indicates whether this connector is selected */
	private boolean selected = false;
	/**	The upper left corner of the rectangle that would contain this path */
	private Point labelUpperLeft;
	/**	The lower right corner of the rectangle that would contain this path */
	private Point labelLowerRight;
	/** The target of the current drag action */
	private Object dragTarget = null;
	/**	The element frame this connector begins at */
	ElementFrame source;
	/**	The element frame this connector ends at */
	ElementFrame destination; 
	/**	The current size of the rectangle that contains this connector's label */
	Point labelSize = null;
	
	/**
	 * Creates a new connector frame instance that represents the provided ui
	 * connector and begins at the source element frame and ends at the
	 * destination element frame.
	 * 
	 * @param source The element frame this connector begins at
	 * @param destination The element frame this connector ends at
	 * @param uiConnector The ui connector this frame represents
	 */
	public MantisConnectorFrame(ElementFrame source, ElementFrame destination, IDesignConnector uiConnector)
	{
		super(uiConnector);
		this.source = source;
		this.destination = destination;
		this.uiConnector = uiConnector;
		uiConnector.addListener(this);
		uiConnector.addPropertyListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.vtp.desktop.editors.core.theme.ConnectorFrame#getUIConnector()
	 */
	public IDesignConnector getDesignConnector()
	{
		return uiConnector;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.vtp.desktop.editors.core.theme.ComponentFrame#adjustPosition(int, int)
	 */
	public void adjustPosition(int xoffset, int yoffset)
	{
		if(dragTarget != null)
		{
			if(dragTarget instanceof IDesignConnectorMidpoint)
			{
				((IDesignConnectorMidpoint)dragTarget).adjustPosition(xoffset, yoffset);
			}
			else if(dragTarget instanceof IDesignConnectorLabel)
			{
				IDesignConnectorLabel lineLabel = (IDesignConnectorLabel)dragTarget;
				Point offset = lineLabel.getOffsetPosition();
				lineLabel.setPosition(lineLabel.getAnchorSegment(), offset.x + xoffset, offset.y + yoffset);
			}
		}
		else
		{
			List<IDesignConnectorMidpoint> midPoints = uiConnector.getMidpoints();
			for(int i = 0; i < midPoints.size(); i++)
			{
				IDesignConnectorMidpoint midPoint = midPoints.get(i);
				midPoint.adjustPosition(xoffset, yoffset);
			}
		}
		this.fireChange();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.vtp.desktop.editors.core.theme.ComponentFrame#getBounds()
	 */
	public Rectangle getBounds()
	{
		if(labelUpperLeft == null)
			return null;
		Rectangle ret = new Rectangle(labelUpperLeft.x, labelUpperLeft.y, labelLowerRight.x - labelUpperLeft.x + 1, labelLowerRight.y - labelUpperLeft.y + 1);

		Point[] ps = uiConnector.getPoints();
		for(int i = 0; i < (ps.length - 1); i++)
		{
			ret.add(new Rectangle(Math.min(ps[i].x, ps[i + 1].x) - 10, Math.min(ps[i].y, ps[i + 1].y) - 10, Math.abs(ps[i + 1].x - ps[i].x) + 20, Math.abs(ps[i + 1].y - ps[i].y) + 20));
		}
		return ret;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.vtp.desktop.editors.themes.mantis.MantisComponentFrame#mouseDoubleClick(org.eclipse.vtp.desktop.editors.core.commands.CommandListener, int, int, int)
	 */
	public void mouseDoubleClick(CommandListener commandListener, int x, int y, int modifiers)
	{
		super.mouseDoubleClick(commandListener, x, y, modifiers);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.vtp.desktop.editors.core.theme.ComponentFrame#mouseDown(org.eclipse.vtp.desktop.editors.core.commands.CommandListener, int, int, int)
	 */
	public void mouseDown(CommandListener commandListener, int x, int y, int modifiers)
	{
		Rectangle labelRect = new Rectangle(labelUpperLeft.x, labelUpperLeft.y, labelLowerRight.x - labelUpperLeft.x, labelLowerRight.y - labelUpperLeft.y);
		if(labelRect.contains(x, y))
		{
			dragTarget = uiConnector.getConnectorLabel();
			commandListener.executeCommand(new StartMove());
		}
		else
		{
			List<IDesignConnectorMidpoint> midPoints = uiConnector.getMidpoints();
			for(int i = 0; i < midPoints.size(); i++)
			{
				IDesignConnectorMidpoint midPoint = midPoints.get(i);
				Point midPointCenter = midPoint.getPosition();
				if(new Rectangle(midPointCenter.x - 4, midPointCenter.y - 4, 8, 8).contains(x, y))
				{
					dragTarget = midPoint;
					commandListener.executeCommand(new StartMove());
					break;
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.vtp.desktop.editors.core.theme.ComponentFrame#mouseMove(org.eclipse.vtp.desktop.editors.core.commands.CommandListener, int, int, int)
	 */
	public void mouseMove(CommandListener commandListener, int x, int y, int modifiers)
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.vtp.desktop.editors.core.theme.ComponentFrame#mouseUp(org.eclipse.vtp.desktop.editors.core.commands.CommandListener, int, int, int)
	 */
	public void mouseUp(CommandListener commandListener, int x, int y, int modifiers)
	{
	}

	/**
	 * Determines the center point of the line segment of this connector with
	 * the given index.
	 * 
	 * @param idex The index of the line segment
	 * @return The line segment center point
	 */
	public Point getLineSegmentCenterPoint(int idex)
	{
		Point[] points = uiConnector.getPoints();
		Point p1 = points[idex];
		Point p2 = points[idex + 1];

		return new Point(((p1.x + p2.x) / 2), ((p1.y + p2.y) / 2));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.vtp.desktop.editors.themes.mantis.MantisComponentFrame#renderFrame(org.eclipse.swt.graphics.GC, int, int, java.util.Map)
	 */
	public void renderFrame(GC gc, int renderingPhase, int options, Map<String, Object> resourceMap)
	{

//		gc.setAntialias(SWT.ON);
		Font labelFont = getFont(gc, resourceMap, "attaction.linelabel.font", "Arial", 9, SWT.NORMAL);
		Color labelExitColor = getColor(gc, resourceMap, "attraction.linelabel.exit.color", 0, 0, 0);
		Color labelErrorColor = getColor(gc, resourceMap, "attraction.linelabel.error.color", 255, 0, 0);
		Color lineColor = getColor(gc, resourceMap, "attraction.line.color", 0, 0, 0);
		Color lineBendColor = getColor(gc, resourceMap, "attraction.line.color", 0, 0, 0);
		Color selectionColor = getColor(gc, resourceMap, "attraction.selection.color", 82, 140, 55);
		Color foreground = gc.getForeground();
		Color background = gc.getBackground();
		Font originalFont = gc.getFont();

		Point[] ps = uiConnector.getPoints();

		if(ps[0] != null)
		{
			if(selected)
			{
				gc.setForeground(selectionColor);
			}
			else
			{
				gc.setForeground(lineColor);
			}
			//draw line segments
			for(int i = 0; i < (ps.length - 2); i++)
			{
				if((ps[i] != null) && (ps[i + 1] != null))
				{
					int point1X = ps[i].x;
					int point1Y = ps[i].y;
					if(i == 0)
					{
						//need to adjust starting point based on incursion point into source
						Point incPoint = source.getIncursionPoint(ps[i + 1].x, ps[i + 1].y);
						if(incPoint != null)
						{
							point1X = incPoint.x;
							point1Y = incPoint.y;
						}
					}
//					int point2X = ps[i + 1].x;
//					int point2Y = ps[i + 1].y;
					double rise1 = ps[i + 1].y - point1Y;
					double run1 = ps[i + 1].x - point1X;
					double dist =
						Math.sqrt(Math.pow(rise1, 2d) + Math.pow(run1, 2d));
					double line1Xnorm = run1 / dist;
					double line1Ynorm = rise1 / dist;
					double perpX = point1X + (((dist / 2d) + 2d) * line1Xnorm);
					double perpY = point1Y + (((dist / 2d) + 2d) * line1Ynorm);
					double perpX2 = point1X + (((dist / 2d) - 2d) * line1Xnorm);
					double perpY2 = point1Y + (((dist / 2d) - 2d) * line1Ynorm);
					double a1x = perpX2 + (-6d * line1Ynorm);
					double a1y = perpY2 + (6d * line1Xnorm);
					double a2x = perpX2 + (6d * line1Ynorm);
					double a2y = perpY2 + (-6d * line1Xnorm);
					gc.drawLine(ps[i].x, ps[i].y, ps[i + 1].x, ps[i + 1].y);
					gc.drawLine((int)perpX, (int)perpY, (int)a2x, (int)a2y);
					gc.drawLine((int)a1x, (int)a1y, (int)perpX, (int)perpY);
				}
			}
			if(ps[ps.length - 1] != null)
			{
				Point oi = ps[ps.length - 2];
				Point incursion = destination.getIncursionPoint(oi.x, oi.y);
				if(incursion != null)
				{
					double rise1 = incursion.y - oi.y;
					double run1 = incursion.x - oi.x;
					double dist =
						Math.sqrt(Math.pow(rise1, 2d) + Math.pow(run1, 2d));
					double line1Xnorm = run1 / dist;
					double line1Ynorm = rise1 / dist;
					double perpX = incursion.x + (-12d * line1Xnorm);
					double perpY = incursion.y + (-12d * line1Ynorm);
					double a1x = perpX + (-6d * line1Ynorm);
					double a1y = perpY + (6d * line1Xnorm);
					double a2x = perpX + (6d * line1Ynorm);
					double a2y = perpY + (-6d * line1Xnorm);
					double perpX2 = incursion.x + (-9d * line1Xnorm);
					double perpY2 = incursion.y + (-9d * line1Ynorm);
					double a3x = perpX2 + (-4.5d * line1Ynorm);
					double a3y = perpY2 + (4.5d * line1Xnorm);
					double a4x = perpX2 + (4.5d * line1Ynorm);
					double a4y = perpY2 + (-4.5d * line1Xnorm);
					double perpX3 = incursion.x + (-6d * line1Xnorm);
					double perpY3 = incursion.y + (-6d * line1Ynorm);
					double a5x = perpX3 + (-3d * line1Ynorm);
					double a5y = perpY3 + (3d * line1Xnorm);
					double a6x = perpX3 + (3d * line1Ynorm);
					double a6y = perpY3 + (-3d * line1Xnorm);
					double perpX4 = incursion.x + (-3d * line1Xnorm);
					double perpY4 = incursion.y + (-3d * line1Ynorm);
					double a7x = perpX4 + (-1.5d * line1Ynorm);
					double a7y = perpY4 + (1.5d * line1Xnorm);
					double a8x = perpX4 + (1.5d * line1Ynorm);
					double a8y = perpY4 + (-1.5d * line1Xnorm);
					gc.drawLine(oi.x, oi.y, incursion.x, incursion.y);
					gc.drawLine((int)a1x, (int)a1y, (int)a2x, (int)a2y);
					gc.drawLine((int)a3x, (int)a3y, (int)a4x, (int)a4y);
					gc.drawLine((int)a5x, (int)a5y, (int)a6x, (int)a6y);
					gc.drawLine((int)a7x, (int)a7y, (int)a8x, (int)a8y);
				}
			}

			//draw mid points
			if(selected)
			{
				for(int i = 0; i < (ps.length - 1); i++)
				{
					if((ps[i] != null) && (ps[i + 1] != null))
					{
						if(i != 0)
						{
							gc.setBackground(lineBendColor);
							gc.fillRectangle(ps[i].x - 4, ps[i].y - 4, 8, 8);
						}
					}
				}
			}
			
			//calculate label size
			gc.setFont(labelFont);
			int nw = 0;
			int nh = 0;
			List<IDesignElementConnectionPoint> connectionPoints = uiConnector.getConnectionPoints();
			for(IDesignElementConnectionPoint cr : connectionPoints)
			{
				org.eclipse.swt.graphics.Point np =
					gc.stringExtent(cr.getName());
				nw = Math.max(nw, np.x);
				nh = nh + 2 + np.y;
			}
			nh -= 2;
			Point labelSegmentCenterPoint = getLineSegmentCenterPoint(uiConnector.getConnectorLabel().getAnchorSegment());
			Point offset = uiConnector.getConnectorLabel().getOffsetPosition();
			Point labelCenterPoint = new Point(labelSegmentCenterPoint.x + offset.x, labelSegmentCenterPoint.y + offset.y);
			labelUpperLeft = new Point(labelCenterPoint.x - ((nw / 2) + 2),
					labelCenterPoint.y - ((nh / 2) + 2));
			labelLowerRight = new Point(labelCenterPoint.x + ((nw / 2) + 2),
					labelCenterPoint.y + ((nh / 2) + 2));
			
			//draw label
			int ry = labelUpperLeft.y + 1;
			for(IDesignElementConnectionPoint cr : connectionPoints)
			{

				if(cr.getType() == IDesignElementConnectionPoint.ConnectionPointType.ERROR_POINT)
				{
					gc.setForeground(labelErrorColor);
				}
				else
				{
					gc.setForeground(labelExitColor);
				}

				Point np = gc.stringExtent(cr.getName());
				gc.drawString(cr.getName(), labelUpperLeft.x + 2, ry, true);
				ry += np.y;
				ry += 2;
			}
			
			gc.setForeground(foreground);
			gc.setBackground(background);
			gc.setFont(originalFont);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.vtp.desktop.editors.core.theme.ComponentFrame#setSelected(boolean)
	 */
	public void setSelected(boolean selected)
	{
		this.selected = selected;
		dragTarget = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.vtp.desktop.editors.core.theme.ComponentFrame#touchesComponent(int, int)
	 */
	public boolean touchesComponent(int x, int y)
	{
		boolean ret = false;
		ret = new Rectangle(labelUpperLeft.x, labelUpperLeft.y, labelLowerRight.x - labelUpperLeft.x + 1, labelLowerRight.y - labelUpperLeft.y + 1).contains(x, y);

		if(!ret)
		{
			Point[] ps = uiConnector.getPoints();

			for(int i = 0; i < (ps.length - 1); i++)
			{
				if(x < (Math.min(ps[i].x, ps[i + 1].x) - 5))
				{
					continue;
				}
				else if(x > (Math.max(ps[i].x, ps[i + 1].x) + 5))
				{
					continue;
				}
				else if(y < (Math.min(ps[i].y, ps[i + 1].y) - 5))
				{
					continue;
				}
				else if(y > (Math.max(ps[i].y, ps[i + 1].y) + 5))
				{
					continue;
				}
				else if(ps[i].x == ps[i + 1].x)
				{
					ret = ret | (Math.abs(x - ps[i].x) < 5);
				}
				else if(ps[i].y == ps[i + 1].y)
				{
					ret = ret | (Math.abs(y - ps[i].y) < 5);
				}
				else
				{
					ret = ret
						| (
							Geom.DistancePointLine(Geom.newPoint3D(x, y, 0),
								Geom.newPoint3D(ps[i].x, ps[i].y, 0),
								Geom.newPoint3D(ps[i + 1].x, ps[i + 1].y, 0)) < 5
						);
				}

				if(ret)
				{
					break;
				}
			}
		}

		return ret;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.vtp.desktop.editors.core.theme.ComponentFrame#initializeGraphics(org.eclipse.swt.graphics.GC, java.util.Map)
	 */
	public void initializeGraphics(GC gc, Map<String, Object> resourceMap)
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.vtp.desktop.editors.core.theme.ConnectorFrame#toggleMidPoint(int, int)
	 */
	public void toggleMidPoint(int x, int y)
	{
		float oldDistance = Float.MAX_VALUE;
		Point[] ps = uiConnector.getPoints();
		for(int i = 0; i < ps.length; i++)
		{
			Rectangle midPointRect = new Rectangle(ps[i].x - 5, ps[i].y - 5, 10, 10);
			if(midPointRect.contains(x, y))
			{
				uiConnector.removeMidpoint(i);
				return;
			}
		}
		int index = ps.length - 1;
		for(int i = 0; i < ps.length - 1; i++)
		{
			if(x < (Math.min(ps[i].x, ps[i + 1].x) - 5))
			{
				continue;
			}
			else if(x > (Math.max(ps[i].x, ps[i + 1].x) + 5))
			{
				continue;
			}
			else if(y < (Math.min(ps[i].y, ps[i + 1].y) - 5))
			{
				continue;
			}
			else if(y > (Math.max(ps[i].y, ps[i + 1].y) + 5))
			{
				continue;
			}
			float distance = Geom.DistancePointLine(Geom.newPoint3D(x, y, 0),
					Geom.newPoint3D(ps[i].x, ps[i].y, 0),
					Geom.newPoint3D(ps[i + 1].x, ps[i + 1].y, 0));
			if(distance < oldDistance)
				index = i + 1;
			oldDistance = distance;
		}
		uiConnector.insertMidpoint(index, x, y);
	}

}

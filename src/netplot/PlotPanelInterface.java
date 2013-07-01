/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot;

import org.jfree.data.time.Millisecond;

public interface PlotPanelInterface
{
  //Set plot attributes
  public void setAttribute(String name, String value) throws NetPlotException;
  
  //This can be called to add the plot
  public abstract void addPlot() throws NetPlotException;
  
  //These are called to add a plot value
  public abstract void addPlotValue(double xValue, double yValue) throws NetPlotException;
  public abstract void addPlotValue(int plotIndex, double xValue, double yValue) throws NetPlotException;
  public abstract void addPlotValue(int plotIndex, Millisecond ms, double yValue) throws NetPlotException;

  public abstract void init();
  public abstract int  getPlotCount();
  
  //clear/emtpy all the values in a plot.
  public abstract void clear(int plotIndex) throws NetPlotException;
  
  //Restart a plot. Subsequent plot points added will replace the previous ones.
  //Note that the X axis values must alreay be present.
  public abstract void replot(int plotIndex) throws NetPlotException;
  
}

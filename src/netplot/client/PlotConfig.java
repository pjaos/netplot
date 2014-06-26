/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot.client;

/**
 * Responsible for holding the configuration for a single plot trace.
 */
public class PlotConfig
{
  public String     plotName="";
  public String     xAxisName="";
  public String     yAxisName="";
  public boolean    enableLines=true;
  public int		lineWidth=1;
  public boolean    enableShapes=true;
  public boolean    enableAutoScale=true;
  public double     minScaleValue=0;
  public double     maxScaleValue=1E6;
  public int        maxAgeSeconds=3600;
  public boolean    enableLogYAxis=false;
  public boolean    enableZeroOnXAxis=true;
  public boolean    enableZeroOnYAxis=true;
  public int        tickCount=0;
}

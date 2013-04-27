/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot;

import javax.swing.*;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import java.awt.*;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.util.LogFormat;
import org.jfree.data.Range;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.labels.*;

public class GenericPlotPanel extends JPanel 
{
  public static Color       PlotColours[] = {Color.blue, Color.black, Color.red, Color.green, Color.cyan, Color.darkGray, Color.gray, Color.lightGray, Color.magenta, Color.orange, Color.pink, Color.yellow };

  //Plot attributes 
  //Not all plot types may use all the attributes. See the specific plot planel for details
  String    plotTitle="";
  String    plotName="";
  String    xAxisName="";
  String    yAxisName="";
  boolean   linesEnabled=true;
  boolean   shapesEnabled=true;
  boolean   autoScaleEnabled=true;
  double    minScaleValue=0;
  double    maxScaleValue=0;
  int       maxAgeSeconds=60;
  boolean   logYAxis=false;
  boolean   zeroOnXScale=true;
  boolean   zeroOnYScale=false;
  boolean   enableLegend=true;
  int       yAxisTickCount=0;
  
  int yAxisIndex=0;
  
  public GenericPlotPanel(LayoutManager layoutManager)
  {
    super(layoutManager);
  }
  
  public String toString()
  {
    StringBuffer strBuffer = new StringBuffer();
    strBuffer.append("plotTitle        = "+plotTitle+"\n");
    strBuffer.append("plotName         = "+plotName+"\n");
    strBuffer.append("xAxisName        = "+xAxisName+"\n");
    strBuffer.append("yAxisName        = "+yAxisName+"\n");
    strBuffer.append("linesEnabled     = "+linesEnabled+"\n");
    strBuffer.append("shapesEnabled    = "+shapesEnabled+"\n");
    strBuffer.append("autoScaleEnabled = "+autoScaleEnabled+"\n");
    strBuffer.append("minScaleValue    = "+minScaleValue+"\n");
    strBuffer.append("maxScaleValue    = "+maxScaleValue+"\n");
    strBuffer.append("maxAgeSeconds    = "+maxAgeSeconds+"\n");
    strBuffer.append("logYAxis         = "+logYAxis+"\n");
    return strBuffer.toString();
  }
  
  public void setAttribute(String name, String value) throws NetPlotException
  {
    if( name == null || value == null || name.length() == 0 )
    {
      throw new NetPlotException("setAttribute name/value invalid ("+name+"/"+value);
    }
    if( name.equals(KeyWords.PLOT_TITLE) )
    {
      plotTitle=value;
    }
    else if( name.equals(KeyWords.PLOT_NAME) )
    {
      plotName=value;
    }
    else if( name.equals(KeyWords.X_AXIS_NAME) )
    {
      xAxisName=value;
    }
    else if( name.equals(KeyWords.Y_AXIS_NAME) )
    {
      yAxisName=value;
    }
    else if( name.equals(KeyWords.ENABLE_LINES) )
    {
      if( value.equals("true") )
      {
        linesEnabled=true;
      }
      else if( value.equals("false") )
      {
        linesEnabled=false;
      }
      else
      {
        throw new NetPlotException(value+" is an invalid value for "+KeyWords.ENABLE_LINES+", must be true or false");
      }
    }
    else if( name.equals(KeyWords.ENABLE_SHAPES) )
    {
      if( value.equals("true") )
      {
        shapesEnabled=true;
      }
      else if( value.equals("false") )
      {
        shapesEnabled=false;
      }
      else
      {
        throw new NetPlotException(value+" is an invalid value for "+KeyWords.ENABLE_SHAPES+", must be true or false");
      }
    }
    else if( name.equals(KeyWords.ENABLE_AUTOSCALE) )
    {
      if( value.equals("true") )
      {
        autoScaleEnabled=true;
      }
      else if( value.equals("false") )
      {
        autoScaleEnabled=false;
      }
      else
      {
        throw new NetPlotException(value+" is an invalid value for "+KeyWords.ENABLE_AUTOSCALE+", must be true or false");
      }
    }
    else if( name.equals(KeyWords.MIN_SCALE_VALUE) )
    {
      try
      {
        minScaleValue=Double.parseDouble(value);
      }
      catch(NumberFormatException e)
      {
        throw new NetPlotException(value+" is an invalid value for "+KeyWords.MIN_SCALE_VALUE+", must be a double value");
      }
    }    
    else if( name.equals(KeyWords.MAX_SCALE_VALUE) )
    {
      try
      {
        maxScaleValue=Double.parseDouble(value);
      }
      catch(NumberFormatException e)
      {
        throw new NetPlotException(value+" is an invalid value for "+KeyWords.MAX_SCALE_VALUE+", must be a double value");
      }
    }    
    else if( name.equals(KeyWords.MAX_AGE_SECONDS) )
    {
      try
      {
        maxAgeSeconds=Integer.parseInt(value);
      }
      catch(NumberFormatException e)
      {
        throw new NetPlotException(value+" is an invalid value for "+KeyWords.MAX_AGE_SECONDS+", must be a integer value");
      }
    }    
    else if( name.equals(KeyWords.ENABLE_LOG_Y_AXIS) )
    {
      if( value.equals("true") )
      {
        logYAxis=true;
      }
      else if( value.equals("false") )
      {
        logYAxis=false;
      }
      else
      {
        throw new NetPlotException(value+" is an invalid value for "+KeyWords.ENABLE_LOG_Y_AXIS+", must be true or false");
      }
    }
    else if( name.equals(KeyWords.ENABLE_ZERO_ON_X_SCALE) )
    {
      if( value.equals("true") )
      {
        zeroOnXScale=true;
      }
      else if( value.equals("false") )
      {
        zeroOnXScale=false;
      }
      else
      {
        throw new NetPlotException(value+" is an invalid value for "+KeyWords.ENABLE_ZERO_ON_X_SCALE+", must be true or false");
      }
    }
    else if( name.equals(KeyWords.ENABLE_ZERO_ON_Y_SCALE) )
    {
      if( value.equals("true") )
      {
        zeroOnYScale=true;
      }
      else if( value.equals("false") )
      {
        zeroOnYScale=false;
      }
      else
      {
        throw new NetPlotException(value+" is an invalid value for "+KeyWords.ENABLE_ZERO_ON_Y_SCALE+", must be true or false");
      }
    }
    else if( name.equals(KeyWords.ENABLE_LEGEND) )
    {
      if( value.equals("true") )
      {
        enableLegend=true;
      }
      else if( value.equals("false") )
      {
        enableLegend=false;
      }
      else
      {
        throw new NetPlotException(value+" is an invalid value for "+KeyWords.ENABLE_LEGEND+", must be true or false");
      }
    }
    else if( name.equals(KeyWords.TICK_COUNT) )
    {
      try
      {
        yAxisTickCount=Integer.parseInt(value);
      }
      catch(NumberFormatException e)
      {
        throw new NetPlotException(value+" is an invalid value for "+KeyWords.TICK_COUNT+", must be a integer value");
      }
    }    
    else
    {
      throw new NetPlotException(name+" is an unknown attribute (value="+value);      
    }
  }
  
  void genericConfig(JFreeChart chart, XYPlot plot, int plotIndex)
  {
    if( !enableLegend )
    {
      chart.removeLegend();
    }
    XYItemRenderer xyItemRenderer = plot.getRenderer();
    //May also be XYBarRenderer
    if( xyItemRenderer instanceof XYLineAndShapeRenderer ) {
      XYToolTipGenerator xyToolTipGenerator = xyItemRenderer.getBaseToolTipGenerator();
      //If currently an XYLineAndShapeRenderer replace it so that we inc the colour for every plotIndex
      XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(linesEnabled, shapesEnabled);
      //Ensure we don't loose the tool tips on the new renderer
      renderer.setBaseToolTipGenerator(xyToolTipGenerator);
      renderer.setSeriesPaint(0, getPlotColour(plotIndex) );
      renderer.setSeriesStroke(plotIndex, new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
      ((XYPlot)chart.getPlot()).setRenderer(plotIndex, renderer);

    }
    
    //If we have a new y axis then we need a new data set
    if( yAxisName != null && yAxisName.length() >  0 )
    {    
      if( logYAxis ) {
        LogAxis yAxis = new LogAxis(yAxisName);  
        yAxis.setAutoRange(false);
        yAxis.setNumberFormatOverride(new LogFormat(10, "10", true));
        yAxis.setRange(minScaleValue, maxScaleValue);
        yAxis.setLowerBound(minScaleValue);
        yAxis.setUpperBound(maxScaleValue);
        plot.setRangeAxis(yAxisIndex, yAxis);
        plot.setRangeAxisLocation(yAxisIndex, AxisLocation.BOTTOM_OR_LEFT);
      }
      else {
        NumberAxis axis = new NumberAxis(yAxisName);
        axis.setAutoRangeIncludesZero(zeroOnYScale);
        if( autoScaleEnabled )
        {
          axis.setAutoRange(true);
        }
        else
        {
          Range range = new Range(minScaleValue, maxScaleValue);
          axis.setRangeWithMargins(range, true, true);
        }     
        if ( yAxisTickCount > 0 ) {
          NumberTickUnit tick = new NumberTickUnit(yAxisTickCount);
          axis.setTickUnit(tick);        
        }
        plot.setRangeAxis(yAxisIndex, axis);
        plot.setRangeAxisLocation(yAxisIndex, AxisLocation.BOTTOM_OR_LEFT);
      }
      yAxisIndex++;    
    }
    plot.mapDatasetToRangeAxis(plotIndex,yAxisIndex-1);
    ValueAxis a = plot.getDomainAxis();
    if( xAxisName.length() > 0)
    {
      a.setLabel(xAxisName);
    }
    //We can enable/disable zero on the axis if we have a NumberAxis
    if( a instanceof NumberAxis )
    {
      ((NumberAxis)a).setAutoRangeIncludesZero(zeroOnXScale);
    }
  }
  
  Color getPlotColour(int plotIndex)
  {
    int colourIndex;
    if( plotIndex >= GenericPlotPanel.PlotColours.length )
    {
      colourIndex=plotIndex%GenericPlotPanel.PlotColours.length;
    }
    else
    {
      colourIndex=plotIndex;
    }
    return GenericPlotPanel.PlotColours[colourIndex];
  }


}
/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot;

import java.awt.BorderLayout;
import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.*;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import java.util.*;

public class TimeSeriesPlotPanel extends GenericPlotPanel implements PlotPanelInterface
{
  private JFreeChart                    chart;
  private Vector<TimeSeries>            timeSeriesList;
  
  public TimeSeriesPlotPanel() {
     super(new BorderLayout());
     init();
   }
     
   public void init()
   {
     finalize();
     timeSeriesList = new Vector<TimeSeries>();
     chart = createChart(null);
     ChartPanel chartPanel = new ChartPanel(chart);
     chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
     add(chartPanel);
  }
   
   private JFreeChart createChart(XYDataset dataset) {
     
     JFreeChart chart = ChartFactory.createTimeSeriesChart(
         plotTitle,     // title
         xAxisName,     // x-axis label
         yAxisName,     // y-axis label
         dataset,       // data
         enableLegend,  // create legend?
         true,          // generate tooltips?
         true           // generate URLs?
     );
     
     XYPlot plot = (XYPlot)chart.getPlot();
     plot.setBackgroundPaint(Color.WHITE);
     plot.setDomainGridlinePaint(Color.DARK_GRAY);
     plot.setRangeGridlinePaint(Color.DARK_GRAY);
     return chart;
 }

  @SuppressWarnings("deprecation")
  public void addPlot()
  {
    int plotIndex=timeSeriesList.size();
    TimeSeries timeSeries = new TimeSeries(plotName, Millisecond.class);
    timeSeriesList.add(timeSeries);
    
    TimeSeriesCollection dataset = new TimeSeriesCollection();
    dataset.addSeries( timeSeries );
    ((XYPlot)chart.getPlot()).setDataset(plotIndex, dataset);
    genericConfig(chart, (XYPlot)chart.getPlot(), plotIndex);  
  }
  
  public int getPlotCount() { return ((XYPlot)chart.getPlot()).getSeriesCount(); }
   
  public void addPlotValue(double plotIndex, double yValue)
  {
    timeSeriesList.get((int)plotIndex).addOrUpdate(new Millisecond(), yValue);
  }
  
  public void addPlotValue(int plotIndex, double xValue, double yValue) throws NetPlotException 
  { 
    throw new NetPlotException("addPlotValue(int plotIndex, double xValue, double yValue) should not be used on TimeSeriesPlotPanel"); 
  }

  public void removePlots()
  {
    init();
    validate();
  }
  
  public void finalize()
  {
    if( timeSeriesList != null ) {
      timeSeriesList.removeAllElements();
    }
    removeAll();
    chart=null;
    //System.out.println("PJA: TimeSeriesPlotPanel FreeMem="+Runtime.getRuntime().freeMemory());
  }
  
  public void clear(int plotIndex) throws NetPlotException {
    boolean cleared=false;
    if( timeSeriesList != null ) {
      try {
        TimeSeries timeSeries = timeSeriesList.get(plotIndex);
        if( timeSeries != null ) {
          timeSeries.clear();
          cleared=true;
        }
      }
      catch(ArrayIndexOutOfBoundsException e) {}
    }
    if( !cleared ) {
      throw new NetPlotException("Failed to clear time plot "+plotIndex+". Please ensure the plot exists before clearing it.");
    }
  }

  /**
   * Reset the current plot point count to 0 so that we rewrite the previously entered plot points.
   * 
   * As the X axis is time it does not make sense to replot the values, use clear instead.
   */
  public void replot(int plotIndex) throws NetPlotException {}

}

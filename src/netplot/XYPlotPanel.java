/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import java.util.*;
import java.awt.*;

public class XYPlotPanel extends GenericPlotPanel implements PlotPanelInterface
{
  private JFreeChart            chart;
  private Vector<XYSeries>      xySeriesList;
  private Vector<Integer>       itemCountList;
  
  public XYPlotPanel() {
     super(new BorderLayout());
     init();
   }
     
   public void init()
   {
     xySeriesList = new Vector<XYSeries>();
     chart = createChart(null);
     ChartPanel chartPanel = new ChartPanel(chart);
     chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
     itemCountList = new Vector<Integer>();
     add(chartPanel);
  }
   
   private JFreeChart createChart(XYDataset dataset) {
     
     JFreeChart chart = ChartFactory.createXYLineChart(
         plotTitle,
         xAxisName, 
         yAxisName, 
         dataset,
         PlotOrientation.VERTICAL,
         enableLegend,
         true,
         true
     );
     XYPlot plot = (XYPlot)chart.getPlot();
     plot.setBackgroundPaint(Color.WHITE);
     plot.setDomainGridlinePaint(Color.DARK_GRAY);
     plot.setRangeGridlinePaint(Color.DARK_GRAY);
     return chart;
 }

  public void addPlot()
  {
    int plotIndex=xySeriesList.size();
    XYSeries xySeries = new XYSeries(plotName);
    xySeriesList.add(xySeries);
    
    XYSeriesCollection dataset = new XYSeriesCollection();
    dataset.addSeries( xySeries );
    ((XYPlot)chart.getPlot()).setDataset(plotIndex, (XYDataset)dataset);    
    genericConfig(chart, (XYPlot)chart.getPlot(), plotIndex); 
    itemCountList.add( new Integer(0) );
  }
  
  public int getPlotCount() { return ((XYPlot)chart.getPlot()).getSeriesCount(); }
  
  public void addPlotValue(int plotIndex, double xValue, double yValue)
  {   
    int curentItemCount = itemCountList.get(plotIndex);
    //If we want to change a plot point that should have already been plotted
    if( curentItemCount < xySeriesList.get((int)plotIndex).getItemCount() ) {
      xySeriesList.get((int)plotIndex).update(xValue, yValue);
    }
    else {
      xySeriesList.get((int)plotIndex).add(xValue, yValue);
    }
    curentItemCount++;
    itemCountList.set(plotIndex, curentItemCount );
  }

  public void addPlotValue(double xValue, double yValue) throws NetPlotException 
  { 
    throw new NetPlotException("addPlotValue(double xValue, double yValue) is invalid for XYPlotPanel"); 
  }
  
  public void removePlots()
  {
    init();
    validate();
    itemCountList.removeAllElements();
  }
  
  public void finalize()
  {
    if( xySeriesList != null ) {
      xySeriesList.removeAllElements();
    }
    itemCountList.removeAllElements();
    removeAll();
    chart=null;
    //System.out.println("PJA: XYPlotPanel FreeMem="+Runtime.getRuntime().freeMemory());
  }
  
  public void clear(int plotIndex) throws NetPlotException {
    boolean cleared=false;
    if( xySeriesList != null ) {
      try {
        XYSeries xySeries = xySeriesList.get(plotIndex);
        if( xySeries != null ) {
          xySeries.clear();
          cleared=true;
        }
      }
      catch(ArrayIndexOutOfBoundsException e) {}
    }
    if( !cleared ) {
      throw new NetPlotException("Failed to clear xy plot "+plotIndex+". Please ensure the plot exists before clearing it.");
    }
  }
   
  /**
   * Reset the current plot point count to 0 so that we rewrite the previously entered plot points
   */
  public void replot(int plotIndex) throws NetPlotException {
    itemCountList.set(plotIndex, new Integer(0) );
  }

}

















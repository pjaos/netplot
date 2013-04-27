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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class BarPlotPanel extends GenericPlotPanel implements PlotPanelInterface
{
  private JFreeChart        chart;
  private XYSeries          series;
  private XYPlot            plot;
  private int               xValue=1;
  XYSeriesCollection        collection;
  XYBarDataset              dataset;
  private XYBarRenderer     renderer;

  public BarPlotPanel() {
    super(new BorderLayout());
    init();
  }
    
  public void init()
  {
    finalize();
    series = new XYSeries("");
    chart = createChart();
    add( new ChartPanel(chart) );
  }
  
  
  private JFreeChart createChart() {
    collection = new XYSeriesCollection();
    collection.addSeries(series);
    dataset = new XYBarDataset(collection, 0.9);

    chart = ChartFactory.createXYBarChart(
        plotTitle,
        "",
        false,
        "",
        dataset,
        PlotOrientation.VERTICAL,
        enableLegend,
        true,
        true
    );

    plot = (XYPlot) chart.getPlot();
    plot.setBackgroundPaint(Color.WHITE);
    plot.setDomainGridlinePaint(Color.DARK_GRAY);
    plot.setRangeGridlinePaint(Color.DARK_GRAY);
    NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
    domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

    renderer = (XYBarRenderer) plot.getRenderer();
    renderer.setDrawBarOutline(true);
    return chart; 
  }

  public void addPlot()
  {
    plot.getRangeAxis().setAutoRange(autoScaleEnabled);
    if( !logYAxis && !autoScaleEnabled )
    {
      Range range = new Range(minScaleValue, maxScaleValue);
      plot.getRangeAxis().setRangeWithMargins(range, true, true);
    }
    genericConfig(chart, plot, 0);    
  }
  
  public int getPlotCount()
  {
    //We only ever have one plot on the bar graph
    return 1;
  }
  
  /**
   * As only one plot is allowed plotIndex is not used.
   */
  public void addPlotValue(double plotIndex, double yValue)
  {  
    //If we want to change a plot point that should have already been plotted
    if( xValue < series.getItemCount() ) {
      series.updateByIndex(xValue, yValue);
    }
    else {
      series.add(xValue, yValue);
    }
    xValue++;
  }
  
  //Not implemented on TimeSeriesPlot
  public void addPlotValue(int plotIndex, double xValue, double yValue) {}

  public void removePlots()
  {
    init();
    validate();
  }
  
  public void finalize()
  {
    removeAll();
    if( collection != null )
    {
      collection.removeAllSeries();
      collection=null;
    }
    dataset=null;
    series=null;
    chart=null;
    //System.out.println("PJA: BarPlotPanel FreeMem="+Runtime.getRuntime().freeMemory());
  }

  public void clear(int plotIndex)  throws NetPlotException {
    if( plotIndex != 0) {
      throw new NetPlotException("clear "+plotIndex+" is invalid for a bar plot. A bar plot can only contain a single plot. Try clear 0 instead.");
    }
    if( series != null ) {
      series.clear();
    }
  }
  
  /**
   * Reset the current pplot point count to 0 so that we rewrite the previously entered plot points.
   */
  public void replot(int plotIndex) throws NetPlotException {  
    xValue=1;
  }

}

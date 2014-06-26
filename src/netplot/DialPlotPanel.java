/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Point;

import javax.swing.JFrame;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.dial.DialBackground;
import org.jfree.chart.plot.dial.DialCap;
import org.jfree.chart.plot.dial.DialPlot;
import org.jfree.chart.plot.dial.DialPointer;
import org.jfree.chart.plot.dial.DialTextAnnotation;
import org.jfree.chart.plot.dial.DialValueIndicator;
import org.jfree.chart.plot.dial.StandardDialFrame;
import org.jfree.chart.plot.dial.StandardDialScale;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.time.Millisecond;
import org.jfree.ui.GradientPaintTransformType;
import org.jfree.ui.StandardGradientPaintTransformer;

/**
 * A sample application showing the use of a {@link DialPlot}.
 */
public class DialPlotPanel extends GenericPlotPanel  implements PlotPanelInterface
{
  static final long serialVersionUID=2;
  public static final Color  Plot0Color = Color.BLUE;
  public static final Color  Plot1Color = Color.RED;
  
  DefaultValueDataset   dataset1;
  DefaultValueDataset   dataset2;
  int                   plotCount=0;
  DialPlot              plot;
  JFreeChart            chart;
  
  /** 
   * Creates a new instance.
   *
   * @param title  the frame title.
   */
  public DialPlotPanel() {
    super( new BorderLayout() );
    init();
  }
    
    public void init()
    {
      finalize();
      this.dataset1 = new DefaultValueDataset(10.0);
      this.dataset2 = new DefaultValueDataset(50.0);
      
      // get data for diagrams
      plot = new DialPlot();
      plot.setView(0.0, 0.0, 1.0, 1.0);
      plot.setDataset(0, this.dataset1);
      plot.setDataset(1, this.dataset2);
      StandardDialFrame dialFrame = new StandardDialFrame();
      dialFrame.setBackgroundPaint(Color.lightGray);
      dialFrame.setForegroundPaint(Color.darkGray);
      plot.setDialFrame(dialFrame);
      
      GradientPaint gp = new GradientPaint(new Point(), 
              new Color(255, 255, 255), new Point(), 
              new Color(170, 170, 220));
      DialBackground db = new DialBackground(gp);
      db.setGradientPaintTransformer(new StandardGradientPaintTransformer(GradientPaintTransformType.VERTICAL));
      plot.setBackground(db);

      DialCap cap = new DialCap();
      cap.setRadius(0.10);
      plot.setCap(cap);
      
      chart = new JFreeChart(plot);
      ChartPanel cp1 = new ChartPanel(chart);
      cp1.setPreferredSize(new Dimension(400, 400));
      
      add(cp1);
    }
    
    public void addPlot() throws NetPlotException
    {
      chart.setTitle(plotTitle);

      DialTextAnnotation annotation = new DialTextAnnotation(plotName);
      annotation.setFont(new Font("Dialog", Font.BOLD, 14));
      if( plotCount == 0 )
      {
        DialValueIndicator dvi = new DialValueIndicator(0);
        dvi.setFont(new Font("Dialog", Font.PLAIN, 10));
        annotation.setRadius(0.7);    
        annotation.setPaint(DialPlotPanel.Plot0Color);
        dvi.setRadius(0.60);
        dvi.setAngle(-103.0);
        dvi.setOutlinePaint(DialPlotPanel.Plot0Color);

        StandardDialScale scale = new StandardDialScale(minScaleValue, maxScaleValue, -120, -300, yAxisTickCount, yAxisTickCount/2-1);
        scale.setTickRadius(0.88);
        scale.setTickLabelOffset(0.15);
        scale.setTickLabelFont(new Font("Dialog", Font.PLAIN, 14));
        plot.addScale(0, scale);
        plot.mapDatasetToScale(0, 0);     
        
        DialPointer.Pointer p = new DialPointer.Pointer(0);
        p.setFillPaint(DialPlotPanel.Plot0Color);
        plot.addPointer(p);

        plot.addLayer(annotation);
        plot.addLayer(dvi);

      }
      else if( plotCount == 1 )
      {
        annotation.setRadius(0.8);    
        annotation.setPaint(DialPlotPanel.Plot1Color);
        DialValueIndicator dvi = new DialValueIndicator(1);
        dvi.setFont(new Font("Dialog", Font.PLAIN, 10));
        dvi.setRadius(0.60);
        dvi.setAngle(-77.0);
        dvi.setOutlinePaint(DialPlotPanel.Plot1Color);

        StandardDialScale scale2 = new StandardDialScale(minScaleValue, maxScaleValue, -120, -300,  yAxisTickCount, yAxisTickCount/2-1);
        scale2.setTickRadius(0.50);
        scale2.setTickLabelOffset(0.15);
        scale2.setTickLabelFont(new Font("Dialog", Font.PLAIN, 10));
        scale2.setMajorTickPaint(DialPlotPanel.Plot1Color);
        plot.addScale(1, scale2);
        plot.mapDatasetToScale(1, 1);

        DialPointer.Pointer p = new DialPointer.Pointer(1);
        p.setFillPaint(DialPlotPanel.Plot1Color);
        p.setRadius(0.55);
        plot.addPointer(p);

        plot.addLayer(annotation);
        plot.addLayer(dvi);

      }
      else
      {
        throw new NetPlotException("DialPlotPanel.addPlot(). Only two plots available on this plot, this is the third.");
      }

      plotCount++;
    }
    
    public void addPlotValue(double plotIndex, double yValue) throws NetPlotException
    {
      int index=(int)plotIndex;
      
      if( index == 0 )
      {
        this.dataset1.setValue(yValue);
      }
      else if ( index == 1 )
      {
        this.dataset2.setValue(yValue);
      }
      else
      {
        throw new NetPlotException("DialPlotPanel.addPlotValue(double plotIndex, double yValue) plotIndex="+index+" invalid (0 or 1 are valid)"); 
      }
    }

    public void addPlotValue(int plotIndex, double xValue, double yValue) throws NetPlotException 
    { 
      throw new NetPlotException("addPlotValue(double xValue, double yValue) is invalid for DialPlotPanel"); 
    }
    
    public void addPlotValue(int plotIndex, Millisecond ms, double yValue) throws NetPlotException 
    { 
      throw new NetPlotException("addPlotValue(int plotIndex, Millisecond ms, double yValue) should not be used on DialPlotPanel"); 
    }

    public void finalize()
    {
      removeAll();
      dataset1=null;
      dataset2=null;
      plot=null;
      plotCount=0;
    }
    
    public int  getPlotCount() { return plotCount; }

    /**
     * Starting point for the demo application.
     * 
     * @param args  ignored.
     */
    public static void main(String[] args) {
      JFrame jf = new JFrame();
      DialPlotPanel dialPlotPanel = new DialPlotPanel();
      try
      {   
        dialPlotPanel.setAttribute(KeyWords.PLOT_TITLE, "Number And MAX");
        dialPlotPanel.setAttribute(KeyWords.PLOT_NAME, "Number");
        dialPlotPanel.setAttribute(KeyWords.TICK_COUNT, "10");
        dialPlotPanel.setAttribute(KeyWords.MIN_SCALE_VALUE, "0");
        dialPlotPanel.setAttribute(KeyWords.MAX_SCALE_VALUE, "150");
        dialPlotPanel.setAttribute(KeyWords.PLOT_NAME, "Number");
        dialPlotPanel.addPlot();
        dialPlotPanel.setAttribute(KeyWords.PLOT_NAME, "MAX");
        dialPlotPanel.setAttribute(KeyWords.MIN_SCALE_VALUE, "-20");
        dialPlotPanel.addPlot();
        
        jf.getContentPane().add( dialPlotPanel );
        jf.pack();
        jf.setVisible(true);
        double max=-1;
        while(true)
        {
          double v0 = Math.random()*50;
          if( max == -1 )
          {
            max=v0;
          }
          if( max < v0 )
          {
            max=v0;
          }
          dialPlotPanel.addPlotValue(0, v0);
          dialPlotPanel.addPlotValue(1, max);
          Thread.sleep(2000);
        }
      }
      catch(Exception e) 
      {
        e.printStackTrace();
      }
    }

    public void clear(int plotIndex) throws NetPlotException {
      if( plotIndex == 0 ) {
        if( dataset1 == null ) {
          throw new NetPlotException("Cannot clear dial plot 0, as dial plot 0 does not exist.");
        }
        else {
          dataset1.setValue(0);
        }
      }
      else if( plotIndex == 1 ) {
        if( dataset2 == null ) {
          throw new NetPlotException("Cannot clear dial plot 1, as dial plot 1 does not exist.");
        }
        else {
          dataset2.setValue(0);
        }
      }
      else {
        throw new NetPlotException("clear "+plotIndex+" is an invalid plot index for a dial plot. Must be 0 or 1.");        
      }
    }
    
    /**
     * Reset the current pplot point count to 0 so that we rewrite the previously entered plot points.
     * 
     * Not implemented for dial plot as clear could be used instead
     */
    public void replot(int plotIndex) throws NetPlotException {}

}


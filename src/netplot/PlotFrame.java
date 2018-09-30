/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

/*
 * Change log
 *
 * 2.5
 * - Use the latest jfreechart 1.0.19
 * - Add check for the netplot baseport
 *
 * 2.4
 * - Allow plot images bigger than the screen size to be saved.
 * - Allow plot images to be GIF or PNG files.
 *
 * 2.3
 * - Add menu option to save all plots on a grid as a png file.
 * 
 * 2.2
 * - Previous fix, stopped showing the log axis name, if set. Fixed.
 *
 * 2.1
 * - Fixed build of Java demo client. Previously it would not run.
 * - Log plot now shows log values correctly.
 * 
 * 2.0
 * - Don't resize frame/window (call pack() on frame) when the client sets the grid 
 *   dimensions as it is unwanted behaviour. 
 * - When closed the position and size of the window is saved. When restarted the 
 *   last position and size of the window is restored.
 * - Split the Netplot client demo out into separate Java src file in order to 
 *   improve clarity of code.
 *   
 * 1.9
 * - Maximising the frame is painful to use. Revert to previous approach.
 *
 * 1.8
 * - Enable auto flush on PrintWriter back to client in order to improve plot speed.
 * - When starting up Netplot frame startup maximised.
 *
 * 1.7
 * - Add ability to set the thickness of the plot lines to netplot. Updated all
 *   clients and all demo's.
 *
 * 1.6
 * -   If we don't have a valid number to plot for a timeseries plot we ignore it.
 *   and do not generate an error as on a time series graph we may not have
 *   values for all points in time for all plots.
 *
 * 1.5
 * - Add the ability to plot time periods not in real time. Previously when a value
 *   to be plotted was added the time at which the value was received was used as
 *   X axis time value. No it is possible to pass the time stamp from the client
 *   to the server in order to plot old time series data.
 *
 * 1.4
 * - If a bind error on the TCP ports used by the netplot GUI server then report the
 *   port error in the error message.
 *
 * 1.3
 * - Fixed the input of the max plot count on the command line. -p argument was used
 *   twice, rather than -p and -m.
 * - Previously when 10 chart panels were requested 11 servers were opened. Now it will
 *   open 10 as it should do.
 *
 * 1.21
 * Possible fix when invalid xy plot values are detected.
 *
 * 1.20
 * Added two keywords to the commands
 * enable_status : Enable/disable adding messages to the GUI status message history window.
 * replot        : Restart the plotting of data so that the next plot point added replaces
 *                 the first plot point, the next replaces the second displayed plot point
 *                 and so on.
 *
 * 1.19
 * - Update the help text on the status log window.
 * - Add clear command to empty a plot of data.
 *
 * 1.18
 * Set all plots background to white rather than light grey as is several plots are displayed
 * light grey is used to display a plot.
 *
 * 1.17
 * - Remove stdout debug print messages
 *
 * 1.16
 * Don't resize frame when graph init occurs as the user may want to size/position the graph
 * window themselves.
 * Fix bug in java netplot client that caused always added new Y axis even when no Y axis name
 * had been set.
 *
 * 1.15
 * Added disconnect method to the NetplotClient class.
 *
 * 1.14 Initial release
 *
 */

/**
 * Show a frame with a status bar along the lower edge.
 * This status bar may be double clicked to get the help text
 * for commands that may be used to plot data.
 */
public class PlotFrame extends JFrame implements ActionListener
{
  static final long serialVersionUID=5;
  public static final double NETPLOT_VERSION=2.5;
  String helpLines[] = {
      "* All netplot commands are text strings which makes the client code simple to implement.",
      "* Java and python clients are supplied by default but you may implement you own clients",
      "* using the information below.",
      "* ",
      "* The commands (all text strings) that may be used over the TCP/IP network connections",
      "* on ports "+NetPlotter.basePort+" - "+(+NetPlotter.basePort+NetPlotter.maxPlotCount)+" are shown below",
      "* ",
      "*** GLOBAL ATTRIBUTES ***",
      "* set grid=2,2",
      "* Set chart grid 2,2 gives 4 charts. Connect to the base TCP/IP port to set",
      "* chart 0, base TCP/IP port+1 for chart 1, etc. This should be called after",
      "* connecting. Max of "+NetPlotter.maxPlotCount+" charts available on above TCP/IP ports.",
      "* ",
      "* set "+KeyWords.FRAME_TITLE+"=The Frame Title",
      "* Sets the text in the title bar of the window holding the charts.",
      "* ",
      "* ",
      "*** CHART SPECIFIC ATTRIBUTES ***",
      "* ",
      "* set graph=time",
      "* Init a timeplot graph (This is the default graph type).",
      "*",
      "* set graph=bar",
      "* Init a barplot graph (plots a single value repeatedly as different bars).",
      "*",
      "* set graph=xy",
      "* Init an X/Y plot graph. Plot may include lines and shapes.",
      "* ",
      "* init",
      "* Init the graph. This should be called after the set graph= command has been issued.",
      "* ",
      "* set "+KeyWords.ENABLE_LEGEND+"=true",
      "* true/false, enables/disables the inclusion of the plot titles (Legend) at the bottom of the graph.",
      "* ",
      "* enable_status 1",
      "* Enables the status messages in the GUI (default), enable_status 0 will disable the",
      "* status messages in the GUI. The messages will not then be shown in the status message history window.",
      "* This may speed up the plotting of data if CPU bound.",
      "* ",
      "* ",
      "*** PLOT SPECIFIC ATTRIBUTES ***",
      "* ",
      "* set "+KeyWords.PLOT_TITLE+"=The plot title",
      "* Set the chart title.",
      "* ",
      "* set "+KeyWords.PLOT_NAME+"=The plot name",
      "* Set the name of a plot",
      "* ",
      "* set "+KeyWords.X_AXIS_NAME+"=X axis name",
      "* Set the name of the x axis. All plots on a graph have the same X axis.",
      "* ",
      "* set "+KeyWords.Y_AXIS_NAME+"=Y axis name",
      "* Set the name of the y axis. Multiple Y axis may be added to a chart.",
      "* ",
      "* set "+KeyWords.ENABLE_LINES+"=true",
      "* true/false, enables/disables printed lines on a plot",
      "* ",
      "* set "+KeyWords.LINE_WIDTH+"=X",
      "* Set the width (in pixels) of the lines to be used when above option is enabled (default="+GenericPlotPanel.DEFAULT_LINE_WIDTH+").",
      "* ",
      "* set "+KeyWords.ENABLE_SHAPES+"=true",
      "* true/false, enables/disables printed shapes (plot points) on a plot",
      "* ",
      "* set "+KeyWords.ENABLE_AUTOSCALE+"=true",
      "* true/false, enables/disables autoscaling of plot. Autoscaling is disabled on Log plots.",
      "* ",
      "* set "+KeyWords.MIN_SCALE_VALUE+"=-100",
      "* Defines the min Y scale value when autoscale is disabled.",
      "* ",
      "* set "+KeyWords.MAX_SCALE_VALUE+"=100",
      "* Defines the max Y scale value when autoscale is disabled.",
      "* ",
      "* set "+KeyWords.MAX_AGE_SECONDS+"=60",
      "* Only valid on time plots. Sets the max age of the plot. After this period the plot slides off the left side of the chart.",
      "* ",
      "* set "+KeyWords.ENABLE_LOG_Y_AXIS+"=true",
      "* true/false, enables/disables a logarithmic Y scale. The min and max Y scales values are used.",
      "* ",
      "* set "+KeyWords.ENABLE_ZERO_ON_X_SCALE+"=true",
      "* true/false, enables/disables the inclusion of the value 0 the X scale when autoscale is enabled.",
      "* ",
      "* set "+KeyWords.ENABLE_ZERO_ON_Y_SCALE+"=true",
      "* true/false, enables/disables the inclusion of the value 0 the Y scale when autoscale is enabled.",
      "* ",
      "* set "+KeyWords.TICK_COUNT+"=0",
      "* Sets the tick count on the Y axis. A tick will occur at this interval on the Y axis of time, bar and xy charts.",
      "* The default is 0 which will automatically set the Y axis tick count.",
      "* ",
      "* clear 0",
      "* Clears all values in plot 0.",
      "* ",
      "* replot 0",
      "* Restarts the plotting of plot 0 at the first plot point. The next plot point added will replace the first",
      "* plot point, the next the second, and so on.",
      "* ",
      "*** ADDING NUMBERS TO PLOTS ***",
      "* ",
      "* Adding plot points to all plots except xy plots can be done as follows.",
      "* ",
      "* To add the value 40 to a single time plot you would send",
      "* 40",
      "* ",
      "* If two time plots have been defined and you wish to add 10 to the first plot and 20 to the second, you would send",
      "* 10,20",
      "* ",
      "* Multiple time plots are possible.",
      "* Only single bar plots are possible.",
      "* Only two dial plots are possible.",
      "* Mulltiple xy plots are possible.",
      "* ",
      "* To add the value x=10,y=20 to xy plot 0 you would send",
      "* 0:10:20",
      "* ",
      "* To add the value x=10,y=20 to xy plot 0 and x=50, y=60 to plot 1 you would send",
      "* 0:10:20",
      "* then send",
      "* 1:50:60",
      "* ",
      "* When plotting to a time series plot you would send",
      "* 0:2013;00;02;23;10;05;587:1.234",
      "*  Plot index 0 (digit before first colon)",
      "*  Timestamp=Jan 02 2013 23:10:05 and 587 microseconds",
      "*  value=1.234",
      "* ",
      };

  StatusBar statusBar;
  private final JPanel mainPanel = new JPanel( new BorderLayout() );
  private final JPanel chartPanel = new JPanel();
  private boolean showStatusMessages=true;
  private JMenuBar menuBar;
  private JMenu fileMenu;
  private JMenuItem savePlotsMenuItem;
  private JFileChooser saveImageJFC = new JFileChooser();
  
  public PlotFrame()
  {
    statusBar = new StatusBar();
    statusBar.println("Version "+PlotFrame.NETPLOT_VERSION);
    for( String line : helpLines )
    {
      statusBar.println(line);
    }
    getContentPane().add(mainPanel);
    mainPanel.add(statusBar,BorderLayout.SOUTH);
    JScrollPane jScrollPane = new JScrollPane(chartPanel);
    jScrollPane.setPreferredSize( new Dimension(1024,768));
    mainPanel.add(jScrollPane,BorderLayout.CENTER);
    setChartLayout(1,1);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    menuBar = new JMenuBar();
    fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);
    menuBar.add(fileMenu);
    savePlotsMenuItem = new JMenuItem("Save Image", KeyEvent.VK_S);
    savePlotsMenuItem.getAccessibleContext().setAccessibleDescription("Save plots as an image file");
    fileMenu.add(savePlotsMenuItem);
    setJMenuBar(menuBar);
    savePlotsMenuItem.addActionListener(this);
    saveImageJFC.setToolTipText("Save window to a PNG image file.");
    
    try {
    	NetPlotter.PersistentConfig.load(NetPlotter.NetPlotterPersistentConfigFile);
    	//Set the location of the window when previously shutdown
    	//But protect from being moved off the screen
    	if( NetPlotter.PersistentConfig.guiXPos < 0 ) {
    		NetPlotter.PersistentConfig.guiXPos=0;
    	}    	
    	if( NetPlotter.PersistentConfig.guiYPos < 0 ) {
    		NetPlotter.PersistentConfig.guiYPos=0;
    	}
    	setLocation(NetPlotter.PersistentConfig.guiXPos, NetPlotter.PersistentConfig.guiYPos);
    	setSize(NetPlotter.PersistentConfig.guiWidth, NetPlotter.PersistentConfig.guiHeight);
    }
    catch(Exception e) {
        mainPanel.add(jScrollPane,BorderLayout.CENTER);
        pack();    	
    }
    ShutDownHandler shutDownHandler = new ShutDownHandler(this);
    Runtime.getRuntime().addShutdownHook(shutDownHandler);
    
  }

  public void addPanel(Component panel, int index)
  {
    chartPanel.add(panel,index);
  }

  public void removePanel(int index)
  {
    chartPanel.remove(index);
  }

  private void removeAllPanels()
  {
    chartPanel.removeAll();
  }

  public int getPanelCount()
  {
    return chartPanel.getComponentCount();
  }

  public void setChartLayout(int rows, int columns)
  {
    removeAllPanels();
    chartPanel.setLayout( new GridLayout(rows,columns) );
  }

  public static void main(String args[])
  {
    PlotFrame plotFrame = new PlotFrame();
    plotFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    TimeSeriesPlotPanel plotPanel = new TimeSeriesPlotPanel();
    plotFrame.getContentPane().add(plotPanel);
    plotFrame.pack();

    plotFrame.setVisible(true);
    try
    {
      plotPanel.setAttribute(KeyWords.PLOT_NAME, "plot1");
      plotPanel.setAttribute(KeyWords.ENABLE_SHAPES, "true");
      plotPanel.setAttribute(KeyWords.ENABLE_LINES, "true");
      plotPanel.setAttribute(KeyWords.ENABLE_AUTOSCALE, "true");
      plotPanel.addPlot();
      plotPanel.setAttribute(KeyWords.PLOT_NAME, "plot2");
      plotPanel.addPlot();
      while(true)
      {
        plotPanel.addPlotValue(0,Math.random());
        plotPanel.addPlotValue(1,Math.random()*-100);
        try
        {
          Thread.sleep(500);
        }
        catch(Exception e) {}
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  public void addStatusMessage(String line)
  {
    if( showStatusMessages ) {
      statusBar.println(line);
    }
  }

  public void SetEnableStatusMessages(boolean enabled) {
    showStatusMessages=enabled;
  }

  @Override
  /**
   * @brief Process menu selection events
   */
  public void actionPerformed(ActionEvent e) {

	  if( e.getSource() == savePlotsMenuItem ) {
		  captureFrame();
	  }
	  
  }
  
  /**
   * @brief Capture all plots to a png file.
   */
  private void captureFrame() {
      
      try {
    	  int retVal = saveImageJFC.showSaveDialog(null);
		  int width  = chartPanel.getWidth();
		  int height = chartPanel.getHeight();
		  
		  if( height > 0  && height > 0 ) {
			  
			  if(retVal==JFileChooser.APPROVE_OPTION){
		          BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		          chartPanel.paint(im.getGraphics());
		          String filename = saveImageJFC.getSelectedFile().getAbsolutePath();
		          
		          String fileSuffix = "";
		          if( filename.toLowerCase().endsWith(".gif") ) {
		        	  fileSuffix="GIF";
		          } else if( filename.toLowerCase().endsWith(".png") ) {
		        	  fileSuffix="PNG";
		          }
		          
		          if( fileSuffix.length() > 0 ) {
		        	  ImageIO.write(im, fileSuffix, saveImageJFC.getSelectedFile());
		          }
		          else {
		        	  JOptionPane.showMessageDialog(this, "Invalid image file type. File extension must be gif or png.", "Error", JOptionPane.ERROR_MESSAGE);
		          }
    		  }
    	  }
		  else {
        	  JOptionPane.showMessageDialog(this, "Plot window to small to save as an image.", "Error", JOptionPane.ERROR_MESSAGE);			  
		  }
           
      } catch (IOException e) {
          e.printStackTrace();
      }
  }

}

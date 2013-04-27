/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot.client;

import java.net.*;
import java.util.StringTokenizer;
import java.io.*;
import netplot.KeyWords;
import java.util.*;

/**
 * Responsible for providing a client interface to the netplot server.
 */
public class NetplotClient
{
  public boolean    debug;
  public double     serverVersion;
  Socket            socket;
  BufferedReader    br;
  PrintWriter       pw;
  int               serverResponseTimeout=30000;
  boolean           cacheEnabled; //In cached plot mode the commands to add plot points to a graph or graphs 
                                  //are cached until the update() method is called. This means that all plot
                                  //points are sent to the server together. When not in fast mode (default), 
                                  //each time addPlotValues or addXYPlotValues is called then data is sent 
                                  //to the server to add these plot points.
  Vector<String>    plotValueCache;  
  
  public NetplotClient()
  {
  }
  public NetplotClient(boolean debug)
  {
    this.debug=debug;
    plotValueCache = new Vector<String>();
  }

  /**
   * Display debug messages if required
   */
  void debugPrint(String message)
  {
    if( debug)
      Println("DEBUG: "+message);
  }
  
  /**
   * Connect to the server running the netplot GUI (Java) program
   * @param hostAddress The netplot server address.
   * @param port        The netplot server TCP/IP port.
   */
  public void connect(String hostAddress, int port) throws NetplotClientException, IOException
  {
    debugPrint("Connecting to "+hostAddress+":"+port);
    socket = new Socket(hostAddress,port);
    //Set the read timeout on the server response
    socket.setSoTimeout(serverResponseTimeout);
    br = new BufferedReader( new InputStreamReader(socket.getInputStream()));
    pw = new PrintWriter(socket.getOutputStream());
    debugPrint("Connected to "+hostAddress+":"+port);
    //Wait for initial connection message
    String line = br.readLine();
    debugPrint("line="+line);
    boolean netPlotServer=true;
    StringTokenizer strTok = new StringTokenizer(line,"=");
    if( line.indexOf("netplot_version=") != 0 || strTok.countTokens() != 2 )
    {
      netPlotServer=false;
    }
    if(netPlotServer) 
    {
      try
      {
        strTok.nextToken();
        serverVersion=Double.parseDouble(strTok.nextToken());
      }
      catch(Exception e) 
      {
        netPlotServer=false;
      }
    }
    if( !netPlotServer )
    {
      throw new NetplotClientException(hostAddress+":"+port+" is not a netplot server. Received "+line);
    }
  }
  
  /**
   * Disconnect form the netplot server and clean up resources.
   */
  public void disconnect()
  {
    if( socket != null )
    {
      try
      {
        socket.close();
      }
      catch(IOException e) {}
      socket=null;
      br=null;
      pw.close();
      pw=null;
    }
  }
  
  /**
   * Get the netplot server version received when we connected to the netplot server.
   * 
   * @return The server version
   */
  public double getServerVersion()
  {
    return serverVersion;
  } 

  
  /**
   * Send a command to the netplot server
   * 
   * @param cmd the command
   */
  void sendCmd(String cmd) throws NetplotClientException, IOException
  {
    debugPrint("CMD: "+cmd);
    pw.println(cmd);
    pw.flush();
    //Get response from server
    //We expect and OK response from the server
    String line = br.readLine();
    debugPrint(line);
    if( line.indexOf("ERROR: ") == 0)
    {
      throw new NetplotClientException(line);
    }
  }
  
  /**
   * Send a set command
   * 
   * @param var     The variable name
   * @param value   The variable value
   * @throws NetplotClientException
   */
  void sendSetCmd(String var, String value) throws NetplotClientException, IOException
  {
    sendCmd("set "+var+"="+value);
  }

  /**
   * Set the plot grid GUI layout
   */
  public void setGrid(int rows, int columns) throws NetplotClientException, IOException
  {
    sendSetCmd(KeyWords.GRID,""+rows+","+columns);
  }
  
  /**
   * Set the GUI window title
   * 
   * @param windowTitle The title text
   */
  public void setWindowTitle(String windowTitle) throws NetplotClientException, IOException
  {
    sendSetCmd(KeyWords.FRAME_TITLE, windowTitle);
  }
  
  /**
   * Enable/disable the chart legend
   * 
   * @param enabled If true the legend is enabled.
   */
  public void setChartLegendEnabled(boolean enabled) throws NetplotClientException, IOException
  {
    sendSetCmd(KeyWords.ENABLE_LEGEND,""+enabled);
  }

  /**
   * Init the chart
   */
  public void init() throws NetplotClientException, IOException
  {
    sendCmd(KeyWords.INIT);
  }

  /**
   * Check for a valid plot type.
   * 
   * @param plotType The plot type string 
   * @return true if valid
   */
  public boolean isValidPlotType(String plotType)
  {
    if( plotType.equals("time") ||
        plotType.equals("xy")   ||
        plotType.equals("bar")  ||
        plotType.equals("dial") ) 
    {
      return true;
    }
    return false;  
  }
  
  public void setPlotType(String plotType) throws NetplotClientException, IOException
  {
    if( !isValidPlotType(plotType) )
    {
      throw new NetplotClientException(plotType+" is an invalid plot type");
    }
    sendSetCmd(KeyWords.GRAPH,plotType);
  }
  
  public void setPlotType(String plotType, String title) throws NetplotClientException, IOException
  {
    setPlotType(plotType);
    //If we have a 
    if( title != null && title.length() > 0 )
    {
      sendSetCmd(KeyWords.PLOT_TITLE,title);
    }
    init();
  }
    
  /**
   * Add a plot trace
   * 
   * @param plotConfig The config for the plot
   */
  public void addPlot(PlotConfig plotConfig) throws NetplotClientException, IOException
  {
    if( plotConfig != null)
    {
      sendSetCmd(KeyWords.PLOT_NAME,plotConfig.plotName);
      sendSetCmd(KeyWords.X_AXIS_NAME,plotConfig.xAxisName);
      sendSetCmd(KeyWords.Y_AXIS_NAME,plotConfig.yAxisName);
      sendSetCmd(KeyWords.ENABLE_LINES,""+plotConfig.enableLines);
      sendSetCmd(KeyWords.ENABLE_SHAPES,""+plotConfig.enableShapes);
      sendSetCmd(KeyWords.ENABLE_AUTOSCALE,""+plotConfig.enableAutoScale);
      sendSetCmd(KeyWords.MIN_SCALE_VALUE,""+plotConfig.minScaleValue);
      sendSetCmd(KeyWords.MAX_SCALE_VALUE,""+plotConfig.maxScaleValue);
      sendSetCmd(KeyWords.MAX_AGE_SECONDS,""+plotConfig.maxAgeSeconds);
      sendSetCmd(KeyWords.ENABLE_LOG_Y_AXIS,""+plotConfig.enableLogYAxis);
      sendSetCmd(KeyWords.ENABLE_ZERO_ON_X_SCALE,""+plotConfig.enableZeroOnXAxis);
      sendSetCmd(KeyWords.ENABLE_ZERO_ON_Y_SCALE,""+plotConfig.enableZeroOnYAxis);
      sendSetCmd(KeyWords.TICK_COUNT,""+plotConfig.tickCount);
    }
    sendCmd(KeyWords.ADD_PLOT);
  }
  
  /**
   * Add values to plot/s
   * 
   * @param values An array of values to be plotted, one element for each 
   *               plot added. The number of plots is determined by the number 
   *               of times addPlot has been called. Therefore the first element 
   *               in the list is added to the first plot, second to the seconds 
   *               and so on.
   */
  public void addPlotValues(double values[]) throws NetplotClientException, IOException
  {
    if( values.length == 0 )
    {
      return;
    }
    StringBuffer strBuf = new StringBuffer(""+values[0]);
    boolean firstValue=true;
    for( double value : values )
    {
      if( firstValue )
      {
        firstValue=false;
        continue;
      }
      strBuf.append(","+value);
    }
    debugPrint("Adding plot values: "+strBuf);
    if( cacheEnabled ) {
      plotValueCache.add(strBuf.toString());
    }
    else {
      sendCmd(strBuf.toString());
    }
  }
  
  /**
   * Add values to an XY plot
   * @param plotIndex  The index of the plot (0 = first plot added, 1=second etc)
   * @param xValue     The value on the X axis
   * @param yValue     The value on the Y axis
   */
  public void addXYPlotValues(int plotIndex, double xValue, double yValue) throws NetplotClientException, IOException
  {
    debugPrint("Adding XY plot "+plotIndex+" values: "+xValue+","+yValue);
    if( cacheEnabled ) {
      plotValueCache.add(plotIndex+":"+xValue+":"+yValue);
    }
    else {
      sendCmd(plotIndex+":"+xValue+":"+yValue);
    }
  }
 
  /**
   * display a message.
   * 
   * @param message
   */
  public static void Println(String message)
  {
    System.out.println(message);
  }

  /**
   * Clear the selected plot
   */
  public void clear(int plotIndex) throws NetplotClientException, IOException
  {
    sendCmd(KeyWords.CLEAR+" "+plotIndex);
  }

  /**
   * Clear the selected plot
   */
  public void replot(int plotIndex) throws NetplotClientException, IOException
  {
    sendCmd(KeyWords.REPLOT+" "+plotIndex);
  }

  /**
   * Enable/Disable the GUI status messages.
   * If CPU bouind then not prointing the status messages to the status history may speed up plotting.
   */
  public void enableStatusMessages(boolean enabled) throws NetplotClientException, IOException
  {
    if( enabled ) {
      sendCmd("enable_status 1");
    }
    else {
      sendCmd("enable_status 0");      
    } 
  }
  
  /**
   * Enable/Disable the cache.
   * If enabled then the update method must be called to plot the data.
   * 
   * @param enabled
   */
  public void enableCache(boolean enabled) {
    this.cacheEnabled=enabled;
  }

  /**
   * Send all plotValueCache plot points. Only call this when cacheEnabled is True
   */
  public void update() throws NetplotClientException, IOException {
    int cmdCount = plotValueCache.size();
    
    //If there is nothing to send
    if( cmdCount == 0 ) {
      //quit
      return;
    }
      
    //Build a single string containing all the commands
    StringBuffer cmd = new StringBuffer();
    for( String plotValue : plotValueCache ) {
      cmd.append(plotValue+"\n");
    }
    //Empty the cache
    plotValueCache.removeAllElements();
      
    debugPrint("CMD: "+cmd);
    pw.println(cmd);
    pw.flush();
    
    //Wait for responses
    int rxCmdCount=0;
    while( rxCmdCount < cmdCount ) {
      //Get response from server
      //We expect and OK response from the server
      String line = br.readLine();
      debugPrint(line);
      if( line.indexOf("ERROR: ") == 0)
      {
        throw new NetplotClientException(line);
      }
      rxCmdCount++;
    }
  }
      
  /**
   * This completes the Netplot client code. The follwoing code is example code.
   */
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  public static void Delay(long delayMilliSeconds)
  {
    try
    {
      Thread.sleep(delayMilliSeconds);
    }
    catch(InterruptedException e) {}
  }
  
  /**
   * Single plot on a time series chart
   */
  public static void TimeExample1(NetplotClient netPlot) throws NetplotClientException, IOException
  {
    PlotConfig plotConfig = new PlotConfig();
    plotConfig.plotName="Plot 0";
    plotConfig.xAxisName="The X axis";
    plotConfig.yAxisName="The Y axis (Plot0)";
    plotConfig.enableLines=true;
    plotConfig.enableShapes=true;
    plotConfig.enableAutoScale=false;
    plotConfig.minScaleValue=0;
    plotConfig.maxScaleValue=10000;
    plotConfig.maxAgeSeconds=5;
    plotConfig.tickCount=1000;
    
    netPlot.setPlotType("time", "TIME chart, single plot");
    //Uncomment this to remove the Legend on the chart
    //netPlot.setChartLegendEnabled(0)
    netPlot.addPlot(plotConfig);
    int i=0;
    while(i< 10)
    {
      double values[] = { Math.random()*10000 };
      netPlot.addPlotValues(values);
      i=i+1;
    }
  }
 
  /**
   * Three plots on a time series chart with the same Y axis.
   */
  public static void TimeExample2(NetplotClient netPlot) throws NetplotClientException, IOException
  {
	String []plotNames = {"Sine","Cosine","Tangent"};
    PlotConfig plotConfig = new PlotConfig();
    plotConfig.xAxisName="The X axis";
    plotConfig.yAxisName="Y axis (Plot0)";
    plotConfig.enableLines=true;
    plotConfig.enableShapes=true;
    plotConfig.enableAutoScale=false;
    plotConfig.minScaleValue=-5;
    plotConfig.maxScaleValue=5;
    
    netPlot.setPlotType("time", "TIME chart, two traces with different linear Y scales, both autoscaled");
    
    for( String plotName : plotNames ) {
    	plotConfig.plotName=plotName;
    	netPlot.addPlot(plotConfig);  
        //We only want one Y axis so null plot axis name
    	plotConfig.xAxisName="";
    	plotConfig.yAxisName="";
    }
    
    double x=0.01;
    while(x < 25)
    {
      double values[] = { Math.sin(x), Math.cos(x), Math.tan(x) };
      netPlot.addPlotValues(values);
      x=x+0.1;
      try 
      {
    	  Thread.sleep(10);
      }
      catch(InterruptedException e) {}
    }
  }

  /**
   * Single plot on a time series chart
   */
  public static void TimeExample3(NetplotClient netPlot) throws NetplotClientException, IOException
  {
    PlotConfig plotConfig = new PlotConfig();
    plotConfig.plotName="Plot 0";
    plotConfig.xAxisName="The X axis";
    plotConfig.yAxisName="Y axis (Plot0)";
    plotConfig.enableLines=true;
    plotConfig.enableShapes=true;
    plotConfig.enableAutoScale=true;
    //plotConfig.minScaleValue=0;
    //plotConfig.maxScaleValue=10000;
    plotConfig.maxAgeSeconds=5;
    netPlot.setPlotType("time", "TIME chart, two traces with different linear Y scales, both autoscaled");
    netPlot.addPlot(plotConfig);  
    plotConfig.plotName="Plot 1";
    //Add a new Y Axis
    plotConfig.yAxisName="Y Axis (Plot1)";
    netPlot.addPlot(plotConfig);
    
    int i=0;
    while(i< 10)
    {
      double values[] = { Math.random()*10000 , Math.random()*100 };
      netPlot.addPlotValues(values);
      i=i+1;
    }
  }
  
  /**
   * Bar chart example
   * 
   * @param netPlot
   * @param delayMilliSeconds
   * @throws NetplotClientException
   * @throws IOException
   */
  public static void BarExample(NetplotClient netPlot) throws NetplotClientException, IOException
  {
    PlotConfig plotConfig0 = new PlotConfig();
    plotConfig0.plotName="Plot 0";
    plotConfig0.xAxisName="The X axis";
    plotConfig0.yAxisName="The Y axis";
    plotConfig0.enableAutoScale=true;
    //plotConfig0.minScaleValue=0;
    //plotConfig0.maxScaleValue=10000
    //log axis not currently supported on Bar charts    
    netPlot.setPlotType("bar", "BAR chart");
    netPlot.addPlot(plotConfig0);
    int i=0;
    while(i< 10)
    {
      double values[] = { Math.random()*10000 };
      netPlot.addPlotValues(values);
      i=i+1;
    }
  }
  
  /**
   * two XY plots with different linear Y scales
   * @param netPlot
   * @param delayMilliSeconds
   * @throws NetplotClientException
   * @throws IOException
   */
  public static void XYExample1(NetplotClient netPlot) throws NetplotClientException, IOException
  {
    PlotConfig plotConfig0 = new PlotConfig();
    plotConfig0.plotName="Plot 0";
    plotConfig0.xAxisName="The X axis name";
    plotConfig0.yAxisName="Y axis (Plot 0)";
    plotConfig0.enableLines=true;
    plotConfig0.enableShapes=true;
    plotConfig0.enableAutoScale=true;
    plotConfig0.enableZeroOnXAxis=false;
    plotConfig0.enableZeroOnYAxis=false;
    netPlot.setPlotType("xy", "XY chart, two traces with different linear Y scales, both autoscaled");
    netPlot.addPlot(plotConfig0);
    
    PlotConfig plotConfig1 = new PlotConfig();
    plotConfig1.plotName="Plot 1";
    plotConfig1.yAxisName="Y axis (Plot 1)";
    plotConfig1.enableLines=true;
    plotConfig1.enableShapes=true;
    plotConfig1.enableAutoScale=true;
    plotConfig1.enableZeroOnXAxis=false;
    plotConfig1.enableZeroOnYAxis=false;
    netPlot.addPlot(plotConfig1);
    int i=0;
    while( i<10 )
    {
      netPlot.addXYPlotValues(0,-90+(Math.random()*20), 130+(Math.random()*20) );
      netPlot.addXYPlotValues(1,-60+(Math.random()*10), 75+(Math.random()*5));
      i++;
    }
  }
  
  /**
   * XY plot with log Y scale
   * 
   * @param netPlot
   * @param delayMilliSeconds
   * @throws NetplotClientException
   * @throws IOException
   */
  public static void XYExample2(NetplotClient netPlot) throws NetplotClientException, IOException
  {
    PlotConfig plotConfig0 = new PlotConfig();
    plotConfig0.plotName="Plot 0";
    plotConfig0.xAxisName="The X axis name";
    plotConfig0.yAxisName="Log Y axis";
    plotConfig0.enableLines=true;
    plotConfig0.enableShapes=true;
    plotConfig0.enableLogYAxis=true;
    plotConfig0.minScaleValue=1E-10;
    plotConfig0.maxScaleValue=1E-2;
    netPlot.setPlotType("xy", "XY chart with log Y scale");
    netPlot.addPlot(plotConfig0);
    netPlot.addXYPlotValues(0,-50, 1E-9);
    netPlot.addXYPlotValues(0,-55, 1E-7);
    netPlot.addXYPlotValues(0,-60, 1E-6);
    netPlot.addXYPlotValues(0,-70, 1E-5);
    netPlot.addXYPlotValues(0,-80, 1E-4);
    netPlot.addXYPlotValues(0,-90, 1E-3);
  }
  
  /**
   * XY chart with 2 lin and 2 log Y scales
   * @param netPlot
   * @param delayMilliSeconds
   * @throws NetplotClientException
   * @throws IOException
   */
  public static void XYExample3(NetplotClient netPlot) throws NetplotClientException, IOException
  {
    PlotConfig plotConfig0 = new PlotConfig();
    plotConfig0.plotName="Plot 0";
    plotConfig0.xAxisName="The X axis name";
    plotConfig0.yAxisName="Log Y axis (Plot 0)";
    plotConfig0.enableLines=true;
    plotConfig0.enableShapes=true;
    plotConfig0.enableLogYAxis=true;
    plotConfig0.minScaleValue=1E-10;
    plotConfig0.maxScaleValue=1E-2;
    netPlot.setPlotType("xy", "XY chart with 2 lin and 2 log Y scales");
    netPlot.addPlot(plotConfig0);  
    
    PlotConfig plotConfig1 = new PlotConfig();
    plotConfig1.plotName="Plot 1";
    plotConfig1.yAxisName="Y axis (Plot 1)";
    plotConfig1.enableLines=true;
    plotConfig1.enableShapes=true;
    plotConfig1.enableAutoScale=true;
    plotConfig1.enableZeroOnXAxis=true;
    plotConfig1.enableZeroOnYAxis=true;
    netPlot.addPlot(plotConfig1);

    PlotConfig plotConfig2 = new PlotConfig();
    plotConfig2.plotName="Plot 2";
    plotConfig2.yAxisName="Y axis (Plot 2)";
    plotConfig2.enableLines=true;
    plotConfig2.enableShapes=true;
    plotConfig2.enableAutoScale=true;
    plotConfig2.enableZeroOnXAxis=false;
    plotConfig2.enableZeroOnYAxis=false;
    netPlot.addPlot(plotConfig2);
    
    PlotConfig plotConfig3 = new PlotConfig();
    plotConfig3.plotName="Plot 3";
    plotConfig3.yAxisName="Log Y axis (Plot 3)";
    plotConfig3.enableLines=true;
    plotConfig3.enableShapes=true;
    plotConfig3.enableLogYAxis=true;
    plotConfig3.minScaleValue=1E-10;
    plotConfig3.maxScaleValue=1E-2;
    netPlot.addPlot(plotConfig3);
    
    netPlot.addXYPlotValues(0,-50, 1E-9);
    netPlot.addXYPlotValues(0,-55, 1E-7);
    netPlot.addXYPlotValues(0,-60, 1E-6);
    netPlot.addXYPlotValues(0,-70, 1E-5);
    netPlot.addXYPlotValues(0,-80, 1E-4);
    netPlot.addXYPlotValues(0,-90, 1E-3);
    netPlot.addXYPlotValues(1,-10, 10);
    netPlot.addXYPlotValues(1,-9, 12);
    netPlot.addXYPlotValues(1,-8, 14);
    netPlot.addXYPlotValues(1,-7, 16);
    netPlot.addXYPlotValues(1,-6, 18);
    netPlot.addXYPlotValues(1,-5, 20);
    
    netPlot.addXYPlotValues(2,-35, 10);
    netPlot.addXYPlotValues(2,-95, 12);
    netPlot.addXYPlotValues(2,-85, 14);
    netPlot.addXYPlotValues(2,-75, 16);
    netPlot.addXYPlotValues(2,-65, 18);
    netPlot.addXYPlotValues(2,-55, 20);

    netPlot.addXYPlotValues(3,1, 1E-9);
    netPlot.addXYPlotValues(3,2, 1E-7);
    netPlot.addXYPlotValues(3,3, 1E-6);
    netPlot.addXYPlotValues(3,4, 1E-5);
    netPlot.addXYPlotValues(3,5, 1E-4);
    netPlot.addXYPlotValues(3,6, 1E-3);
  }
  
  public static void DialExample(NetplotClient netPlot) throws NetplotClientException, IOException
  {
    double values[] = new double[2];
    
    netPlot.setPlotType("dial", "Number and MAX");

    PlotConfig plotConfig = new PlotConfig();
    plotConfig.plotName="Number";
    plotConfig.minScaleValue=0;
    plotConfig.maxScaleValue=200;
    plotConfig.tickCount=10;
     
    netPlot.addPlot(plotConfig);
    plotConfig.plotName="MAX";
    netPlot.addPlot(plotConfig);
     
    double max=0;
    double value=0;
    while( value < 200 )
    {
      value=value+(-10+Math.random()*30);
      if(value > 200)
      {
        value=200;
      }
      else if( value < 0)
      {
        value=0;
      }
      if( max < value )
      {
        max=value;
      }
      values[0]=value;
      values[1]=max;
      netPlot.addPlotValues(values);
      NetplotClient.Delay(100);
    }
  }
  
  
  /**
   * XY plot with log Y scale but in this case we send the data for all the
   *  points to be plotted and then use the update command to plot them all
   *  quickly.
   * 
   * @param netPlot
   * @param delayMilliSeconds
   * @throws NetplotClientException
   * @throws IOException
   */
  public static void CachedPlot(NetplotClient netPlot) throws NetplotClientException, IOException
  {
    PlotConfig plotConfig0 = new PlotConfig();
    plotConfig0.plotName="Plot 0";
    plotConfig0.xAxisName="The X axis name";
    plotConfig0.yAxisName="Log Y axis";
    plotConfig0.enableLines=true;
    plotConfig0.enableShapes=false;
    plotConfig0.minScaleValue=0;
    plotConfig0.maxScaleValue=1;
    plotConfig0.enableAutoScale=false;
    netPlot.setPlotType("xy", "Cached, fast XY chart with log Y scale");
    netPlot.addPlot(plotConfig0);
    
    //These must be called after the plot type is added
    //Enables cached operation, update() will draw all plot points
    netPlot.enableCache(true);
    //Not printing status messages may speed up plotting if CPU bound
    //netPlot.enableStatusMessages(false);    
    
    while(true) {
      netPlot.replot(0);
      int v=0;
      while( v < 100) {
        netPlot.addXYPlotValues(0, v, Math.random());
        v++;
      }
      netPlot.update();
    }
  }
  
  /**
   * Show all plots
   * 
   * @param address    The netplot server address
   * @param initWindow If true the window will be initialized.
   */
  public static void ShowAllExamples(String address, boolean initWindow) throws NetplotClientException, IOException
  {
    boolean debug=false;
    int     port=9600;
    NetplotClient netPlot1=null;
    NetplotClient netPlot2=null;
    NetplotClient netPlot3=null;
    NetplotClient netPlot4=null;
    NetplotClient netPlot5=null;
    NetplotClient netPlot6=null;
    NetplotClient netPlot7=null;

    NetplotClient netPlot0 = new NetplotClient(debug);          
    netPlot0.connect(address, port);
    
    if( initWindow )
    {
      //Set these non plot or chart specific parameters on first connection
      netPlot0.setGrid(3,3);
      netPlot0.setWindowTitle("Java netplot client demo");
    }
    try {
      NetplotClient.TimeExample1(netPlot0);
  
      netPlot1 = new NetplotClient(debug);          
      netPlot1.connect(address, port+1);
      NetplotClient.TimeExample2(netPlot1);
  
      netPlot1 = new NetplotClient(debug);          
      netPlot1.connect(address, port+2);
      NetplotClient.TimeExample3(netPlot1);
  
      netPlot2 = new NetplotClient(debug);          
      netPlot2.connect(address, port+3);
      NetplotClient.BarExample(netPlot2);
  
      netPlot3 = new NetplotClient(debug);          
      netPlot3.connect(address, port+4);
      NetplotClient.XYExample1(netPlot3);
  
      netPlot4 = new NetplotClient(debug);          
      netPlot4.connect(address, port+5);
      NetplotClient.XYExample2(netPlot4);
  
      netPlot5 = new NetplotClient(debug);          
      netPlot5.connect(address, port+6);
      NetplotClient.XYExample3(netPlot5);
  
      netPlot6 = new NetplotClient(debug);          
      netPlot6.connect(address, port+7);
      NetplotClient.DialExample(netPlot6);
  
      netPlot7 = new NetplotClient(debug);          
      netPlot7.connect(address, port+8);
      NetplotClient.CachedPlot(netPlot7);
    }
    finally {
      if( netPlot0 != null ) {
        netPlot0.disconnect();
      }
      if( netPlot1 != null ) {
        netPlot1.disconnect();
      }
      if( netPlot2 != null ) {
        netPlot2.disconnect();
      }
      if( netPlot3 != null ) {
        netPlot3.disconnect();
      }
      if( netPlot4 != null ) {
        netPlot4.disconnect();
      }
      if( netPlot5 != null ) {
        netPlot5.disconnect();
      }
      if( netPlot6 != null ) {
        netPlot6.disconnect();
      }
      if( netPlot7 != null ) {
        netPlot7.disconnect();
      }
    }
  }
  
  /**
   * show program usage options
   */
  public static void Usage()
  {
    Println("              : If no options are provided then an attempt to connect to the netplot server");
    Println("                on localhost will be made. Then an attempt to display the example plots will");
    Println("                be made.");
    Println("--host        : Followed by the netplot server host address (default=1270.0.1)");
    Println("-h, --help    : Show this help text");
  }
  
  /*
   * Create high quality GUI (multiplatform) graphs with as little coding as possible.
   * 
   * netplot relies on the Java netplot server software to plot the GUI's. The server
   * software should be running (execute java -jar netplot.jar) before running the
   * netplot client software.
   *
   * NetPlot requires that you run the server application on a machine that is reachable 
   * over an IP network from the machine using the python netplot module. This server will
   * listen on TCP/IP ports and display the data sent to it. Each port provides a connection
   * to a different plot.
   * Therefore you must start the netplot server (execute java -jar netplot.jar) on the local 
   * or remote machine before attempting to send plot information to it.
   *
   * Normally this python module will be imported into your application to provide the plots 
   * you define. 
   * The main entry point here allows some example plots to be displayed.
   */
  public static void main(String args[])
  {
    String address="127.0.0.1";
 
    boolean readHostAddress=false;
    boolean showExamples=true;
    
    for( String arg : args )
    {
      if( readHostAddress )
      {
        address=arg;
        readHostAddress=false;
        continue;
      }
    
      if( arg.equals("-h") || arg.equals("--help") )
      {
        NetplotClient.Usage();
        System.exit(0);
      }
      else if( arg.equals("--host") )
      {
        readHostAddress=true;
      }
      else
      {
        NetplotClient.Usage();
        Println("\n!!! Unknown command line option ("+arg+")");
        System.exit(-1);
      }
    }
    try
    {
      if( showExamples )
      {
        NetplotClient.ShowAllExamples(address, true);
      }
    }
    catch(NetplotClientException e)
    {
      e.printStackTrace();
    }
    catch(IOException e)
    {
      e.printStackTrace();      
    }
  }
}

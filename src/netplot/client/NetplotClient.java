/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot.client;

import java.net.*;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.io.*;
import netplot.KeyWords;
import netplot.LineProcessor;

import java.util.*;
import org.jfree.data.time.Millisecond;

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
   * display a message.
   * 
   * @param message
   */
  public static void Println(String message)
  {
    System.out.println(message);
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
    pw = new PrintWriter(socket.getOutputStream(), true);
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
      sendSetCmd(KeyWords.LINE_WIDTH,""+plotConfig.lineWidth);
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
   * Add values to an XY plot
   * @param plotIndex  The index of the plot (0 = first plot added, 1=second etc)
   * @param xValue     The value on the X axis
   * @param yValue     The value on the Y axis
   */
  public void addTimeSeriesPlotValue(int plotIndex, Millisecond ms, double yValue) throws NetplotClientException, IOException
  {
    debugPrint("Adding TimeSeries plot "+plotIndex+" Millisecond "+ms+" value "+yValue);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime( ms.getStart() );
    String dateString = this.getDateString(calendar, ms.getMillisecond() );
    sendCmd(plotIndex+":"+dateString+":"+yValue);
  }
  
  /**
   * Get date string.
   * @param date The Date object to be converted to a string.
   * @param ms The MilliSecond value to be included in the Date String.
   * @return The String representation of the Date.
   * 
   * The format of the string returned is 
   * <YEAR>;<MONTH>;<DAY OF MONTH>;<HOUR OF DAY>;<MINUTE>;<SECOND>;<MILLISECOND>
   * 
   * <YEAR>  = Year (1900-9999)
   * <MONTH> = Month (1-12)
   * <DAY OF MONTH> = Month (1-31)
   * <HOUR OF DAY>  = Hour of day (0-23)
   * <MINUTE>       = Minute (0-59)
   * <SECOND>       = Second (0-59)
   * <MILLISECOND>  = Millisecond (0-999)
   * E.G
   * 2013;00;02;23;10;05;587
   * 
   * Represents
   * 
   * 23:20:05:587 on 1 Jan 2013
   * 
   */
  private String getDateString(Calendar calendar, long ms) {
    StringBuffer sb= new StringBuffer();

    sb.append(calendar.get(Calendar.YEAR));
    sb.append(LineProcessor.TIMESTAMP_DELIM);
    sb.append(calendar.get(Calendar.MONTH)+1);
    sb.append(LineProcessor.TIMESTAMP_DELIM);
    sb.append(calendar.get(Calendar.DAY_OF_MONTH));
    sb.append(LineProcessor.TIMESTAMP_DELIM);
    sb.append(calendar.get(Calendar.HOUR_OF_DAY));
    sb.append(LineProcessor.TIMESTAMP_DELIM);
    sb.append(calendar.get(Calendar.MINUTE));
    sb.append(LineProcessor.TIMESTAMP_DELIM);
    sb.append(calendar.get(Calendar.SECOND));
    sb.append(LineProcessor.TIMESTAMP_DELIM);
    sb.append(ms);

    return sb.toString();
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
      

  
}

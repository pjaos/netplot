/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot;

import java.net.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

/*
 * Changelog can be found in PlotFrame
 */

/**
 * Responsible for reading from a socket and dynamically updating graphs.
 * This is designed to ease the process of plotting data as a remote program
 * may send text containing numbers to be plotted. The text may also include
 * information to setup axis on the plot and set plot names etc.
 * 
 */
public class NetPlotter
{
  public static final String SERVER_PORT="SERVER_PORT";
  private File configFile = new File(System.getProperty("user.home"), ".NetPlotter.cfg");
  private Properties configProperties = new Properties();

  private boolean   serverRunning;
  public static int basePort;
  public static int maxPlotCount;
  public Vector<NetPlotSocketHandler> netPlotSocketHandlers = new Vector<NetPlotSocketHandler>();
  
  /**
   * Parameterless JavaBean constructor
   */
  public NetPlotter()
  {
  }
  
  /**
   * Start the all the server threads. Each server thread is associated with the server port, which in turn is associated with a graph.
   * 
   */
  public void startServers()
  {
    class ServerThread extends Thread
    {
      private int port;
      public ServerThread(int port)
      {
        this.port=port;
      }
      public void run()
      {
        runServer(port);
      }
    }
    UO.Info("You may connect netplot clients to TCP/IP ports "+basePort+"-"+(basePort+maxPlotCount-1));
    for( int i= 0 ; i<maxPlotCount ; i++ )
    {
      ServerThread serverThread = new ServerThread(basePort+i);
      serverThread.start();
    }
  }
  
  /**
   * When started the server thread will attempt to serve FileTransferHandlers the connected sockets.
   */
  public void runServer(int port)
  {
    serverRunning=true;
    try
    {
      Socket socket;
      ServerSocket serverSocket = new ServerSocket();
      InetSocketAddress localAddress = new InetSocketAddress(port);
      serverSocket.bind(localAddress);

      UO.Info("NetPlot server waiting for connections on TCP/IP port "+port);
      while(serverRunning)
      {
        socket = serverSocket.accept();
        UO.Info(socket.getInetAddress()+" connected to the server");
        NetPlotSocketHandler netPlotSocketHandler = new NetPlotSocketHandler(port-basePort, maxPlotCount);
        netPlotSocketHandler.handle(socket);
        netPlotSocketHandlers.add(netPlotSocketHandler);
      }
    }
    catch(IOException e)
    {
      UO.Debug("TCP port "+port+": "+e);
    }
  }
  
  public void loadConfig()
  {
    //Set default data file
    configProperties.put(SERVER_PORT, "9600");
    FileInputStream in=null;
    try {
      in = new FileInputStream(configFile);
      configProperties.load(in);
    } 
    catch (IOException e) {}
    finally
    {
      if( in != null )
      {
        try
        {
         in.close();
        } 
        catch (IOException e) {}
      }
    }
  }
  
  public void saveConfig()
  {
    FileOutputStream out=null;
    try
    {
      out = new FileOutputStream(configFile);
      configProperties.store(out, this.getClass().getName()+" config file");
    }
    catch(IOException e) {}
    finally
    {
      if( out != null )
      {
        try
        {
         out.close();
        } 
        catch (IOException e) {}
      }
    }
  }
  
  /**
   * Allow the user to enter the port number
   */
  private void userInputServer()
  {
    loadConfig();
    String ans = JOptionPane.showInputDialog(null, "Please enter the required NetPlot base server port",configProperties.get(SERVER_PORT));
    if( ans != null && ans.length() > 0 )
    {
      while(true)
      {
        try
        {
          basePort=Integer.parseInt(ans);
          if( basePort < 1 || basePort > 65535 )
          {
            JOptionPane.showMessageDialog(null, "The base port number must be from 1 to 65535");
          }
          else
          {
            configProperties.put(SERVER_PORT, ""+basePort);
            saveConfig();
            return;
          }
        }
        catch(NumberFormatException e)
        {
          JOptionPane.showMessageDialog(null, ans+" is not a valid port number. The port number must be from 1 to 65535");
        }
      }
    }
    System.exit(0);
  }

  public void setBasePort(int port) { NetPlotter.basePort=port; }
  public int  getBasePort() { return basePort; }
  
  public static void Usage()
  {
    UO.Info("netplot usage");
    UO.Info("-g : GUI input mode. Prompt user for TCP/IP base port etc");
    UO.Info("-p : Followed by the TCP/IP base port (default=9600).");
    UO.Info("     This is the TCP/IP port used to plot on the first chart");
    UO.Info("-m : Followed by the max number of charts (default=10). ");
    UO.Info("-h : This help text. ");
  }
  
  public static void main(String args[])
  {
    boolean guiInputMode=false;
    basePort=9600;
    maxPlotCount=10;
    boolean readBasePort=false;
    boolean readMaxPlotCount=false;
    
    UO.Info("Netplot server version "+PlotFrame.NETPLOT_VERSION);
    
    for( String arg : args )
    {
      if( readBasePort )
      {
        try
        {
          basePort=Integer.parseInt(arg);
          if( basePort > 0 && basePort < 65536 )
          {
            readBasePort=false;
          }
        }
        catch(NumberFormatException e) {}
      }
      if( readMaxPlotCount )
      {
        try
        {
          maxPlotCount=Integer.parseInt(arg);
          if( maxPlotCount > 0 && maxPlotCount < 1000 )
          {
            readMaxPlotCount=false;
          }
        }
        catch(NumberFormatException e) {}
      }
      if( arg.equals("-h") )
      {
        NetPlotter.Usage();
        System.exit(0);
      }
      if( arg.equals("-g") )
      {
        guiInputMode=true;
      }
      if( arg.equals("-p") )
      {
        readBasePort=true;
      }
      if( arg.equals("-m") )
      {
        readMaxPlotCount=true;
      }
    }
    if( readBasePort )
    {
      UO.Error("Unable to read a valid (1-65535) TCP/IP base port.");
      System.exit(-1);
    }
    if( readMaxPlotCount )
    {
      UO.Error("Unable to read a max chart count (1-1000).");
      System.exit(-1);
    }
    UO.EnableDebug(true);
    NetPlotter networkPlotter = new NetPlotter();
    if( guiInputMode )
    {
      networkPlotter.userInputServer();
    }
    UO.InitWindow();
    networkPlotter.setBasePort(basePort);
    networkPlotter.startServers();
  }
  
}

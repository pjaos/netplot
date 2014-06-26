/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetPlotSocketHandler extends Thread
{
  private Socket            socket;
  private final LineProcessor     lineProcessor;

  public NetPlotSocketHandler(int panelIndex, int maxPanelIndex)
  {
    lineProcessor = new LineProcessor(panelIndex, maxPanelIndex);
  }

  public synchronized void handle(Socket socket)
  {
    this.socket=socket;
    start();
  }

  /**
   * Thread to handle the socket.
   */
  @Override
public void run()
  {
    String line;

    lineProcessor.initPlot();
    BufferedReader br=null;
    PrintWriter    pw=null;
    try
    {
      br = new BufferedReader( new InputStreamReader(socket.getInputStream()) );
      pw = new PrintWriter( new PrintWriter(socket.getOutputStream()), true );
      //Send initial ID message
      pw.println("netplot_version="+PlotFrame.NETPLOT_VERSION);

      while(true)
      {
        line = br.readLine();
        if( line == null )
        {
          break;
        }
        lineProcessor.processLine(line);
        pw.println("OK");
      }
    }
    catch(Exception e)
    {
      String m = e.getLocalizedMessage();
      //If an error has occurred then send message back to source
      if( m != null && m.length() > 0 )
      {
        pw.println("ERROR: "+m);
      }
      else
      {
        pw.println("ERROR: unknown");
      }
      UO.Debug(e);
    }
    finally
    {
      if( br != null )
      {
        try
        {
          br.close();
          br=null;
        }
        catch(IOException e) {}
      }
      if( pw != null )
      {
        pw.close();
        pw=null;
      }
    }
  }
}

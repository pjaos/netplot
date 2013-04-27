/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot;

import java.awt.*;

/**
 * Responsible for sending information to the user (UO=USerOutput, but quicker to type)
 * 
 */
public class UO
{
  private static boolean    Debug=true;
  private static PlotFrame  PlotFrame;
  
  /**
   * Responsible for creating the single frame to hold the graphs.
   * 
   * @param plotPanel This should be a JPanel that has a PlotPanelInterface
   */
  public synchronized static void InitWindow()
  {
    PlotFrame = new PlotFrame();
    UO.PlotFrame.setVisible(true);
  }

  public synchronized static void SetFrameTitle(String title)
  {
    UO.PlotFrame.setTitle(title);
  }
  
  /**
   * Add a component to the output frame
   * 
   * @param comp
   * @param index
   */
  public synchronized static void Set(Component comp, int index)
  {
    UO.Remove(index);
    UO.PlotFrame.addPanel(comp, index);
  }
  
  private synchronized static void Remove(int index)
  {
    if( index < UO.PlotFrame.getPanelCount()  )
    {
      UO.PlotFrame.removePanel(index);
    }
  }  
  
  /**
   * Set the number of rows and colums in the output frame.
   * Allows multiple graphs to be displayed. These are updated 
   * from different socket connections on different server ports.
   * 
   * @param rows
   * @param columns
   */
  public synchronized static void SetRowsColumns(int rows, int columns)
  {
    UO.PlotFrame.setChartLayout( rows, columns );
  }
  
  /**
   * Enable/Disable debugging.
   * By default debugging is disabled.
   * @param debug True if debugging is to be enabled.
   */
  public synchronized static void EnableDebug(boolean debug) { UO.Debug=debug; }
  
  /**
   * Return the debug state.
   * @return True if debugging is enabled.
   */
  public synchronized static boolean IsDebugEnabled() { return UO.Debug; }
  
  /**
   * Will display a stack trace of the exception on stdout if debug is enabled.
   * 
   * @param exception The exception to be used for debugging purposes.
   */
  public synchronized static void Debug(Exception exception)
  {
    if( UO.Debug )
    {
      if( exception != null ) {
    	  exception.printStackTrace();
      }
    }
    if( exception != null )
    {
      String message = exception.getLocalizedMessage();
      if( message != null && message.length() > 0 )
      {
        UO.Debug(message);
      }
    }
  }
  
  /**
   * Will display a message if debug is enabled.
   * 
   * @param message The message to be used for debugging purposes.
   */
  public synchronized static void Debug(String line)
  {
    if( UO.Debug )
    {
      UO.Println("DEBUG: "+line);
    }
  }

  /**
   * Will display an information message.
   * 
   * @param message The line of text to be displayed
   */
  public synchronized static void Info(String line)
  {
    UO.Println("INFO:  "+line);
  }

  /**
   * Will display an error message.
   * 
   * @param message The line of text to be displayed
   */
  public synchronized static void Error(String line)
  {
    UO.Println("ERROR: "+line);
  }

  /**
   * Print a line of test to stdout
   * 
   * @param line
   */
  private synchronized static void Println(String line)
  {
    if( UO.PlotFrame == null )
    {
      System.out.println(line);
    }
    else
    {
      UO.PlotFrame.addStatusMessage(line);
    }
  }
  
  public synchronized static void SetEnableStatusMessages(boolean enabled)
  {
    UO.PlotFrame.SetEnableStatusMessages( enabled );
  }
  
}

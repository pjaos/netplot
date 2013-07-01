/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot;

import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.*;

import org.jfree.data.time.Millisecond;

import java.util.*;

/**
 * Responsible foe processing lines of text either read from file or 
 * from a network connection.
 * 
 * NOTE !!!
 * If commands are added to this file the help text in PlotFrame should be updated.
 */
public class LineProcessor
{
  private boolean               plotInitialized;
  private PlotPanelInterface    plotPanelInterface;
  private int                   panelIndex;
  private int                   maxPanelIndex;
  public static final String    TIMESTAMP_DELIM=";";
  
  public LineProcessor(int panelIndex, int maxPanelIndex)
  {
    this.panelIndex=panelIndex;
    this.maxPanelIndex=maxPanelIndex;
  }
  
  /**
   * Process a line of text read from the dataFile.
   * 
   * @param line
   */
  public void processLine(String line) throws NetPlotException
  {
    UO.Debug(line);
    if( line == null || line.length() < 1 || line.startsWith("#") || line.startsWith("/") )
    {
      //Ignore empty lines and comments
      return;
    } 
    //If we have an attribute
    else if( line.startsWith(KeyWords.SET_PREFIX) )
    {
      String nameValueStr = line.substring(4);
      StringTokenizer strTok = new StringTokenizer(nameValueStr, "=");
      if( strTok.countTokens() >= 1 )
      {
        String name = strTok.nextToken();
        //Set empty value
        String value = "";
        //If we have a value
        if( strTok.countTokens() >= 1 )
        {
          value = strTok.nextToken();
        }
        if( name.equals(KeyWords.GRAPH) )
        {
          plotInitialized=false;
          if( value.equals(KeyWords.TIME) )
          {
            TimeSeriesPlotPanel timeSeriesPlotPanel = new TimeSeriesPlotPanel();
            plotPanelInterface = timeSeriesPlotPanel;            
          }
          else if( value.equals(KeyWords.BAR) )
          {
            BarPlotPanel barPlotPanel = new BarPlotPanel();
            plotPanelInterface = barPlotPanel;
          }
          else if( value.equals(KeyWords.XY) )
          {
            XYPlotPanel xyPlotPanel = new XYPlotPanel();
            plotPanelInterface = xyPlotPanel;
          }
          else if( value.equals(KeyWords.DIAL) )
          {
            DialPlotPanel dialPlotPanel = new DialPlotPanel();
            plotPanelInterface = dialPlotPanel;
          }
          else
          {
            throw new NetPlotException(value+" is an unknown graph type");
          }
        }
        else if( name.equals(KeyWords.GRID) )
        {
          strTok = new StringTokenizer(value, ",");
          if( strTok.countTokens() == 2 )
          {         
            int rows = Integer.parseInt( strTok.nextToken() );
            int columns = Integer.parseInt( strTok.nextToken() );
            if( rows*columns > maxPanelIndex )
            {
              throw new NetPlotException("Cannot have a chart grid of "+rows+" by "+columns+" as a max chart count is "+maxPanelIndex);
            }
            UO.SetRowsColumns(rows, columns);
          }
        }
        else if( name.equals(KeyWords.FRAME_TITLE) )
        {
          UO.SetFrameTitle(value);
        }
        else 
        {
          if( plotPanelInterface == null )
          {
            throw new NetPlotException("Attempt to set an attribute before a plot type has been defined.");
          }
          plotPanelInterface.setAttribute(name, value);
        }
      }
    }
    else if ( line.equals(KeyWords.INIT))
    {
      if( plotPanelInterface == null )
      {
        throw new NetPlotException("Attempt to init a graph before setting a graph type.");
      }
      plotPanelInterface.init();
      plotInitialized=true;
    }
    else if ( line.equals(KeyWords.ADD_PLOT))
    {
      if( plotPanelInterface == null )
      {
        throw new NetPlotException("Attempt to add a plot before setting a graph type.");
      }
      plotPanelInterface.addPlot();
      UO.Set((JPanel)plotPanelInterface, panelIndex);
    }
    else if ( line.startsWith(KeyWords.CLEAR))
    {
      Scanner scanner = new Scanner(line);
      scanner.next();
      int index = scanner.nextInt();
      plotPanelInterface.clear(index);
    }
    else if ( line.startsWith(KeyWords.ENABLE_STATUS) ) {
      Scanner scanner = new Scanner(line);
      scanner.next();
      String enabled = scanner.next().toLowerCase();
      if( enabled.equals("0") || 
          enabled.equals("false") || 
          enabled.equals("no") ) {
        UO.SetEnableStatusMessages(false);
      }
      else {
        UO.SetEnableStatusMessages(true);        
      }
    }
    else if ( line.startsWith(KeyWords.REPLOT) )
    {
      Scanner scanner = new Scanner(line);
      scanner.next();
      int index = scanner.nextInt();
      plotPanelInterface.replot(index);
    }
    //If we have some values to plot
    else if( plotPanelInterface != null )
    {
      //If the plot has not been initialised yet
      if( plotInitialized == false )
      {
        throw new NetPlotException("Cannot add values to plot as it has not been initialised yet.");
      }
      if( line.indexOf(':') != -1 )
      {
        //If the line contains chars indicating it contains a time stamp
        if( line.indexOf(LineProcessor.TIMESTAMP_DELIM) != -1 )
        {
          Scanner dateStrScanner = new Scanner(line);
          dateStrScanner.useDelimiter(":");
          //Extract the parameters
          int plotIndex = dateStrScanner.nextInt();
          String dateString = dateStrScanner.next();
          double yValue = dateStrScanner.nextDouble();
          Scanner dateScanner = new Scanner(dateString);
          dateScanner.useDelimiter(LineProcessor.TIMESTAMP_DELIM);
          int year        = dateScanner.nextInt();
          int month       = dateScanner.nextInt();
          int day         = dateScanner.nextInt();
          int hour        = dateScanner.nextInt();
          int minute      = dateScanner.nextInt();
          int second      = dateScanner.nextInt();
          int milliSecond = dateScanner.nextInt();
          Millisecond ms  = new Millisecond(milliSecond,
                                           second,
                                           minute,
                                           hour,
                                           day,
                                           month,
                                           year);          
          plotPanelInterface.addPlotValue(plotIndex, ms, yValue);
        }
        else {
          Double valueList[] = getValues(line);
          for( int i=0 ; i<valueList.length ; i=i+3 )
          {
            plotPanelInterface.addPlotValue((int)valueList[i].doubleValue(), valueList[i+1], valueList[i+2]);
          }     
        }
      }
      else
      {
        Double values[] = getValues(line);
        int plotIndex=0;
        for( Double value : values )
        {
          plotPanelInterface.addPlotValue(plotIndex, value.doubleValue());
          plotIndex++;
        }
      }
    }
  }
  
  public Double[] getValues(String line)
  {
    String tok;
    
    Vector<Double>values = new Vector<Double>();
    StringTokenizer strTok = new StringTokenizer(line," ,");
    while( strTok.hasMoreTokens() )
    {
      tok = strTok.nextToken();
      //If we have an X/Y value pair
      if( tok.indexOf(':') > 0 )
      {
        StringTokenizer strTok2 = new StringTokenizer(line,":");
        if( strTok2.countTokens() == 3 )
        {
          Double plotIndexValue = new Double(Double.parseDouble(strTok2.nextToken())); 
          Double xValue = new Double(Double.parseDouble(strTok2.nextToken()));
          Double yValue = new Double(strTok2.nextToken());
          //v1.21 hopeful fix here
          values.add( plotIndexValue );
          values.add( xValue );
          values.add( yValue );
        }
      }
      else
      {
        values.add( new Double(Double.parseDouble(tok)) );
      }
    }
    Double array[] = new Double[values.size()];
    int index=0;
    for( Double value : values )
    {
      array[index]=value;
      index++;
    }
    return array;
  }
  
  public void initPlot()
  {
    if( plotPanelInterface != null )
    {
      plotPanelInterface.init();
    }
  }
  
}

/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

package netplot;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Responsible for displaying the status line (typically at the bottom of a frame)
 * Maybe double clicked to show the history.
 * 
 * @author Paul Austen
 */
public class StatusBar extends JPanel implements ActionListener, MouseListener
{
  private JLabel statusBarLabel = new JLabel();
  private JFrame historyFrame;
  private JTextArea messageField;
  public  static String ToolTip="Double click for message history"; 
  private Timer t;
  
  public StatusBar()
  {
    this.setLayout(new FlowLayout(FlowLayout.LEFT));
    add(statusBarLabel);
    setBorder( BorderFactory.createLoweredBevelBorder());
    status(" ",false);
    
    historyFrame = new JFrame("Status message history");
    messageField = new JTextArea(40,132);
    messageField.setFont( new Font("Monospaced", Font.PLAIN, 11) );
    historyFrame.getContentPane().add( new JScrollPane(messageField) );
    historyFrame.pack();
//    RefineryUtilities.centerFrameOnScreen(historyFrame);
    addMouseListener(this);
    statusBarLabel.addMouseListener(this);
    statusBarLabel.setToolTipText(ToolTip);
    this.setToolTipText(ToolTip);
  }
  
  private void status(String line, boolean clearDown)
  {
    statusBarLabel.setText(line);
    if( messageField != null )
    {
      messageField.append(line+"\n");
      if( messageField.getText().length() > 65536 )
      {
        //Chop it down so we don't use to much memory
        messageField.replaceRange("", 0, 32768);
      }
    }
    if( clearDown )
    {
      if( t == null )
      {
        t = new Timer(3000,this);
      }
      t.restart();
    }
  }
  
  public void println(String line)
  {
    status(line, true);
  }
  
  public void println_persistent(String line)
  {
    status(line, false);
  }
  
  //Called to clear down the last message
  public void actionPerformed(ActionEvent e) 
  {
    //Clear status text, keep space as this ensures that the vertical size of the status bar does not change.
    statusBarLabel.setText(" ");
  }
  
  public void mouseClicked(MouseEvent e) 
  {
    //If double click 
    if( e.getClickCount() == 2 )
    {
      //Display the history frame
      historyFrame.setVisible(true);
    }
  }
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
 
}

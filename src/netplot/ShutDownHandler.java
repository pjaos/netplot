package netplot;

import java.awt.Point;
import java.awt.Dimension;
import java.io.IOException;

public class ShutDownHandler extends Thread {
	PlotFrame plotFrame;
	
	/**
	 * Handle the NetPlotter shutdown.
	 * This involves saving the size and position of the frame persistently.
	 * 
	 * @param plotFrame
	 */
	public ShutDownHandler( PlotFrame plotFrame ) {
		this.plotFrame=plotFrame;
	}
	
	/**
	 * Called when the program shuts down
	 */
	public void run()
	{
		Point location = plotFrame.getLocation();
		Dimension size = plotFrame.getSize();
		NetPlotter.PersistentConfig.guiHeight = size.height;
		NetPlotter.PersistentConfig.guiWidth = size.width;
		NetPlotter.PersistentConfig.guiXPos = location.x;
		NetPlotter.PersistentConfig.guiYPos = location.y;
		try {
			NetPlotter.PersistentConfig.save(NetPlotter.NetPlotterPersistentConfigFile);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		catch(IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}

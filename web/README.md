# Netplot Web interface
Up to the advent of the Web interface the Java application had to be running
that display a window on which the plots appeared.

This is not required for the web interface as the netplot_server.py file must be running instead. Once running servers listen on TCP/IP ports 9600 - 9699 for connections from netplot clients.

When a netplot client sends plots to netplot_server.py, netplot_server.py  
will save each plot trace as a separate JSON file in the local folder.

In order to display the plots via a web interface a web server must be running 
and use the location where the above JSON files sit (same folder where 
netplot_server.py sits) as it's root folder. 

Once the web server is running a user can connect to it using a browser and
the graphs that the netplot client sent will be displayed as the folder contains an index.html file. If the user selects the legends.html then a list of trace legends is displayed that relates each plot ID.

A plot ID can be found by hovering a mouse over a trace in the browser. The format of this is XX_YYY

- XX The plot area number. Top left = 00, the one to the right of it = 01 and so on until the bottom right plot area is reached.

- YYY The plot trace number. A number that details the trace number of the plot.

If the user select the legends.html a page is displayed that shows how the above plot ID releates to the plot name as defined by the netplot client.

** Currently only XY graphs are supported on the web interface**


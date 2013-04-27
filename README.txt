== netplot ==

What is it

Netplot is a tool for plotting values on a GUI from platforms with no GUI interface
over a network connection. It was designed as tool that is useful to incorporate into 
programs (particularly aimed at embedded Linux systems) when attempting to visualise 
data when debugging or attmpting to visualise internal state.


How do I build it

Java netplot gui and client code
Run the command 'ant clean build jar' command (in the same path as the build.xml file, to 
compile the Java code and build the output jar files. Once the build is complete the 
netplot_gui.jar and netplot_client_demo.jar files should appear alongside the build.xml
file. This requires that Java (SDK) and ant is installed.

Python netplot client.
The python_client folder contains the python client. This file may be executed directly
('python netplot_client_demo.py' command) to display the demo graphs. This requires that Python is
installed.

C client code
This resides in the c_client folder. From this folder run the 'codeblocks netplot_client.cbp'
and select the Build option from the Build menu. This requires that codeblocks is installed.


How do I run it

- Run 'java -jar netplot_gui.jar' on the computer with a GUI interface and a java JVM
  installed. This will display an empty window and start a number of TCP servers on 
  separate TCP sockets. Each TCP socket can be used to display a single plot on the 
  GUI interface. When a netplot client connects to the server it can display plots
  on the GUI.
  
- Run the 'java -jar netplot_client_demo.jar' command on the same computer (it can be 
  executed on a different computer if the --host command line argument is used) to
  display the demo plots.
  
- To run the python netplot client demo code start the netplot_gui as detailed above then 
  run 'python netplot_client_demo.py' from the python_client folder.
  
- To run the C python netplot client demo code, build the code as detailed previously 
  the run the netplot_client program from the c_client/bin/Debug folder.
  
The netplot client in the above example is Java based. A python and C implementation of the 
netplot clients (and demo program code) are included for adding netplot capabilities into
software that use these languages.


You can now take the client code (which is fairly small) and build it into your
programs in order to add netplot support to them.

Licensing
netplot is distributed under the terms of the GNU Lesser General Public License


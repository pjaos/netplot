# Netplot Serial Debug Tool
Numbers presented on a serial port can be plotted directly using this tool.
This allows the user to see the patterns contained in the data more easily
and so debug hardware issues on bringup and for bug fixing.

## Building python package
The pipenv2deb package must be installed prior to attempting to build the package. See https://github.com/pjaos/pipenv2deb
for information on this.

Run the following command to build the debian package
```
sudo pipenv2deb
```

The packages folder will now contain the debian installer.
To install this file run the following command.
```
sudo dpkg -i packages/python-netplot-spd-2.0-all.deb
```

## Using the Netplot serial debug tool
If on the serial porty the following text is being received
```
adc=12
adc=24
adc=23
```

Then run netplot (run 'netplot' command with netplot installed) on the 
local machine to open the plot GUI.

The run the following command
```
netplot_spd --port /dev/ttyUSB1 --baud 115200 --text adc=
INFO:  Netplot Serial port debugger (V2.0)
INFO:  Open serial port: /dev/ttyUSB1
INFO:  Opened serial port
INFO:  Connecting to netplot server (127.0.0.1:9600)
INFO:  Connected
INFO:  PLOT: [12]
INFO:  PLOT: [24]
INFO:  PLOT: [23]
```

And the values will be plottted in the netplot GUI.
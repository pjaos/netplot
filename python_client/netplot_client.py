#!/usr/bin/python

#/*****************************************************************************************
#*                             Copyright 2009 Paul Austen                                *
#*                                                                                       *
#* This program is distributed under the terms of the GNU Lesser General Public License  *
#*****************************************************************************************/

import socket
import types
import types

VALID_PLOT_TYPES=['time','bar','xy', 'dial']

PLOT_TITLE		= "plot_title"
PLOT_NAME		= "plot_name"
X_AXIS_NAME		= "x_axis_name"
Y_AXIS_NAME		= "y_axis_name"
ENABLE_LINES		= "enable_lines"
ENABLE_SHAPES		= "enable_shapes"
ENABLE_AUTOSCALE	= "enable_autoscale"
MIN_SCALE_VALUE		= "min_scale_value"
MAX_SCALE_VALUE		= "max_scale_value"
MAX_AGE_SECONDS		= "max_age_seconds"
ENABLE_LOG_Y_AXIS	= "enable_log_y_axis"
ENABLE_ZERO_ON_X_SCALE  = "enable_zero_on_x_scale"
ENABLE_ZERO_ON_Y_SCALE  = "enable_zero_on_y_scale"
FRAME_TITLE		= "frame_title"
ENABLE_LEGEND		= "enable_legend"
TICK_COUNT 		= "tick_count"

class NetPlotError(Exception):
  pass
  
class PlotConfig:
  def __init__(self):
    self.plotName=""
    self.xAxisName=""
    self.yAxisName=""
    self.enableLines=1
    self.enableShapes=1
    self.enableAutoScale=1
    self.minScaleValue=0
    self.maxScaleValue=1E6
    self.maxAgeSeconds=3600
    self.enableLogYAxis=0
    self.enableZeroOnXAxis=1
    self.enableZeroOnYAxis=1
    self.tickCount=0
    
class NetPlot:
    
  TIMESTAMP_DELIM=";"

  def __init__(self,debug=0):
    #If debug set on then debug messages will be shown on stdout
    self.__debug=debug
    self.__hostAddress=None
    self.__port=None
    self.__serverVersion=None
    self.__cacheEnabled=False   #In cached plot mode the commands to add plot points to a graph or graphs 
                                #are cached until the update() method is called. This means that all plot
                                #points are sent to the server together. When not in fast mode (default), 
                                #each time addPlotValues or addXYPlotValues is called then data is sent 
                                #to the server to add these plot points.
    self.__plotValueCache=[]    
    self.sock = None


  def __debugPrint(self, message):
    """Display debug messages if required"""
    if self.__debug:
      print "DEBUG: %s" % (message)

  def connect(self, hostAddress, port):
    """Connect to the server running the netplot GUI (Java) program"""
    self.__hostAddress=hostAddress
    self.__port=port
    try:
        self.__debugPrint('Connecting to %s:%d' % (self.__hostAddress, self.__port) )
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.connect((self.__hostAddress, self.__port))
    except socket.error:
        raise socket.error("netplot connect failed to %s:%d" % (self.__hostAddress, self.__port) )
        
    self.__debugPrint('Connected to %s:%d' % (self.__hostAddress, self.__port) )
    #Wait for initial connection message
    rxData = self.sock.recv(256)
    self.__debugPrint("rxData=%s" % (rxData))
    netPlotServer=1
    elems=rxData.split('=')
    if rxData.find('netplot_version=') != 0 or len(elems) != 2:
      netPlotServer=0
    if netPlotServer:
      try:
        self.__serverVersion=float(elems[1])
      except ValueError:
        netPlotServer=0
    if netPlotServer == 0:
      raise NetPlotError("%s:%d is not a netplot server. Received %s" % (self.__hostAddress, self.__port, rxData) )
    self.sock.setblocking(0)
    
  def disconnect(self):
    """Close a connection to a netplot server"""
    if self.sock != None:
      self.sock.close()
      
  def getServerVersion(self):
    return self.__serverVersion
    
  def sendCmd(self, cmd):
    """Send a command to the netplot server"""
    self.__debugPrint('CMD: %s' % (cmd) )
    self.sock.send('%s\n' % (cmd) )
    #Wait for response
    while 1:
	    try:
	    	rxData = self.sock.recv(256)
	    	self.__debugPrint("rxData=%s" % (rxData))
	    	if rxData.find('OK') == 0:
	    		break
	    	elif rxData.find('ERROR: ') == 0:
	    	  raise NetPlotError(rxData)
	    except socket.error:
	      pass
    
  def setGrid(self, rows, columns):
    """Set the number of graphs and their layout"""
    self.sendCmd("set grid=%d,%d" % (rows, columns) )

  def setWindowTitle(self, windowTitle):
    """Set the frame title"""
    self.sendCmd("set %s=%s" % (FRAME_TITLE,windowTitle) )
    
  def setChartLegendEnabled(self, enabled):
    """enable/disable legends on the chart"""
    varValue="false"
    if enabled:
      varValue="true"
    self.sendCmd("set %s=%s" % (ENABLE_LEGEND,varValue) )
    
  def init(self):
    """Initialise the plot. May be called to clear a plot"""
    self.sendCmd("init")

  def setPlotType(self, plotType, title=None):
    if plotType not in VALID_PLOT_TYPES:
      raise NetPlotError("%s is an invalid plot type" % (plotType) )
    self.sendCmd("set graph=%s" % (plotType) )
    if title != None:
      self.sendCmd("set %s=%s" % (PLOT_TITLE,title) )
    self.init()
    #By default we enable status messages
    self.enableStatusMessages(True)
    
  def addPlot(self, plotConfig=None):
    if plotConfig != None:
      if plotConfig.plotName != None:
      	self.sendCmd("set %s=%s" % (PLOT_NAME,plotConfig.plotName) )
      if plotConfig.xAxisName != None:
        self.sendCmd("set %s=%s" % (X_AXIS_NAME,plotConfig.xAxisName) )
      if plotConfig.yAxisName != None:
        self.sendCmd("set %s=%s" % (Y_AXIS_NAME,plotConfig.yAxisName) )
      if plotConfig.enableLines != None:
        varValue="false"
        if plotConfig.enableLines:
          varValue="true"
        self.sendCmd("set %s=%s" % (ENABLE_LINES,varValue) )
      if plotConfig.enableShapes != None:
        varValue="false"
        if plotConfig.enableShapes:
          varValue="true"
        self.sendCmd("set %s=%s" % (ENABLE_SHAPES,varValue) )
      if plotConfig.enableAutoScale != None:
        varValue="false"
        if plotConfig.enableAutoScale:
          varValue="true"
        self.sendCmd("set %s=%s" % (ENABLE_AUTOSCALE,varValue) )
      if plotConfig.minScaleValue != None:
        self.sendCmd("set %s=%s" % (MIN_SCALE_VALUE,str(plotConfig.minScaleValue)) )
      if plotConfig.maxScaleValue != None:
        self.sendCmd("set %s=%s" % (MAX_SCALE_VALUE,str(plotConfig.maxScaleValue)) )
      if plotConfig.maxAgeSeconds != None:
        self.sendCmd("set %s=%s" % (MAX_AGE_SECONDS,str(plotConfig.maxAgeSeconds)) )
      if plotConfig.enableLogYAxis != None:
        varValue="false"
        if plotConfig.enableLogYAxis:
          varValue="true"
        self.sendCmd("set %s=%s" % (ENABLE_LOG_Y_AXIS,varValue) )
      if plotConfig.enableZeroOnXAxis != None:
        varValue="false"
        if plotConfig.enableZeroOnXAxis:
          varValue="true"
        self.sendCmd("set %s=%s" % (ENABLE_ZERO_ON_X_SCALE,varValue) )
      if plotConfig.enableZeroOnYAxis != None:
        varValue="false"
        if plotConfig.enableZeroOnYAxis:
          varValue="true"
        self.sendCmd("set %s=%s" % (ENABLE_ZERO_ON_Y_SCALE,varValue) )
      if plotConfig.tickCount != None:
        self.sendCmd("set %s=%s" % (TICK_COUNT,str(plotConfig.tickCount)) )  	       		
    self.sendCmd("add_plot")

  def __getValue(self, value):
    if type(value) == types.StringType:
      return value
    elif type(value) == types.LongType:
      return "%d" % (value)
    elif type(value) == types.IntType:
      return "%d" % (value)
    elif type(value) == types.FloatType:
      return "%f" % (value)
    else:
      self.__debugPrint("%s is of an unsupported type (%s), cannot plot" % (repr(value), repr(type(value)))) 

  def _getDateTimeString(self, dateTimeObj):
    """Get the date time string in the required format to plot time series plots."""   
    return ""+str(dateTimeObj.year)+NetPlot.TIMESTAMP_DELIM+\
              str(dateTimeObj.month)+NetPlot.TIMESTAMP_DELIM+\
              str(dateTimeObj.day)+NetPlot.TIMESTAMP_DELIM+\
              str(dateTimeObj.hour)+NetPlot.TIMESTAMP_DELIM+\
              str(dateTimeObj.minute)+NetPlot.TIMESTAMP_DELIM+\
              str(dateTimeObj.second)+NetPlot.TIMESTAMP_DELIM+\
              str(dateTimeObj.microsecond/1000)
    
    
  def addPlotValues(self, values):
    """Add the plot values

       values must be a list of values that contains one element for each 
       plot added. The number of plots is determined by the number of 
       times addPlot has been called. Therefore the first element in 
       the list is added to the first plot, second to the seconds and so on. 
    """ 
    self.__debugPrint('Adding plot values: %s' % (repr(values))  )
    cmdString=""
    firstValue=1
    for value in values:
        if firstValue:
          cmdString=self.__getValue(value)
        else:
          cmdString="%s,%s" % (cmdString,self.__getValue(value))
        firstValue=0
        if self.__cacheEnabled:
          self.__plotValueCache.append(cmdString)
          self.updateIfRequired()
        else:
          self.sendCmd(cmdString)
        
  def updateIfRequired(self):
      """Call a plot update if we need to do so because there are a lot of plot points outstanding."""
      if self.__cacheEnabled:
          #Ensure we don't have more than 50 plot points outstanding
          if len(self.__plotValueCache) > 50:
              self.update()

  def addTimePlotValue(self, plotIndex, dateTimeObj, plotValue):
    """The recommended way to send time series plot values where the time is passed from the 
       netplot client to the netplot server (GUI)."""
    timeStampStr = self._getDateTimeString(dateTimeObj)
    cmdString="%d:%s:%s" % (plotIndex,timeStampStr, self.__getValue(plotValue))
    if self.__cacheEnabled:
      self.__plotValueCache.append(cmdString)
      self.updateIfRequired()
      
    else:
      self.sendCmd(cmdString)
    
  def addXYPlotValues(self, plotIndex, xValue, yValue):
    """Add the XY plot values
    """ 
    self.__debugPrint('Adding XY plot values: %f:%f' % (xValue, yValue)  )
    msg="%d:%E:%E" % (plotIndex,xValue,yValue)
    if self.__cacheEnabled:
      self.__plotValueCache.append(msg)
      self.updateIfRequired()
    else:
      self.sendCmd(msg)

  def clear(self, plotIndex):
    """Clear the plot referenced by plotIndex"""
    self.__debugPrint('Clearing plot %d' % (plotIndex)  )
    self.sendCmd("clear %d" % (plotIndex) )
    
  def replot(self, plotIndex):
    """replot causes subsequent values to overwrite previous ones.
       This is more efficient than clear and stops screen flicker
       when all plot points are erased.
    """
    self.__debugPrint('Clearing plot %d' % (plotIndex)  )
    self.sendCmd("replot %d" % (plotIndex) )

  def enableStatusMessages(self, enabled):
    """Enable/disable the status messages on the display window"""
    if enabled:
      self.sendCmd("enable_status 1")
    else:	
      self.sendCmd("enable_status 0")
      
  def enableCache(self, enabled):
    """Enable/Disable the plot cache"""
    self.__cacheEnabled=enabled
	
  def update(self):
    """Send all plotValueCache plot points.
       Only call this when__cacheEnabled is True"""
    cmdCount=len(self.__plotValueCache)
    #If there is nothing to send
    if cmdCount == 0:
    	#quit
      return
      
    #Build a single string containing all the commands
    cmd=""
    for plotValue in self.__plotValueCache:
      cmd="%s%s\n" % (cmd,plotValue)
    #empty the cache
    self.__plotValueCache = []
      
    self.__debugPrint('CMD: %s' % (cmd) )
    self.sock.send('%s\n' % (cmd) )
    #Wait for responses
    rxCmdCount=0
    while rxCmdCount < cmdCount:
      try:
        rxData = self.sock.recv(65536)
        rxCmdCount = rxCmdCount + self.__processResponse(rxData)
      except socket.error:
        pass
       
  def __processResponse(self, rxData):
    """Process the data received from the netplot server
    	Return: The number of ok responses received
    	Throws NetPlotError if the server returns an error
    """
    if rxData == None:
    	raise NetPlotError("rxData from server == None")
    	
    if len(rxData) == 0:
      raise NetPlotError("No rxData received from netplot server")
    
    offset=0
    #Process all responses received in the rxData
    while offset < len(rxData):
      if rxData[offset:].find('OK') == 0:
        offset=offset+2
        
      elif rxData[offset:].find('ERROR: ') == 0:
	      raise NetPlotError(rxData)
	      
      if rxData[offset] == '\n' or\
         rxData[offset] == '\r':
        offset=offset+1
      
    #Return the number of OK responses received
    return offset/2
     

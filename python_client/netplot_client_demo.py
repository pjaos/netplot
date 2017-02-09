#!/usr/bin/python

#/*****************************************************************************************
#*                             Copyright 2009 Paul Austen                                *
#*                                                                                       *
#* This program is distributed under the terms of the GNU Lesser General Public License  *
#*****************************************************************************************/

# This file contains example code of how netplot may be used
    
import time
from datetime import datetime, timedelta
import random
from math import sin, cos, tan
from netplot_client import PlotConfig, NetPlot, NetPlotError

def timeExample1(netPlot, plotNumber):
  """Single plot on a time series chart"""
  plotConfig = PlotConfig()
  plotConfig.plotName="Plot 0"
  plotConfig.xAxisName="The X axis"
  plotConfig.yAxisName="The Y axis (Plot0)"
  plotConfig.enableLines=1
  plotConfig.enableShapes=1
  plotConfig.enableAutoScale=0
  plotConfig.minScaleValue=0
  plotConfig.maxScaleValue=10000
  plotConfig.maxAgeSeconds=5  
  plotConfig.tickCount=1000
  
  netPlot.setPlotType('time', title="%d: TIME chart, single plot" % (plotNumber) )
  #Uncomment this to remove the Legend on the chart
  #netPlot.setChartLegendEnabled(0)
  netPlot.addPlot(plotConfig)
  i=0
  while i< 10:
    netPlot.addPlotValues([random.randint(1000,10000)])
    i=i+1
      
def timeExample2(netPlot, plotNumber, timePeriod=0.01):
  """Three plots on a time series chart with the same Y axis."""
  plotConfig = PlotConfig()
  plotConfig.xAxisName="The X axis"
  plotConfig.yAxisName="The Y axis (Plot0)"
  plotConfig.enableLines=1
  plotConfig.enableShapes=1
  plotConfig.enableAutoScale=0
  plotConfig.minScaleValue=-5
  plotConfig.maxScaleValue=5
  
  netPlot.setPlotType('time', title="%d: TIME chart, multiple plots, same Y scale" % (plotNumber) )
  #Uncomment this to remove the Legend on the chart
  #netPlot.setChartLegendEnabled(0)
  
  for plotName in ["Sine","Cosine","Tangent"]:
    plotConfig.plotName=plotName
    netPlot.addPlot(plotConfig)
    #We only want one Y axis so null plot axis names
    plotConfig.xAxisName=""
    plotConfig.yAxisName=""
  
  x=0.01
  while x< 25:
    netPlot.addPlotValues([sin(x), cos(x), tan(x)])
    x=x+0.1
    time.sleep(timePeriod)

def timeExample3(netPlot, plotNumber):
  """Two plots on a time series chart with different Y axis"""
  plotConfig = PlotConfig()
  plotConfig.plotName="Plot 0"
  plotConfig.xAxisName="The X axis"
  plotConfig.yAxisName="Y axis (Plot0)"
  plotConfig.enableLines=1
  plotConfig.enableShapes=1
  plotConfig.enableAutoScale=1
  #plotConfig.minScaleValue=0
  #plotConfig.maxScaleValue=10000
  plotConfig.maxAgeSeconds=5
  netPlot.setPlotType('time', title="%d: TIME chart, two traces with different linear Y scales, both autoscaled" % (plotNumber) )
  netPlot.addPlot(plotConfig)  
  plotConfig.plotName="Plot 1"
  #Add a new Y Axis
  plotConfig.yAxisName="Y Axis (Plot1)"
  netPlot.addPlot(plotConfig)  
  i=0
  while i<10:
    netPlot.addPlotValues([random.randint(1000,10000),random.randint(10,100)])
    i=i+1

def timeExample4(netPlot, plotNumber):
  """Time series chart passing the time (from Netplot.GetTimeNowString()) and the y value"""
  plotConfig = PlotConfig()
  plotConfig.plotName="Plot 0"
  plotConfig.xAxisName="The X axis"
  plotConfig.yAxisName="Y axis (Plot0)"
  plotConfig.enableLines=1
  plotConfig.enableShapes=1
  plotConfig.enableAutoScale=1
  plotConfig.maxAgeSeconds=5
  netPlot.setPlotType('time', title="%d: TIME chart using Netplot.GetTimeNowString()." % (plotNumber) )
  netPlot.addPlot(plotConfig)  
  plotConfig.plotName="Plot 1"
  #Add a new Y Axis
  plotConfig.yAxisName="Y Axis (Plot1)"
  netPlot.addPlot(plotConfig)  
  
  #Update first plot
  i=0
  while i<10:    
    netPlot.addTimePlotValue(0, NetPlot.GetTimeNowString(), float(1.395*random.randint(0,10) ) )
    i=i+1
    time.sleep(0.1)

  netPlot.update()
  
def timeExample5(netPlot, plotNumber):
  """Time series chart passing the time and the y value"""
  plotConfig = PlotConfig()
  plotConfig.plotName="Plot 0"
  plotConfig.xAxisName="The X axis"
  plotConfig.yAxisName="Y axis (Plot0)"
  plotConfig.enableLines=1
  plotConfig.enableShapes=1
  plotConfig.enableAutoScale=1
  plotConfig.maxAgeSeconds=5
  netPlot.setPlotType('time', title="%d: TIME chart, passing the time and the y value (cached)." % (plotNumber) )
  netPlot.addPlot(plotConfig)  
  plotConfig.plotName="Plot 1"
  #Add a new Y Axis
  plotConfig.yAxisName="Y Axis (Plot1)"
  netPlot.addPlot(plotConfig)  
  #We enable cache on this plot. Therefore 
  #data won't be sent until the netPlot.update() method is called.
  netPlot.enableCache(True)


  t1 = datetime.now()
  
  #Update first plot
  i=0
  while i<10:    
    netPlot.addTimePlotValue(0, t1, float(1.395*random.randint(0,10) ) )
    t1=t1+timedelta(seconds=60*60*24*365)
    i=i+1
                             
  t2 = datetime.now()
  t2=t2+timedelta(seconds=10000)
  #Update second plot
  i=0
  while i<10:    
    netPlot.addTimePlotValue(1, t2, float(1.395*random.randint(0,10) ) )
    t2=t2+timedelta(seconds=60*60*24*365)
    i=i+1
        
  netPlot.update()

    
def barExample(netPlot, plotNumber):
  """Bar chart example"""
  plotConfig0 = PlotConfig()
  plotConfig0.plotName="Plot 0"
  plotConfig0.xAxisName="The X axis"
  plotConfig0.yAxisName="The Y axis"
  plotConfig0.enableAutoScale=1
  #plotConfig0.minScaleValue=1
  #plotConfig0.maxScaleValue=10000
  #log axis not currently supported on Bar charts    
  netPlot.setPlotType('bar', title="%d: BAR chart" % (plotNumber))
  netPlot.addPlot(plotConfig0)
  i=0
  while i<10:
    netPlot.addPlotValues([random.randint(1000,10000)])
    i=i+1

def xyExample1(netPlot, plotNumber):
  """two plots with different linear Y scales"""
  plotConfig0 = PlotConfig()
  plotConfig0.plotName="Plot 0"
  plotConfig0.xAxisName="The X axis name"
  plotConfig0.yAxisName="Y axis (Plot 0)"
  plotConfig0.enableLines=1
  plotConfig0.enableShapes=1
  plotConfig0.enableAutoScale=1
  plotConfig0.enableZeroOnXAxis=0
  plotConfig0.enableZeroOnYAxis=0
  netPlot.setPlotType('xy', title="%d: XY chart, two traces with different linear Y scales, both autoscaled" % (plotNumber) )     
  netPlot.addPlot(plotConfig0)   
  
  plotConfig1 = PlotConfig()
  plotConfig1.plotName="Plot 1"
  plotConfig1.yAxisName="Y axis (Plot 1)"
  plotConfig1.enableLines=1
  plotConfig1.enableShapes=1
  plotConfig1.enableAutoScale=1
  plotConfig1.enableZeroOnXAxis=0
  plotConfig1.enableZeroOnYAxis=0
  netPlot.addPlot(plotConfig1)
  i=0
  while i<10:
    netPlot.addXYPlotValues(0,random.randint(-90,-70), random.randint(130,150))
    netPlot.addXYPlotValues(1,random.randint(-60,-50), random.randint(75,80))
    i=i+1
  
def xyExample2(netPlot, plotNumber):
  """XY plot with log Y scale"""
  plotConfig0 = PlotConfig()
  plotConfig0.plotName="Plot 0"
  plotConfig0.xAxisName="The X axis name"
  plotConfig0.yAxisName="Log Y axis"
  plotConfig0.enableLines=1
  plotConfig0.enableShapes=1
  plotConfig0.enableLogYAxis=1
  plotConfig0.minScaleValue=1E-10
  plotConfig0.maxScaleValue=1E-2
  netPlot.setPlotType('xy', title="%d: XY chart with log Y scale" % (plotNumber) )
  netPlot.addPlot(plotConfig0)
  netPlot.addXYPlotValues(0,-50, 1E-9)
  netPlot.addXYPlotValues(0,-55, 1E-7)
  netPlot.addXYPlotValues(0,-60, 1E-6)
  netPlot.addXYPlotValues(0,-70, 1E-5)
  netPlot.addXYPlotValues(0,-80, 1E-4)
  netPlot.addXYPlotValues(0,-90, 1E-3)

def xyExample3(netPlot, plotNumber):
  """XY chart with 2 lin and 2 log Y scales"""
  plotConfig0 = PlotConfig()
  plotConfig0.plotName="Plot 0"
  plotConfig0.xAxisName="The X axis name"
  plotConfig0.yAxisName="Log Y axis (Plot 0)"
  plotConfig0.enableLines=1
  plotConfig0.enableShapes=1
  plotConfig0.enableLogYAxis=1
  plotConfig0.minScaleValue=1E-10
  plotConfig0.maxScaleValue=1E-2
  netPlot.setPlotType('xy', title="%d: XY chart with 2 lin and 2 log Y scales" % (plotNumber) )
  netPlot.addPlot(plotConfig0)  
  
  plotConfig1 = PlotConfig()
  plotConfig1.plotName="Plot 1"
  plotConfig1.yAxisName="Y axis (Plot 1)"
  plotConfig1.enableLines=1
  plotConfig1.enableShapes=1
  plotConfig1.enableAutoScale=1
  plotConfig1.enableZeroOnXAxis=0
  plotConfig1.enableZeroOnYAxis=0
  netPlot.addPlot(plotConfig1)

  plotConfig2 = PlotConfig()
  plotConfig2.plotName="Plot 2"
  plotConfig2.yAxisName="Y axis (Plot 2)"
  plotConfig2.enableLines=1
  plotConfig2.enableShapes=1
  plotConfig2.enableAutoScale=1
  plotConfig2.enableZeroOnXAxis=0
  plotConfig2.enableZeroOnYAxis=0
  netPlot.addPlot(plotConfig2)
  

  plotConfig3 = PlotConfig()
  plotConfig3.plotName="Plot 3"
  plotConfig3.yAxisName="Log Y axis (Plot 3)"
  plotConfig3.enableLines=1
  plotConfig3.enableShapes=1
  plotConfig3.enableLogYAxis=1
  plotConfig3.minScaleValue=1E-10
  plotConfig3.maxScaleValue=1E-2
  netPlot.addPlot(plotConfig3)
  
  netPlot.addXYPlotValues(0,-50, 1E-9)
  netPlot.addXYPlotValues(0,-55, 1E-7)
  netPlot.addXYPlotValues(0,-60, 1E-6)
  netPlot.addXYPlotValues(0,-70, 1E-5)
  netPlot.addXYPlotValues(0,-80, 1E-4)
  netPlot.addXYPlotValues(0,-90, 1E-3)
  netPlot.addXYPlotValues(1,-10, 10)
  netPlot.addXYPlotValues(1,-9, 12)
  netPlot.addXYPlotValues(1,-8, 14)
  netPlot.addXYPlotValues(1,-7, 16)
  netPlot.addXYPlotValues(1,-6, 18)
  netPlot.addXYPlotValues(1,-5, 20)
  
  netPlot.addXYPlotValues(2,-35, 10)
  netPlot.addXYPlotValues(2,-95, 12)
  netPlot.addXYPlotValues(2,-85, 14)
  netPlot.addXYPlotValues(2,-75, 16)
  netPlot.addXYPlotValues(2,-65, 18)
  netPlot.addXYPlotValues(2,-55, 20)

  netPlot.addXYPlotValues(3,1, 1E-9)
  netPlot.addXYPlotValues(3,2, 1E-7)
  netPlot.addXYPlotValues(3,3, 1E-6)
  netPlot.addXYPlotValues(3,4, 1E-5)
  netPlot.addXYPlotValues(3,5, 1E-4)
  netPlot.addXYPlotValues(3,6, 1E-3)

def showDialExample(netPlot, plotNumber):
	
  netPlot.setPlotType('dial', title="%d: Number and MAX" % (plotNumber))

  plotConfig = PlotConfig()
  plotConfig.plotName="Number"
  plotConfig.minScaleValue=0
  plotConfig.maxScaleValue=200
  plotConfig.tickCount=10
   
  netPlot.addPlot(plotConfig)
  plotConfig.plotName="MAX"
  netPlot.addPlot(plotConfig)
   
  maxV=0
  value=0
  while value < 200:
    value=value+random.randint(-10,20)
    if value > 200:
      value=200
    elif value < 0:
      value=0
    if maxV < value:
      maxV=value
    netPlot.addPlotValues([value,maxV])
    time.sleep(0.1)
    
    
def cachedPlot(netPlot, plotNumber):
  """XY plot with log Y scale but in this case we send the data for all the
     points to be plotted and then use the update command to plot them all
     quickly."""
  plotConfig0 = PlotConfig()
  plotConfig0.plotName="Plot 0"
  plotConfig0.xAxisName="The X axis name"
  plotConfig0.yAxisName="Log Y axis"
  plotConfig0.enableLines=1
  plotConfig0.enableShapes=0
  plotConfig0.enableAutoScale=False
  plotConfig0.minScaleValue=0
  plotConfig0.maxScaleValue=1
 
  netPlot.setPlotType('xy', title="%d: Cached, fast XY chart with log Y scale" % (plotNumber) ) 
  netPlot.addPlot(plotConfig0)
  
  #These must be called after the plot type is added
  #Enables cached operation, update() will draw all plot points
  netPlot.enableCache(True)
  #Not printing status messages may speed up plotting if CPU bound
  netPlot.enableStatusMessages(False)
  
  while True:
    netPlot.replot(0)
    for v in range(1,100):
      netPlot.addXYPlotValues(0,v, random.random())
    #Update. plot values will be sent now as the NetPlot cache is enabled
    netPlot.update()  
    
  return  

def showAllExamples(address, initWindow, debug=0, port=9600):
  """Show all the example plots"""
  
  plotList = [1,2,3,4,5,6,7,8,9,10,11]
  
  netPlotList = []
  for plotNumber in plotList:
      netPlot = NetPlot(debug=False)          
      netPlot.connect(address, port)
      
      if len(netPlotList) == 0:
          
          if initWindow:
            print 'Netplot server version = %f' % (netPlot.getServerVersion())
            #Set these non plot or chart specific parameters on first connection
            netPlot.setGrid(4,4)
            netPlot.setWindowTitle("Python netplot client demo")
      
      netPlotList.append(netPlot)
      
      if plotNumber == 1:

          timeExample1(netPlot, plotNumber)
          
      elif plotNumber == 2:
          
          timeExample2(netPlot, plotNumber)
          
      elif plotNumber == 3:
      
          timeExample3(netPlot, plotNumber)
          
      elif plotNumber == 4:
      
          timeExample4(netPlot, plotNumber)
                
      elif plotNumber == 5:
      
          timeExample5(netPlot, plotNumber)
                      
      elif plotNumber == 6:

          barExample(netPlot, plotNumber)

      elif plotNumber == 7:
          
          xyExample1(netPlot, plotNumber)
          
      elif plotNumber == 8:
      
          xyExample2(netPlot, plotNumber)
          
      elif plotNumber == 9:
          
          xyExample3(netPlot, plotNumber)
          
      elif plotNumber == 10:
      
          showDialExample(netPlot, plotNumber)
          
      elif plotNumber == 11:
      
          cachedPlot(netPlot, plotNumber)
          
      port = port+1
      
  for netPlot in netPlotList:
    netPlot.disconnect()
    
def usage():
  """show program usage options"""
  print '              : If no options are provided then an attempt to connect to the netplot server'
  print '                on localhost will be made. Then an attempt to display the example plots will'
  print '                be made.'
  print '--host        : Followed by the netplot server host address (default=1270.0.1)'
  print '-h, --help    : Show this help text'
  
if __name__== '__main__':
  """
     Create high quality GUI (multiplatform) graphs with as little coding as possible.
     
     netplot relies on the Java netplot server software to plot the GUI's. The server
     software should be running (execute java -jar netplot.jar) before running the
     netplot client software.
  
     NetPlot requires that you run the server application on a machine that is reachable 
     over an IP network from the machine using the python netplot module. This server will
     listen on TCP/IP ports and display the data sent to it. Each port provides a connection
     to a different plot.
     Therefore you must start the netplot server (execute java -jar netplot.jar) on the local 
     or remote machine before attempting to send plot information to it.

     Normally this python module will be imported into your application to provide the plots 
     you define. 
     The main entry point here allows some example plots to be displayed.
  """
  import sys

  address='127.0.0.1'
  
  readHostAddress=0
  showExamples=1
  arg0=1
  for arg in sys.argv:
    if arg0:
      arg0=0
      continue
    if readHostAddress:
      address=arg
      readHostAddress=0
      continue
      
    if arg == '-h' or arg == '--help':
      usage()
      sys.exit(0) 
    elif arg == '--host':
      readHostAddress=1
    else:
      usage()
      print '\n!!! Unknown command line option (%s)' % (arg)
      sys.exit(-1)
      
  if showExamples:
    showAllExamples(address, True)
  
    
  	

  

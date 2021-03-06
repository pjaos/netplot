#!/usr/bin/env python

import  sys
from    optparse import OptionParser
from    time import sleep

import  serial

from    spd_libs.netplot_client import PlotConfig, NetPlot, DEFAULT_NETPLOT_PORT

VERSION = 2.2

class SerialDebuggerError(Exception):
    pass

class UO(object):
    """@brief responsible for user viewable output"""
    
    def __init__(self, debug=False):
        self._debug = debug
    
    def info(self, text):
        print( 'INFO:  ' + str(text) )

    def debug(self, text):
        if self._debug:
            print( 'DEBUG: ' + str(text) )
        
    def warn(self, text):
        print( 'WARN:  ' + str(text) )

    def error(self, text):
        print( 'ERROR: ' + str(text) )


class Plotter(object):
    """@brief Responsible for handling the plotting of data via netplot"""

    TITLE = "Serial Debug Values"
    NextPlotID = 0

    def __init__(self, uo, options, plotID):
        self._uo = uo
        self._options = options
        self._plotID = plotID
        self._netPlot = None
        self.plotNameList = []
        self._netplotConnect()

    def _netplotConnect(self):
        """@brief Connect to the netplot server"""
        self._uo.info("Connecting to netplot server ({}:{})".format(self._options.np, DEFAULT_NETPLOT_PORT + self._plotID))
        self._netPlot = NetPlot()
        self._netPlot.connect(self._options.np, DEFAULT_NETPLOT_PORT + self._plotID)
        self._netPlot.setPlotType('time', title=Plotter.TITLE)
        self._uo.info("Connected")

    def addPlot(self, plotName):
        """@brief Add a plot to the plot pane
           @param plotName The name of the plot to add (added to the plot legend list)"""
        self.plotNameList.append(plotName)

        plotConfig = PlotConfig()
        plotConfig.plotName = plotName
        plotConfig.xAxisName = "Time"
        plotConfig.yAxisName = "{} Value".format(plotName)
        plotConfig.enableLines = 1
        plotConfig.enableShapes = 1
        plotConfig.enableAutoScale = 1

        if not self._netPlot:
            raise SerialDebuggerError("Cannot add plot as not yet connected to netplot server.")

        self._netPlot.addPlot(plotConfig)

    def isPlotAdded(self, plotName):
        """@brief Determine if the plot has allready been added to the list of plots
           @return True if the plot has been added, False if not."""
        if plotName in self.plotNameList:
            return True
        return False

    def plotValueList(self, valueList):
        """@brief Plot a list of values. The number of values must equal the number of plots 
                  (number of times addPlot() has been called"""
        self._netPlot.addPlotValues(valueList)


class SerialDebugger(object):
    """@brief Provide serial deug capabilities using netplot"""

    DECIMAL_DIGIT_LIST = ('-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    HEXADECIMAL_DIGIT_LIST = ('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    WHITESPACE_CHARS = ( " ", "\t" )

    def __init__(self, uo, options):
        self._uo = uo
        self._options = options

        self._init()

        self._serial = None
        self._captureTextList = None
        self._netPlot = None
        self._plotCount = 0
        self._plotterList = []
        self._plotOneGraphDict = {}

        self._init()

    def _init(self):
        """@brief Init the serial debugger object"""

        self._netPlot = None

        if not self._options.text:
            raise SerialDebuggerError("Please enter serial debug text to capture number from")

        self._captureTextList = self._options.text.split(",")

    def _openSerialPort(self):
        """@brief Open the serial port with the required parameters"""
        self._uo.info("Open serial port: {}".format(self._options.port))
        self._serial = serial.Serial(
            port=self._options.port,
            baudrate=115200,
            parity=serial.PARITY_NONE,
            stopbits=serial.STOPBITS_ONE,
            bytesize=serial.EIGHTBITS
        )
        self._uo.info("Opened serial port")

    def _plotValue(self, plotName, plotValue):
        """@brief Plot a single value"""
        foundPlotter = False

        if not self._options.one_graph:

            for plotter in self._plotterList:
                if plotter.plotNameList[0] == plotName:
                    plotter.plotValueList([plotValue])
                    foundPlotter = True
                    break

            if not foundPlotter:
                plotter = Plotter(self._uo, self._options, len(self._plotterList))
                plotter.addPlot(plotName)
                plotter.plotValueList([plotValue])
                self._plotterList.append(plotter)

        else:
            if len(self._plotterList) < 1:
                plotter = Plotter(self._uo, self._options, len(self._plotterList))
                self._plotterList.append(plotter)

            if not self._plotterList[0].isPlotAdded(plotName):
                self._plotterList[0].addPlot(plotName)

            self._plotOneGraphDict[plotName] = plotValue

            # If we have all values to plot
            if len(self._plotOneGraphDict) == len(self._captureTextList):
                plotValueList = []
                for captureText in self._captureTextList:
                    plotValueList.append(self._plotOneGraphDict[captureText])
                self._plotterList[0].plotValueList(plotValueList)
                self._plotOneGraphDict = {}

    def _processLine(self, line):
        """@brief Process a text received on the serial port
           @param line The line of text received from the serial port."""
        debugLine = line.rstrip("\n")
        self._uo.debug(debugLine)

        valueTextList = []
        lineElements = line.split(',')
        for lineElement in lineElements:
            # Check through all the text we expect to find numbers after
            for captureText in self._captureTextList:
                pos = lineElement.find(captureText)
                if pos >= 0:
                    valueText = lineElement[pos + len(captureText):]
                    found, value = self._extractNumber(valueText)
                    if found:
                        self._plotValue(captureText, value)
                        valueTextList.append(value)
        if len(valueTextList) > 0:
            self._uo.info("PLOT: {}".format(repr(valueTextList)))

    def _extractNumber(self, text):
        """@brief Extract a number (decimal int value, hex int value or float value) 
           from the text. The text must start with a number.
           @param text The text to extract the number from
           @return A Tuple containing (found, value) 
            found = True if a number was extracted
            value = the number value"""
        extractedCharList = []
        decimalPointCount = 0
        index = 0
        hexadecimalNumber = False
        floatFound = False
        
        if text.upper().startswith("0X"):
            text = text[2:]
            hexadecimalNumber = True

        #The bash shell may add a space char
        if text.upper().startswith(" 0X"):
            text = text[3:]
            hexadecimalNumber = True

        self._uo.debug("_extractNumber({})".format(text))
        while index < len(text):
            currentChar = text[index].upper()

            if currentChar in SerialDebugger.DECIMAL_DIGIT_LIST:
                extractedCharList.append(currentChar)

            elif currentChar in SerialDebugger.HEXADECIMAL_DIGIT_LIST:
                extractedCharList.append(currentChar)

            # If the number is a float, add the decimal point
            elif decimalPointCount == 0 and not hexadecimalNumber and len(extractedCharList) > 0 and currentChar == '.':
                extractedCharList.append(currentChar)
                floatFound = True

            else:
                self._uo.debug("Invalid number character found: <{}>".format( ord(currentChar) ))
                break

            index = index + 1

        value = None
        valueStr = "".join(extractedCharList)
        numberExtracted = False
        if len(valueStr) > 0:
            if hexadecimalNumber:
                try:
                    # Check for hexadecimal value
                    value = int(valueStr, 16)
                    numberExtracted = True
                except ValueError:
                    pass
            elif floatFound:
                try:
                    # Check for hexadecimal value
                    value = float(valueStr)
                    numberExtracted = True
                except ValueError:
                    pass
            else:
                try:
                    # Check for decimal value
                    value = int(valueStr)
                    numberExtracted = True
                except ValueError:
                    pass

        return (numberExtracted, value)

    def run(self):
        """@brief Run the debugger and plot the values captured"""
        try:

            self._openSerialPort()

            line = ''
            while True:
                if self._serial.inWaiting() > 0:
                    data = self._serial.read(1)
                    line += data.decode()
                    pos = line.find("\n")
                    if pos >= 0:
                        self._processLine(line)
                        line = ''
                else:
                    #If no data is received, don't spin lock
                    sleep(0.05)

        finally:

            if self._serial:
                self._serial.close()

            if self._netPlot:
                self._netPlot.disconnect()

if __name__ == '__main__':
    uo = UO()

    uo.info("Netplot Serial port debugger (V{:.1f})".format(VERSION))
    opts = OptionParser(
        usage='SDP = Serial Debug Plot allows you to open a serial port and capture numbers from the received data and plot them. Used for debugging hardware that has a serial port that can be used to send debug data.')
    opts.add_option("--debug", help="Enable debugging.", action="store_true", default=False)
    opts.add_option("--port", help="The serial port (default=/dev/ttyUSB0)", default="/dev/ttyUSB0")
    opts.add_option("--baud", help="The serial port baud rate (115200 bps).", type="int", default=115200)
    opts.add_option("--np", help="Followed by the netplot server (default=127.0.0.1)", default="127.0.0.1")
    opts.add_option("--text",
                    help="Followed by a comma separated list of text that appears just before the number that you wish to plot. Hexadecimal numbers must be prefixed with 0X.",
                    default=None)
    opts.add_option("--one_graph",
                    help="Plot all values on the same graph. By default each value is plotted on separate graphs.",
                    action="store_true", default=False)

    try:
        (options, args) = opts.parse_args()
        uo = UO(debug=options.debug)

        serialDebugger = SerialDebugger(uo, options)
        serialDebugger.run()

    # If the program throws a system exit exception
    except SystemExit:
        pass

    # Don't print error information if CTRL C pressed
    except KeyboardInterrupt:
        pass

    except Exception as ex:
        if options.debug:
            raise

        else:
            uo.error(str(ex))



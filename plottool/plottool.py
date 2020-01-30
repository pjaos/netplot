#!/usr/bin/python3

import  sqlite3
import  os
import  socketserver
import  threading
import  http.server
import  cgitb
import  logging
import  cgi
import  json
import  shutil
import  math

from    optparse import OptionParser
from    time import time
from    shutil import copyfile

class PlotToolError(Exception):
  pass

class UO(object):
    """@brief responsible for user viewable output"""

    def info(self, text):
        print( 'INFO:  '+str(text) )

    def debug(self, text):
        print( 'DEBUG: '+str(text) )

    def warn(self, text):
        print( 'WARN:  '+str(text) )

    def error(self, text):
        print( 'ERROR: '+str(text) )

class ServerHandler(http.server.CGIHTTPRequestHandler):

    QUERY_STRING = "QUERY_STRING"

    def do_GET(self):
        logging.warning("======= GET STARTED =======")
        logging.warning(self.headers)
        http.server.CGIHTTPRequestHandler.do_GET(self)

    def do_POST(self):
        logging.warning("======= POST STARTED =======")
        logging.warning(self.headers)
        form = cgi.FieldStorage(
            fp=self.rfile,
            headers=self.headers,
            environ={'REQUEST_METHOD':'POST',
                     'CONTENT_TYPE':self.headers['Content-Type'],
                     })
        logging.warning("======= POST VALUES =======")
        for item in form.list:
            logging.warning("%s=%s" % (item.name, item.value) )
        logging.warning("\n")
        http.server.CGIHTTPRequestHandler.do_POST(self)

class WebServer(object):
    """@brief Responsible for serving file to allow the plot data to be displayed
              in a web page."""

    def __init__(self, uo, options):
        """@brief Constructor
           @param uo A UO instance
           @param options An optparse options instance."""
        self._uo = uo
        self._options = options

    def serve(self):
        """@brief Run the server on all ports.
           @return None"""

        #Start the web server to display the plots
        wsThread = threading.Thread(target=self._startWebServer)
        wsThread.start()

    def _startWebServer(self):
        """@brief Start the web server."""

        try:
            #Set to the web server root
            os.chdir(self._options.root)
            self._uo.info("Web Server Root: {}".format(self._options.root))
            cgitb.enable()

            Handler = ServerHandler

            Handler.cgi_directories = [self._options.cgi]

            self._uo.info("serving at port {:d}".format(self._options.port) )

            socketserver.TCPServer.allow_reuse_address = True
            socketserver.ThreadingTCPServer.allow_reuse_address = True
            server = http.server.HTTPServer(("", options.port), Handler)

            server.serve_forever()

        #If the user presses CTRL C
        except KeyboardInterrupt:
    	    self.shutdown(server)

        #If the program throws a system exit exception
        except SystemExit:
    	    self.shutdown(server)

        except:
             raise

class PlotDataStore(object):
    """@brief Responsible for storing the plot data to be read
              by the javascript when the web page is loaded"""

    GLOBAL_ATTR             = "GLOBAL_ATTR"
    TRACES_ATTR             = "TRACES_ATTR"
    TRACES_DATA_ATTR        = "TRACES_DATA_ATTR"

    PLOT_GRID               = "PLOT_GRID"
    PLOT_TITLE              = "PLOT_TITLE"
    GRAPH_TYPE              = "GRAPH_TYPE"

    EXCLUDE_FOLDER_LIST     = ("assets", ".git")
    JSON_INDENT             = 0
    ASSETS_FOLDER           = "assets"
    HTML_FOLDER             = "html"
    INDEX_HTML_FILE         = "index.html"
    JAVASCRIPT_FOLDER       = "js"
    PLOT_JS_FILE            = "plot_processor.js"
    PLOT_LIST_FILENAME      = "plot_list.txt"
    PLOT_DATA_FILE          = "plot_data.json"

    @staticmethod
    def GetFileLines(theFile):
        """@brief Read lines of text from a file.
           @param theFile The file to read from.
           @return A list of lines read from the file (empty if the file doesn ot exist)."""
        lines = []
        if os.path.isfile(theFile):
            fd = open(theFile, "r")
            lines = fd.readlines()
            fd.close()
        return lines

    @staticmethod
    def WriteFileLines(theFile, textLines):
        """@brief Write lines of test to a file.
           @param theFile The file to write to.
           @param textLines Lines of text to write to the file.
           @return None"""
        fd = open(theFile, "w")
        for line in textLines:
            fd.write(line+"\n");
        fd.close()

    def __init__(self, uo, options):
        """@brief Constructor
           @param uo A UO instance
           @param options An optparse options instance."""
        self._uo = uo
        self._options = options

    def _getPlotPath(self):
        """@brief Get the plot path to save the plot into."""
        return os.path.join( self._options.path, self._options.n)

    def _createPlotPath(self):
        """@brief Create the plot path to store the plot file.
           @return The plot path. This will be an empty string if no plot title has been set."""
        plotPath = self._getPlotPath()
        if( len(plotPath) ):
            if self._options.create and os.path.isdir(plotPath):
                shutil.rmtree(plotPath)
                self._uo.info("Removed {}".format(plotPath))

            if not os.path.isdir(plotPath):
                os.makedirs(plotPath)
                self._uo.info("Created {}".format(plotPath))
        return plotPath

    def _updateIndexHtmlFile(self, plotPath):
        """@brief Update the index.html file in the plot folder.
           @param plotPath The path for this plot (top level folder).
           @return None"""
        srcPath = os.path.join(self._options.path, PlotDataStore.ASSETS_FOLDER)
        srcPath = os.path.join(srcPath, PlotDataStore.HTML_FOLDER)
        srcFile = os.path.join(srcPath, PlotDataStore.INDEX_HTML_FILE)
        destPath = plotPath
        destFile = os.path.join(destPath, PlotDataStore.INDEX_HTML_FILE)
        if not os.path.isdir(destPath):
            os.makedirs(destPath)
        copyfile(srcFile, destFile)

    def _updateJavaScriptFile(self, plotPath, javaScriptFilename):
        """@brief Copy a javascript file to the assets folder for a plot.
           @param plotPath The path for this plot (top level folder).
           @param javaScriptFilename The name of the javascript file to be copied.
           @return None"""
        srcPath = os.path.join(self._options.path, PlotDataStore.ASSETS_FOLDER)
        srcPath = os.path.join(srcPath, PlotDataStore.JAVASCRIPT_FOLDER)
        srcFile = os.path.join(srcPath, javaScriptFilename)
        destPath = os.path.join(plotPath, PlotDataStore.ASSETS_FOLDER)
        destPath = os.path.join(destPath, PlotDataStore.JAVASCRIPT_FOLDER)
        destFile = os.path.join(destPath, javaScriptFilename)
        if not os.path.isdir(destPath):
            os.makedirs(destPath)
        copyfile(srcFile, destFile)

    def _updateplotFolderList(self):
        """@brief Update the list of folders that contain plots.
                  This file is read by plot_table_build.js to build the table of available plots.
           @return None"""
        folderEntries = os.listdir(self._options.path)
        for folderEntry in folderEntries:
            absPath = os.path.join(self._options.path, folderEntry)
            if os.path.isfile(absPath) or folderEntry in PlotDataStore.EXCLUDE_FOLDER_LIST:
                continue
            lines = PlotDataStore.GetFileLines(PlotDataStore.PLOT_LIST_FILENAME)

            newLines=[]
            for line in lines:
                line=line.rstrip('\r')
                line=line.rstrip('\n')
                if len(line) > 0 and line != folderEntry:
                    newLines.append(line)
            newLines.append("{}\n".format(folderEntry))
            PlotDataStore.WriteFileLines(PlotDataStore.PLOT_LIST_FILENAME, newLines)
            self._uo.info("Added {}".format(self._options.n))

    def _updatePlotDataFile(self, plotPath, plotLayout, trace):
        """@brief Update the index.html file in the plot folder.
           @param plotPath The path for this plot (top level folder).
           @param plotLayout The plotly plot layout dict.
           @param trace The trace (dict) to be added.
           @return None"""
        dataFile = os.path.join(plotPath, PlotDataStore.PLOT_DATA_FILE)
        saveDict = None
        #If appending to an existing plot
        if self._options.append:
            fd = open(dataFile, 'r')
            jsonStr=fd.read()
            fd.close()
            saveDict = json.loads(jsonStr)

        if not saveDict:
            saveDict={}

        if PlotTool.PLOTLAYOUT in saveDict:
            saveDict[PlotTool.PLOTLAYOUT].update(plotLayout)
        else:
            saveDict[PlotTool.PLOTLAYOUT]=plotLayout

        if PlotTool.PLOTTRACES in saveDict:
            saveDict[PlotTool.PLOTTRACES].append(trace)
        else:
            saveDict[PlotTool.PLOTTRACES]=[]
            saveDict[PlotTool.PLOTTRACES].append(trace)

        jsonStr = json.dumps(saveDict, indent=PlotDataStore.JSON_INDENT, sort_keys=True, default=str)
        fd = open(dataFile, 'w')
        fd.write(jsonStr)
        fd.close()

    def store(self, plotLayout, trace):
        """@brief Save the files required to deliver the plot via the web server.
           @param plotLayout The plotly plot layout dict.
           @param trace The trace (dict) to be added."""
        plotPath = self._createPlotPath()
        self._updatePlotDataFile(plotPath, plotLayout, trace)
        self._updateIndexHtmlFile(plotPath)
        self._updateJavaScriptFile(plotPath, PlotDataStore.PLOT_JS_FILE)
        self._updateplotFolderList()

class PlotTool(object):
    #These are plotly attributes
    XAXIS           = "xaxis"
    YAXIS           = "yaxis"
    TITLE           = "title"
    TYPE            = "type"
    SHOWTICKLABELS  = "showticklabels"
    AUTORANGE       = "autorange"
    LINEAR          = "linear"
    SCATTER         = "scatter"
    LOG             = "log"
    SHOWLEGEND      = "showlegend"
    NAME            = "name"
    LINE            = "line"
    WIDTH           = "width"
    X               = "x"
    Y               = "y"
    MARGIN          = "margin"
    L               = "l"
    R               = "r"
    B               = "b"
    T               = "t"
    PAD             = "pad"
    HOVERMODE       = "hovermode"
    CLOSEST         = "closest"
    GRID            = "grid"
    ROWS            = "rows"
    COLUMNS         = "columns"
    PATTERN         = "pattern"
    INDEPENDANT     = "independent"
    LINES           = "line"
    DASH            = "dash"

    TRACE           = "trace"
    PLOTTRACES      = "plottraces"
    PLOTLAYOUT      = "plotlayout"
    TIME            = "time"
    XY              = "xy"

    """@brief Allows the user to plot data easily from an SQLite database."""
    def __init__(self, uo, options):
        """@brief Constructor
           @param uo A UO instance
           @param options An optparse options instance."""
        self._uo = uo
        self._options = options
        self._fieldList = None

    def processFile(self):
        """@brief Process the sqlite file"""
        if not self._options.f:
            raise PlotToolError("No SQLIte database file defined.")
        self._processFile()

    def info(self, text):
        """Display info text is a UserOutput object was provided"""
        if self._uo != None:
            self._uo.info(text)

    def error(self, text):
        """Display error text is a UserOutput object was provided"""
        if self._uo != None:
            self._uo.error(text)

    def _connect(self):
        """Build a connection to the database and return the connection and cursor objects in a tuple"""
        self.info("Connecting to {}".format(self._options.f))
        conn = sqlite3.connect(self._options.f, detect_types=sqlite3.PARSE_DECLTYPES|sqlite3.PARSE_COLNAMES)
        cursor = conn.cursor()
        self.info("Connected.")
        return (conn, cursor)

    def _showDetails(self):
        """@brief Show the database details.
           @return None"""
        res = self._conn.execute("SELECT name FROM sqlite_master WHERE type='table';")
        for tableNameList in res:
            for tableName in tableNameList:
                self.info("TABLE: {}".format(tableName))
                cursor = self._conn.execute('select * from {}'.format(tableName))
                fieldCount = 1
                fieldNames = [description[0] for description in cursor.description]
                for fieldName in fieldNames:
                    self.info("    {:3d}: {}".format(fieldCount, fieldName))
                    fieldCount=fieldCount+1

    def _getData(self):
        """@brief Get the data from a connected database."""
        if not self._options.t:
            raise PlotToolError("Define the table to plot data from on the command line.")

        if not self._options.l:
            raise PlotToolError("Define a list of the fields to be plotted on the command line.")

        self._fieldList = self._options.l.split(",")
        if len(self._fieldList) != 2:
            raise PlotToolError("Please define 2 field names (X and Y axis) from the database.")

        self.info("Reading database...")
        startTime = time()

        if self._options.start_datetime and self._options.stop_datetime:
            sqlCmd = "SELECT {} from {} WHERE ts >= \"{}\" and ts <= \"{}\" ORDER BY ts".format(",".join(self._fieldList), self._options.t, self._options.start_datetime, self._options.stop_datetime)
        elif self._options.start_datetime:
            sqlCmd = "SELECT {} from {} WHERE ts >= \"{}\" ORDER BY ts".format(",".join(self._fieldList), self._options.t, self._options.start_datetime)
        elif self._options.stop_datetime:
            sqlCmd = "SELECT {} from {} WHERE ts <= \"{}\" ORDER BY ts".format(",".join(self._fieldList), self._options.t, self._options.stop_datetime)
        else:
            sqlCmd = "SELECT {} from {} ORDER BY ts".format(",".join(self._fieldList), self._options.t)

        self._cursor.execute(sqlCmd)
        rows = self._cursor.fetchall()
        self.info("Took {:.1f} seconds to read the database.".format(time()-startTime))

        if len(rows) > self._options.maxPlotCount:
            startTime = time()
            orgPointCount=len(rows)
            stride=math.ceil( len(rows)/self._options.maxPlotCount )
            rows=rows[::stride]
            self._uo.warn("!!! Plot point count is greater than the max ({})".format(self._options.maxPlotCount))
            self._uo.warn("Reduced data set from {} to {} records to plot".format(orgPointCount, len(rows)))
            self.info("Took {:.1f} seconds to reduce data set size.".format(time()-startTime))

        return rows

    def _plotRows(self, rows):
        """@brief Called to plot data.
           @param rows The rows of data to be plotted.
           @return None"""
        startTime = time()
        plotDataStore = PlotDataStore(self._uo, self._options)

        plotLayout={}
        if self._options.create:
            plotLayout[PlotTool.MARGIN]={}
            plotLayout[PlotTool.MARGIN][PlotTool.L]=60
            plotLayout[PlotTool.MARGIN][PlotTool.R]=60
            plotLayout[PlotTool.MARGIN][PlotTool.B]=60
            plotLayout[PlotTool.MARGIN][PlotTool.T]=30
            plotLayout[PlotTool.MARGIN][PlotTool.PAD]=2
            plotLayout[PlotTool.HOVERMODE]=PlotTool.CLOSEST

            plotLayout[PlotTool.TITLE]=self._options.title

            gridElems = self._options.pg.split(",")
            plotLayout[PlotTool.GRID]={}
            plotLayout[PlotTool.GRID][PlotTool.ROWS]=gridElems[0]
            plotLayout[PlotTool.GRID][PlotTool.COLUMNS]=gridElems[1]
            plotLayout[PlotTool.GRID][PlotTool.PATTERN]=PlotTool.INDEPENDANT

        if self._options.pgi > 0:
            xAxisKey="{}{}".format(PlotTool.XAXIS, self._options.pgi+1)
        else:
            xAxisKey=PlotTool.XAXIS

        #Set X axis label name
        plotLayout[xAxisKey]={}
        plotLayout[xAxisKey][PlotTool.TITLE]=self._fieldList[0]
        if self._options.graphType == PlotTool.TIME:
            plotLayout[xAxisKey][PlotTool.TYPE]=PlotTool.SCATTER
        else:
            plotLayout[xAxisKey]=[PlotTool.TYPE]=PlotTool.LINEAR
        plotLayout[xAxisKey][PlotTool.SHOWTICKLABELS]="false"
        plotLayout[xAxisKey][PlotTool.AUTORANGE]="true"

        #Set Y axis label name
        if self._options.pgi > 0:
            yAxisKey="{}{}".format(PlotTool.YAXIS, self._options.pgi+1)
        else:
            yAxisKey=PlotTool.YAXIS

        plotLayout[yAxisKey]={}
        plotLayout[yAxisKey][PlotTool.TITLE]=self._fieldList[1]
        if self._options.log:
            plotLayout[yAxisKey][PlotTool.TYPE]=PlotTool.LOG
        else:
            plotLayout[yAxisKey][PlotTool.TYPE]=PlotTool.LINEAR
        plotLayout[yAxisKey][PlotTool.AUTORANGE]="true"

        xValueList = [element[0] for element in rows]
        yValueList = [element[1] for element in rows]

        trace={}
        trace[PlotTool.NAME]="{}_{}".format(self._options.pgi, self._fieldList[1])
        trace[PlotTool.SHOWLEGEND]="false"
        trace[PlotTool.TYPE]=''
        trace[PlotTool.X]=xValueList
        trace[PlotTool.Y]=yValueList
        #The xaxis in the trace has to tie up with the xaxis of the plotLayout xAxis
        #in order to plot on a grid position.
        if self._options.pgi > 0:
            trace[PlotTool.XAXIS]="{}{}".format(PlotTool.X, self._options.pgi+1)
            trace[PlotTool.YAXIS]="{}{}".format(PlotTool.Y, self._options.pgi+1)
        else:
            trace[PlotTool.XAXIS]="{}".format(PlotTool.X)
            trace[PlotTool.YAXIS]="{}".format(PlotTool.Y)

        trace[PlotTool.LINE]={}
        trace[PlotTool.LINE][PlotTool.WIDTH]=options.lw
        if options.dash:
            trace[PlotTool.LINE][PlotTool.DASH]=PlotTool.DASH

        plotDataStore.store(plotLayout, trace)
        self.info("Took {:.1f} seconds to save the JSON file the web server requires.".format(time()-startTime))

        self.runWebServer()

    def runWebServer(self):
        """@brief A web server is required for the user to load the plot page using a browser"""
        webServer = WebServer(self._uo, self._options)
        webServer.serve()

    def _processFile(self):
        """@brief Process the sqlite file. The file must have been defioned on the command line."""
        conn = None
        try:

            self._conn, self._cursor = self._connect()

            if self._options.show:
                self._showDetails()

            else:
                if not self._options.create and not self._options.append:
                    raise PlotToolError("You must either create or append plots")

                if self._options.create and self._options.append:
                    raise PlotToolError("You cannot create and append plots")

                rows = self._getData()
                self._plotRows(rows)

        finally:
            if conn:
                try:
                    conn.close()
                    self.info("Closed connection to {}".format(self._options.f))
                except Exception as e:
                    self.error("close_conn: {}".format(e))

if __name__== '__main__':
    uo = UO()

    opts=OptionParser(usage='A tool to allow data in an sqllite database to be plotted easily.')
    opts.add_option("--create", help="Create Plot.", action="store_true", default=False)
    opts.add_option("--append", help="Append to an existing plot.", action="store_true", default=False)
    opts.add_option("-f",       help="The sqlite database file.", default=None)
    opts.add_option("-t",       help="The database table to plot data from.", default=False)
    opts.add_option("-l",       help="A comma separated list of two field names. The first is the X value, the seconds is the Y value.",default=False)
    opts.add_option("-n",       help="The plot name (default=Plot).", default="Plot")
    opts.add_option("-m",       help="Max plot points (default=50000)", type="int", default=50000)
    opts.add_option("--pg",     help="Plot grid (default = 1,1 I.E a single graph).",default="1,1")
    opts.add_option("--pgi",    help="The plot grid index (default = 0).",type="int", default=0)
    opts.add_option("--log",    help="Set a Log Y axis.", action="store_true", default=False)
    opts.add_option("--lw",     help="Define the trace line width (default=1.0)", type="float", default=1.0)
    opts.add_option("--dash",   help="Set the trace line to a dashed line.", action="store_true", default=False)

    opts.add_option("--start",  help="A start time for plot records (format = 2020-01-24 04:11:55.845981 of part of, E.G 2020-01-24).",default=None)
    opts.add_option("--stop",   help="A stop time to plot records (format as above).",default=None)
    opts.add_option("--title",  help="The title of the plot (default = MAIN_PLOT).",default="Main Plot")
    opts.add_option("--show",   help="Show the database schema. Shows each tables and the fields available in each table.", action="store_true", default=False)
    opts.add_option("--ws",     help="Run the web server. Use this if you wish to view an existing plot.", action="store_true", default=False)
    opts.add_option("--port",   help="The web server port (default=8080)", type="int", default=8080)
    opts.add_option("--debug",  help="Enable debugging.", action="store_true", default=False)

    try:
        (options, args) = opts.parse_args()

        options.path = os.getcwd()
        options.root="."
        options.cgi="/cgi-bin"
        options.plotTitle=options.title
        options.graphType="time"
        options.plotGrid=options.pg
        options.maxPlotCount=options.m
        options.start_datetime=options.start
        options.stop_datetime =options.stop

        plotTool = PlotTool(uo, options)
        if options.ws:
            plotTool.runWebServer()
        else:
            plotTool.processFile()

    #If the program throws a system exit exception
    except SystemExit:
      pass
    #Don't print error information if CTRL C pressed
    except KeyboardInterrupt:
      pass
    except Exception as ex:
     if options.debug:
       raise

     else:
       uo.error( str(ex) )

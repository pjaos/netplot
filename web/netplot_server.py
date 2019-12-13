#!/usr/bin/python3

#/*****************************************************************************************
# *                             Copyright 2019 Paul Austen                                *
# *                                                                                       *
# * This program is distributed under the terms of the GNU Lesser General Public License  *
# *****************************************************************************************/
 
import sys
from   optparse import OptionParser
import threading
import socket
import os
import json
import http.server
import cgitb
import socketserver
import logging
import cgi
from   time import sleep
from   shutil import copyfile

class NetplotError(Exception):
  pass

class UO(object):
    """@brief responsible for user viewable output"""

    def info(self, text):
        print('INFO: {}'.format(text))

    def debug(self, text):
        print('DEBUG: {}'.format(text))

    def warn(self, text):
        print('WARN:  {}'.format(text))

    def error(self, text):
        print('ERROR: {}'.format(text))    

class ConnectionHandler(object):
    """@brief Responsible for handling the data from a connected socket"""
    
    GRID_CMD                = "set grid="
    ADD_PLOT                = "add_plot"
    OUTPUT_LINE_LIST        = None
    OUTPUT_FILENAME         = "netplot_commands.txt"
    PLOT_GRID_ID            = "plot_grid_id"
    PLOT_TITLE              = "set frame_title"
    ASSETS_FOLDER           = "assets"
    HTML_FOLDER             = "html"
    JAVASCRIPT_FOLDER       = "js"
    INDEX_HTML_FILE         = "index.html"
    NETPLOT_JS_FILE         = "netplot.js"
    NETPLOT_LEGENDS_JS_FILE = "netplot_legends.js"
    PLOT_LIST_FILENAME      = "plot_list.txt"

    def __init__(self, uo, options, _socket, plotGrid):
        self._uo              = uo
        self._options         = options
        self._socket          = _socket
        self._plotGridID      = plotGrid
        self._fileSaveTimer   = None
        self._plotCreateTimer = None
        self.plotCount      = 0

    def _sendString(self, _socket, _string):
        """@brief Send a string on a socket
           @param _socket The open socket to send the string data on
           @param _string The string to send
           @return None"""
        _bytes = bytes(_string, 'utf-8')
        _socket.send(_bytes)

    def _receiveString(self, _socket):
        """@brief Receive a string on a socket.
           @param _socket The connected socket."""
        rxBytes = _socket.recv(NetplotServer.BUFFER_SIZE)
        return rxBytes.decode('utf-8')

    def handleConnection(self):
        """@brief Handle the data from a connected socket"""
        
        self._sendString( self._socket, "netplot_version={:.1f}\n".format(NetplotServer.NETPLOT_SVR_VERSION) )                    
        try:
            while True:
                rxData = self._receiveString(self._socket)
                self._handleRXData(rxData)
                self._sendString( self._socket, "OK\n")
        except IOError:
            pass

    def _handleRXData(self, rxData):
        """@brief Handle RX data from the client.
           @param rxData The RX data from the client.
           @return None"""
        #print("rxData=<"+rxData+">")  
        #This should be the first command we receive on the first socket connection
        if self._plotGridID == 0 and rxData.startswith(ConnectionHandler.GRID_CMD):
            self._removeOutputFile()
            ConnectionHandler.OUTPUT_LINE_LIST = []

        if rxData.startswith(ConnectionHandler.ADD_PLOT):
            ConnectionHandler.OUTPUT_LINE_LIST.append("set plot_grid={:d}\n".format(self._plotGridID) )
                        
        ConnectionHandler.OUTPUT_LINE_LIST.append(rxData)
            
        if self._fileSaveTimer:
            self._fileSaveTimer.cancel()

        self._fileSaveTimer = threading.Timer(0.2, self._saveOutputFile)
        self._fileSaveTimer.start()
        
    def _getPlotPath(self):
        """@brief Get the plot path to save the plot into."""
        plotPath = ""
        for line in ConnectionHandler.OUTPUT_LINE_LIST:
            if line.startswith(ConnectionHandler.PLOT_TITLE):
                line = line.rstrip("\r")
                line = line.rstrip("\n")
                elems = line.split("=")
                if len(elems) > 1:
                    plotPath = elems[1];
                    
        plotPath = plotPath.replace(" ","_")
        plotPath = plotPath.replace("/","_")
        return os.path.join( self._options.path, plotPath)
        
    def _createPlotPath(self):
        """@brief Create the plot path to store the plot file.
           @return The plot path. This will be an empty string if no plot title has been set."""
        plotPath = self._getPlotPath()
        if( len(plotPath) ):
            if not os.path.isdir(plotPath):
                os.makedirs(plotPath)
        print("PJA: plotPath={}".format(plotPath))
        return plotPath
                
    def _updateNetplotFile(self, plotPath):
        """@brief Update the netplot file.
           @param plotPath The path to save the file.
           @return None"""
        netplotCmdsFile = os.path.join(self._options.path, ConnectionHandler.OUTPUT_FILENAME)
        #!!! We must create this every time as it may be called several times before the plot is complete.
        destFile = os.path.join(plotPath, ConnectionHandler.OUTPUT_FILENAME)
        copyfile(netplotCmdsFile, destFile)
            
    def _updateIndexHtmlFile(self, plotPath):
        """@brief Update the index.html file in the plot folder.
           @param plotPath The path for this plot (top level folder).
           @return None"""
        srcPath = os.path.join(self._options.path, ConnectionHandler.ASSETS_FOLDER)
        srcPath = os.path.join(srcPath, ConnectionHandler.HTML_FOLDER)
        srcFile = os.path.join(srcPath, ConnectionHandler.INDEX_HTML_FILE)           
        destPath = plotPath
        destFile = os.path.join(destPath, ConnectionHandler.INDEX_HTML_FILE)                           
        if not os.path.isdir(destPath):
            os.makedirs(destPath)
        copyfile(srcFile, destFile)
        
    def _updateJavaScriptFile(self, plotPath, javaScriptFilename):
        """@brief Copy a javascript file to the assets folder for a plot.
           @param plotPath The path for this plot (top level folder).
           @param javaScriptFilename The name of the javascript file to be copied.
           @return None"""
        srcPath = os.path.join(self._options.path, ConnectionHandler.ASSETS_FOLDER)
        srcPath = os.path.join(srcPath, ConnectionHandler.JAVASCRIPT_FOLDER)
        srcFile = os.path.join(srcPath, javaScriptFilename)           
        destPath = os.path.join(plotPath, ConnectionHandler.ASSETS_FOLDER)
        destPath = os.path.join(destPath, ConnectionHandler.JAVASCRIPT_FOLDER)
        destFile = os.path.join(destPath, javaScriptFilename)                           
        if not os.path.isdir(destPath):
            os.makedirs(destPath)
        copyfile(srcFile, destFile)
                
    def _getFileLines(self, theFile):
        """@brief Read lines of text from a file.
           @param theFile The file to read from.
           @return A list of lines read from the file (empty if the file doesn ot exist)."""
        lines = []
        if os.path.isfile(theFile):
            fd = open(ConnectionHandler.PLOT_LIST_FILENAME, "r")
            lines = fd.readlines()
            fd.close()
        return lines
                    
    def _writeFileLines(self, theFile, textLines):
        """@brief Write lines of test to a file.
           @param theFile The file to write to.
           @param textLines Lines of text to write to the file.
           @return None"""
        fd = open(theFile, "w")
        print("PJA: len=%d" % ( len(textLines) ) )
        for line in textLines:
            print("PJA: write <%s>" % (line) )
            fd.write(line+"\n");
        fd.close()
                           
    def _updateplotFolderList(self):
        """@brief Update the list of folders that contain plots.
                  This file is read by plot_table_build.js to build the table of available plots.
           @return None"""
        folderEntries = os.listdir(self._options.path)
        for folderEntry in folderEntries:
            absPath = os.path.join(self._options.path, folderEntry)
            if( os.path.isdir(absPath) and folderEntry not in ("assets", ) ):
                lines = self._getFileLines(ConnectionHandler.PLOT_LIST_FILENAME)

                newLines=[]
                for line in lines:
                    line=line.rstrip('\r')
                    line=line.rstrip('\n')
                    if len(line) > 0 and line != folderEntry:
                        newLines.append(line)
                newLines.append("{}\n".format(folderEntry))
                self._writeFileLines(ConnectionHandler.PLOT_LIST_FILENAME, newLines)
        
    def _saveOutputFile(self):
        """@brief Save all the commands received to the output file."""
        
        #Save the output file.
        absFilename = os.path.join( self._options.path, ConnectionHandler.OUTPUT_FILENAME)
        fd = open(absFilename,"w")
        for line in ConnectionHandler.OUTPUT_LINE_LIST:
            fd.write(line)
        fd.close()
        self._uo.info("Saved {}".format(ConnectionHandler.OUTPUT_FILENAME))
        
        plotPath = self._createPlotPath()
        self._updateNetplotFile(plotPath)
        self._updateIndexHtmlFile(plotPath)
        self._updateJavaScriptFile(plotPath, ConnectionHandler.NETPLOT_JS_FILE)
        self._updateJavaScriptFile(plotPath, ConnectionHandler.NETPLOT_LEGENDS_JS_FILE)
        self._updateplotFolderList()
        



        
    def _removeOutputFile(self):
        absPath = os.path.join(self._options.path, ConnectionHandler.OUTPUT_FILENAME)
        if os.path.isfile(absPath):
            os.remove(absPath)
            self._uo.info("Removed {}".format(ConnectionHandler.OUTPUT_FILENAME))
        
class NetplotServer(object):
    """@brief Responsibe for reciving data from netplot clients and saving it to local files"""
    
    DEFAULT_BASE_PORT   = 9600
    DEFAULT_PORT_COUNT  = 100
    BUFFER_SIZE         = 65535
    SERVER_HOST_IP      = '0.0.0.0'
    NETPLOT_SVR_VERSION = 2.5;

    def __init__(self, uo, options):
        self._uo = uo
        self._options = options
        
    def serve(self):
        """@brief Run the server on all ports.
           @return None"""
           
        #Start the web server to display the plots
        wsThread = threading.Thread(target=self.startWebServer)
        wsThread.start()
        
        for port in range(self._options.bp, self._options.bp+self._options.pc):
            _thread = threading.Thread(target=self._servePort, args=(port,))
            _thread.start()
            
    def _servePort(self, port):
        """@brief Run the server on all ports.
           @param port The TCP port to accept connections on.
           @return None"""

        tcpServer = socket.socket(socket.AF_INET, socket.SOCK_STREAM) 
        tcpServer.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) 
        tcpServer.bind((NetplotServer.SERVER_HOST_IP, port))
        self._uo.info("Serve on TCP/IP port {:d}".format(port))
        while True: 
            tcpServer.listen(1) 
            (_socket, (ip,remotePort)) = tcpServer.accept()
            self._uo.info("Client connected on port {:d}".format(port))
            self._handleConnection(_socket, port)
            
    def _handleConnection(self, _socket, localPort):
        """@brief Handle a connected socket
           @param _socket The connected socket
           @param localPort The local TCP port on which the socket is connected.
           @return None"""
        if localPort == self._options.bp:
            #Reset global data as we're starting a new set of plots
            ConnectionHandler.SavedGlobalData = False
        connectionHandler = ConnectionHandler(self._uo, self._options, _socket, localPort-self._options.bp)
        connectionHandler.handleConnection()

    def startWebServer(self):
        """@brief Start the web server."""
        
        #Delay so that we see the output from the HTTP server after the 100 tcp ports are shown.
        sleep(1)
        
        try:
            #Set to the web server root
            os.chdir(self._options.root)
            self._uo.info("Web Server Root: {}".format(self._options.root))
            cgitb.enable()
            
            Handler = ServerHandler
            port    = self._options.port
            
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
        
    def shutdown(self, server):
        """Shutdown the web server"""
        if server != None:
            server.socket.close()
            self._uo.info("Shutdown server on port {:d}".format(self._options.port) )
        
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

#Very simple cmd line template using optparse
if __name__== '__main__':
    uo = UO()

    opts=OptionParser(usage='Run a netplot server. This has two functions 1: Receives data from netplot clients and saves the data to the netplot_commands.txt file. 2: A web server to view the plot data.')
    opts.add_option("--debug",help="Enable debugging.", action="store_true", default=False)
    opts.add_option("--bp",   help="The netplot base TCP port (default = {}).".format(NetplotServer.DEFAULT_BASE_PORT), type="int", default=NetplotServer.DEFAULT_BASE_PORT)
    opts.add_option("--pc",   help="The number of TCP ports to listen on (default = {}).".format(NetplotServer.DEFAULT_PORT_COUNT), type="int", default=NetplotServer.DEFAULT_PORT_COUNT)
    opts.add_option("--path", help="The path to store the json files (default={}).".format(os.getcwd()), default=os.getcwd())
    opts.add_option("--port", help="Followed by the web server port (default=8080)", type="int", default=8080)
    opts.add_option("--root", help="Followed by the web server root path", default=".")
    opts.add_option("--cgi",  help="A folder (in the root path) containing the cgi scripts (default=/cgi-bin).", default="/cgi-bin")

    try:
        (options, args) = opts.parse_args()
            
        netplotServer = NetplotServer(uo, options)
        netplotServer.serve()
        
    #If the program throws a system exit exception
    except SystemExit:
      pass
    #Don't print error information if CTRL C pressed
    except KeyboardInterrupt:
      pass
    except:
     if options.debug:
       raise
       
     else:
       uo.error(sys.exc_value)

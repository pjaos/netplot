#!/usr/bin/python3
#/*****************************************************************************************
# *                             Copyright 2019 Paul Austen                                *
# *                                                                                       *
# * This program is distributed under the terms of the GNU Lesser General Public License  *
# *****************************************************************************************/
 
import  sys
from    optparse import OptionParser
import  threading
import  socket
import  os
import  json

class ProgramError(Exception):
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

    SET_CMD     = "set "
    PLOT_NAME   = "plot_name"
    INIT        = "init"
    GRID        = "grid"
    FRAME_TITLE = "frame_title"
    PLOT_TITLE  = "plot_title"
    GRAPH       = "graph"

    PLOT_COUNT  = 0
    
    SavedGlobalData = False
    PlotTitle       = ""
    
    def __init__(self, uo, options, _socket, localPort):
        self._uo = uo
        self._options = options
        self._socket = _socket
        self._localPort = localPort

        self._plotDict = {}
        self._plotArea = localPort-options.bp
        self._plotNumber = -1
        self._fileSaveTimer = None
        self._fileList = []
        
        #PJA
        self.lastPlotNumber = -1
        
        
        
    def handleConnection(self):
        """@brief Handle the data from a connected socket"""
        
        self._sendString( self._socket, "netplot_version={:.1f}\n".format(NetplotServer.NETPLOT_SVR_VERSION) )
        if self._localPort == self._options.bp:
            self.cleanLocalFiles()
                    
        try:
            while True:
                rxData = self._receiveString(self._socket)
                self._handleRXData(rxData, self._localPort)
                self._sendString( self._socket, "OK\n")
        except IOError:
            pass

    def cleanLocalFiles(self):
        """@brief clean the local JSON files.
           @return None."""
        entries = os.listdir(self._options.path)
        entries.sort()
        for entry in entries:
            absPath = os.path.join(self._options.path, entry)
            if entry.endswith(".json") and os.path.isfile(absPath):
                os.remove(absPath)
                self._uo.info("Removed {}".format(entry))

    def _sendString(self, _socket, _string):
        """@brief Send a string on a socket
           @param _socket The open socket to send the string data on
           @param _string The string to send
           @return None"""
        _bytes = bytes(_string, 'utf-8')
        _socket.send(_bytes)

    def _receiveString(self, _socket):
        """@brief Receive a string on a socket."""
        rxBytes = _socket.recv(NetplotServer.BUFFER_SIZE)
        return rxBytes.decode('utf-8')
    
    def _getKeyValue(self, rxData):
        rxData = rxData.rstrip("\r")
        rxData = rxData.rstrip("\n")
        elems = rxData.split("=")
        if len(elems) == 2:
            key = elems[0][4:]
            value = elems[1]
            return (key, value) 
        return None
       
    def addToDict(self, rxData):
        """@brief Add data to the dictionary
           @param rxData Received data"""
        elems = self._getKeyValue(rxData)
        if elems and len(elems) == 2:
            if elems[0] == ConnectionHandler.FRAME_TITLE or\
               elems[0] == ConnectionHandler.PLOT_TITLE or \
               elems[0] == ConnectionHandler.GRAPH:
                self._plotDict[elems[0]]=elems[1]
            elif elems[0] == ConnectionHandler.GRID:
                elems = elems[1].split(",")
                if len(elems) == 2:
                    x=int(elems[0])
                    y=int(elems[1])
                    self._plotDict[ConnectionHandler.GRID]=(x,y)
            elif elems[0] == ConnectionHandler.PLOT_NAME:
                self._plotDict[elems[0]]=elems[1]
                if elems and len(elems) == 2:
                    plotName = elems[1]
                    elems = plotName.split(" ")
                    self._plotNumber = int(elems[0])

            else:
                key = elems[0]
                value = elems[1]
                self._plotDict[key]=value

        else:
            elems = rxData.split(":")
            if len(elems) == 3:
                if "plot_values" not in self._plotDict:
                    self._plotDict["plot_values"]=[]
                plotIdx = int(elems[0])
                xVal = float(elems[1])
                yVal = float( elems[2].strip("\n") )
                self._plotDict["plot_values"].append( (plotIdx, xVal, yVal) )
                
    def _saveDict(self, plotDict, filename):
        """@brief Save dict to file.
           @param plotDict The dict to save.
           @param filename The file to save into.
           @return The absolute filename"""
        absFilename = os.path.join( self._options.path, filename)
        jsonData = json.dumps(plotDict, indent=4, sort_keys=True)
        #Ensure we don't overwrite a file
        if not os.path.isfile(absFilename):
            fd = open(absFilename,"w")
            fd.write(jsonData)
            fd.close()
            #self._uo.info("PJA: Saved {} to {}".format(jsonData, filename))
        return absFilename
    
    def _savePlotFile(self):
        """@brief Save dict to plot file."""
        #If we have a valid plot number
        if self._plotNumber != -1:
            self._plotDict['plot_title']=ConnectionHandler.PlotTitle
            filename = "{:d}_{:03d}.json".format(self._plotArea, self._plotNumber)
            absFilename = self._saveDict(self._plotDict, filename)
            self._fileList.append(absFilename)

    def _saveGlobal(self):
        """@brief Save global dict file."""
        filename = "global_config.json" #PJA parameterize 
        absFilename = self._saveDict(self._plotDict, filename)
        self._fileList.append(absFilename)
        
    def _saveFileListFile(self):
        """@brief Save the list of files we have saved."""
        entries = os.listdir(self._options.path)
        entries.sort()
        jsonFiles = []
        for entry in entries:
            if entry.endswith(".json"):
                jsonFiles.append(entry)
                
        filename = "filelist.json" #PJA parameterize 
        absFilename = os.path.join( self._options.path, filename)
        jsonData = json.dumps(jsonFiles, indent=4, sort_keys=True)
        fd = open(absFilename,"w")
        fd.write(jsonData)
        fd.close()
        self._uo.info("Saved {}".format(filename))
        
    def _handleRXData(self, rxData, localPort):
        """@brief Handle RX data from the client.
           @param rxData The RX data from the client.
           @param localPort The local TCP port on which the socket is connected.
           @return None"""
        #print("rxData=<"+rxData+">")

        if rxData.find(ConnectionHandler.PLOT_NAME) >= 0:
            #if bool(self._plotDict) and 'plot_values' in self._plotDict.keys() and len(self._plotDict['plot_values']) > 0:
                self._savePlotFile()
                self._plotDict={}
                                
        if rxData.find(ConnectionHandler.PLOT_TITLE) >= 0:
            #We receive the global data first (grid etc)
            if not ConnectionHandler.SavedGlobalData:
                self._saveGlobal()
                ConnectionHandler.SavedGlobalData = True
                self._plotDict={}

            #Read the plot title
            elems = self._getKeyValue(rxData)
            if len(elems) > 1:
                ConnectionHandler.PlotTitle = elems[1]
                #self._plotDict[elems[0]]=elems[1]
                
        #We expect to receive and empty line after all plot values have been received.
        elif( ConnectionHandler.SavedGlobalData and bool(self._plotDict) and len(rxData) ==0 ):
            if self._plotDict and 'plot_values' in self._plotDict.keys() and len(self._plotDict['plot_values']) > 0:
                self._savePlotFile()
                self._plotDict={}

        else:
            self.addToDict(rxData)
        
        if self._fileSaveTimer:
            self._fileSaveTimer.cancel()

        self._fileSaveTimer = threading.Timer(0.2, self._saveFileListFile)
        self._fileSaveTimer.start()
        
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
        connectionHandler = ConnectionHandler(self._uo, self._options, _socket, localPort)
        connectionHandler.handleConnection()

#Very simple cmd line template using optparse
if __name__== '__main__':
    uo = UO()

    opts=OptionParser(usage='Run a netplot server. This recives data from client connections and saves the plots to local files.')
    opts.add_option("--debug",      help="Enable debugging.", action="store_true", default=False)
    opts.add_option("--bp",         help="The netplot base TCP port (default = {}).".format(NetplotServer.DEFAULT_BASE_PORT), type="int", default=NetplotServer.DEFAULT_BASE_PORT)
    opts.add_option("--pc",         help="The number of TCP ports to listen on (default = {}).".format(NetplotServer.DEFAULT_PORT_COUNT), type="int", default=NetplotServer.DEFAULT_PORT_COUNT)
    opts.add_option("--path",       help="The path to store the json files (default={}).".format(os.getcwd()), default=os.getcwd())

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

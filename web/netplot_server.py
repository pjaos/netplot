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
    
    GRID_CMD         = "set grid="
    ADD_PLOT         = "add_plot"
    OUTPUT_LINE_LIST = None
    OUTPUT_FILENAME  = "netplot_commands.txt"
    PLOT_GRID_ID     = "plot_grid_id"

    def __init__(self, uo, options, _socket, plotGrid):
        self._uo            = uo
        self._options       = options
        self._socket        = _socket
        self._plotGridID    = plotGrid
        self._fileSaveTimer = None
        
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
        
    def _saveOutputFile(self):
        """@brief Save all the commands received to the output file."""
        absFilename = os.path.join( self._options.path, ConnectionHandler.OUTPUT_FILENAME)
        fd = open(absFilename,"w")
        for line in ConnectionHandler.OUTPUT_LINE_LIST:
            fd.write(line)
        fd.close()
        self._uo.info("Saved {}".format(ConnectionHandler.OUTPUT_FILENAME))
        
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

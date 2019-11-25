/*****************************************************************************************
 *                             Copyright 2019 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/
 
var net = require('net');
var fs = require('fs');

const HOST                   = "0.0.0.0";
const BASE_TCP_PORT          = 9600;
const TCP_PORT_COUNT         = 100;
const NETPLOT_VERSION        = 2.5;

const SET_PREFIX             = "set ";
const GRAPH                  = "graph";
const TIME                   = "time";
const BAR                    = "bar";
const DIAL                   = "dial";
const XY                     = "xy";
const INIT                   = "init";
const ADD_PLOT               = "add_plot";
const GRID                   = "grid";  
const ENABLE_STATUS          = "enable_status";

const PLOT_PANEL_INDEX       = "plot_panel_index";
const PLOT_INDEX             = "plot_index";
const PLOT_TITLE             = "plot_title";
const PLOT_NAME              = "plot_name";
const X_AXIS_NAME            = "x_axis_name";
const Y_AXIS_NAME            = "y_axis_name";
const ENABLE_LINES           = "enable_lines";
const LINE_WIDTH             = "line_width";
const ENABLE_SHAPES          = "enable_shapes";
const ENABLE_AUTOSCALE       = "enable_autoscale";
const MIN_SCALE_VALUE        = "min_scale_value";
const MAX_SCALE_VALUE        = "max_scale_value";
const MAX_AGE_SECONDS        = "max_age_seconds";
const ENABLE_LOG_Y_AXIS      = "enable_log_y_axis";
const ENABLE_ZERO_ON_X_SCALE = "enable_zero_on_x_scale";
const ENABLE_ZERO_ON_Y_SCALE = "enable_zero_on_y_scale";
const FRAME_TITLE            = "frame_title";
const ENABLE_LEGEND          = "enable_legend";
const TICK_COUNT             = "tick_count";
const CLEAR                  = "clear";
const REPLOT                 = "replot";
const DELIM_A                = ":";
const TIMESTAMP_DELIM        = ";";
const PLOT_VALUES            = "plot_values";
const PLOT_FILENAME          = "plot_filename";
const SAVE_MS                = "save_ms";
const LAST_SAVE_MS           = "last_save_ms";

const PLOT_ATTR_NAME               = "plot_name";
const PLOT_ATTR_X_AXIS_NAME        = "x_axis_name";
const PLOT_ATTR_Y_AXIS_NAME        = "y_axis_name";
const PLOT_ATTR_ENABLE_LINES       = "enable_lines";
const PLOT_ATTR_LINE_WIDTH         = "line_width";
const PLOT_ATTR_ENABLE_SHAPES      = "enable_shapes";
const PLOT_ATTR_ENABLE_AUTOSCALE   = "enable_autoscale";
const PLOT_ATTR_MIN_SCALE          = "min_scale_value";
const PLOT_ATTR_MAX_SCALE          = "max_scale_value";
const PLOT_ATTR_MAX_AGE_SECS       = "max_age_seconds";
const PLOT_ATTR_ENABLE_LOG_Y_SCALE = "enable_log_y_axis";
const PLOT_ATTR_ZERO_ON_X_SCALE    = "enable_zero_on_x_scale";
const PLOT_ATTR_ZERO_ON_Y_SCALE    = "enable_zero_on_y_scale";
const PLOT_ATTR_TICK_COUNT         = "tick_count";

const JSON_GLOBAL_CONFIG_FILE      = "global_config.json";
const JSON_FILE_LIST_FILENAME      = "filelist.json";

//Define default global options for plotly here
var GlobalDict                    = {
    paper_bgcolor: 'white',
    plot_bgcolor: 'white'
}

var   fileDictList                = {};
var   saveTimer                   = null;

/**
 * @brief Define the interface that we require from all plot panels
 *        As Javascript does not support directly we define a set 
 *        methods that if called (not overrideen by subclasses) 
 *        that will throw errors.
 **/
class PlotPanelInterface {

      constructor(uo, plotPanelIndex) {
          this.uo=uo;
          this.plotPanelIndex = plotPanelIndex;
          this.attributeHash={};
          this.setAttribute(PLOT_PANEL_INDEX, plotPanelIndex);
      } 

      /**
       * @brief Save the state of plot panel to a file.
       *        The file will have a prefix of the plot panel number and
       *        an extension of json.
       **/        
      save() {
          var plotName = this.attributeHash[PLOT_NAME];
          var plotPanelIndex = this.attributeHash[PLOT_PANEL_INDEX];
          if( plotPanelIndex != null && plotName != null ) {
              var filename = ""+plotPanelIndex+"_"+plotName+".json";
              //Store the filename in the hashtable
              this.attributeHash[PLOT_FILENAME] = filename;
              LineProcessor.SaveDict(filename, this.attributeHash); 
          }
      }
      
      /**
       * @brief Set an attribute associated with a plot.
       * @param name The name of the attribute
       * @param value The value associated with the attribute.
       **/
      setAttribute(name, value) {
          this.attributeHash[name]=value;
          var jSON = JSON.stringify(this.attributeHash);
      }
      
      /**
       * @brief Get the attribte by name
       * @param name The name of the attribute.
       **/
      getAttribute(name) {
          return this.attributeHash[name];
      }
      
      //This can be called to add the plot
      addPlot() {
          throw "PlotPanelInterface.addPlot() not implemented.";
      }
      
      addPlotValue(xValue, yValue) {
          var plotValues = this.getAttribute(PLOT_VALUES);
          plotValues.push( [xValue, yValue] );
          this.save();
      }
      addPlotValue(plotIndex, xValue, yValue) {
          var plotValues = this.getAttribute(PLOT_VALUES);
          plotValues.push( [plotIndex, xValue, yValue] );
          this.save();
      }
      
      init() {
          throw "PlotPanelInterface.init() not implemented.";
      }
      
      getPlotCount() {
          throw "PlotPanelInterface.getPlotCount() not implemented.";
      }

      //clear/emtpy all the values in a plot.
      clear() {
          throw "PlotPanelInterface.clear() not implemented.";
      }

      //Restart a plot. Subsequent plot points added will replace the previous ones.
      //Note that the X axis values must alreay be present.
      replot() {
          throw "PlotPanelInterface.replot() not implemented.";
      }
  
}

/**
 * @brief Define the PlotPanel for time series plot panels
 **/
class TimeSeriesPlotPanel extends PlotPanelInterface {
      constructor(uo, plotIndex) {
          super(uo, plotIndex);
          uo.debug("Created a TimeSeriesPlotPanel instance.");
      }
      
      //This can be called to add the plot
      addPlot() {
          this.setAttribute(GRAPH, TIME);
          this.setAttribute(PLOT_VALUES, []);
      }
      
      init() {
          uo.debug("TODO: TimeSeriesPlotPanel.init().");
      }
      
      getPlotCount() {
          uo.debug("TODO: TimeSeriesPlotPanel.getPlotCount().");
      }

      //clear/emtpy all the values in a plot.
      clear() {
          uo.debug("TODO: TimeSeriesPlotPanel.clear().");
      }

      //Restart a plot. Subsequent plot points added will replace the previous ones.
      //Note that the X axis values must alreay be present.
      replot() {
          uo.debug("TODO: TimeSeriesPlotPanel.replot().");
      }
  
}


/**
 * @brief Define the PlotPanel for bar plot panels
 **/
class BarPlotPanel extends PlotPanelInterface  {
    
      constructor(uo, plotIndex) {
          super(uo, plotIndex);
          uo.debug("Created a BarPlotPanel instance.");
      }
      
      //This can be called to add the plot
      addPlot() {
          this.setAttribute(GRAPH, BAR);
          this.setAttribute(PLOT_VALUES, []);
      }
      
      //Not used
      init() {
          uo.debug("TODO: BarPlotPanel.init().");
      }
      
      getPlotCount() {
          uo.debug("TODO: BarPlotPanel.getPlotCount().");
      }

      //clear/emtpy all the values in a plot.
      clear() {
          uo.debug("TODO: BarPlotPanel.clear().");
      }

      //Restart a plot. Subsequent plot points added will replace the previous ones.
      //Note that the X axis values must alreay be present.
      replot() {
          uo.debug("TODO: BarPlotPanel.replot().");
      }
  
}


/**
 * @brief Define the PlotPanel for XY plot panels
 **/
class XYPlotPanel extends PlotPanelInterface {

        constructor(uo, plotIndex) {
          super(uo, plotIndex);
          uo.debug("Created a XYPlotPanel instance.");
        }
        
        //This can be called to add the plot
        addPlot() {
            this.setAttribute(GRAPH, XY);
            this.setAttribute(PLOT_VALUES, []);
        }
        
        init() {
            uo.debug("TODO: XYPlotPanel.init().");
        }
        
        getPlotCount() {
            uo.debug("TODO: XYPlotPanel.getPlotCount().");
        }

        //clear/emtpy all the values in a plot.
        clear() {
            uo.debug("TODO: XYPlotPanel.clear().");
        }

        //Restart a plot. Subsequent plot points added will replace the previous ones.
        //Note that the X axis values must alreay be present.
        replot() {
            uo.debug("TODO: XYPlotPanel.replot().");
        }
  
}


/**
 * @brief Define the PlotPanel for Dial plot panels
 **/
class DialPlotPanel extends PlotPanelInterface {
      constructor(uo, plotIndex) {
          super(uo, plotIndex);
          uo.debug("Created a DialPlotPanel instance.");
      }
      
      //This can be called to add the plot
      addPlot() {
          this.setAttribute(GRAPH, DIAL);
          this.setAttribute(PLOT_VALUES, []);
      }
      
      init() {
          uo.debug("TODO: DialPlotPanel.init().");
      }
      
      getPlotCount() {
          uo.debug("TODO: DialPlotPanel.getPlotCount().");
      }

      //clear/emtpy all the values in a plot.
      clear() {
          uo.debug("TODO: DialPlotPanel.clear().");
      }

      //Restart a plot. Subsequent plot points added will replace the previous ones.
      //Note that the X axis values must alreay be present.
      replot() {
          uo.debug("TODO: DialPlotPanel.replot().");
      }
  
}


class UO {
    constructor(debugEnabled) {
        this.debugEnabled=debugEnabled;
    }
    
    info(msg) {
        console.log("INFO:  "+msg);
    }
    
    warn(msg) {
        console.log("WARN:  "+msg);
    }
    
    error(msg) {
        console.log("ERROR: "+msg);
    }
    
    debug(msg) {
        if( this.debugEnabled ) {
            console.log("DEBUG: "+msg);
        }
    }
}

class LineProcessor {
    
    /**
     * @brief Constructor for the LineProcessor
     * @param uo A UO instance
     * @param plotIndex The index of the plot pane on the plot grid (0=top left)
     **/
    constructor(uo, plotIndex) {
        this.uo = uo;
        this.plotIndex = plotIndex;
        this.plotPanelInterface = null;
        this.plotInitialized = false;
        this.enableStatusMessages = false;
    }
    
    /**
     * @brief Extract a list of values from a received line of text.
     * @return A list of Number instances
     **/
    getValues(line) {
        var values = [];
        //If we have an X/Y value pair with the plot index
        if( line.indexOf(":") >= 0 ) {
            var elems2 = line.split(":");
            if( elems2.length == 3 ) {
                var plotIndexValue = Number(elems2[0]);
                var xValue = Number(elems2[1]);
                var yValue = Number(elems2[2]);
                values.push(plotIndexValue);
                values.push(xValue);
                values.push(yValue);                    
            }
        }
        else {
            values.push( Number(elem) );
        }
        return values;
    }
    
    
    /**
     * @brief Save the global dict to the global config file.
     **/
    saveGlobalDict() {
        LineProcessor.SaveDict(JSON_GLOBAL_CONFIG_FILE, GlobalDict);
    }

    /**
     * @brief Write a file.
     * @param filename The filename to write to.
     * @param contents The data to write to the file.
     * @return null
     **/
    static WriteFile(filename, contents) {
        console.log("WRITE FILE: "+filename);
        fs.writeFile(filename, contents, 'utf8', function(err) {
            if (err) {
                throw err;
            }
        });
    }
    
    /**
     * @brief Save the files held in the fileDictList to local folder.
     **/
    static SaveFiles() {
        var fileList = [];
        //var fileList = fileDictList.keys();
        for( var filename in fileDictList ) {
            fileList.push(filename);
            var json = JSON.stringify(fileDictList[filename], null, 2);
            LineProcessor.WriteFile(filename, json);
        }
        var json = JSON.stringify(fileList, null, 2);
        LineProcessor.WriteFile(JSON_FILE_LIST_FILENAME, json);
    }
    
    /**
     * @brief Save a dict to a JSON file.
     **/
    static SaveDict(filename, dict) {
        //Store the dict in another dict referenced by the filename.
        fileDictList[filename]=dict;
        //We save the files on a timer timeout and cancel any currectly running 
        //timer so that we reduce the fileio to a minimum
        if( saveTimer != null ) {
            clearTimeout(saveTimer);
        }
        saveTimer = setTimeout(function () {
            LineProcessor.SaveFiles();
        }, 250);
    }
    
    /**
     * @brief Process a line of data received from a netplot client.
     * @param null
     **/
    processLine(line) {

        if( line == null || line.length < 1 || line.startsWith("#") || line.startsWith("/") || (line.length == 1 && line[0] == '\n') )
        {
          //Ignore empty lines and comments
          return;
        } 

        //Strip return and line feed characters from the line
        if( line.endsWith('\r') ) {
            line = line.slice(0,-1);
        }
        if( line.endsWith('\n') ) {
            line = line.slice(0,-1);
        }

        uo.debug('line: <' + line + ">");

        if( line.startsWith(SET_PREFIX) ) {
            //Extract the name=value text
            var nameValueStr = line.substring(4);
            var nameValueList = nameValueStr.split("=");
            if( nameValueList.length == 2 && nameValueList[0].length > 0 && nameValueList[1].length > 0 ) {
                var name = nameValueList[0];
                var value = nameValueList[1];
                //PJA console.log("name:  "+name+", value="+value);
                if( name == GRAPH ) {
                    if( value == TIME ) {
                        this.plotPanelInterface = new TimeSeriesPlotPanel(uo, this.plotIndex);
                    }
                    else if ( value == BAR ) {
                        this.plotPanelInterface = new BarPlotPanel(uo, this.plotIndex);
                    }
                    else if ( value == XY ) {
                        this.plotPanelInterface = new XYPlotPanel(uo, this.plotIndex);
                    }
                    else if ( value == DIAL ) {
                        this.plotPanelInterface = new DialPlotPanel(uo, this.plotIndex);
                    }
                    else {
                        throw value+" is an unknown graph type.";
                    }
                }
                else if( name == GRID ) {
                    var attrList = value.split(",");
                    if( attrList.length == 2 ) {
                        var rows = Number(attrList[0]);
                        var columns = Number(attrList[1]);
                        if( rows*columns > this.maxPlotCount ) {
                            throw "Grid of "+rows+" by "+columns+" is invalid (max plot count="+this.maxPlotCount+")";
                        }
                        GlobalDict[GRID]=[rows,columns];
                        this.saveGlobalDict();
                        uo.debug('Rows: '+rows+', Cols: '+columns);
                    }
                    else {
                        throw value+" is an invalid grid dimension.";
                    }
                }
                else if( name == FRAME_TITLE ) {
                    GlobalDict[FRAME_TITLE]=value;
                    this.saveGlobalDict();
                    uo.info('Title: '+value);
                    uo.debug('Title: '+value);
                }
                else if( this.plotPanelInterface == null ) {
                    throw "Attempt to set an attribute before a plot type has been defined.";
                }
                else {
                    this.plotPanelInterface.setAttribute(name, value);
                }
            }        
        }
        else if( line == INIT ) {
            if( this.plotPanelInterface == null ) {
                throw "Attempt to init a graph before setting a graph type.";
            }
            else {
                this.plotPanelInterface.init();
                this.plotInitialized=true;
            }
        }    
        else if( line == ADD_PLOT ) {
            if( this.plotPanelInterface == null ) {
                throw "Attempt to add a plot before setting a graph type.";
            }
            else {
                this.plotPanelInterface.addPlot();
                uo.debug("Added plot: "+this.plotIndex);
                this.plotIndex++;
            }            
        }
        else if( line.startsWith(CLEAR) ) {
            var keyValueList = line.split(" ");
            if( keyValueList.length == 2 ) {
                var plotIndex = Number(keyValueList[1]);
                if( this.plotPanelInterface == null ) {
                    throw "Unable to clear plot "+plotIndex;
                }
                else {
                    this.plotPanelInterface.clear(plotIndex);
                }
                uo.info("Cleared plot: "+plotIndex);
            }
        }
        else if( line.startsWith(ENABLE_STATUS) ) {
            var keyValueList = line.split(" ");
            if( keyValueList.length == 2 ) {
                this.enableStatusMessages=true;
                if( keyValueList[1] == "0" ||
                    keyValueList[1] == "false" ||
                    keyValueList[1] == "no" ) {
                    this.enableStatusMessages=false;
                }
                uo.debug("EnableStatusMessages: "+this.enableStatusMessages);
            }
        }
        else if( line.startsWith(REPLOT) ) {
            var keyValueList = line.split(" ");
            if( keyValueList.length == 2 ) {
                var plotIndex = Number(keyValueList[1]);
                if( this.plotPanelInterface == null ) {
                    throw "Unable to re plot "+plotIndex;
                }
                else {
                    this.plotPanelInterface.replot(plotIndex);
                }
                uo.info("Replotted: "+plotIndex);
            }
        }
        //If we have some values to plot
        else if( this.plotPanelInterface != null ) {
            //If the plot has not been initialised yet
            if( this.plotInitialized == false )
            {
                throw "Cannot add values to plot as it has not been initialised yet.";
            }
            if( line.indexOf(DELIM_A) != -1 ) {
                if( line.indexOf(TIMESTAMP_DELIM) != -1 ) {
                    //If the line contains chars indicating it contains a time stamp
                    var elems = line.split(TIMESTAMP_DELIM);
                    if( elems.length >= 3 ) {
                        var plotIndex = Number(elems[0]);
                        var dateString = elems[1];
                        var yValue = Number(elems[2]);
                        var elems = line.split(TIMESTAMP_DELIM_2);
                        if( elems.length >= 7 ) {
                            var year        = Number(elems[0]);
                            var month       = Number(elems[1]);
                            var day         = Number(elems[2]);
                            var hour        = Number(elems[3]);
                            var minute      = Number(elems[4]);
                            var second      = Number(elems[5]);
                            var milliSecond = Number(elems[6]);         
                            var date = Date.parse(hour+":"+minute+":"+second+":"+milliSecond+" "+month+", "+day+", "+year);
                            this.plotPanelInterface.addPlotValue(plotIndex, date.getMilliseconds(), yValue);    
                        }
                    }
                }
                else {
                    var valueList = this.getValues(line);
                    for( var i=0 ; i<valueList.length ; i=i+3 ) {
                        this.plotPanelInterface.addPlotValue(valueList[i], valueList[i+1], valueList[i+2]);
                    }
                }
            }
            else
            {
                var valueList = this.getValues(line);
                var plotIndex = 0;
                for( var value in valueList ) {
                    this.plotPanelInterface.addPlotValue(plotIndex, value);
                    plotIndex++;
                }
            }
        }
    }
}


class ServerManager {
    
    /**
     * @brief Constructor for the Server Manager class.
     **/
    constructor(uo, firstPort, lastPort) {
        this.uo=uo;
        this.firstPort=firstPort;
        this.lastPort=lastPort;
        this.maxPlotCount=this.lastPort-this.firstPort+1;
    }

    /**
     * @brief Remove old json files whose names start with a number followed by 
     *        a '_' character. 
     **/
    static CleanLocalFiles() {    
        var path = process.cwd();
        fs.readdir(path, function(err, items) {
            for (var i=0; i<items.length; i++) {
                var file = items[i]
                if( file == JSON_GLOBAL_CONFIG_FILE ) {
                    fs.unlinkSync(file);
                    uo.info('Removed '+file);    
                }
                else if( file.endsWith(".json") ) {
                    var elems = file.split("_");
                    if( elems.length > 0 ) {
                        var plotIndex = Number(elems[0]);
                        if( !isNaN(plotIndex) ) {
                            fs.unlinkSync(file);
                            uo.info('Removed '+file);            
                        }
                    }
                }
            }
        });         
    }
    
    /**
     * @brief Start all the TCP server sockets for the required TCP listener ports.
     **/
    startListening() {
        for( var socketIndex = this.firstPort ; socketIndex <= this.lastPort ; socketIndex++ ) {
            var server = net.createServer();
            //We need to pass the callback on the server instance
            server.handleRXData = this.handleRXData
            server.listen(socketIndex, HOST);
            uo.info("Netplot server on "+HOST+":"+(HOST , socketIndex ));
            server.on('connection', function(sock) {
                uo.info('CONNECTED: ' + sock.remoteAddress + ':' + sock.localPort);
                //Send greeting msg to client
                sock.write("netplot_version="+NETPLOT_VERSION+"\n");
                //We clean up files for first connection
                if( sock.localPort == BASE_TCP_PORT ) {
                    ServerManager.CleanLocalFiles();
                }
                var plotIndex = sock.localPort-BASE_TCP_PORT;              
                var lineProcessor = new LineProcessor(uo, plotIndex);
                //uo.info('sock.localPort: ' +sock.localPort);
                sock.on('data', function(data) {
                    //Handle the data received on the socket
                    if( data.length > 0 ) {
                        var rxStr = String.fromCharCode.apply(null, data);
                        var rxLines = rxStr.split("\n")
                        for( var index=0 ; index<rxLines.length ; index++ ) {
                            lineProcessor.processLine(rxLines[index]);             
                            //Acnowledge the recipt of the line of text from the client
                            sock.write("OK");   
                        }
                    }
                    
                });
            });
        }
    }

}

uo = new UO(true);
serverManager = new ServerManager(uo, BASE_TCP_PORT, BASE_TCP_PORT+TCP_PORT_COUNT-1);
serverManager.startListening();
const FILE_LIST_FILE                = 'filelist.json';
const GLOBAL_CONFIG_JSON_FILE       = "global_config.json";

//netplot attributes
const FRAME_TITLE_PARAM             = "frame_title";
const GRID_PARAM                    = "grid";

const GRAPH                         = "graph";
const TIME                          = "time";
const BAR                           = "bar";
const DIAL                          = "dial";
const XY                            = "xy";

const PLOT_LAYOUT_PAPER_BGCOLOR     = 'paper_bgcolor';
const PLOT_LAYOUT_PLOT_BGCOLOR      = 'plot_bgcolor';
    
const PLOT_TITLE                    = "plot_title";
const PLOT_NAME                     = "plot_name";
const PLOT_X_AXIS_NAME              = "x_axis_name";
const PLOT_Y_AXIS_NAME              = "y_axis_name";
const ENABLE_LOG_Y_AXIS             = "enable_log_y_axis"; //true or false
const PLOT_VALUES                   = "plot_values";
const LINE_WIDTH                    = "line_width";

const PLOT_DIV_NAME                 = "plotDiv";

var ReceivedFileList = false;
var PlotFileList     = [];
var FileDictList     = {};

var plotTraces = [];
var plotLayout = {
    margin: {
      l: 60,
      r: 60,
      b: 60,
      t: 30,
      pad: 2
  },
  hovermode:'closest'
};

var myPlot = document.getElementById(PLOT_DIV_NAME);

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

uo = new UO();
var globalDict = null;

class Plotter {
    //Responsible for plotting data received from the http server
    constructor(plotFileDict) {
        this.plotFileDict = plotFileDict;
        
        this.plotTitle              = null;
        this.gridXY                 = null;
        this.plotType               = undefined
        this.plotLayoutPaperBGColor = "white";
        this.plotLayoutPlotBGColor  = "white";
        
    }
    
    setGlobalLayoutParams() {
        //First pull the global params
        var json = FileDictList[GLOBAL_CONFIG_JSON_FILE];
        globalDict = JSON.parse(json);
        this.plotType = globalDict[GRAPH];
        
        console.log("*** Global layout params ***");
        for( var key in globalDict ) {
            if( key == GRID_PARAM ) {
                var gridXY = globalDict[GRID_PARAM];
                console.log("GRID: X="+gridXY[0]+", Y="+gridXY[1]);
                plotLayout["grid"]={rows: gridXY[0], columns: gridXY[1], pattern: 'independent'};
            }
            else if( key == FRAME_TITLE_PARAM ) {
                plotLayout["title"]=globalDict[key];
            }
            else {
                plotLayout[key]=globalDict[key];
            }
        }   
    }
    
    layoutAddAxis(plotAxisName, plotTitle, addXAxis, plotNumber, logAxis) {
        var axisPrefix = undefined;
        if( plotAxisName ) {
            if( addXAxis ) {
                axisPrefix = "xaxis";
            }
            else {
                axisPrefix = "yaxis";
            }
            if( plotNumber > 1 ) {
                axisPrefix=axisPrefix+plotNumber;
            }
            if( !(axisPrefix in plotLayout) ) {
                plotLayout[axisPrefix]={};
            }
            if( axisPrefix.startsWith("xaxis") ) {
                plotLayout[axisPrefix]['title']=plotAxisName+", "+plotTitle;
            }
            else {
                plotLayout[axisPrefix]['title']=plotAxisName;
            }
            if( logAxis == 'true' ) {
                plotLayout[axisPrefix]['type']="log";
            }
            else {
                plotLayout[axisPrefix]['type']="lin";
            }
            plotLayout[axisPrefix]['autorange']=true;
        }
    }
    
    addXY(trace, plotValues) {
        for( var plotIndex in plotValues ) {
            if( this.plotType == XY ) {
                trace.x.push(plotValues[plotIndex][1]);
                trace.y.push(plotValues[plotIndex][2]);
            }        
        }
    }
    
    addGridPos(trace, plotAreaNumber) {
        var xAxisGridPos = undefined;
        var yAxisGridPos = undefined;
        
        if( plotAreaNumber > 1 ) {
            trace['xaxis']="x"+plotAreaNumber;
            trace['yaxis']="y"+plotAreaNumber;
        }        
    }
    
    getPlotAreaAndID(filename) {
        var plotArea = undefined;
        var plotID = undefined;
        
        var elems = filename.split("_");
        if( elems.length == 2 ) {
            plotArea = Number(elems[0])+1;
            plotID = elems[1];
            plotID=plotID.replace(".json", "");
        } 
        
        return [plotArea, plotID];
    }
    
    getLegendID(jsonFilename) {
        return jsonFilename.replace(".json", "");
    }
    
    processData() {
//PJA console.log("PJA: processData() START Object.keys(FileDictList).length = " + Object.keys(FileDictList).length );
        
        this.setGlobalLayoutParams();
        
        var plotNumber = 1;
        var trace = undefined;
        for( var filename in FileDictList ) {
            if( filename == GLOBAL_CONFIG_JSON_FILE || filename == FILE_LIST_FILE ) {
                continue;
            }
            //console.log("PJA: 1--> filename="+filename)
            var json = FileDictList[filename];
            //PJA console.log("PJA: -----------------------> json="+json);

            var plotDict = JSON.parse(json);
            var keys = Object.keys(plotDict);
            /*
            //PJA
            for( var key in keys ) {
                console.log(key+" = <"+key+">" );
                console.log(key+" = <"+plotDict[key]+">" );
            }
            */
            var getPlotAreaAndID = this.getPlotAreaAndID(filename);
            var plotAreaNumber = getPlotAreaAndID[0];
            var plotID = getPlotAreaAndID[1];
            if( plotAreaNumber && plotID ) {
                var plotTitle  = plotDict[PLOT_TITLE];
                var plotName  = plotDict[PLOT_NAME];
                var plotXAxisName = plotDict[PLOT_X_AXIS_NAME];
                var plotYAxisName = plotDict[PLOT_Y_AXIS_NAME];
                var logYAxis = plotDict[ENABLE_LOG_Y_AXIS];
                var plotValues = plotDict[PLOT_VALUES];
                var lineWidth = plotDict[LINE_WIDTH];
                var legendID = this.getLegendID(filename);
                    
                /*    
                console.log("PJA: -----------------------> plotTitle     = "+plotTitle);
                console.log("PJA: -----------------------> plotName      = "+plotName);
                console.log("PJA: -----------------------> plotXAxisName = "+plotXAxisName);
                console.log("PJA: -----------------------> plotYAxisName = "+plotYAxisName);
                console.log("PJA: -----------------------> logYAxis      = "+logYAxis);
                console.log("PJA: -----------------------> plotValues    = "+plotValues);
                console.log("PJA: -----------------------> lineWidth     = "+lineWidth);
                console.log("PJA: -----------------------> legendID      = "+legendID);
                */
                
                this.layoutAddAxis(plotXAxisName, plotTitle, true,  plotAreaNumber, false);
                this.layoutAddAxis(plotYAxisName, plotTitle, false, plotAreaNumber, logYAxis);

                trace = {
                    name: legendID,
                    //name: plotName,
                    showlegend: false,
                    type: '',
                    x: [],
                    y: [],    
                };
                
                if( lineWidth == '1' ) {
                    trace['line']= {width: 0.5};
                }
                else {
                    trace['line']= {
                            dash: 'dash',
                            width: 2
                    };
                }

                this.addGridPos(trace, plotAreaNumber);
                this.addXY(trace, plotValues);
                plotTraces.push(trace);
                plotNumber = plotNumber + 1;
            }
        }
        //Debug data, slows down plotting
        console.log(plotLayout); 
        
        Plotly.newPlot(PLOT_DIV_NAME, plotTraces, plotLayout);
    }


}

class NetplotFileReader {
    //Responsible for reading data from the http server    
    constructor() {
        this.rxFileCount = 0;
    }
    
    read() {
        this.getFileFromServer(FILE_LIST_FILE);
        return FileDictList;
    }
    
    getFileFromServer = function (filename) {
      PlotFileList = [];
      $.ajax({
        url:filename,
        success: function (data){
          var fileList = data.toString().split(",");
          for( var index in fileList ) {
              PlotFileList.push(fileList[index]);
          }
          netplotFileReader.processFiles();
        },
        complete: function (data) {
            ReceivedFileList = true;
        }
      });
    }
    
    processFiles = function () {
        for( var index in PlotFileList ) {
            netplotFileReader.processFile(PlotFileList[index]);
        }
    }
    
    processFile = function (filename) {
      //console.log("PJA ProcessFile: "+filename);
      $.ajax({
        url:filename,
        success: function (data) {
            var jsonData = JSON.stringify(data);
            FileDictList[filename]=jsonData;
            //If we have received all the files
            if( ReceivedFileList && PlotFileList.length == Object.keys(FileDictList).length ) {
                var plotter = new Plotter(FileDictList);
                plotter.processData();
            }
        }
      });
    }
    
}
var netplotFileReader = new NetplotFileReader();
netplotFileReader.read();

console.log("PJA START");
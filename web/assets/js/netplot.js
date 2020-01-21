const INPUT_FILENAME        = 'netplot_commands.txt';

const FRAME_TITLE_CMD       = "set frame_title";
const GRID_CMD              = "set grid";
const GRAPH                 = "set graph";
const INIT                  = "init";
const ENABLE_STATUS         = "enable_status 1";

const GLOBAL_CMD_LIST       = [FRAME_TITLE_CMD, GRID_CMD, GRAPH, INIT, ENABLE_STATUS];

const INIT_CMD              = "init";
const ENABLE_STATUS_CMD     = "enable_status";

const XY_GRAPH_TYPE         = "xy";
const TIME_GRAPH_TYPE       = "time";

const PLOT_GRID_CMD         = "set plot_grid";
const PLOT_TITLE_CMD        = "set plot_title";
const PLOT_NAME_CMD         = "set plot_name";
const PLOT_X_AXIS_NAME_CMD  = "set x_axis_name";
const PLOT_Y_AXIS_NAME_CMD  = "set y_axis_name";
const ENABLE_LINES_CMD      = "set enable_lines";
const LINE_WIDTH_CMD        = "set line_width";
const ENABLE_SHAPES_CMD     = "set enable_shapes";
const ENABLE_AUTOSCALE_CMD  = "set enable_autoscale";
const MIN_SCALE_VALUE_CMD   = "set min_scale_value";
const MAX_SCALE_VALUE_CMD   = "set max_scale_value";
const MAX_AGE_SECONDS_CMD   = "set max_age_seconds";
const ENABLE_LOG_Y_AXIS_CMD = "set enable_log_y_axis";
const ENABLE_ZERO_ON_X_AXIS = "set enable_zero_on_x_scale";
const ENABLE_ZERO_ON_Y_AXIS = "set enable_zero_on_y_scale";
const TICK_COUNT_CMD        = "set tick_count";

const PLOT_CMD_LIST         = [PLOT_GRID_CMD, PLOT_TITLE_CMD, PLOT_NAME_CMD, PLOT_X_AXIS_NAME_CMD, PLOT_Y_AXIS_NAME_CMD,
                               ENABLE_LINES_CMD, LINE_WIDTH_CMD, ENABLE_SHAPES_CMD, ENABLE_AUTOSCALE_CMD,
                               MIN_SCALE_VALUE_CMD, MAX_SCALE_VALUE_CMD, MAX_AGE_SECONDS_CMD, 
                               ENABLE_LOG_Y_AXIS_CMD, ENABLE_ZERO_ON_X_AXIS, ENABLE_ZERO_ON_Y_AXIS,
                               TICK_COUNT_CMD];

const ADD_PLOT_CMD          = "add_plot";
const PLOT_DIV_NAME         = "plotDiv";
const STATUS_BAR            = "statusBar";

var newPlot = document.getElementById(PLOT_DIV_NAME);
var statusBar = document.getElementById(STATUS_BAR);

var plotNameList = [];

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

var clickCount = 0;

class PlotConfig {
    constructor() {
        this.plotGridIndex     = 0;
        this.plotTraceIndex    = 0;
        this.plotTitle         = "";
        this.plotName          = "";
        this.plotXAxisName     =  "";
        this.plotYAxisName     = "";
        this.enableLines       = true;   //Not used in web interface
        this.lineWidth         = 1;
        this.enableShapes      = false;  //Not used in web interface
        this.enableAutoScale   = true;
        this.minScaleValve     = 0;
        this.maxScaleValve     = 0;
        this.maxAgeSeconds     = 0;      //Not used in web interface
        this.enableLogYAxis    = false;
        this.enableZeroOnXAxis = false;  //Not used in web interface
        this.enableZeroOnYAxis = false;  //Not used in web interface
        this.tickCount         = 0;      //Not used in web interface
    }
    
    incPlotTraceIndex () {
        this.plotTraceIndex=this.plotTraceIndex+1;
    }
    
    /**
     * @brief Handle a global command
     * @param cmd The command to be processed.
     * @param arg The argument to be processed.
     * @return Null
     **/
    handlePlotCmd(cmd, arg) {
        //uo.info("PJA: "+cmd+"="+arg);
        if( cmd.startsWith(PLOT_GRID_CMD) ) {
            this.plotGridIndex=Number(arg);
        }
        else if( cmd.startsWith(PLOT_TITLE_CMD) ) {
            this.plotTitle=arg;
        }
        else if( cmd.startsWith(PLOT_NAME_CMD) ) {
            this.plotName=arg;
            plotNameList.push(this.plotName);
        }
        else if( cmd.startsWith(PLOT_X_AXIS_NAME_CMD) ) {
            this.plotXAxisName=arg;
        }
        else if( cmd.startsWith(PLOT_Y_AXIS_NAME_CMD) ) {
            this.plotYAxisName=arg;
        }
        else if( cmd.startsWith(ENABLE_LINES_CMD) ) {
            if( arg == 'true') {
                this.enableLines=true;
            }
            else {
                this.enableLines=false;
            }
        }
        else if( cmd.startsWith(LINE_WIDTH_CMD) ) {
            this.lineWidth=Number(arg);
        }
        else if( cmd.startsWith(ENABLE_SHAPES_CMD) ) {
            if( arg == 'true') {
                this.enableShapes=true;
            }
            else {
                this.enableShapes=false;
            }
        }
        else if( cmd.startsWith(ENABLE_AUTOSCALE_CMD) ) {
            if( arg == 'true') {
                this.enableAutoScale=true;
            }
            else {
                this.enableAutoScale=false;
            }
        }
        else if( cmd.startsWith(MIN_SCALE_VALUE_CMD) ) {
            this.minScaleValve=Number(arg);
        }
        else if( cmd.startsWith(MAX_SCALE_VALUE_CMD) ) {
            this.maxScaleValve=Number(arg);
        }
        else if( cmd.startsWith(MAX_AGE_SECONDS_CMD) ) {
            this.maxAgeSeconds=Number(arg);
        }
        else if( cmd.startsWith(ENABLE_LOG_Y_AXIS_CMD) ) {
            if( arg == 'true') {
                this.enableLogYAxis=true;
            }
            else {
                this.enableLogYAxis=false;
            }
        }
        else if( cmd.startsWith(ENABLE_ZERO_ON_X_AXIS) ) {
            if( arg == 'true') {
                this.enableZeroOnXAxis=true;
            }
            else {
                this.enableZeroOnXAxis=false;
            }
        }
        else if( cmd.startsWith(ENABLE_ZERO_ON_Y_AXIS) ) {
            if( arg == 'true') {
                this.enableZeroOnYAxis=true;
            }
            else {
                this.enableZeroOnYAxis=false;
            }
        }        
        else if( cmd.startsWith(TICK_COUNT_CMD) ) {
            this.tickCount=Number(arg);
        }
    }

    show() {
        uo.info("-------------------------");
        uo.info("plotGridIndex     = "+this.plotGridIndex);
        uo.info("plotTitle         = "+this.plotTitle);
        uo.info("plotName          = "+this.plotName);
        uo.info("plotXAxisName     = "+this.plotXAxisName);
        uo.info("plotYAxisName     = "+this.plotYAxisName);
        uo.info("enableLines       = "+this.enableLines);
        uo.info("lineWidth         = "+this.lineWidth);
        uo.info("enableShapes      = "+this.enableShapes);
        uo.info("enableAutoScale   = "+this.enableAutoScale);
        uo.info("minScaleValve     = "+this.minScaleValve);
        uo.info("maxScaleValve     = "+this.maxScaleValve);
        uo.info("maxAgeSeconds     = "+this.maxAgeSeconds);
        uo.info("enableLogYAxis    = "+this.enableLogYAxis);
        uo.info("enableZeroOnYAxis = "+this.lineWidth);
        uo.info("lineWidth         = "+this.enableZeroOnYAxis);
        uo.info("tickCount         = "+this.tickCount);
    }
    
}

plotPlotConfig          = new PlotConfig();
    
class Netplot {
    
    //Responsible for plotting data received from the http server
    constructor() {
        this.rows                   = 0;
        this.cols                   = 0;
        this.graphType              = "";
        this.trace                  = 0;
        this.lastPlotGridIndex      = 0;

        this.plotLayoutPaperBGColor = "white";
        this.plotLayoutPlotBGColor  = "white";
        
        statusBar.ondblclick = this.statusBarDoubleClick;
    }
    
    /**
     * @brief Called when the status bar is double clicked to show the legends 
     *        page.
     **/
    statusBarDoubleClick() {
        window.location.href = "legends.html";
    }
    
    /**
     * @brief Break the netplot command into the command and value components.
     * @param netplotCmd The netplot command string.
     * @return A list containing
               0 = The command
               1 = The value (maybe None)
     */
    getCmdValue(netplotCmd) {           
        var cmd   = "";
        var value = "";

        netplotCmd=netplotCmd.replace('\n', '')
        var elems = netplotCmd.split("=");
        if( elems.length > 0 ) {
            cmd = elems[0];            
        }
        if( elems.length > 1 ) {
            value = elems[1];
        }
        
        return [cmd, value];    
    }

    /**
     * @brief Handle a global command
     * @param cmd The command to be processed.
     * @param arg The argument to be processed.
     * @return Null
     **/
    handleGlobalCmd(cmd, arg) {
        if( cmd.startsWith(GRID_CMD) ) {
            var elems = arg.split(',');
            if( elems.length == 2 ) {
                this.rows = Number(elems[0]);
                this.cols = Number(elems[1]);
                plotLayout["grid"]={rows: this.rows, columns: this.cols, pattern: 'independent'};
            }
        }
        else if( cmd.startsWith(FRAME_TITLE_CMD) ) {
            plotLayout["title"]=arg;
        }
        else if( cmd.startsWith(GRAPH) ) {
            //PJA TODO handle all graph types
            this.graphType = arg;
            plotLayout["graph"]=this.graphType;
            if( ![XY_GRAPH_TYPE, TIME_GRAPH_TYPE].includes(this.graphType) ) {
                throw this.graphType+" is currently unsupported on the web interface."
            }
        }
    }

    addPlotConfig() {
        var xAxisPrefix = "xaxis";
        var yAxisPrefix = "yaxis";
        
        if( this.trace ) {
            plotTraces.push(this.trace);
        }
        
        //Reset the plotPlotConfig.plotTraceIndex if we've moved to a new plot area
        if( this.lastPlotGridIndex != plotPlotConfig.plotGridIndex ) {
            plotPlotConfig.plotTraceIndex=0;
            this.lastPlotGridIndex=plotPlotConfig.plotGridIndex;
        }

        uo.info("Add Plot Trace")
        //plotPlotConfig.show();


        if( plotPlotConfig.plotGridIndex > 0 ) {
            xAxisPrefix=xAxisPrefix+(plotPlotConfig.plotGridIndex+1);
            yAxisPrefix=yAxisPrefix+(plotPlotConfig.plotGridIndex+1);
        }
    
        if( plotPlotConfig.plotXAxisName.length > 0  ) {
            if( !(xAxisPrefix in plotLayout) ) {
                plotLayout[xAxisPrefix]={};
            }
            plotLayout[xAxisPrefix]['title']=plotPlotConfig.plotXAxisName+", "+plotPlotConfig.plotTitle; 
            if( this.graphType == TIME_GRAPH_TYPE ) {
                plotLayout[xAxisPrefix]['type']="scatter";
            }
            else {
                plotLayout[xAxisPrefix]['type']="linear";
            }
            plotLayout[xAxisPrefix]['autorange']=true;
        }
        
        if( plotPlotConfig.plotYAxisName.length > 0  ) {
            if( !(yAxisPrefix in plotLayout) ) {
                plotLayout[yAxisPrefix]={};
            }
            plotLayout[yAxisPrefix]['title']=plotPlotConfig.plotYAxisName;
             
            if( plotPlotConfig.enableLogYAxis ) {
                plotLayout[yAxisPrefix]['type']="log";
            }
            else {
                plotLayout[yAxisPrefix]['type']="linear";
            }
            plotLayout[yAxisPrefix]['autorange']=true;
        }

        var traceLegend = ""+plotPlotConfig.plotGridIndex+"_"+plotPlotConfig.plotTraceIndex;

        this.trace = {
                name: traceLegend,
                showlegend: false,
                type: '',
                x: [],
                y: [],    
        };

        if( plotPlotConfig.lineWidth == '1' ) {
            this.trace['line']= {width: 0.5};
        }
        else {
            this.trace['line']= {
                    dash: 'dash',
                    width: 2
            };
        }

        if( plotPlotConfig.plotGridIndex >= 1 ) {
            this.trace['xaxis']="x"+(plotPlotConfig.plotGridIndex+1);
            this.trace['yaxis']="y"+(plotPlotConfig.plotGridIndex+1);
        }  
        
        plotPlotConfig.incPlotTraceIndex();
    }

    addPlotValues(valuesString) {
        var supported = false;
        var elems = valuesString.split(":");
        if( elems.length == 3 ) {        
            var plotIndex = Number(elems[0]);
            if( this.graphType == XY_GRAPH_TYPE ) {
                var xValue = Number(elems[1]);
                var yValue = Number(elems[2]);
                this.trace.x.push(xValue);
                this.trace.y.push(yValue);
            }
            else if( this.graphType == TIME_GRAPH_TYPE ) {
                var timeElems = elems[1].split(';')
                var xValue = timeElems[0]+"-"+timeElems[1]+"-"+timeElems[2]+" "+timeElems[3]+":"+timeElems[4]+":"+timeElems[5]+":"+timeElems[6];
                var yValue = Number(elems[2]);
                this.trace.x.push(xValue);
                this.trace.y.push(yValue);
            }
        }
        else {
            uo.info("Unsupported  VALUES STRING: "+valuesString);
        }
    }
    
    processCmd(netplotCmd) {
        //console.log("PJA netplotCmd = <"+netplotCmd+">");
        var response = this.getCmdValue(netplotCmd);
        var cmd = response[0];
        var value = response[1];
        
        if( GLOBAL_CMD_LIST.includes(cmd) ) {
            this.handleGlobalCmd(cmd, value);
        }
        else if( PLOT_CMD_LIST.includes(cmd) ) {
            plotPlotConfig.handlePlotCmd(cmd, value);
        }
        else if( cmd.startsWith(ADD_PLOT_CMD) ) {
            this.addPlotConfig()
        }
        else {
            this.addPlotValues(cmd);
        }
        
    }

    showPlot() {
        //Add the last trace to the list of traces.
        if( this.trace ) {
            plotTraces.push(this.trace);
        }

        console.log(plotLayout); 
        console.log(plotTraces); 
        Plotly.newPlot(PLOT_DIV_NAME, plotTraces, plotLayout);

        newPlot.on('plotly_click', function(data){
            var plotIndex = data.points[0].curveNumber;
            statusBar.value = plotNameList[plotIndex];
        });

    }

}

class NetplotFileReader {
    //Responsible for reading data from the http server    
    constructor() {
        this.rxFileCount = 0;
    }
    
    read() {
        this.getFileFromServer(INPUT_FILENAME);
    }
    
    getFileFromServer = function (filename) {
      $.ajax({
        url:filename,
        success: function (data){
          var netPlot = new Netplot();
          var netPlotCmdList = data.toString().split("\n");
          for( var index in netPlotCmdList ){
              netPlot.processCmd(netPlotCmdList[index]);
          }
          netPlot.showPlot();
        }
      });
    }
    
}
var netplotFileReader = new NetplotFileReader();
netplotFileReader.read();

console.log("PJA START");
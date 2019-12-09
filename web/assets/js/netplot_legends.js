const INPUT_FILENAME        = 'netplot_commands.txt';
const PLOT_GRID_CMD         = "set plot_grid";
const PLOT_NAME_CMD         = 'set plot_name';

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

var uo = new UO()

var legends = {}

var traceNumber = 0;

var lastPlotGridIndex = 0;

var legendsTable = document.getElementById('legendsTable');

class NetplotFileReader {

    //Responsible for reading data from the http server    
    constructor() {
        this.plotName = "";
        //Add title line
        legendsTable.innerHTML += '<tr><th>LEGEND</th><th>PLOT NAME</th></tr>';
    }
    
    read() {
        console.log("READ: "+INPUT_FILENAME);
        this.getFileFromServer(INPUT_FILENAME);
    }
    
    getFileFromServer = function (filename) {
      $.ajax({
        url:filename,
        success: function (data){
          if( data ) {
              var lines = data.split('\n');
              for( var index in lines ) {
                var line = lines[index];

                if( line.startsWith(PLOT_NAME_CMD) ) {
                    line.replace("\r", "");
                    line.replace("\n", "");
                    var elems = line.split("=");
                    if( elems.length  >= 2 ) {
                        this.plotName = elems[1];
                    }
                }
                else if( line.startsWith(PLOT_GRID_CMD) ) {
                   line.replace("\r", "");
                   line.replace("\n", "");
                   var elems = line.split("=");
                   if( elems.length  == 2 ) {
                       var plotGrid = Number(elems[1]);
                       //If the plot grid has changed we reset the trace number
                       if( plotGrid != lastPlotGridIndex ) {
                           traceNumber=0;
                       }

                       var legend = plotGrid+"_"+traceNumber;
                       legends[legend] = this.plotName;
                       uo.info(legend+" = "+this.plotName);

                       if( this.plotName ) {
                           legendsTable.innerHTML += '<tr><td >'+legend+'</td><td style="text-align:left">'+this.plotName+'</td></tr>';
                       }
                       
                       lastPlotGridIndex=plotGrid;
                   }
                   traceNumber=traceNumber+1;
                }
              }
          }
        }

      });
    }
    
}
var netplotFileReader = new NetplotFileReader();
netplotFileReader.read();


const INPUT_FILENAME        = "plot_data.json";
const PLOT_DIV_NAME         = "plotDiv";
const PLOTLAYOUT            = "plotlayout";
const PLOTTRACES            = "plottraces";

class DataPlotter {

    constructor(plotDict) {
        this.plotDict = plotDict;
    }

    plot() {
      var plotLayout = this.plotDict[PLOTLAYOUT]
      var plotTraces = this.plotDict[PLOTTRACES]
      //// DEBUG:
      //console.log(plotLayout)
      //console.log(plotTraces)
      Plotly.newPlot(PLOT_DIV_NAME, plotTraces, plotLayout);
    }
}

class PlotDataReader {
    //Responsible for reading the plot data file from the http server
    read() {
        this.getFileFromServer(INPUT_FILENAME);
    }

    getFileFromServer = function (filename) {
      $.ajax({
        url:filename,
        success: function (data){
          var dataPlotter = new DataPlotter(data);
          dataPlotter.plot();
        }
      });
    }

}

var plotDataReader = new PlotDataReader();
plotDataReader.read();

console.log("PJA START");

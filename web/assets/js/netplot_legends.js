const FILE_LIST_FILE                = 'filelist.json';

var PlotFileList;
var FileDictList     = {};

var legendsDiv = document.getElementById('legendsID');

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
            var plotParamDict = JSON.parse(jsonData);
            var plotName = plotParamDict['plot_name'];
            if( plotName ) {
                legendsDiv.innerHTML += '<li>' + filename + ':    ' + plotName + '</li>';
            }
        }
      });
    }
    
}
var netplotFileReader = new NetplotFileReader();
netplotFileReader.read();

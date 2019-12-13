const TABLE_ROWS_DIV = "tableRows";
const INPUT_FILENAME = "plot_list.txt"

var tableRowsHtml    = document.getElementById(TABLE_ROWS_DIV);

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

var htmlText = "";
        
class NetplotFileReader {

    //Responsible for reading data from the http server    
    constructor() {
    }
    
    read() {
        console.log("READ: "+INPUT_FILENAME);
        this.getFileFromServer(INPUT_FILENAME);
    }
    
    getFileFromServer = function (filename) {
      htmlText=""
      $.ajax({
        url:filename,
        success: function (data) {
            var lines = data.split("\n")
            htmlText = "";
            htmlText += "<div class=\"datagrid\">\n"
            htmlText += "  <table>\n"
            htmlText += "    <thead><tr><th>Available Plots</th></tr></thead>\n"
            htmlText += "    <tbody>\n"

            for( var index in lines ) {
                if( lines[index].length > 0 ) {
                    htmlText += "      <tr><td>"+"<a href=\""+lines[index]+"/index.html\">"+lines[index]+"</a>"+"</td><td>\n";
                }
            }
            
            htmlText += "    </tbody>\n"
            htmlText += "  </table>\n"
            htmlText += "</div>\n"
            
            uo.info("Available plots table HTML\n"+htmlText);
            tableRowsHtml.innerHTML += htmlText;
        }
      });
    }
    
}

function setTableRows() {
    var netplotFileReader = new NetplotFileReader();
    netplotFileReader.read();
}

window.onload = function(e) {
    setTableRows(true);
}
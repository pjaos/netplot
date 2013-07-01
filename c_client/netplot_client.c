/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <sys/socket.h>
#include <syslog.h>
#include <arpa/inet.h>
#include <string.h>
#include <unistd.h>
#include <netinet/in.h>
#include <netinet/tcp.h>

#include "netplot_client.h"

static struct _netplot_config   netplot_config; //This struct holds all netplot config
static char rx_buffer[RX_BUFFSIZE];             //AlL comands when received are loaded into this buffer
static int  rx_byte_count;                      //The number of bytes curently loaded into the rx buffer

//----------------User IO functions start ----------------

/**
 * show an information level message to the user
 **/
void info(const char *fmt, ...)
{
    char line[1024];
    va_list     ap;
    va_start(ap, fmt);
    vsnprintf(line, 1024, fmt, ap);
    va_end(ap);
    printf("INFO:  %s\n",line);
}

/**
 * show an error level message to the user
 **/
void error(const char *fmt, ...)
{
    char line[1024];
    va_list     ap;
    va_start(ap, fmt);
    vsnprintf(line, 1024, fmt, ap);
    va_end(ap);
    printf("ERROR: %s\n",line);
}

/**
 * show a fatal level message to the user, and exit with an error code
 **/
void
fatal(const char *fmt, ...)
{
    char line[1024];
    va_list     ap;
    va_start(ap, fmt);
    vsnprintf(line, 1024, fmt, ap);
    va_end(ap);
    printf("ERROR: %s\n",line);
    exit(-1);
}

/**
 * show an debug level message to the user, if netplot client debugging is enabled
 **/
void debug(const char *fmt, ...)
{
    if( !netplot_config.debug_enabled )
    {
        return;
    }
    char line[1024];
    va_list     ap;
    va_start(ap, fmt);
    vsnprintf(line, 1024, fmt, ap);
    va_end(ap);
    printf("DEBUG: %s\n",line);
}

//---------------- User IO functions end ----------------

//---------------- Start of static functions ----------------

/**
 * Remove the end of line caharcters from a line of text.
 * The return and line feed characters are set to 0.
 */
static void remove_eol(char *line)
{
    int i;

    int len = strlen(line);

    for( i=0 ; i<len ; i++ )
    {
        if( line[i] == '\n' || line[i] == '\r' )
        {
            line[i]=0;
        }
    }
}

/**
 * Send a commnd to the netplot GUI server
 * returns 0 = Sent command
 *        -1 = No conection to server
 *        -2 = Failed send to server
 **/
static int send_command(int server_connection_index, char *cmd)
{
    debug("TX: socket: %d <%s>",netplot_config.sockets[server_connection_index], cmd);
    int cmd_len = strlen(cmd);
    //Is the socket connected
    if( netplot_config.sockets[server_connection_index] == 0 )
    {
        return -1;
    }
    if (send(netplot_config.sockets[server_connection_index], cmd, cmd_len, 0) != cmd_len)
    {
        return -2;
    }
    return 0;
}

/**
 * Receive a response from the netplot server.
 * May be called after sending a comand to receive a response.
 * returns   : The number of characters received (also loaded into the global var rx_byte_count).
 *        -1 : Failed to received response
 *        -2 : Read 0 bytes
 *        -3 : Error received from server
 **/
static int receive_response(int server_connection_index)
{
    memset(rx_buffer, 0 , RX_BUFFSIZE);
    rx_byte_count = recv(netplot_config.sockets[server_connection_index], rx_buffer, RX_BUFFSIZE, 0);
    if( rx_byte_count < 1 )
    {
        return -1;
    }
    debug("RX: socket: %d <%s>",netplot_config.sockets[server_connection_index], rx_buffer);
    if( rx_byte_count == 0 )
    {
        return -2;
    }
    if( strstr(rx_buffer, "ERROR") != NULL )
    {
        return -3;
    }
    return rx_byte_count;
}

/**
 * Receive a repsonse from the server and expect it tyo by OK.
 * Returns 0 : ok
 *       -ve : Failed
 **/
static int receive_ok(int server_connection_index)
{
    int rc = receive_response(server_connection_index);
    if( rc < 0 )
    {
        return rc;
    }
    if( strstr(rx_buffer, "OK") != NULL )
    {
        return 0;
    }
    return -1;
}


/**
 * This is called just after the socket is conected to the netplot server
 * in order to get the server version
 * Returns 0 : ok
 *       -ve : Failed
 *
 **/
static int load_server_version(int sock)
{
    int rc=-1;
    char ver_str[10];
    if( receive_response(sock) < 1 )
    {
        return rc;
    }
    if( strstr(rx_buffer, "netplot_version=") > 0 )
    {
        strncpy(ver_str,rx_buffer+16,10);
        remove_eol(ver_str);
        netplot_config.serverVersion=atof(ver_str);
        rc=0;
    }
    return rc;
}

/**
 * Send a command and expect ok to be the repsonse.
 * Returns 0 : ok
 *       -ve : Failed
 **/
static int send_command_expect_ok(int server_connection_index, char *cmd)
{
    if( send_command(server_connection_index, cmd) )
    {
        return -1;
    }
    if( receive_ok(server_connection_index) < 0 )
    {
        return -1;
    }
    return 0;
}

/**
 * Add values to the cache, so that the values can be sent later all at once.
 * The cache is an aresa of memory that is used to store the values (as a string)
 * that will be sent at some future time to the server when the cache is enabled.
 * The netplot_update() function can be called to send the data in the cache to
 * the server. After the netplot_update() function has been called the cache will
 * be empty. The cache is disabled by default.
 *
 * Returns 0 : ok
 *       -ve : Failed
 **/
static int add_string_to_cache(char *str)
{
    int     newMemLen, oldMemLen;

    //calc how much memory we need to store this string
    newMemLen = strlen(str)+1;

    //If the cache is currently empty
    if(netplot_config.cachePtr == NULL )
    {
        //Claim enough memory to store this string
        netplot_config.cachePtr=calloc(newMemLen, sizeof(char) );
        if( netplot_config.cachePtr == NULL )
        {
            return -1;
        }
        memcpy(netplot_config.cachePtr, str, newMemLen);
    }
    else
    {
        //Take a copy of the cache ptr
        netplot_config.oldCachePtr=netplot_config.cachePtr;
        //Determine how much mem this currently points to
        //2 = line feed + null char
        oldMemLen=strlen(netplot_config.oldCachePtr)+2;
        //Claim a new chunk of memory that will hold the old cache and the new value
        netplot_config.cachePtr=calloc(oldMemLen+newMemLen, sizeof(char) );
        if( netplot_config.cachePtr == NULL )
        {
            return -1;
        }
        //copy the the data from the old cache
        memcpy(netplot_config.cachePtr, netplot_config.oldCachePtr, oldMemLen);
        //free the old cache memory
        free(netplot_config.oldCachePtr);
        netplot_config.oldCachePtr=NULL;
        //Add the new value to the cache
        strncat(netplot_config.cachePtr, str, oldMemLen+newMemLen);
    }
    return 0;
}

/**
 * Add values to the cache, so that the values can be sent later all at once.
 * Returns 0 : ok
 *       -ve : Failed
 **/
static int add_floats_to_cache(float *values, int value_count)
{
    char val_buf[CMD_BUFFER_SIZE];
    int i;
    int rc=0;
    int first=1;

    for( i=0 ; i<value_count ; i++ )
    {
        memset(val_buf, 0 , CMD_BUFFER_SIZE);
        //If cache is empty
        if( first )
        {
            snprintf(val_buf, CMD_BUFFER_SIZE, "%f", values[i]);
        }
        else
        {
            snprintf(val_buf, CMD_BUFFER_SIZE, ",%f", values[i]);
        }
        rc = add_string_to_cache(val_buf);
        if( rc < 0 )
        {
            break;
        }
        first=0;
    }
    if( rc == 0 )
    {
        add_string_to_cache("\n");
    }
    return rc;
}

/**
 * Empty the cache and free the memory is uses
 **/
static void empty_cache()
{
    if( netplot_config.cachePtr != NULL )
    {
        free(netplot_config.cachePtr);
        netplot_config.cachePtr=NULL;
    }
}

/**
 * If cache is empty return 1, exlse return 0
 **/
static int is_cache_empty()
{
    if( netplot_config.cachePtr == NULL)
    {
        return 1;
    }
    return 0;
}

/**
 * Get the data from the cache
 **/
static char *get_cache_data()
{
    return netplot_config.cachePtr;
}

/**
 * Get the command count.
 * Each command is terminated by a \n charatcer so we count these.
 **/
static int get_cmd_count(char *cmds)
{
    int len=strlen(cmds);
    int i;
    int cmd_count=0;

    for( i=0 ; i<len ; i++ )
    {
        if( cmds[i] == '\n' )
        {
            cmd_count++;
        }
    }
    return cmd_count;
}

//---------------- End of static functions ----------------





/**
 * Return the last message received from the netplot server.
 * If an eror has just occurred then this will be the error message.
 * Returns The last message received from the netplot server
 **/
char *netplot_get_last_message()
{
    return rx_buffer;
}

/**
 * Load the plot_config with the default values
 **/
void load_default_plotConfig(struct _plot_config *plot_config)
{
    strncpy(plot_config->plotName, "", MAX_STR_LEN);
    strncpy(plot_config->xAxisName,"", MAX_STR_LEN);
    strncpy(plot_config->yAxisName,"", MAX_STR_LEN);
    plot_config->enableLines=1;
    plot_config->enableShapes=1;
    plot_config->enableAutoScale=1;
    plot_config->minScaleValue=0;
    plot_config->maxScaleValue=1E6;
    plot_config->maxAgeSeconds=3600;
    plot_config->enableLogYAxis=0;
    plot_config->enableZeroOnXAxis=1;
    plot_config->enableZeroOnYAxis=1;
    plot_config->tickCount=0;
}

/**
 * Init variables internal to the netplot client
 **/
void netplot(char *address, int base_port, int plot_count, unsigned char debug_enabled)
{
    strncpy(netplot_config.hostAddress, address, MAX_STR_LEN);
    netplot_config.port=base_port;
    netplot_config.serverVersion=0;
    netplot_config.cacheEnabled=0;
    netplot_config.debug_enabled=debug_enabled;
    netplot_config.oldCachePtr=NULL;
    netplot_config.cachePtr=NULL;
    netplot_config.cache_item_count=0;
    //Allocate the memory to hold all socket descriptors
    netplot_config.sockets = calloc(plot_count, sizeof(int));
    netplot_config.socket_count=plot_count;
}

/**
 * Display the netplot_client variables to the user
 **/
void netplot_show_vars()
{
    int     i;
    info("NETPLOT CLIENT VARIABLE STATES");
    info("netplot_config.cacheEnabled         = %d",netplot_config.cacheEnabled);
    info("netplot_config.cache_item_count     = %d",netplot_config.cache_item_count);
    if( netplot_config.cache_item_count > 0 )
    {
        info((char *)netplot_config.cachePtr);
    }
    info("netplot_config.cachePtr             = %d",netplot_config.cachePtr);
    info("netplot_config.debug_enabled        = %d",netplot_config.debug_enabled);
    info("netplot_config.hostAddress          = %s",netplot_config.hostAddress);
    info("netplot_config.port                 = %d",netplot_config.port);
    info("netplot_config.serverVersion        = %f",netplot_config.serverVersion);

    info("netplot_config.socket_count         = %d",netplot_config.socket_count);
    for( i=0 ; i< netplot_config.socket_count ; i++ )
    {
        //If not zero then display
        if( netplot_config.sockets[i] != 0 )
        {
            info("netplot_config.socket[%d] fd         = %d",i,netplot_config.sockets[i]);
        }
    }
}

/**
 * Get the version of the netplot server. Should be called only once connected to the server.
 * @return The server version
 **/
float netplot_get_server_version() {
    return netplot_config.serverVersion;
}

/**
 * Init variables internal to the netplot client to their default values
 **/
void netplot_defaults()
{
    netplot("127.0.0.1", DEFAULT_BASE_PORT, DEFAULT_PLOT_COUNT, 1);
}


/**
 * Build all the required connections to the netplot server
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_connect()
{
    int                 socket_count;
    int                 sock;
    int                 tcp_port=netplot_config.port;
    struct sockaddr_in  server_addr;
    int                 rc;
    int                 flag = 1;

    //Loop to connect all sockets to the netplot server
    for( socket_count=0 ; socket_count<netplot_config.socket_count; socket_count++ )
    {
        if ((sock = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP)) < 0)
        {
            error("Failed to create socket");
            return -1;
        }
        server_addr.sin_family = AF_INET;
        server_addr.sin_addr.s_addr = inet_addr(netplot_config.hostAddress);
        server_addr.sin_port = htons(tcp_port);
        if (connect(sock, (struct sockaddr *) &server_addr, sizeof(server_addr)) < 0)
        {
            error("Failed to connect to %s:%d",netplot_config.hostAddress,tcp_port);
            return -1;
        }
        //Turn off nagle algorithm as we want timely delivery of data accross a network
        // in ordere to keep a plot up to date.
        setsockopt( sock, IPPROTO_TCP, TCP_NODELAY, (char *)&flag, sizeof(flag) );

        //store the connected socket
        netplot_config.sockets[socket_count]=sock;
        rc =load_server_version(socket_count);
        if( rc < 0 )
        {
            return rc;
        }
        tcp_port++;
    }
    return 0;
}

/**
 * Disconnect all connections to the netplot server
 **/
void netplot_disconnect()
{
    int socket_count;
    //Loop to connect all sockets to the netplot server
    for( socket_count=0 ; socket_count<netplot_config.socket_count; socket_count++ )
    {
        if( netplot_config.sockets[socket_count] != 0 )
        {
            close(netplot_config.sockets[socket_count]);
            netplot_config.sockets[socket_count]=0;
        }
    }
}

/**
 * Set the number of graphs and their layout.
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_set_grid(int rows, int cols)
{
    char buffer[CMD_BUFFER_SIZE];

    memset(buffer, 0 , CMD_BUFFER_SIZE);
    snprintf(buffer, CMD_BUFFER_SIZE, "set grid=%d,%d\n", rows, cols);
    //The init grid command is always sent on the first socket
    return send_command_expect_ok(0, buffer);
}

/**
 * Set the window title.
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_set_window_title(char *window_title)
{
    char buffer[CMD_BUFFER_SIZE];

    memset(buffer, 0 , CMD_BUFFER_SIZE);
    snprintf(buffer, CMD_BUFFER_SIZE, "set frame_title=%s\n", window_title);
    //Set the window title on first socket
    return send_command_expect_ok(0, buffer);
}

/**
 * enable/disable the display of the legend on the plots.
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_set_chart_legend_enabled(int server_connection_index, int enabled)
{
    char buffer[CMD_BUFFER_SIZE];

    memset(buffer, 0 , CMD_BUFFER_SIZE);
    if( enabled )
    {
        strncpy(buffer, "set enable_legend=true\n", CMD_BUFFER_SIZE);
    }
    else
    {
        strncpy(buffer, "set enable_legend=false\n", CMD_BUFFER_SIZE);
    }
    return send_command_expect_ok(server_connection_index, buffer);
}

/**
 * Initialise the plot. May be called to clear a plot.
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_init(int server_connection_index)
{
    return send_command_expect_ok(server_connection_index, "init\n");
}

/**
 * set the type of plot
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_set_plot_type(int server_connection_index, int plot_type, char *plot_title)
{
    int rc;
    char buffer[CMD_BUFFER_SIZE];

    memset(buffer, 0 , CMD_BUFFER_SIZE);
    if( plot_type == PLOT_TYPE_TIME )
    {
        strncpy(buffer, "set graph=time\n", CMD_BUFFER_SIZE);
    }
    else if( plot_type == PLOT_TYPE_BAR )
    {
        strncpy(buffer, "set graph=bar\n", CMD_BUFFER_SIZE);
    }
    else if( plot_type == PLOT_TYPE_XY )
    {
        strncpy(buffer, "set graph=xy\n", CMD_BUFFER_SIZE);
    }
    else if( plot_type == PLOT_TYPE_DIAL )
    {
        strncpy(buffer, "set graph=dial\n", CMD_BUFFER_SIZE);
    }
    else
    {
        return -2;
    }
    rc = send_command_expect_ok(server_connection_index, buffer);
    if( rc == 0 )
    {
        if( strlen(plot_title) > 0 )
        {
            memset(buffer, 0 , CMD_BUFFER_SIZE);
            snprintf(buffer, CMD_BUFFER_SIZE, "set plot_title=%s\n",plot_title);
            rc = send_command_expect_ok(server_connection_index, buffer);
        }
        if( rc == 0 )
        {
            rc = netplot_init(server_connection_index);
            if( rc == 0 )
            {
                netplot_enable_status_messages(1);
            }
        }
    }
    return rc;
}

/**
 * enable/disable status messages in the GUI.
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_enable_status_messages(int enabled)
{
    char buffer[CMD_BUFFER_SIZE];

    memset(buffer, 0 , CMD_BUFFER_SIZE);
    if( enabled )
    {
        strncpy(buffer, "enable_status 1\n", CMD_BUFFER_SIZE);
    }
    else
    {
        strncpy(buffer, "enable_status 0\n", CMD_BUFFER_SIZE);
    }
    return send_command_expect_ok(0, buffer);
}

/**
 * Add a plot to the netplot server from the attributed of the plot_config.
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_add_plot(int server_connection_index, struct _plot_config pc)
{
    char buffer[CMD_BUFFER_SIZE];
    int rc=0;

    memset(buffer, 0 , CMD_BUFFER_SIZE);
    snprintf(buffer, CMD_BUFFER_SIZE, "set plot_name=%s\n", pc.plotName);
    rc = send_command_expect_ok(server_connection_index, buffer);
    if( rc == 0 )
    {
        memset(buffer, 0 , CMD_BUFFER_SIZE);
        snprintf(buffer, CMD_BUFFER_SIZE, "set x_axis_name=%s\n", pc.xAxisName);
        rc = send_command_expect_ok(server_connection_index, buffer);
    }
    if( rc == 0 )
    {
        memset(buffer, 0 , CMD_BUFFER_SIZE);
        snprintf(buffer, CMD_BUFFER_SIZE, "set y_axis_name=%s\n", pc.yAxisName);
        rc = send_command_expect_ok(server_connection_index, buffer);
    }
    if( rc == 0 )
    {
        memset(buffer, 0 , CMD_BUFFER_SIZE);
        if( pc.enableLines )
        {
            strncpy(buffer, "set enable_lines=true\n", CMD_BUFFER_SIZE);
        }
        else
        {
            strncpy(buffer, "set enable_lines=false\n", CMD_BUFFER_SIZE);
        }
        rc = send_command_expect_ok(server_connection_index, buffer);
    }
    if( rc == 0 )
    {
        memset(buffer, 0 , CMD_BUFFER_SIZE);
        if( pc.enableShapes )
        {
            strncpy(buffer, "set enable_shapes=true\n", CMD_BUFFER_SIZE);
        }
        else
        {
            strncpy(buffer, "set enable_shapes=false\n", CMD_BUFFER_SIZE);
        }
        rc = send_command_expect_ok(server_connection_index, buffer);
    }
    if( rc == 0 )
    {
        memset(buffer, 0 , CMD_BUFFER_SIZE);
        if( pc.enableAutoScale )
        {
            strncpy(buffer, "set enable_autoscale=true\n", CMD_BUFFER_SIZE);
        }
        else
        {
            strncpy(buffer, "set enable_autoscale=false\n", CMD_BUFFER_SIZE);
        }
        rc = send_command_expect_ok(server_connection_index, buffer);
    }
    if( rc == 0 )
    {
        memset(buffer, 0 , CMD_BUFFER_SIZE);
        snprintf(buffer, CMD_BUFFER_SIZE, "set min_scale_value=%E\n", pc.minScaleValue);
        rc = send_command_expect_ok(server_connection_index, buffer);
    }
    if( rc == 0 )
    {
        memset(buffer, 0 , CMD_BUFFER_SIZE);
        snprintf(buffer, CMD_BUFFER_SIZE, "set max_scale_value=%E\n", pc.maxScaleValue);
        rc = send_command_expect_ok(server_connection_index, buffer);
    }
    if( rc == 0 )
    {
        memset(buffer, 0 , CMD_BUFFER_SIZE);
        snprintf(buffer, CMD_BUFFER_SIZE, "set max_age_seconds=%d\n", pc.maxAgeSeconds);
        rc = send_command_expect_ok(server_connection_index, buffer);
    }
    if( rc == 0 )
    {
        memset(buffer, 0 , CMD_BUFFER_SIZE);
        if( pc.enableLogYAxis )
        {
            strncpy(buffer, "set enable_log_y_axis=true\n", CMD_BUFFER_SIZE);
        }
        else
        {
            strncpy(buffer, "set enable_log_y_axis=false\n", CMD_BUFFER_SIZE);
        }
        rc = send_command_expect_ok(server_connection_index, buffer);
    }
    if( rc == 0 )
    {
        memset(buffer, 0 , CMD_BUFFER_SIZE);
        if( pc.enableZeroOnXAxis )
        {
            strncpy(buffer, "set enable_zero_on_x_scale=true\n", CMD_BUFFER_SIZE);
        }
        else
        {
            strncpy(buffer, "set enable_zero_on_x_scale=false\n", CMD_BUFFER_SIZE);
        }
        rc = send_command_expect_ok(server_connection_index, buffer);
    }
    if( rc == 0 )
    {
        memset(buffer, 0 , CMD_BUFFER_SIZE);
        if( pc.enableZeroOnYAxis )
        {
            strncpy(buffer, "set enable_zero_on_y_scale=true\n", CMD_BUFFER_SIZE);
        }
        else
        {
            strncpy(buffer, "set enable_zero_on_y_scale=false\n", CMD_BUFFER_SIZE);
        }
        rc = send_command_expect_ok(server_connection_index, buffer);
    }
    if( rc == 0 )
    {
        memset(buffer, 0 , CMD_BUFFER_SIZE);
        snprintf(buffer, CMD_BUFFER_SIZE, "set tick_count=%d\n", pc.tickCount);
        rc = send_command_expect_ok(server_connection_index, buffer);
    }
    if( rc == 0 )
    {
        rc = send_command_expect_ok(server_connection_index, "add_plot\n");
    }
    return rc;
}

/**
 * Add a list of values to a plot. This must be used for all plot
 * types except x/y plots.
 * values must be a list of values that contains one element for each
 * plot added. The number of plots is determined by the number of
 * times addPlot has been called. Therefore the first element in
 * the list is added to the first plot, second to the seconds and so on.
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_add_plot_values(int server_connection_index,  float *values, int value_count)
{
    char    buffer[CMD_BUFFER_SIZE];
    char    val_buf[CMD_BUFFER_SIZE];
    int     i;
    int     first_num=1;
    int     rc=0;

    memset(buffer, 0 , CMD_BUFFER_SIZE);
    for( i=0 ; i<value_count ; i++ )
    {
        if( netplot_config.cacheEnabled )
        {
            rc = add_floats_to_cache(values, value_count);
        }
        else
        {
            memset(val_buf, 0 , CMD_BUFFER_SIZE);
            if( first_num )
            {
                snprintf(val_buf, CMD_BUFFER_SIZE, "%f",values[i]);
            }
            else
            {
                snprintf(val_buf, CMD_BUFFER_SIZE, ",%f",values[i]);
            }
            strncat(buffer, val_buf, CMD_BUFFER_SIZE);
            first_num=0;
        }
    }
    if( rc == 0 && !netplot_config.cacheEnabled )
    {
        strncat(buffer, "\n", CMD_BUFFER_SIZE);
        rc =  send_command_expect_ok(server_connection_index, buffer);
    }
    return rc;
}

/**
 * Add a time series value to a plot. This must be used for time series plots
 * where the timestamp is passed from the netplot client code. For time series
 * plots where the time is allocated at the time the server receives a value,
 * netplot_add_plot_values should be used.
 * The fast plot option where all plot points are cached locally and sent to the
 * server is not supported for these time series plots.
 *
 * @param server_connection_index The zero based indexed for the plot to send this value to.
 * @param tsp                     The structure that holds the value to be plotted along with
 *                                it time.
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_add_time_series_plot_value(int server_connection_index, int plot_index, struct _time_series_point *tsp )
{
    char    buffer[CMD_BUFFER_SIZE];

    memset(buffer, 0 , CMD_BUFFER_SIZE);

    //Add the plot index
    snprintf(buffer, CMD_BUFFER_SIZE, "%d:%d;%d;%d;%d;%d;%d;%d:%f\n",plot_index, tsp->year, tsp->month, tsp->day, tsp->hour, tsp->minute, tsp->second, tsp->mill_second, tsp->value);
    return  send_command_expect_ok(server_connection_index, buffer);
}

/**
 * Add the x and y values to the plot.
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_add_xy_plot_values(int server_connection_index, int plot_index, float xValue, float yValue)
{
    char buffer[CMD_BUFFER_SIZE];

    memset(buffer, 0 , CMD_BUFFER_SIZE);
    debug("Adding XY values to plot: %E/%E",xValue, yValue);
    snprintf(buffer, CMD_BUFFER_SIZE, "%d:%E:%E\n", plot_index, xValue, yValue);
    if( netplot_config.cacheEnabled )
    {
        return add_string_to_cache(buffer);
    }
    return send_command_expect_ok(server_connection_index, buffer);
}

/**
 * Clear a single plot
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_clear(int server_connection_index)
{
    char buffer[CMD_BUFFER_SIZE];

    memset(buffer, 0 , CMD_BUFFER_SIZE);
    snprintf(buffer, CMD_BUFFER_SIZE, "clear %d\n", server_connection_index);

    return send_command_expect_ok(server_connection_index, buffer);
}

/**
 * replot() causes subsequent values to overwrite previous ones.
 * This is more efficient than clear and stops screen flicker
 * when all plot points are erased.
 * Returns 0 : ok
 *       -ve : Failed
 **/
int netplot_replot(int server_connection_index, int plot_index)
{
    char buffer[CMD_BUFFER_SIZE];

    memset(buffer, 0 , CMD_BUFFER_SIZE);
    snprintf(buffer, CMD_BUFFER_SIZE, "replot %d\n", plot_index);

    return send_command_expect_ok(server_connection_index, buffer);
}

/**
 * Enable/Disable the plot cache
 * Returns 0 : ok
 *       -ve : Failed
 **/
void netplot_enable_cache(int enabled)
{
    netplot_config.cacheEnabled=enabled;
}

/**
 * Get the number of occurances of OK in the msg.
 **/
int get_ok_count(char *msg)
{
    int ok_count=0;
    int i;
    int len=strlen(msg);

    for( i=0 ; i<len ; i++ )
    {
        if( strstr(msg+i, "OK") != NULL )
        {
            ok_count++;
        }
    }
    return ok_count;
}

/**
 * Send all plot values from the cache to the netplot server.
 *  Only call this when netplot_enable_cache(1) has been called.
 **/
int netplot_update(int server_connection_index)
{
    char    *cache_data;
    int     cmd_count;
    int     ok_rx_count;
    int     rc=0;

    //If we have some data to send
    if( !is_cache_empty() )
    {
        cache_data = get_cache_data();
        cmd_count = get_cmd_count(cache_data);
        //Send the cache dat
        send_command(server_connection_index, cache_data );
        //Expect cmd_count ok messages to be recieved
        ok_rx_count=0;
        while( ok_rx_count < cmd_count )
        {
            rc = receive_response(server_connection_index);
            if( rc == -1 )
            {
                break;
            }
            ok_rx_count+=get_ok_count(rx_buffer);
        }
        //flush the cache
        empty_cache();
    }
    return rc;
}

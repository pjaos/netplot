/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

#ifndef NETPLOT_CLIENT_H_INCLUDED
#define NETPLOT_CLIENT_H_INCLUDED

#define MAX_STR_LEN         256
#define DEFAULT_BASE_PORT   9600
#define DEFAULT_PLOT_COUNT  100
#define RX_BUFFSIZE         65536
#define CMD_BUFFER_SIZE     256

#define PLOT_TYPE_TIME      1
#define PLOT_TYPE_BAR       2
#define PLOT_TYPE_XY        3
#define PLOT_TYPE_DIAL      4

//holds the config for the netplot_client program
struct _netplot_config
{
    char             hostAddress[MAX_STR_LEN];
    float            serverVersion;
    int              port;
    int              *sockets;
    int              socket_count;
    int              cache_item_count;
    unsigned char    cacheEnabled;
    char             *oldCachePtr;
    char             *cachePtr;
    unsigned char    debug_enabled;
};

//Holds the config for a single plot
struct _plot_config
{
    char            plotName[MAX_STR_LEN];
    char            xAxisName[MAX_STR_LEN];
    char            yAxisName[MAX_STR_LEN];
    unsigned char   enableLines;
    unsigned char   enableShapes;
    unsigned char   enableAutoScale;
    unsigned char   enableLogYAxis;
    unsigned char   enableZeroOnXAxis;
    unsigned char   enableZeroOnYAxis;
    int             maxAgeSeconds;
    int             tickCount;
    float           minScaleValue;
    float           maxScaleValue;
};

struct _time_series_point
{
    float           value; //The Y axis value to be plotted
    int             year;
    int             month;
    int             day;
    int             hour;
    int             minute;
    int             second;
    int             mill_second;
};

void info(const char *fmt, ...);
void error(const char *fmt, ...);
void fatal(const char *fmt, ...) ;
void debug(const char *fmt, ...);
void load_default_plotConfig(struct _plot_config *plot_config);

char *netplot_get_last_message();
void netplot(char *address, int base_port, int plot_count, unsigned char debug_enabled);
void netplot_defaults();
void show_netplot_vars();
int  netplot_connect();
void netplot_disconnect();
int netplot_set_grid(int rows, int cols);
int netplot_set_window_title(char *window_title);
int netplot_set_chart_legend_enabled(int server_connection_index, int enabled);
int netplot_init(int server_connection_index);
int netplot_set_plot_type(int server_connection_index, int plot_type, char *plot_title);
int netplot_enable_status_messages(int enabled);
int netplot_add_plot(int server_connection_index, struct _plot_config pc);
int netplot_add_plot_values(int server_connection_index,  float *values, int value_count);
int netplot_add_xy_plot_values(int server_connection_index, int plot_index, float xVvalue, float yValue);
int netplot_clear(int server_connection_index);
int netplot_replot(int server_connection_index, int plot_index);
void netplot_enable_cache(int enabled);
int netplot_update();
int netplot_add_time_series_plot_value(int server_connection_index, int plot_index, struct _time_series_point *tsp);
float netplot_get_server_version();

#endif /* NETPLOT_CLIENT_H_INCLUDED */

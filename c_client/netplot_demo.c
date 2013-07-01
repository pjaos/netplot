/*****************************************************************************************
 *                             Copyright 2009 Paul Austen                                *
 *                                                                                       *
 * This program is distributed under the terms of the GNU Lesser General Public License  *
 *****************************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <math.h>

#include "netplot_client.h"

char netplot_server_address[MAX_STR_LEN];
int debug_enabled=0;

/**
 * Get a random float value between the min and max values
 **/
static float get_random(float min, float max)
{
    float rand_val;
    float range=max-min;

    rand_val = (((float)rand())/RAND_MAX)*range;
    return min+rand_val;
}

/**
 * Show the command line program usage text to the user.
 **/
static void usage()
{
    info("Usage");
    info("--host : Followed by the netplot server host address (default=127.0.0.1).");
    info("-d     : Enable debugging.");
}

/**
 * Load the arguments from the command line input
 **/
static void process_cmd_line_args(int argc, char *argv[])
{
    int handled_arg_count=0;
    unsigned char read_host=0;

    strncpy(netplot_server_address, "127.0.0.1", MAX_STR_LEN);
    for( handled_arg_count=0 ; handled_arg_count < argc ; handled_arg_count++ )
    {
        if( read_host )
        {
            //Set the netplot srver address
            strncpy(netplot_server_address, argv[handled_arg_count], MAX_STR_LEN);
            read_host=0;
            continue;
        }
        if( strcmp(argv[handled_arg_count],"-h") == 0 ||
                strcmp(argv[handled_arg_count],"-h") == 0 )
        {
            usage();
            exit(0);
        }
        else if( strcmp(argv[handled_arg_count],"--host") == 0 )
        {
            read_host=1;
        }
        else if( strcmp(argv[handled_arg_count],"-d") == 0 )
        {
            debug_enabled=1;
        }
        else if( handled_arg_count > 0 )
        {
            printf("Unknown argument: %s\n", argv[handled_arg_count]);
            usage();
            exit(0);
        }
    }
}

/**
 * Single plot on a time series chart
 **/
static void time_example_1(int server_connection_index)
{
    float val;
    struct _plot_config pc;

    memset(&pc, 0 , sizeof(pc) );

    load_default_plotConfig(&pc);
    strncpy(pc.plotName, "Plot 0", MAX_STR_LEN);
    strncpy(pc.xAxisName, "The X axis", MAX_STR_LEN);
    strncpy(pc.yAxisName, "The Y axis (Plot0)", MAX_STR_LEN);
    pc.enableLines=1;
    pc.enableShapes=1;
    pc.enableAutoScale=0;
    pc.minScaleValue=0;
    pc.maxScaleValue=10000;
    pc.maxAgeSeconds=5;
    pc.tickCount=1000;
    netplot_set_plot_type(server_connection_index, PLOT_TYPE_TIME, "TIME chart, single plot");
    netplot_add_plot(server_connection_index, pc);
    int i=0;
    while( i< 10)
    {
        val=get_random(0,10000);
        netplot_add_plot_values(server_connection_index, &val, 1);
        i++;
    }
}

#define PLOT_COUNT 3

/**
 * Three plots on a time series chart, single Y axis
 **/
static void time_example_2(int server_connection_index)
{
    float   vals[3];
    struct  _plot_config pc;
    char    plotNames[PLOT_COUNT][7+1] = {"Sine","Cosine","Tangent"};
    int     plotIndex;
    float   x;

    memset(&pc, 0 , sizeof(pc) );

    load_default_plotConfig(&pc);
    strncpy(pc.xAxisName, "The X axis", MAX_STR_LEN);
    strncpy(pc.yAxisName, "The Y axis (Plot0)", MAX_STR_LEN);
    pc.enableLines=1;
    pc.enableShapes=1;
    pc.enableAutoScale=0;
    pc.minScaleValue=-5;
    pc.maxScaleValue=5;
    netplot_set_plot_type(server_connection_index, PLOT_TYPE_TIME, "TIME chart, multiple plots, same Y scale");

    for( plotIndex=0 ; plotIndex < PLOT_COUNT ; plotIndex++ ) {
        strncpy(pc.plotName, plotNames[plotIndex], MAX_STR_LEN);
        netplot_add_plot(server_connection_index, pc);
        //We only want one Y axis so null plot axis name
        strncpy(pc.xAxisName, "", MAX_STR_LEN);
        strncpy(pc.yAxisName, "", MAX_STR_LEN);
    }

    x=0.01;
    while( x< 25)
    {
        vals[0]=sin(x);
        vals[1]=cos(x);
        vals[2]=tan(x);
        netplot_add_plot_values(server_connection_index, vals, 3);
        x=x+0.1;
        usleep(10000);
    }

}


/**
 * Two plots on a time series chart
 **/
static void time_example_3(int server_connection_index)
{
    float vals[2];
    struct _plot_config pc;

    memset(&pc, 0 , sizeof(pc) );

    load_default_plotConfig(&pc);
    strncpy(pc.plotName, "Plot 0", MAX_STR_LEN);
    strncpy(pc.xAxisName, "The X axis", MAX_STR_LEN);
    strncpy(pc.yAxisName, "The Y axis (Plot0)", MAX_STR_LEN);
    pc.enableLines=1;
    pc.enableShapes=1;
    pc.enableAutoScale=1;
    pc.maxAgeSeconds=5;
    pc.tickCount=1000;
    netplot_set_plot_type(server_connection_index, PLOT_TYPE_TIME, "TIME chart, two traces with different linear Y scales, both autoscaled");
    netplot_add_plot(server_connection_index, pc);

    strncpy(pc.yAxisName, "The Y axis (Plot1)", MAX_STR_LEN);
    netplot_add_plot(server_connection_index, pc);

    int i=0;
    while( i< 10)
    {
        vals[0]=get_random(0,10000);
        vals[1]=get_random(0,10000);

        netplot_add_plot_values(server_connection_index, vals, 2);
        i++;
    }
}

/**
 * Two plots on a time series chartTime series chart passing the time and the y value
 **/
static void time_example_4(int server_connection_index)
{
    struct _plot_config pc;
    struct _time_series_point *tsp0;
    struct _time_series_point *tsp1;

    memset(&pc, 0 , sizeof(pc) );

    load_default_plotConfig(&pc);
    strncpy(pc.plotName, "Plot 0", MAX_STR_LEN);
    strncpy(pc.xAxisName, "The X axis", MAX_STR_LEN);
    strncpy(pc.yAxisName, "The Y axis (Plot0)", MAX_STR_LEN);
    pc.enableLines=1;
    pc.enableShapes=1;
    pc.enableAutoScale=1;
    pc.maxAgeSeconds=5;
    pc.tickCount=1000;
    netplot_set_plot_type(server_connection_index, PLOT_TYPE_TIME, "TIME chart, passing the time and the y value.");
    netplot_add_plot(server_connection_index, pc);

    strncpy(pc.yAxisName, "The Y axis (Plot1)", MAX_STR_LEN);
    netplot_add_plot(server_connection_index, pc);

    tsp0=calloc(1, sizeof(struct _time_series_point) );
    tsp1=calloc(1, sizeof(struct _time_series_point) );

    tsp0->year=2013;
    tsp0->month=1;
    tsp0->day=1;
    tsp0->hour=2;
    tsp0->minute=35;
    tsp0->second=20;
    tsp0->mill_second=495;

    memcpy(tsp1, tsp0, sizeof(struct _time_series_point) );

    int i=0;
    while( i< 10)
    {
        tsp0->value=get_random(0,10000);
        tsp1->value=get_random(0,10000);

        netplot_add_time_series_plot_value(server_connection_index, 0, tsp0);
        netplot_add_time_series_plot_value(server_connection_index, 1, tsp1);
        i++;
        tsp0->year++;
        tsp1->year++;
    }

    free(tsp0);
    free(tsp1);
}

/**
 * Bar chart example
 **/
static void bar_example(int server_connection_index)
{
    float val;
    struct _plot_config pc;

    memset(&pc, 0 , sizeof(pc) );

    load_default_plotConfig(&pc);
    strncpy(pc.plotName, "Plot 0", MAX_STR_LEN);
    strncpy(pc.xAxisName, "The X axis", MAX_STR_LEN);
    strncpy(pc.yAxisName, "The Y axis", MAX_STR_LEN);
    pc.enableAutoScale=1;
    netplot_set_plot_type(server_connection_index, PLOT_TYPE_BAR, "BAR chart");
    netplot_add_plot(server_connection_index, pc);

    int i=0;
    while( i< 10)
    {
        val=get_random(1000,10000);

        netplot_add_plot_values(server_connection_index, &val, 1);
        i++;
    }
}

/**
 * Two plots with different linear Y scales
 **/
static void xy_example1(int server_connection_index)
{
    float x_val, y_val;
    struct _plot_config pc0;
    struct _plot_config pc1;

    memset(&pc0, 0 , sizeof(pc0) );
    memset(&pc1, 0 , sizeof(pc1) );

    load_default_plotConfig(&pc1);
    strncpy(pc0.plotName, "Plot 0", MAX_STR_LEN);
    strncpy(pc0.xAxisName, "The X axis name", MAX_STR_LEN);
    strncpy(pc0.yAxisName, "The Y axis (Plot0)", MAX_STR_LEN);
    pc0.enableLines=1;
    pc0.enableShapes=1;
    pc0.enableAutoScale=1;
    pc0.enableZeroOnXAxis=0;
    pc0.enableZeroOnYAxis=0;
    netplot_set_plot_type(server_connection_index, PLOT_TYPE_XY, "XY chart, two traces with different linear Y scales, both autoscaled");
    netplot_add_plot(server_connection_index, pc0);

    load_default_plotConfig(&pc1);
    strncpy(pc1.plotName, "Plot 1", MAX_STR_LEN);
    strncpy(pc1.xAxisName, "The X axis name", MAX_STR_LEN);
    strncpy(pc1.yAxisName, "The Y axis (Plot1)", MAX_STR_LEN);
    pc1.enableLines=1;
    pc1.enableShapes=1;
    pc1.enableAutoScale=1;
    pc1.enableZeroOnXAxis=0;
    pc1.enableZeroOnYAxis=0;
    netplot_add_plot(server_connection_index, pc1);

    int i=0;
    while( i< 10)
    {
        x_val=get_random(-90,-70);
        y_val=get_random(130,150);

        netplot_add_xy_plot_values(server_connection_index, 0, x_val, y_val);

        x_val=get_random(-60,-50);
        y_val=get_random(75,80);

        netplot_add_xy_plot_values(server_connection_index, 1, x_val, y_val);
        i++;

    }

}

/**
 * XY plot with log Y scale
 **/
static void xy_example2(int server_connection_index)
{
    struct _plot_config pc;

    memset(&pc, 0 , sizeof(pc) );

    load_default_plotConfig(&pc);
    strncpy(pc.plotName, "Plot 0", MAX_STR_LEN);
    strncpy(pc.xAxisName, "The X axis name", MAX_STR_LEN);
    strncpy(pc.yAxisName, "Log Y axis", MAX_STR_LEN);
    pc.enableLines=1;
    pc.enableShapes=1;
    pc.enableLogYAxis=1;
    pc.minScaleValue=1E-10;
    pc.maxScaleValue=1E-2;
    netplot_set_plot_type(server_connection_index, PLOT_TYPE_XY, "XY chart with log Y scale");
    netplot_add_plot(server_connection_index, pc);

    netplot_add_xy_plot_values(server_connection_index, 0, -50, 1E-9);
    netplot_add_xy_plot_values(server_connection_index, 0, -55, 1E-7);
    netplot_add_xy_plot_values(server_connection_index, 0, -60, 1E-6);
    netplot_add_xy_plot_values(server_connection_index, 0, -70, 1E-5);
    netplot_add_xy_plot_values(server_connection_index, 0, -80, 1E-4);
    netplot_add_xy_plot_values(server_connection_index, 0, -90, 1E-3);

}

/**
 * XY chart with 2 lin and 2 log Y scales
 **/
static void xy_example3(int server_connection_index)
{
    struct _plot_config pc0, pc1, pc2, pc3;

    memset(&pc0, 0 , sizeof(pc0) );
    memset(&pc1, 0 , sizeof(pc1) );
    memset(&pc2, 0 , sizeof(pc2) );
    memset(&pc3, 0 , sizeof(pc3) );

    load_default_plotConfig(&pc0);
    strncpy(pc0.plotName, "Plot 0", MAX_STR_LEN);
    strncpy(pc0.xAxisName, "The X axis name", MAX_STR_LEN);
    strncpy(pc0.yAxisName, "Log Y axis (Plot 0)", MAX_STR_LEN);
    pc0.enableLines=1;
    pc0.enableShapes=1;
    pc0.enableLogYAxis=1;
    pc0.minScaleValue=1E-10;
    pc0.maxScaleValue=1E-2;
    netplot_set_plot_type(server_connection_index, PLOT_TYPE_XY, "XY chart with 2 lin and 2 log Y scales");
    netplot_add_plot(server_connection_index, pc0);

    load_default_plotConfig(&pc1);
    strncpy(pc1.plotName, "Plot 1", MAX_STR_LEN);
    strncpy(pc1.yAxisName, "Log Y axis (Plot 1)", MAX_STR_LEN);
    pc1.enableLines=1;
    pc1.enableShapes=1;
    pc1.enableAutoScale=1;
    pc1.enableZeroOnXAxis=1;
    pc1.enableZeroOnYAxis=1;
    netplot_add_plot(server_connection_index, pc1);

    load_default_plotConfig(&pc2);
    strncpy(pc2.plotName, "Plot 2", MAX_STR_LEN);
    strncpy(pc2.yAxisName, "Log Y axis (Plot 2)", MAX_STR_LEN);
    pc2.enableLines=1;
    pc2.enableShapes=1;
    pc2.enableAutoScale=1;
    pc2.enableZeroOnXAxis=1;
    pc2.enableZeroOnYAxis=1;
    netplot_add_plot(server_connection_index, pc2);

    load_default_plotConfig(&pc3);
    strncpy(pc3.plotName, "Plot 3", MAX_STR_LEN);
    strncpy(pc3.yAxisName, "Log Y axis (Plot 3)", MAX_STR_LEN);
    pc3.enableLines=1;
    pc3.enableShapes=1;
    pc3.enableLogYAxis=1;
    pc3.minScaleValue=1E-10;
    pc3.maxScaleValue=1E-2;
    netplot_add_plot(server_connection_index, pc3);

    netplot_add_xy_plot_values(server_connection_index,0,-50, 1E-9);
    netplot_add_xy_plot_values(server_connection_index,0,-55, 1E-7);
    netplot_add_xy_plot_values(server_connection_index,0,-60, 1E-6);
    netplot_add_xy_plot_values(server_connection_index,0,-70, 1E-5);
    netplot_add_xy_plot_values(server_connection_index,0,-80, 1E-4);
    netplot_add_xy_plot_values(server_connection_index,0,-90, 1E-3);

    netplot_add_xy_plot_values(server_connection_index,1,-10, 10);
    netplot_add_xy_plot_values(server_connection_index,1,-9, 12);
    netplot_add_xy_plot_values(server_connection_index,1,-8, 14);
    netplot_add_xy_plot_values(server_connection_index,1,-7, 16);
    netplot_add_xy_plot_values(server_connection_index,1,-6, 18);
    netplot_add_xy_plot_values(server_connection_index,1,-5, 20);

    netplot_add_xy_plot_values(server_connection_index,2,-35, 10);
    netplot_add_xy_plot_values(server_connection_index,2,-95, 12);
    netplot_add_xy_plot_values(server_connection_index,2,-85, 14);
    netplot_add_xy_plot_values(server_connection_index,2,-75, 16);
    netplot_add_xy_plot_values(server_connection_index,2,-65, 18);
    netplot_add_xy_plot_values(server_connection_index,2,-55, 20);

    netplot_add_xy_plot_values(server_connection_index,3,1, 1E-9);
    netplot_add_xy_plot_values(server_connection_index,3,2, 1E-7);
    netplot_add_xy_plot_values(server_connection_index,3,3, 1E-6);
    netplot_add_xy_plot_values(server_connection_index,3,4, 1E-5);
    netplot_add_xy_plot_values(server_connection_index,3,5, 1E-4);
    netplot_add_xy_plot_values(server_connection_index,3,6, 1E-3);
}

/**
 * Show a dial, speedo style plot
 **/
static void show_dial_example(int server_connection_index)
{
    float value, max_value;
    float values[2];
    struct _plot_config pc;

    memset(&pc, 0 , sizeof(pc) );

    netplot_set_plot_type(server_connection_index, PLOT_TYPE_DIAL, "Number and MAX");

    load_default_plotConfig(&pc);
    strncpy(pc.plotName, "Number", MAX_STR_LEN);
    strncpy(pc.xAxisName, "The X axis name", MAX_STR_LEN);
    strncpy(pc.yAxisName, "Log Y axis (Plot 0)", MAX_STR_LEN);
    pc.minScaleValue=0;
    pc.maxScaleValue=200;
    pc.tickCount=10;
    netplot_add_plot(server_connection_index, pc);

    strncpy(pc.plotName, "MAX", MAX_STR_LEN);
    netplot_add_plot(server_connection_index, pc);

    max_value=0;
    value=0;
    while( value < 200 )
    {
        value += (int)(get_random(0,30)-10);
        if( value > 200 ) {
            value=200;
        }
        if( value < 0 )
        {
            value=0;
        }
        if( value > max_value )
        {
            max_value=value;
        }

        values[0]=value;
        values[1]=max_value;
        netplot_add_plot_values(server_connection_index, values, 2);
        usleep(250000);
    }
}


/**
 * XY plot with log Y scale but in this case we send the data for all the
 * points to be plotted and then use the update command to plot them all
 * quickly.
 **/
static void show_cache_example(int server_connection_index)
{
    float   v;
    struct  _plot_config pc;

    memset(&pc, 0 , sizeof(pc) );

    load_default_plotConfig(&pc);
    strncpy(pc.plotName, "Plot 0", MAX_STR_LEN);
    strncpy(pc.xAxisName, "The X axis name", MAX_STR_LEN);
    strncpy(pc.yAxisName, "Log Y axis", MAX_STR_LEN);
    pc.enableLines=1;
    pc.enableShapes=0;
    pc.enableAutoScale=0;
    pc.minScaleValue=0;
    pc.maxScaleValue=1;

    netplot_set_plot_type(server_connection_index, PLOT_TYPE_XY, "Cached, fast XY chart with log Y scale");
    netplot_add_plot(server_connection_index, pc);

    //These must be called after the plot type is added
    //Enables cached operation, update() will draw all plot points
    netplot_enable_cache(1);
    //Not printing status messages may speed up plotting if CPU bound
    netplot_enable_status_messages(0);

    while(1)
    {
        netplot_replot(server_connection_index, 0);
        for( v=0 ; v<100 ; v++ )
        {
            netplot_add_xy_plot_values(server_connection_index, 0, v , get_random(0,1) );
        }
        netplot_update(server_connection_index);
    }
}

int main(int argc, char *argv[])
{
    info("netplot_client demo started");
    process_cmd_line_args(argc, argv);

    //Init the netplot client
    netplot(netplot_server_address, DEFAULT_BASE_PORT, DEFAULT_PLOT_COUNT, debug_enabled);

    //connect to the netplot server
    if( netplot_connect() )
    {
        fatal("Failed to connect to netplot server @ %s",netplot_server_address);
    }

    info("Netplot server version = %2.2f",netplot_get_server_version());

    //Set the number of panels (plot area's) on the GUI at the server
    netplot_set_grid(4,4);
    //Set the GUI window title at the server
    netplot_set_window_title("C netplot client demo");

    //The following functions place the demo plots on the GUI at the netplot server.
    time_example_1(0);
    time_example_2(1);
    time_example_3(2);
    time_example_4(3);
    bar_example(4);
    xy_example1(5);
    xy_example2(6);
    xy_example3(7);
    show_dial_example(8);
    //This function currently blocks continually updating a plot
    show_cache_example(9);

    //Disconnect all client connectiions to the server
    netplot_disconnect();

    return 0;
}

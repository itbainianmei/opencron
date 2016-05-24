<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ben"  uri="ben-taglib"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <jsp:include page="/WEB-INF/common/resource.jsp"/>

    <script src="/js/socket.io/socket.io.js"></script>
    <script src="/js/socket.io/moment.min.js"></script>

    <script src="/js/highcharts/js/highcharts.js"></script>
    <script src="/js/highcharts/js/highcharts-more.js"></script>
    <script src="/js/highcharts/js/highcharts-3d.js"></script>
    <script src="/js/highcharts/js/modules/exporting.js"></script>


    <script type="text/javascript">

        $(document).ready(function() {
            $.ajax({
                url: "/url",
                data: {
                    workerId: $("#workerId").val()
                },
                dataType: "json",
                success: function (result) {
                    var diskLoad = false;
                    var memLoad = false;
                    var cpuLoad = false;
                    var memChartObj=null;
                    var cpuChartObj=null;
                    var cpuData = [];

                    var socket = io(result.url);
                    socket.on("monitor", function (data) {
                        if(!diskLoad) {
                            diskData(data);
                            diskLoad = true;
                        }

                        if(!memLoad){
                            memChartObj = memChart(data);
                            memLoad = true;
                        }else {
                            memChartObj.series[0].points[0].update(parseFloat(data.memUsage));
                        }

                        if(!cpuLoad){
                            cpuChartObj = cpuChart(data,cpuData);
                            cpuChartObj.setData([cpuData]);
                            cpuChartObj.draw();
                            if(cpuData.length==401){
                                cpuLoad = true;
                            }
                        }else {
                            var res = [];
                            for(var i=1;i<cpuData.length;i++){
                                res.push([i-1,cpuData[i][1]]);
                            }
                            res.push( [res.length,parseFloat(data.cpuUsage)] );
                            cpuData = res;
                            cpuChartObj.setData([cpuData]);
                            cpuChartObj.draw();
                        }


                    });

                    socket.on("disconnect",function (data) {
                        socket = null;
                    });
                }
            });
        });

        function diskData(data) {
            $("#diskpie").html("");
            var diskArr = data.diskUsage;

            var usedTotal = parseFloat(data.usedTotal);
            var freeTotal = parseFloat(data.freeTotal);

            var diskinfo = {
                "disk":"用量总计",
                "used":usedTotal,
                "free":freeTotal,
                "size":120
            };

            var diskSize = diskArr.length;
            if( diskSize%2 == 0 ){
                diskArr.splice(diskSize/2,0,diskinfo);
            }else {
                diskArr.splice(0,0,diskinfo);
            }

            for (var i in diskArr) {
                var disk = diskArr[i].disk;
                var used = parseFloat(diskArr[i].used);
                var free = parseFloat(diskArr[i].free);
                var val = parseInt((used / (used + free)) * 100);
                var html = $("<div class=\"pie-chart-tiny\"><span class=\"percent\"></span><span class=\"pie-title\"><i class=\"icon\">&#61804;</i>&nbsp;" + disk + "</span></div>");
                html.easyPieChart({
                    easing: 'easeOutBounce',
                    barColor: 'rgba(255,255,255,0.75)',
                    trackColor: 'rgba(0,0,0,0.3)',
                    scaleColor: 'rgba(255,255,255,0.3)',
                    lineCap: 'square',
                    lineWidth: 5,
                    size: diskArr[i].size||100,
                    animate: 3000,
                    onStep: function (from, to, percent) {
                        $(this.el).find('.percent').text(Math.round(percent));
                    }
                });
                html.data('easyPieChart').update(val);
                $("#diskpie").append(html);
            }
        }

        function memChart(data){

            var chart = new Highcharts.Chart({
                chart: {
                    type: 'gauge',
                    backgroundColor: 'rgba(0,0,0,0)',
                    plotBorderColor : null,
                    plotBackgroundColor: null,
                    plotBackgroundImage:'',
                    plotBorderWidth: null,
                    plotShadow: false,
                    renderTo: 'line-chart'
                },

                title: {
                    text: ''
                },

                pane: {
                    startAngle: -150,
                    endAngle: 150,
                    background: [{
                        backgroundColor: {
                            linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
                            stops: [
                                [0, '#FFF'],
                                [1, '#333']
                            ]
                        },
                        borderWidth: 0,
                        outerRadius: '109%'
                    }, {
                        backgroundColor: {
                            linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
                            stops: [
                                [0, '#333'],
                                [1, '#FFF']
                            ]
                        },
                        borderWidth: 1,
                        outerRadius: '107%'
                    }, {
                        backgroundColor: '#DDD',
                        borderWidth: 0,
                        outerRadius: '105%',
                        innerRadius: '103%'
                    }]
                },

                yAxis: {
                    min: 0,
                    max: 100,
                    minorTickInterval: 'auto',
                    minorTickWidth: 1,
                    minorTickLength: 10,
                    minorTickPosition: 'inside',
                    minorTickColor: '#666',
                    tickPixelInterval: 30,
                    tickWidth: 2,
                    tickPosition: 'inside',
                    tickLength: 10,
                    tickColor: '#666',
                    labels: {
                        step: 2,
                        rotation: 'auto'
                    },
                    title: {
                        text: '内存使用率'
                    },
                    plotBands: [{
                        from: 0,
                        to: 50,
                        color: '#55BF3B' // green
                    }, {
                        from: 50,
                        to: 80,
                        color: '#DDDF0D' // yellow
                    }, {
                        from: 80,
                        to: 100,
                        color: '#DF5353' // red
                    }]
                },

                series: [{
                    name: '内存使用率',
                    data: [parseFloat(data.memUsage)],
                    tooltip: {
                        valueSuffix: ' %'
                    }
                }]
            });

            return chart;

        }

        function cpuChart(data,cpuData) {
            if (cpuData.length<401) {

                cpuData.push([ cpuData.length, parseFloat(data.cpuUsage)]);
                return $.plot("#dynamic-chart", cpuData, {
                    series: {
                        label: "Data",
                        lines: {
                            show: true,
                            lineWidth: 1,
                            fill: 0.25,
                        },

                        color: 'rgba(255,255,255,0.2)',
                        shadowSize: 0,
                    },
                    yaxis: {
                        min: 0,
                        max: 100,
                        tickColor: 'rgba(255,255,255,0.15)',
                        font :{
                            lineHeight: 13,
                            style: "normal",
                            color: "rgba(255,255,255,0.8)",
                        },
                        shadowSize: 0,

                    },
                    xaxis: {
                        tickColor: 'rgba(255,255,255,0.15)',
                        show: true,
                        font :{
                            lineHeight: 13,
                            style: "normal",
                            color: "rgba(255,255,255,0.8)",
                        },
                        shadowSize: 0,
                        min: 0,
                        max: cpuData.length-1
                    },
                    grid: {
                        borderWidth: 1,
                        borderColor: 'rgba(255,255,255,0.25)',
                        labelMargin:10,
                        hoverable: true,
                        clickable: true,
                        mouseActiveRadius:6,
                    },
                    legend: {
                        show: false
                    }
                });
            }




        }



    </script>


</head>

<jsp:include page="/WEB-INF/common/top.jsp"/>

    <!-- Content -->
    <section id="content" class="container">



        <!-- Messages Drawer -->
        <div id="messages" class="tile drawer animated">
            <div class="listview narrow">
                <div class="media">
                    <a href="">Send a New Message</a>
                    <span class="drawer-close">&times;</span>

                </div>
                <div class="overflow" style="height: 254px">
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/1.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">Nadin Jackson - 2 Hours ago</small><br>
                            <a class="t-overflow" href="">Mauris consectetur urna nec tempor adipiscing. Proin sit amet nisi ligula. Sed eu adipiscing lectus</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/2.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">David Villa - 5 Hours ago</small><br>
                            <a class="t-overflow" href="">Suspendisse in purus ut nibh placerat Cras pulvinar euismod nunc quis gravida. Suspendisse pharetra</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/3.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">Harris worgon - On 15/12/2013</small><br>
                            <a class="t-overflow" href="">Maecenas venenatis enim condimentum ultrices fringilla. Nulla eget libero rhoncus, bibendum diam eleifend, vulputate mi. Fusce non nibh pulvinar, ornare turpis id</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/4.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">Mitch Bradberry - On 14/12/2013</small><br>
                            <a class="t-overflow" href="">Phasellus interdum felis enim, eu bibendum ipsum tristique vitae. Phasellus feugiat massa orci, sed viverra felis aliquet quis. Curabitur vel blandit odio. Vestibulum sagittis quis sem sit amet tristique.</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/1.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">Nadin Jackson - On 15/12/2013</small><br>
                            <a class="t-overflow" href="">Ipsum wintoo consectetur urna nec tempor adipiscing. Proin sit amet nisi ligula. Sed eu adipiscing lectus</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/2.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">David Villa - On 16/12/2013</small><br>
                            <a class="t-overflow" href="">Suspendisse in purus ut nibh placerat Cras pulvinar euismod nunc quis gravida. Suspendisse pharetra</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/3.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">Harris worgon - On 17/12/2013</small><br>
                            <a class="t-overflow" href="">Maecenas venenatis enim condimentum ultrices fringilla. Nulla eget libero rhoncus, bibendum diam eleifend, vulputate mi. Fusce non nibh pulvinar, ornare turpis id</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/4.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">Mitch Bradberry - On 18/12/2013</small><br>
                            <a class="t-overflow" href="">Phasellus interdum felis enim, eu bibendum ipsum tristique vitae. Phasellus feugiat massa orci, sed viverra felis aliquet quis. Curabitur vel blandit odio. Vestibulum sagittis quis sem sit amet tristique.</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/5.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">Wendy Mitchell - On 19/12/2013</small><br>
                            <a class="t-overflow" href="">Integer a eros dapibus, vehicula quam accumsan, tincidunt purus</a>
                        </div>
                    </div>
                </div>
                <div class="media text-center whiter l-100">
                    <a href=""><small>VIEW ALL</small></a>
                </div>
            </div>
        </div>

        <!-- Notification Drawer -->
        <div id="notifications" class="tile drawer animated">
            <div class="listview narrow">
                <div class="media">
                    <a href="">Notification Settings</a>
                    <span class="drawer-close">&times;</span>
                </div>
                <div class="overflow" style="height: 254px">
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/1.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">Nadin Jackson - 2 Hours ago</small><br>
                            <a class="t-overflow" href="">Mauris consectetur urna nec tempor adipiscing. Proin sit amet nisi ligula. Sed eu adipiscing lectus</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/2.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">David Villa - 5 Hours ago</small><br>
                            <a class="t-overflow" href="">Suspendisse in purus ut nibh placerat Cras pulvinar euismod nunc quis gravida. Suspendisse pharetra</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/3.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">Harris worgon - On 15/12/2013</small><br>
                            <a class="t-overflow" href="">Maecenas venenatis enim condimentum ultrices fringilla. Nulla eget libero rhoncus, bibendum diam eleifend, vulputate mi. Fusce non nibh pulvinar, ornare turpis id</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/4.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">Mitch Bradberry - On 14/12/2013</small><br>
                            <a class="t-overflow" href="">Phasellus interdum felis enim, eu bibendum ipsum tristique vitae. Phasellus feugiat massa orci, sed viverra felis aliquet quis. Curabitur vel blandit odio. Vestibulum sagittis quis sem sit amet tristique.</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/1.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">Nadin Jackson - On 15/12/2013</small><br>
                            <a class="t-overflow" href="">Ipsum wintoo consectetur urna nec tempor adipiscing. Proin sit amet nisi ligula. Sed eu adipiscing lectus</a>
                        </div>
                    </div>
                    <div class="media">
                        <div class="pull-left">
                            <img width="40" src="/img/profile-pics/2.jpg" alt="">
                        </div>
                        <div class="media-body">
                            <small class="text-muted">David Villa - On 16/12/2013</small><br>
                            <a class="t-overflow" href="">Suspendisse in purus ut nibh placerat Cras pulvinar euismod nunc quis gravida. Suspendisse pharetra</a>
                        </div>
                    </div>
                </div>
                <div class="media text-center whiter l-100">
                    <a href=""><small>VIEW ALL</small></a>
                </div>
            </div>
        </div>

        <!-- Breadcrumb -->
        <ol class="breadcrumb hidden-xs">
            <li><a href="#">Home</a></li>
            <li><a href="#">Library</a></li>
            <li class="active">Data</li>
        </ol>

        <h4 class="page-title">DASHBOARD</h4>

        <!-- Shortcuts -->
        <div class="block-area shortcut-area">
            <a class="shortcut tile" href="">
                <img src="/img/shortcuts/money.png" alt="">
                <small class="t-overflow">Purchases</small>
            </a>
            <a class="shortcut tile" href="">
                <img src="/img/shortcuts/twitter.png" alt="">
                <small class="t-overflow">Tweets</small>
            </a>
            <a class="shortcut tile" href="">
                <img src="/img/shortcuts/calendar.png" alt="">
                <small class="t-overflow">Calendar</small>
            </a>
            <a class="shortcut tile" href="">
                <img src="/img/shortcuts/stats.png" alt="">
                <small class="t-overflow">Statistics</small>
            </a>
            <a class="shortcut tile" href="">
                <img src="/img/shortcuts/connections.png" alt="">
                <small class="t-overflow">Connection</small>
            </a>
            <a class="shortcut tile" href="">
                <img src="/img/shortcuts/reports.png" alt="">
                <small class="t-overflow">Reports</small>
            </a>
        </div>

        <hr class="whiter" />

        <!-- Quick Stats -->
        <div class="block-area">
            <div class="row">
                <div class="col-md-3 col-xs-6">
                    <div class="tile quick-stats">
                        <div id="stats-line-2" class="pull-left"></div>
                        <div class="data">
                            <h2 data-value="98">0</h2>
                            <small>Tickets Today</small>
                        </div>
                    </div>
                </div>

                <div class="col-md-3 col-xs-6">
                    <div class="tile quick-stats media">
                        <div id="stats-line-3" class="pull-left"></div>
                        <div class="media-body">
                            <h2 data-value="1452">0</h2>
                            <small>Shipments today</small>
                        </div>
                    </div>
                </div>

                <div class="col-md-3 col-xs-6">
                    <div class="tile quick-stats media">

                        <div id="stats-line-4" class="pull-left"></div>

                        <div class="media-body">
                            <h2 data-value="4896">0</h2>
                            <small>Orders today</small>
                        </div>
                    </div>
                </div>

                <div class="col-md-3 col-xs-6">
                    <div class="tile quick-stats media">
                        <div id="stats-line" class="pull-left"></div>
                        <div class="media-body">
                            <h2 data-value="29356">0</h2>
                            <small>Site visits today</small>
                        </div>
                    </div>
                </div>

            </div>

        </div>

        <hr class="whiter" />

        <!-- Main Widgets -->

        <div class="block-area">
            <div class="row">
                <div class="col-md-8">
                    <!-- Main Chart -->
                    <div class="tile">
                        <h2 class="tile-title"><i class="icon">&#61738;</i>&nbsp;&nbsp;CPU使用率</h2>
                        <div class="p-t-10 p-r-5 p-b-5">
                            <div style="height: 200px; padding: 0px; position: relative;" id="dynamic-chart"></div>
                        </div>
                    </div>

                    <!-- Pies -->
                    <div class="tile ">
                        <h2 class="tile-title"><i class="icon">&#61804;</i>&nbsp;&nbsp;磁盘用量监控</h2>
                        <div class="tile-config dropdown">
                            <select class="form-control input-sm m-b-10" style="width: 120px;" id="workerId">
                                <c:forEach var="w" items="${workers}">
                                    <option value="${w.workerId}" ${w.workerId eq workerId ? 'selected' : ''}>${w.name}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="tile-dark p-10 text-center" id="diskpie"></div>
                    </div>


                    <!--  Recent Postings -->
                    <div class="row">
                        <div class="col-md-6">
                            <div class="tile">
                                <h2 class="tile-title">Recent Postings</h2>
                                <div class="tile-config dropdown">
                                    <a data-toggle="dropdown" href="" class="tile-menu"></a>
                                    <ul class="dropdown-menu animated pull-right text-right">
                                        <li><a href="">Refresh</a></li>
                                        <li><a href="">Settings</a></li>
                                    </ul>
                                </div>

                                <div class="listview narrow">
                                    <div class="media p-l-5">
                                        <div class="pull-left">
                                            <img width="40" src="/img/profile-pics/1.jpg" alt="">
                                        </div>
                                        <div class="media-body">
                                            <small class="text-muted">2 Hours ago by Adrien San</small><br/>
                                            <a class="t-overflow" href="">Cras molestie fermentum nibh, ac semper</a>

                                        </div>
                                    </div>
                                    <div class="media p-l-5">
                                        <div class="pull-left">
                                            <img width="40" src="/img/profile-pics/2.jpg" alt="">
                                        </div>
                                        <div class="media-body">
                                            <small class="text-muted">5 Hours ago by David Villa</small><br/>
                                            <a class="t-overflow" href="">Suspendisse in purus ut nibh placerat</a>

                                        </div>
                                    </div>
                                    <div class="media p-l-5">
                                        <div class="pull-left">
                                            <img width="40" src="/img/profile-pics/3.jpg" alt="">
                                        </div>
                                        <div class="media-body">
                                            <small class="text-muted">On 15/12/2013 by Mitch bradberry</small><br/>
                                            <a class="t-overflow" href="">Cras pulvinar euismod nunc quis gravida. Suspendisse pharetra</a>

                                        </div>
                                    </div>
                                    <div class="media p-l-5">
                                        <div class="pull-left">
                                            <img width="40" src="/img/profile-pics/4.jpg" alt="">
                                        </div>
                                        <div class="media-body">
                                            <small class="text-muted">On 14/12/2013 by Mitch bradberry</small><br/>
                                            <a class="t-overflow" href="">Cras pulvinar euismod nunc quis gravida. </a>

                                        </div>
                                    </div>
                                    <div class="media p-l-5">
                                        <div class="pull-left">
                                            <img width="40" src="/img/profile-pics/5.jpg" alt="">
                                        </div>
                                        <div class="media-body">
                                            <small class="text-muted">On 13/12/2013 by Mitch bradberry</small><br/>
                                            <a class="t-overflow" href="">Integer a eros dapibus, vehicula quam accumsan, tincidunt purus</a>

                                        </div>
                                    </div>
                                    <div class="media p-5 text-center l-100">
                                        <a href=""><small>VIEW ALL</small></a>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Tasks to do -->
                        <div class="col-md-6">
                            <div class="tile">
                                <h2 class="tile-title">Tasks to do</h2>
                                <div class="tile-config dropdown">
                                    <a data-toggle="dropdown" href="" class="tile-menu"></a>
                                    <ul class="dropdown-menu pull-right text-right">
                                        <li id="todo-add"><a href="">Add New</a></li>
                                        <li id="todo-refresh"><a href="">Refresh</a></li>
                                        <li id="todo-clear"><a href="">Clear All</a></li>
                                    </ul>
                                </div>

                                <div class="listview todo-list sortable">
                                    <div class="media">
                                        <div class="checkbox m-0">
                                            <label class="t-overflow">
                                                <input type="checkbox">
                                                Curabitur quis nisi ut nunc gravida suscipis
                                            </label>
                                        </div>
                                    </div>
                                    <div class="media">
                                        <div class="checkbox m-0">
                                            <label class="t-overflow">
                                                <input type="checkbox">
                                                Suscipit at feugiat dewoo
                                            </label>
                                        </div>

                                    </div>
                                    <div class="media">
                                        <div class="checkbox m-0">
                                            <label class="t-overflow">
                                                <input type="checkbox">
                                                Gravida wendy lorem ipsum seen
                                            </label>
                                        </div>

                                    </div>
                                    <div class="media">
                                        <div class="checkbox m-0">
                                            <label class="t-overflow">
                                                <input type="checkbox">
                                                Fedrix quis nisi ut nunc gravida suscipit at feugiat purus
                                            </label>
                                        </div>

                                    </div>
                                </div>

                                <h2 class="tile-title">Completed Tasks</h2>

                                <div class="listview todo-list sortable">
                                    <div class="media">
                                        <div class="checkbox m-0">
                                            <label class="t-overflow">
                                                <input type="checkbox" checked="checked">
                                                Motor susbect win latictals bin the woodat cool
                                            </label>
                                        </div>

                                    </div>
                                    <div class="media">
                                        <div class="checkbox m-0">
                                            <label class="t-overflow">
                                                <input type="checkbox" checked="checked">
                                                Wendy mitchel susbect win latictals bin the woodat cool
                                            </label>
                                        </div>

                                    </div>
                                    <div class="media">
                                        <div class="checkbox m-0">
                                            <label class="t-overflow">
                                                <input type="checkbox" checked="checked">
                                                Latictals bin the woodat cool for the win
                                            </label>
                                        </div>

                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="clearfix"></div>
                </div>

                <div class="col-md-4">

                    <!-- Dynamic Chart -->
                    <div class="tile">
                        <h2 class="tile-title"><i class="icon">&#61881;</i>&nbsp;&nbsp;内存使用率</h2>
                        <div class="p-t-10 p-r-5 p-b-5">
                            <div id="line-chart" class="main-chart" style="height: 250px"></div>
                        </div>
                    </div>

                    <!-- Activity -->
                    <div class="tile">
                        <h2 class="tile-title">Social Media activities</h2>
                        <div class="tile-config dropdown">
                            <a data-toggle="dropdown" href="" class="tile-menu"></a>
                            <ul class="dropdown-menu pull-right text-right">
                                <li><a href="">Refresh</a></li>
                                <li><a href="">Settings</a></li>
                            </ul>
                        </div>

                        <div class="listview narrow">

                            <div class="media">
                                <div class="pull-right">
                                    <div class="counts">367892</div>
                                </div>
                                <div class="media-body">
                                    <h6>FACEBOOK LIKES</h6>
                                </div>
                            </div>

                            <div class="media">
                                <div class="pull-right">
                                    <div class="counts">2012</div>
                                </div>
                                <div class="media-body">
                                    <h6>GOOGLE +1s</h6>
                                </div>
                            </div>

                            <div class="media">
                                <div class="pull-right">
                                    <div class="counts">56312</div>
                                </div>
                                <div class="media-body">
                                    <h6>YOUTUBE VIEWS</h6>
                                </div>
                            </div>

                            <div class="media">
                                <div class="pull-right">
                                    <div class="counts">785879</div>
                                </div>
                                <div class="media-body">
                                    <h6>TWITTER FOLLOWERS</h6>
                                </div>
                            </div>
                            <div class="media">
                                <div class="pull-right">
                                    <div class="counts">68</div>
                                </div>
                                <div class="media-body">
                                    <h6>WEBSITE COMMENTS</h6>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="clearfix"></div>
            </div>
        </div>

        <!-- Chat -->
        <div class="chat">

            <!-- Chat List -->
            <div class="pull-left chat-list">
                <div class="listview narrow">
                    <div class="media">
                        <img class="pull-left" src="/img/profile-pics/1.jpg" width="30" alt="">
                        <div class="media-body p-t-5">
                            Alex Bendit
                        </div>
                    </div>
                    <div class="media">
                        <img class="pull-left" src="/img/profile-pics/2.jpg" width="30" alt="">
                        <div class="media-body">
                            <span class="t-overflow p-t-5">David Volla Watkinson</span>
                        </div>
                    </div>
                    <div class="media">
                        <img class="pull-left" src="/img/profile-pics/3.jpg" width="30" alt="">
                        <div class="media-body">
                            <span class="t-overflow p-t-5">Mitchell Christein</span>
                        </div>
                    </div>
                    <div class="media">
                        <img class="pull-left" src="/img/profile-pics/4.jpg" width="30" alt="">
                        <div class="media-body">
                            <span class="t-overflow p-t-5">Wayne Parnell</span>
                        </div>
                    </div>
                    <div class="media">
                        <img class="pull-left" src="/img/profile-pics/5.jpg" width="30" alt="">
                        <div class="media-body">
                            <span class="t-overflow p-t-5">Melina April</span>
                        </div>
                    </div>
                    <div class="media">
                        <img class="pull-left" src="/img/profile-pics/6.jpg" width="30" alt="">
                        <div class="media-body">
                            <span class="t-overflow p-t-5">Ford Harnson</span>
                        </div>
                    </div>

                </div>
            </div>

            <!-- Chat Area -->
            <div class="media-body">
                <div class="chat-header">
                    <a class="btn btn-sm" href="">
                        <i class="fa fa-circle-o status m-r-5"></i> Chat with Friends
                    </a>
                </div>

                <div class="chat-body">
                    <div class="media">
                        <img class="pull-right" src="/img/profile-pics/1.jpg" width="30" alt="" />
                        <div class="media-body pull-right">
                            Hiiii<br/>
                            How you doing bro?
                            <small>Me - 10 Mins ago</small>
                        </div>
                    </div>

                    <div class="media pull-left">
                        <img class="pull-left" src="/img/profile-pics/2.jpg" width="30" alt="" />
                        <div class="media-body">
                            I'm doing well buddy. <br/>How do you do?
                            <small>David - 9 Mins ago</small>
                        </div>
                    </div>

                    <div class="media pull-right">
                        <img class="pull-right" src="/img/profile-pics/2.jpg" width="30" alt="" />
                        <div class="media-body">
                            I'm Fine bro <br/>Thank you for asking
                            <small>Me - 8 Mins ago</small>
                        </div>
                    </div>

                    <div class="media pull-right">
                        <img class="pull-right" src="/img/profile-pics/2.jpg" width="30" alt="" />
                        <div class="media-body">
                            Any idea for a hangout?
                            <small>Me - 8 Mins ago</small>
                        </div>
                    </div>

                </div>

                <div class="chat-footer media">
                    <i class="chat-list-toggle pull-left fa fa-bars"></i>
                    <i class="pull-right fa fa-picture-o"></i>
                    <div class="media-body">
                        <textarea class="form-control" placeholder="Type something..."></textarea>
                    </div>
                </div>

            </div>
        </div>
    </section>

<jsp:include page="/WEB-INF/common/footer.jsp"/>

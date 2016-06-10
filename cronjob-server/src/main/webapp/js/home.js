var cronjobChart = {
    refresh:false,
    path:"/",
    intervalId: null,
    intervalTime: 2000,
    gauge: null,
    gaugeOption:null,
    data: null,
    socket:null,
    overviewDataArr: [
        {"key": "us", "title": "用户占用", "color": "rgba(221,68,255,0.90)"},
        {"key": "sy", "title": "系统占用", "color": "rgba(255,255,255,0.90)"},
        {"key": "memUsage", "title": "内存使用率", "color": ""},
        {"key": "id", "title": "Cpu空闲", "color": "rgba(92,184,92,0.90)"},
        {"key": "swap", "title": "Swap空闲", "color": "rgba(240,173,78,0.90)"}
    ],

    monitorData: function () {

        /**
         * 关闭上一个websocket
         */
        if (this.socket) {
            this.socket.close();
            this.socket = null;
        }

        var diskLoad = false;
        var cpuLoad = false;
        var cpuChartObj = null;
        var cpuX = [];
        var cpuY = [];

        var networkLoad = false;
        var netChartObj = [];

        /**
         * 没有执行器
         */
        if(!$("#workerId").val()) {
            window.setTimeout(function () {
                $(".loader").remove();
            },1000);
            return;
        }

        $.ajax({
            url: cronjobChart.path+"/url",
            data: "workerId=" + $("#workerId").val(),
            dataType: "html",
            success: function (url) {
                cronjobChart.socket = io(url);
                cronjobChart.socket.on("monitor",function (data) {
                    $(".loader").remove();
                    //解决子页面登录失联,不能跳到登录页面的bug
                    if (data.toString().indexOf("login") > -1) {
                        window.location.href = cronjobChart.path;
                    } else {
                        cronjobChart.data = data;
                        if ( !diskLoad ) {
                            diskLoad = true;
                            cronjobChart.diskChart();
                        }

                        if ( !cpuLoad ) {
                            cpuLoad = true;
                            cronjobChart.createItemCpu();
                            cpuChartObj = echarts.init(document.getElementById('cpu-chart'));
                            var option = {};
                            cpuChartObj.setOption(option);
                        } else {
                            var opt = cronjobChart.cpuChart(cpuX, cpuY);
                            cpuChartObj.setOption(opt);
                            cronjobChart.gaugeOption.series[0].data[0].value = parseFloat(cronjobChart.data.memUsage);
                            cronjobChart.gauge.setOption(cronjobChart.gaugeOption, true);
                        }

                        cronjobChart.topData();

                        /*if ( !cronjobChart.refresh && !networkLoad ) {
                            networkLoad = true;
                            netChartObj = cronjobChart.networkChart();
                            cronjobChart.refresh = true;
                        } else {
                            var network = eval('(' + cronjobChart.data.network + ')');
                            var read = network.read;
                            var write = network.write;
                            var series = netChartObj.series[0];
                            var shift = series.data.length > 60 * 2;
                            netChartObj.xAxis[0].categories.push(data.time);
                            netChartObj.series[0].addPoint(parseFloat(read), true, shift);
                            netChartObj.series[1].addPoint(parseFloat(write), true, shift);
                        }*/

                    }
                });
                cronjobChart.socket.on("disconnect",function () {
                    console.log('close');
                    diskLoad = false;
                    cpuLoad = false;
                    cpuChartObj = null;
                    cpuX = [];
                    cpuY = [];
                    networkLoad = false;
                });
            }
        });
    },

    diskChart: function () {
        $("#overview-chart").html("").css("height", "auto");
        $("#disk-view").html("");
        $("#disk-item").html("");

        var diskArr = eval('(' + cronjobChart.data.diskUsage + ')');
        var freeTotal, usedTotal;

        for (var i in diskArr) {
            var disk = diskArr[i].disk;
            var used = parseFloat(diskArr[i].used);
            var free = parseFloat(diskArr[i].free);
            var val = parseInt((used / (used + free)) * 100);

            if (disk == "usage") {
                freeTotal = free;
                usedTotal = used;
                continue;
            }

            var colorCss = "";
            if (val < 60) {
                colorCss = "progress-bar-success";
            } else if (val < 80) {
                colorCss = "progress-bar-warning";
            } else {
                colorCss = "progress-bar-danger";
            }

            var html = '<div class="side-border"><h6><small style="font-weight: lighter"><i class="glyphicon glyphicon-hdd"></i>&nbsp;&nbsp;' + disk + '&nbsp;&nbsp;(已用:' + used + 'G/空闲:' + free + 'G)</small><div class="progress progress-small"><a href="#" data-toggle="tooltip" title="" class="progress-bar tooltips ' + colorCss + '" style="width: ' + val + '%;" data-original-title="' + val + '%"><span class="sr-only">' + val + '%</span></a></div></h6></div>';
            $("#disk-item").append(html);
        }

        $('#disk-view').highcharts({
            chart: {
                type: 'pie',
                backgroundColor: 'rgba(0,0,0,0)',
                options3d: {
                    enabled: true,
                    alpha: 45,
                    beta: 0
                }
            },
            colors: ['rgba(35,224,35,0.65)', 'rgba(237,26,26,0.65)'],
            title: {
                text: ''
            },
            tooltip: {
                pointFormat: '{series.name}:{point.percentage:.1f}%</b>'
            },
            plotOptions: {
                pie: {
                    allowPointSelect: true,
                    cursor: 'pointer',
                    depth: 30,
                    dataLabels: {
                        enabled: false,
                        format: '{point.name}'
                    }
                }
            },
            series: [{
                type: 'pie',
                name: '占比',
                data: [
                    ['空闲磁盘', parseFloat(freeTotal)],
                    ['已用磁盘', parseFloat(usedTotal)]
                ]
            }]
        });

        //config...
        $.each( eval('(' + cronjobChart.data.config + ')'),function(name,value){
            var css = {
                "font-size": "15px",
                "font-weight": "900",
                "color": "rgba(255,255,255,0.8)",
                "margin-top": "3px"
            };

            if (typeof(value) == 'string') {
                if (!value) {
                    $("#view-" + name).remove();
                } else {
                    $("#config-" + name).css(css).html(value);
                }
            } else {
                $.each(value, function (k, v) {
                    if (!v) {
                        $("#view-" + name + "-" + k).remove();
                    } else {
                        $("#config-" + name + "-" + k).css(css).html(v);
                    }
                });
            }
        });

        $("#config-view").fadeIn(1000);
    },

    createItemCpu: function () {
        var overdata = [];
        $.each(cronjobChart.overviewDataArr, function (i, elem) {
            $.each(cronjobChart.data, function (name, obj) {
                if (elem.key == name) {
                    if (name == "swap") {
                        overdata.push([elem.key, 100.00 - parseFloat(obj)]);
                    } else {
                        overdata.push([elem.key, parseFloat(obj)]);
                    }

                }
            });
            var _cpuData = eval('(' + cronjobChart.data.cpuData + ')');
            for (var k in _cpuData) {
                if (elem.key == k) {
                    overdata.push([k, parseFloat(_cpuData[k])]);
                }
            }
        });

        $.each(overdata, function (i, obj) {
            var key = obj[0];
            var val = obj[1];
            var title = "";

            $.each(cronjobChart.overviewDataArr, function (k, v) {
                if (i == k) {
                    title = v.title;
                }
            });

            if (i == 2) {
                var html = "<div style='display: inline-block;position: relative; margin: 3px -25px 0;'><div id=\"gauge-cpu\" class=\"gaugeChart\"></div><span class='gaugeChartTitle' ><i class=\"icon\" >&#61881;</i>&nbsp;" + title + "</span></div>";
                $("#overview-chart").append(html);
                cronjobChart.gauge = echarts.init(document.getElementById('gauge-cpu'));
                cronjobChart.gaugeOption = {
                    series : [
                        {
                            name:'内存使用率',
                            type:'gauge',
                            startAngle: 180,
                            endAngle: 0,
                            center : ['50%', '90%'],
                            radius : 115,
                            axisLine: {
                                lineStyle: {
                                    color: [[0.2, 'rgb(92,184,92)'],[0.8, 'rgb(240,173,78)'],[1, 'rgb(237,26,26)']],
                                    width: 6
                                }
                            },
                            axisTick: {
                                show : true,
                                splitNumber: 4,
                                length :8,
                                lineStyle: {
                                    color: 'auto'
                                }
                            },
                            splitLine: {
                                show: true,
                                length :10,
                                lineStyle: {
                                    color: 'auto'
                                }
                            },
                            pointer: {
                                width:4,
                                length: '90%',
                                color: 'rgba(255, 255, 255, 0.8)'
                            },
                            title : {
                                show : false
                            },
                            detail : {
                                show : true,
                                backgroundColor: 'rgba(0,0,0,0)',
                                borderWidth: 0,
                                borderColor: '#ccc',
                                offsetCenter: [0, -30],
                                formatter:'{value}%',
                                textStyle: {
                                    fontSize : 20
                                }
                            },
                            data:[{value: 50}]
                        }
                    ]
                };
                cronjobChart.gaugeOption.series[0].data[0].value = parseFloat(val);
                cronjobChart.gauge.setOption(cronjobChart.gaugeOption,true);
            } else {
                var html = $("<div class=\"pie-chart-tiny\" id=\"cpu_" + key + "\"><span class=\"percent\"></span><span style='font-weight: lighter' class=\"pie-title\" title='" + title + "(" + key + ")'><i class=\"icon\" >&#61881;</i>&nbsp;" + title + "</span></div>&nbsp;&nbsp;");
                html.easyPieChart({
                    easing: 'easeOutBounce',
                    barColor: cronjobChart.overviewDataArr[i].color,
                    trackColor: 'rgba(0,0,0,0.3)',
                    scaleColor: 'rgba(255,255,255,0.85)',
                    lineCap: 'square',
                    lineWidth: 4,
                    animate: 3000,
                    size:  $.isPC() ? ((i==1||i==3)?90:80) : 110,
                    onStep: function (from, to, percent) {
                        $(this.el).find('.percent').text(Math.round(percent));
                    }
                });
                html.data('easyPieChart').update(val);
                $("#overview-chart").append(html);
                if($.isMobile()) {
                    $(".percent").css("margin-top","35px");
                }
            }
        });

    },

    cpuChart: function (x, y) {
        $.each(cronjobChart.overviewDataArr, function (i, elem) {
            var overelem = $("#cpu_" + elem.key);
            if (overelem.length > 0) {
                $.each(cronjobChart.data, function (name, obj) {
                    if (elem.key == name) {
                        if (name == "swap") {
                            overelem.data('easyPieChart').update(100.00 - parseFloat(obj));
                        } else {
                            overelem.data('easyPieChart').update(parseFloat(obj));
                        }
                    }
                });
                var _cpuData = eval('(' + cronjobChart.data.cpuData + ')');
                for (var k in _cpuData) {
                    if (elem.key == k) {
                        overelem.data('easyPieChart').update(parseFloat(_cpuData[k]));
                    }
                }
            }
        });

        //添加新的
        x.push(cronjobChart.data.time);
        y.push(parseFloat(eval('(' + cronjobChart.data.cpuData + ')')["usage"]));

        if (y.length == 60 * 10) {
            x.shift();
            y.shift();
        }

        var max = parseInt(Math.max.apply({}, y)) + 1;
        if (max > 100) {
            max = 100;
        }
        return cpuOption = {
            tooltip: {
                trigger: 'axis',
                formatter: "监&nbsp;控&nbsp;时&nbsp;间&nbsp;&nbsp;: {b} <br />CPU使用率 : {c}%",
                textStyle: {
                    fontSize: 12
                }
            },
            title: {
                left: 'center',
                text: ''
            },
            grid: {
                top: '9%',
                left: '0%',
                right: '2%',
                bottom: '8%',
                containLabel: true
            },
            xAxis: {
                type: 'category',
                show: false,
                boundaryGap: false,
                splitLine: {show: false},
                data: x
            },
            yAxis: {
                type: 'value',
                boundaryGap: [0, '100%'],
                splitLine: {show: false},
                max: max,
                axisLabel: {
                    show: true,
                    textStyle: {color: 'rgba(255,255,255,0.80)'}
                },
                axisLine: {
                    lineStyle: {color: 'rgba(220,220,255,0.6)'}
                }
            },
            dataZoom: [{
                type: 'inside',
                start: 0,
            }, {
                start: 0,
                backgroundColor: 'rgba(0,0,0,0.05)',
                dataBackgroundColor: 'rgba(0,0,0,0.2)',
                fillerColor: 'rgba(0,0,0,0.3)',
                handleColor: 'rgba(0,0,0,0.9)',
                textStyle: {color: '#aaa'}
            }],
            series: [
                {
                    type: 'line',
                    smooth: false,
                    symbol: 'none',
                    sampling: 'average',
                    itemStyle: {
                        normal: {
                            color: 'rgba(255, 255, 255,0.3)'
                        }
                    },
                    areaStyle: {
                        normal: {
                            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                                offset: 0,
                                color: 'rgba(255, 255, 255,0.3)'
                            }, {
                                offset: 1,

                                color: 'rgba(20, 70, 155,0.2)'
                            }])
                        }
                    },
                    data: y
                }
            ]
        };
    },

    networkChart: function () {
        var network = eval('(' + cronjobChart.data.network + ')');
        var read = network.read;
        var write = network.write;
        return new Highcharts.Chart({
            chart: {
                type: 'spline',
                backgroundColor: 'rgba(0,0,0,0)',
                plotBorderColor: null,
                plotBackgroundColor: null,
                plotBackgroundImage: '',
                plotBorderWidth: null,
                height: 200,
                renderTo: "network-view"
            },
            title: {
                text: "",
                align: 'left',
                style: {
                    color: 'rgba(255,255,255,0.6)'
                }
            },
            subtitle: {
                text: ''
            },
            xAxis: {
                categories: [cronjobChart.data.time],
                boundaryGap: false,
                borderRadius: 2,
                labels: {
                    maxStaggerLines: 1,
                    style: {
                        color: 'rgba(220,220,255,0.8)',
                        fontSize: 0
                    }
                },
                splitLine: {show: false},
                tickLength: 0,
            },
            yAxis: {
                title: {text: ''},
                min: 0,
                minorGridLineWidth: 0,
                alternateGridColor: null,
                gridLineWidth: 0,
                labels: {
                    style: {
                        color: 'rgba(220,220,255,0.8)'
                    }
                },
                lineWidth: 1,
                lineColor: 'rgba(220,220,255,0.8)'
            },
            tooltip: {
                shared: true,
                borderColor: 'rgba(45,45,45,0)',
                backgroundColor: 'rgba(45,45,45,0.75)',
                borderRadius: 5,
                borderWidth: 2,
                shadow: true,
                animation: true,
                crosshairs: {
                    width: 1,
                    color: "rgba(255,255,255,0.5)"
                },
                formatter: function () {
                    var tipHtml = '<b style="color:rgba(255,255,255,0.85);font-size: 12px;">监控时间 :&nbsp;' + cronjobChart.data.time + '</b>';
                    $.each(this.points, function () {
                        tipHtml += '<div class="tooltip-item" style="color: rgba(255,255,255,0.85)"> <span> ' + this.series.name + '&nbsp;:&nbsp;</span>' + Highcharts.numberFormat(this.y, 0) + " kb/s</div>";
                    });
                    return tipHtml;
                },
                useHTML: true
            },

            plotOptions: {
                spline: {
                    lineWidth: 2,
                    states: {
                        hover: {
                            lineWidth: 3
                        }
                    },
                    marker: {
                        enabled: false
                    }
                }
            },

            series: [{
                name: '读取速度',
                data: [read],
                color: 'rgba(128,128,255,0.6)'
            }, {
                name: '写入速度',
                data: [write],
                color: 'rgba(255,205,205,0.7)'
            }]
            ,
            navigation: {
                menuItemStyle: {
                    fontSize: '10px'
                }
            }
        });
    },

    topData:function () {
        //title
        var html='<tr>'+
            '<td class="noborder" title="进程ID">PID</td>'+
            '<td class="noborder" title="进程所属的用户">USER</td>'+
            '<td class="noborder" title="虚拟内存">VIRI</td>'+
            '<td class="noborder" title="常驻内存">RES</td>'+
            '<td class="noborder" title="CPU使用占比">CPU</td>'+
            '<td class="noborder" title="内存使用占比">MEM</td>'+
            '<td class="noborder" title="持续时长">TIME</td>'+
            '<td class="noborder" title="所执行的命令">COMMAND</td>'+
            '</tr>';
        $.each( eval('(' + cronjobChart.data.top + ')') , function (i, data) {
            var text = "<tr>";
            var obj = eval('('+data+')');
            for (var k in obj) {
                if ('cpu' === k || 'mem' === k) {
                    var cpu = '<td><div class="progress progress-small progress-white">'+
                        '<div class="progress-bar progress-bar-white" role="progressbar" data-percentage="'+obj[k]+'%" style="width:'+obj[k]+'%" aria-valuemin="0" aria-valuemax="100"></div>'+
                        '</div></td>';
                    text += cpu;
                }else {
                    text += ("<td>"+obj[k]+"</td>");
                }
            }
            text+='</tr>';
            html+=text;
        });
        $("#topbody").html(html);
    },

    executeChart: function () {

        if($.isMobile()){
            $("#overview_pie_div").remove();
            $("#report_detail").remove();
            $("#overview_report_div").removeClass("col-xs-7").addClass("col-xs-12")
        }

        $.ajax({
            url: cronjobChart.path+"/diffchart",
            data: {
                "startTime": $("#startTime").val(),
                "endTime": $("#endTime").val()
            },
            dataType: "json",
            success: function (data) {
                if ( data!=null ) {

                    //折线图
                    var dataArea = [];
                    var successSum = 0;
                    var failureSum = 0;
                    var killedSum = 0;
                    var singleton = 0;
                    var flow = 0;
                    var crontab = 0;
                    var quartz = 0;
                    var rerun = 0;
                    var auto = 0;
                    var operator = 0;

                    for (var i in data) {
                        dataArea.push({
                            date: data[i].date,
                            success: data[i].success,
                            failure: data[i].failure,
                            killed: data[i].killed
                        });
                        successSum += parseInt(data[i].success);
                        failureSum += parseInt(data[i].failure);
                        killedSum += parseInt(data[i].killed);

                        singleton += parseInt(data[i].singleton);
                        flow += parseInt(data[i].flow);
                        crontab += parseInt(data[i].crontab);
                        quartz += parseInt(data[i].quartz);
                        rerun += parseInt(data[i].rerun);
                        auto += parseInt(data[i].auto);
                        operator += parseInt(data[i].operator);
                    }

                    $("#overview_loader").hide();
                    $("#overview_report_div").show();

                    $("#overview_report").html('');
                    if ($.isPC()) {
                        $("#overview_pie").html('');
                        $("#overview_pie_div").show();
                        $("#report_detail").show();

                        var job_type = parseFloat(auto / (auto + operator)) * 100;
                        if (isNaN(job_type)) {
                            $("#job_type").attr("aria-valuenow", 0).css("width", "0%");
                        } else {
                            $("#job_type").attr("aria-valuenow", job_type).css("width", job_type + "%");
                        }

                        var job_category = parseFloat(singleton / (singleton + flow)) * 100;
                        if (isNaN(job_category)) {
                            $("#job_category").attr("aria-valuenow", 0).css("width", "0%");
                        } else {
                            $("#job_category").attr("aria-valuenow", job_category).css("width", job_category + "%");
                        }

                        var job_model = parseFloat(crontab / (crontab + quartz)) * 100;
                        if (isNaN(job_model)) {
                            $("#job_model").attr("aria-valuenow", 0).css("width", "0%");
                        } else {
                            $("#job_model").attr("aria-valuenow", job_model).css("width", job_model + "%");
                        }

                        var job_rerun = parseFloat((successSum + failureSum + killedSum - rerun) / (successSum + failureSum + killedSum)) * 100;
                        if (isNaN(job_rerun)) {
                            $("#job_rerun").attr("aria-valuenow", 0).css("width", "0%");
                        } else {
                            $("#job_rerun").attr("aria-valuenow", job_rerun).css("width", job_rerun + "%");
                        }

                        var job_status = parseFloat(successSum / (successSum + failureSum + killedSum)) * 100;
                        if (isNaN(job_status)) {
                            $("#job_status").attr("aria-valuenow", 0).css("width", "0%");
                        } else {
                            $("#job_status").attr("aria-valuenow", job_status).css("width", job_status + "%");
                        }

                    }

                    Morris.Line({
                        element: 'overview_report',
                        data: dataArea,
                        grid: true,
                        axes: true,
                        xkey: 'date',
                        ykeys: ['success', 'failure', 'killed'],
                        labels: ['成功', '失败', '被杀'],
                        lineColors: ['rgba(205,224,255,0.5)', 'rgba(237,26,26,0.5)', 'rgba(0,0,0,0.5)'],
                        hoverFillColor: 'rgb(45,45,45)',
                        lineWidth: 4,
                        pointSize: 5,
                        hideHover: 'auto',
                        smooth: false,
                        resize: true
                    });

                    if ($.isPC()) {
                        $('#overview_pie').highcharts({
                            chart: {
                                backgroundColor: 'rgba(0,0,0,0)',
                                plotBackgroundColor: null,
                                plotBorderWidth: null,
                                plotShadow: true,
                                options3d: {
                                    enabled: true,
                                    alpha: 25,
                                    beta: 0
                                }
                            },
                            colors: ['rgba(255,255,255,0.45)', 'rgba(237,26,26,0.45)', 'rgba(0,0,0,0.45)'],
                            title: {text: ''},
                            tooltip: {
                                pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
                            },
                            plotOptions: {
                                pie: {
                                    allowPointSelect: true,
                                    cursor: 'pointer',
                                    dataLabels: {
                                        enabled: false
                                    },
                                    showInLegend: true,
                                    depth: 40
                                }
                            },
                            series: [{
                                type: 'pie',
                                name: '占比',

                                data: [
                                    ['成功', successSum],
                                    ['失败', failureSum],
                                    ['被杀', killedSum]
                                ]

                            }]
                        });
                    }
                }
            }
        });
    }
}

$(document).ready(function () {
    var homejs = document.getElementById('homejs').src;
    cronjobChart.path = homejs.substr(homejs.indexOf("?")+1);
    cronjobChart.monitorData();
    cronjobChart.executeChart();

    $("#workerId").change(
        function(){
            cronjobChart.refresh = false;
            cronjobChart.monitorData();
        }
    );
});

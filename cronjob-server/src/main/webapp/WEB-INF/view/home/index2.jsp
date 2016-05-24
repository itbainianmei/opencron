




<!DOCTYPE html>
<html lang="en">
<head>



    <title>CronJob linux job system</title>
    <meta name="format-detection" content="telephone=no">
    <meta name="description" content="CronJob">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="keywords" content="cronjob,crontab,a better crontab,Let's crontab easy">
    <meta name="author" content="author:benjobs,wechat:wolfboys,Created by languang(http://u.languang.com) @ 2016" />

    <!-- CSS -->
    <link href="/css/bootstrap.min.css" rel="stylesheet">
    <link href="/css/animate.min.css" rel="stylesheet">
    <link href="/css/font-awesome.min.css" rel="stylesheet">
    <link href="/css/font-awesome-ie7.min.css" rel="stylesheet">
    <link href="/css/form.css" rel="stylesheet">
    <link href="/css/calendar.css" rel="stylesheet">
    <link href="/css/style.css" rel="stylesheet">
    <link href="/css/icons.css" rel="stylesheet">
    <link href="/css/generics.css" rel="stylesheet">
    <link href='/css/sweetalert.css' rel='stylesheet'>
    <link href='/css/cronjob.css' rel='stylesheet'>
    <link href='/css/loading.css' rel='stylesheet'>
    <link href='/css/morris.css' rel='stylesheet'>
    <link href='/css/prettify.min.css' rel='stylesheet'>

    <!-- Javascript Libraries -->
    <!-- jQuery -->
    <script src="/js/jquery.min.js"></script> <!-- jQuery Library -->
    <script src="/js/jquery-ui.min.js"></script> <!-- jQuery UI -->
    <script src="/js/jquery.easing.1.3.js"></script> <!-- jQuery Easing - Requirred for Lightbox + Pie Charts-->

    <!-- Bootstrap -->
    <script src="/js/bootstrap.min.js"></script>

    <script src="/js/easypiechart.js"></script> <!-- EasyPieChart - Animated Pie Charts -->

    <!--  Form Related -->
    <script src="/js/icheck.js"></script> <!-- Custom Checkbox + Radio -->

    <script src="/js/select.min.js"></script> <!-- Custom Select -->

    <!-- UX -->
    <script src="/js/scroll.min.js"></script> <!-- Custom Scrollbar -->

    <!-- Other -->
    <script src="/js/calendar.min.js"></script> <!-- Calendar -->
    <script src="/js/feeds.min.js"></script> <!-- News Feeds -->

    <script src="/js/raphael.2.1.2-min.js"></script>

    <script src="/js/prettify.min.js"></script>

    <script src="/js/morris.min.js"></script>

    <script src="/js/jquery.sparkline.min.js"></script>

    <!-- All JS functions -->
    <script src="/js/functions.js"></script>

    <script src="/js/testdevice.js"></script>

    <!-- MD5 -->
    <script src="/js/md5.js"></script>

    <script src="/js/html5.js"></script>

    <script src="/js/gauge.js"></script>

    <script src="/js/jquery.cookie.js"></script>

    <script src="/js/My97DatePicker/WdatePicker.js"></script>

    <script src="/js/sweetalert.min.js"></script>

    <script src="/js/cronjob.js"></script>



    <script type="text/javascript">
        $(document).ready(function(){
            setInterval(function(){

                $("#highlight").fadeOut(3000,function(){
                    $(this).show();
                });

                $.ajax({
                    url:"/record/running",
                    data:{
                        "refresh":1,
                        "size":"",
                        "queryTime":"",
                        "workerId":"",
                        "jobId":"",
                        "execType":"",
                        "pageNo":1,
                        "pageSize":15
                    },
                    dataType:"html",
                    success:function(data){
                        //解决子页面登录失联,不能跳到登录页面的bug
                        if(data.indexOf("login")>-1){
                            window.location.href="/";
                        }else {
                            $("#tableContent").html(data);
                        }
                    }
                });
            },5000);

            $("#size").change(function(){doUrl();});
            $("#workerId").change(function(){doUrl();});
            $("#jobId").change(function(){doUrl();});
            $("#execType").change(function(){doUrl();});
        });
        function doUrl() {
            var pageSize = $("#size").val();
            var queryTime = $("#queryTime").val();
            var workerId = $("#workerId").val();
            var jobId = $("#jobId").val();
            var execType = $("#execType").val();
            window.location.href = "/record/running?queryTime=" + queryTime + "&workerId=" + workerId + "&jobId=" + jobId + "&execType=" + execType + "&pageSize=" + pageSize;
        }

        function killJob(id){
            swal({
                title: "",
                text: "您确定要结束这个任务吗？",
                type: "warning",
                showCancelButton: true,
                closeOnConfirm: false,
                confirmButtonText: "结束",
            }, function() {
                $("#process_"+id).html("停止中");
                $.ajax({
                    url:"/record/kill",
                    data:{"recordId":id}
                });
                alertMsg("结束请求已发送");
                return;
            });

        }

        function restartJob(id,jobId){
            swal({
                title: "",
                text: "您确定要结束并重启这个任务吗？",
                type: "warning",
                showCancelButton: true,
                closeOnConfirm: false,
                confirmButtonText: "重启",
            }, function() {
                $("#process_"+id).html("停止中");
                $.ajax({
                    url:"/record/kill",
                    data:{"recordId":id},
                    success:function(result){
                        if (result == "true"){
                            $.ajax({
                                url:"/job/execute",
                                data:{"id":jobId}
                            });

                        }
                    }
                });
                alertMsg( "该任务已重启,正在执行中.");

                return;
            });

        }

    </script>
</head>




<script type="text/javascript">
    $(document).ready(function() {



        if($.isMobile()){
            $("#time").remove();
            $("#log1").text(" CronJob! Let's crontab easy")
        }

        var skin = $.cookie("cronjob_skin");
        if(skin) {
            $('body').attr('id', skin);
        }

        $('body').on('click', '.template-skins > a', function(e){
            e.preventDefault();
            var skin = $(this).data('skin');
            $('body').attr('id', skin);
            $('#changeSkin').modal('hide');
            $.cookie("cronjob_skin", skin, {
                expires : 30,
                domain:document.domain,
                path:"/"
            });
        });

        $.ajax({
            url: "/notice/info",
            dataType: "html",
            success: function (data) {
                $("#msgList").html(data);
            }
        });
    });
</script>

<body id="skin-blur-night">
<header id="header" class="media">
    <a href="" id="menu-toggle" style="background-image: none"><i class="icon">&#61773;</i></a>
    <a class="logo pull-left" href="/home" id="log1">CronJob V1.0.0</a>
    <div class="media-body">
        <div class="media" id="top-menu" style="float:right;margin-right:15px;">
            <div class="pull-left tm-icon" id="msg-icon">
                <a  class="drawer-toggle" data-drawer="messages" id="toggle_message" href="#">
                    <i class="sa-top-message icon" style="background-image:none;font-size: 32px; background-size: 25px 17px;">&#61710;</i>
                    <i class="n-count">5</i>
                </a>
            </div>
            <div id="time" style="float:right;">
                <span id="hours"></span>:<span id="min"></span>:<span id="sec"></span>
            </div>
        </div>

    </div>
</header>

<div class="clearfix"></div>

<section id="main" class="p-relative" role="main">

    <!-- Sidebar -->
    <aside id="sidebar">

        <!-- Sidbar Widgets -->
        <div class="side-widgets overflow">
            <!-- Profile Menu -->
            <div class="text-center s-widget m-b-25 dropdown" id="profile-menu">
                <a href="" data-toggle="dropdown">
                    <img class="profile-pic animated" src="/img/profile-pic.jpg" alt="">
                </a>
                <h4 class="m-0">benjobs</h4>
                <ul class="dropdown-menu profile-menu">
                    <li><a href="/user/self">个人信息</a> <i class="icon left">&#61903;</i><i class="icon right">&#61815;</i></li>
                    <li><a href="/notice/view">通知&nbsp;&&nbsp;消息</a> <i class="icon left">&#61903;</i><i class="icon right">&#61815;</i></li>
                    <li><a href="/logout">退出登录</a> <i class="icon left">&#61903;</i><i class="icon right">&#61815;</i></li>
                </ul>
            </div>

            <!-- Calendar -->
            <div class="s-widget m-b-25">
                <div id="sidebar-calendar"></div>
            </div>

        </div>

        <!-- Side Menu -->
        <ul class="list-unstyled side-menu">
            <li class="">
                <a href="/home">
                    <i aria-hidden="true" class="fa fa-tachometer"></i><span class="menu-item">效果报告</span>
                </a>
            </li>
            <li class="">
                <a  href="/worker/view">
                    <i aria-hidden="true" class="fa fa-desktop"></i><span class="menu-item">执行器管理</span>
                </a>
            </li>
            <li class="">
                <a href="/job/view">
                    <i aria-hidden="true" class="fa fa-tasks"></i><span class="menu-item">任务管理</span>
                </a>
            </li>

            <li class="dropdown active">
                <a href="#">
                    <i class="fa fa-print" aria-hidden="true"></i><span class="menu-item">调度记录</span>
                </a>
                <ul class="list-unstyled menu-item">
                    <li class="active">
                        <a href="/record/running" class="active">正在运行</a>
                    </li>
                    <li>
                        <a href="/record/done" class="">已完成</a>
                    </li>
                </ul>
            </li>


            <li class="">
                <a href="/user/view">
                    <i class="fa fa-user" aria-hidden="true"></i></i><span class="menu-item">用户管理</span>
                </a>
            </li>
            <li class="">
                <a href="/config/view">
                    <i aria-hidden="true" class="fa fa-cog"></i><span class="menu-item">系统设置</span>
                </a>
            </li>

        </ul>
    </aside>

    <!-- Content -->
    <section id="content" class="container">

        <!-- Messages Drawer -->
        <style type="text/css">
            .media a:hover {
                color: yellow;
            }
        </style>
        <div id="messages" class="tile drawer animated">
            <div class="listview narrow">
                <div class="media">
                    <i class="glyphicon glyphicon-bell"></i>&nbsp;<a href="#">通知&nbsp;&&nbsp;消息&nbsp;&&nbsp;短信</a>
                    <span class="drawer-close">&times;</span>

                </div>
                <div class="overflow" id="msgList">

                </div>
                <div class="media text-center whiter l-100">
                    <i class="glyphicon glyphicon-eye-open"></i>&nbsp;<a href="/notice/view">查看全部</a>
                </div>
            </div>
        </div>

        <!-- Breadcrumb -->
        <ol class="breadcrumb hidden-xs">
            <li class="icon">&#61753;</li>
            当前位置：
            <li><a href="#">CronJob</a></li>
            <li><a href="#">调度记录</a></li>
            <li><a href="#">正在运行</a></li>
        </ol>
        <h4 class="page-title"><i aria-hidden="true" class="fa fa-play-circle"></i>&nbsp;正在运行&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span id="highlight" style="font-size: 14px"><img src='/img/icon-loader.gif' style="width: 14px;height: 14px">&nbsp;调度任务持续进行中...</span></h4>
        <div class="block-area" id="defaultStyle">

            <div>
                <div style="float: left">
                    <label>
                        每页 <select size="1" class="select-self" id="size" style="width: 50px;">
                        <option value="15">15</option>
                        <option value="30" >30</option>
                        <option value="50" >50</option>
                        <option value="100" >100</option>
                    </select> 条记录
                    </label>
                </div>

                <div style="float: right;margin-bottom: 10px">
                    <label for="workerId">执行器：</label>
                    <select id="workerId" name="workerId" class="select-self" style="width: 120px;">
                        <option value="">全部</option>

                        <option value="5" >union-stat</option>

                        <option value="35" >union-test</option>

                        <option value="33" >www_so_usb</option>

                        <option value="34" >union-moniter</option>

                        <option value="36" >log-master</option>

                        <option value="37" >union-web</option>

                        <option value="38" >log-slave</option>

                        <option value="39" >hadoop-master</option>

                        <option value="40" >sysceo</option>

                    </select>
                    &nbsp;&nbsp;&nbsp;
                    <label for="jobId">任务名称：</label>
                    <select id="jobId" name="jobId" class="select-self" style="width: 80px;">
                        <option value="">全部</option>

                        <option value="55" >testjob&nbsp;</option>

                        <option value="59" >curLog&nbsp;</option>

                        <option value="60" >logBackup&nbsp;</option>

                        <option value="62" >mysqlDayBakup&nbsp;</option>

                        <option value="63" >monthPayment&nbsp;</option>

                        <option value="64" >dayPayment&nbsp;</option>

                        <option value="65" >importPc&nbsp;</option>

                        <option value="66" >reportMysql&nbsp;</option>

                        <option value="67" >cleanUser&nbsp;</option>

                        <option value="68" >mysqlDayBakup&nbsp;</option>

                        <option value="69" >monthPayment&nbsp;</option>

                        <option value="70" >dayPayment&nbsp;</option>

                        <option value="71" >importPc&nbsp;</option>

                        <option value="83" >dhbtest&nbsp;</option>

                        <option value="72" >importHadoop&nbsp;</option>

                        <option value="73" >reportHadoop&nbsp;</option>

                        <option value="74" >reportMysql&nbsp;</option>

                        <option value="75" >cutLog&nbsp;</option>

                        <option value="76" >cutLog&nbsp;</option>

                        <option value="77" >mergeLog&nbsp;</option>

                        <option value="78" >mysqlMonthBakup	&nbsp;</option>

                        <option value="79" >mergeLog&nbsp;</option>

                        <option value="80" >sysceoJob&nbsp;</option>

                        <option value="81" >statJob&nbsp;</option>

                        <option value="82" >hadoopJob&nbsp;</option>

                        <option value="86" >benjobs&nbsp;</option>

                        <option value="87" >parse-log&nbsp;</option>

                    </select>
                    &nbsp;&nbsp;&nbsp;
                    <label for="execType">执行方式：</label>
                    <select id="execType" name="execType" class="select-self" style="width: 80px;">
                        <option value="">全部</option>
                        <option value="0" >自动</option>
                        <option value="1" >手动</option>
                        <option value="2" >重跑</option>
                    </select>
                    &nbsp;&nbsp;&nbsp;
                    <label for="queryTime">开始时间：</label>
                    <input type="text" id="queryTime" name="queryTime" value="" onfocus="WdatePicker({onpicked:function(){doUrl(); },dateFmt:'yyyy-MM-dd'})" class="Wdate select-self" style="width: 100px"/>
                </div>
            </div>

            <table class="table tile">
                <thead>
                <tr>
                    <th>任务名称</th>
                    <th>执行器</th>
                    <th>运行状态</th>
                    <th>执行方式</th>
                    <th>执行命令</th>
                    <th>开始时间</th>
                    <th>运行时长</th>
                    <th>任务类型</th>
                    <th><center>操作</center></th>
                </tr>
                </thead>

                <tbody id="tableContent">


                <tr>
                    <td><a href="/job/detail?id=87">parse-log</a></td>
                    <td><a href="/worker/detail?id=5">union-stat</a></td>
                    <td>
                        <div class="progress progress-striped progress-success active" style="margin-top:3px;width: 80%;height: 14px;" >
                            <div style="width:100%;height: 100%;" class="progress-bar">
                                &nbsp;&nbsp;
                                <span id="process_455">
                                    运行中

                                </span>
                                ...&nbsp;&nbsp;
                            </div>
                        </div>
                    </td>
                    <td>

                        <span class="label label-info">&nbsp;&nbsp;手&nbsp;动&nbsp;&nbsp;</span>

                    </td>
                    <td title="sh  /data_disk/job/stat-parser/bin/startup.sh -i /data_disk/logs/pclog/languang/bda/bda.gsie.cn_log.2016-05-17.txt -o /data_disk/job/stat-parser/bda.sql">sh  /data_disk/job/stat-parser...</td>
                    <td>2016-05-18 15:01:10</td>
                    <td>5秒</td>
                    <td>

                        单一任务
                    </td>
                    <td><center>
                        <div class="visible-md visible-lg hidden-sm hidden-xs action-buttons">
                            <a href="#" onclick="killJob('455')" title="结束">
                                <i class="glyphicon glyphicon-stop"></i>
                            </a>&nbsp;&nbsp;

                            <a href="#" onclick="restartJob('455','87')" title="结束并重启">
                                <i class="glyphicon glyphicon-refresh"></i>
                            </a>&nbsp;&nbsp;

                        </div>
                    </center>
                    </td>
                </tr>

                </tbody>
            </table>

            <ul class='pagination fr mr20'><li class='disabled'><a href='javascript:void();'>首页</a></li><li class='disabled'><a href='javascript:void();'>上一页</a></li><li class='active'><a href='javascript:void();'>1</a></li><li class='disabled'><a href='javascript:void();'>下一页</a></li><li class='disabled'><a href='javascript:void();'>末页</a></li></ul>

        </div>

    </section>
    <br/><br/>

</section>

body>
</html>


    <!-- Older IE Message -->
    <!--[if lt IE 9]>
    <div class="ie-block">
        <h1 class="Ops">Ooops!</h1>
        <p> 您正在使用一个过时的互联网浏览器，升级到下列任何一个网络浏览器，以访问该网站的最大功能。 </p>
        <ul class="browsers">
            <li>
                <a href="https://www.google.com/intl/en/chrome/browser/">
                    <img src="/img/browsers/chrome.png" alt="">
                    <div>Google Chrome</div>
                </a>
            </li>
            <li>
                <a href="http://www.mozilla.org/en-US/firefox/new/">
                    <img src="/img/browsers/firefox.png" alt="">
                    <div>Mozilla Firefox</div>
                </a>
            </li>
            <li>
                <a href="http://www.opera.com/computer/windows">
                    <img src="/img/browsers/opera.png" alt="">
                    <div>Opera</div>
                </a>
            </li>
            <li>
                <a href="http://safari.en.softonic.com/">
                    <img src="/img/browsers/safari.png" alt="">
                    <div>Safari</div>
                </a>
            </li>
            <li>
                <a href="http://windows.microsoft.com/en-us/internet-explorer/downloads/ie-10/worldwide-languages">
                    <img src="/img/browsers/ie.png" alt="">
                    <div>Internet Explorer(New)</div>
                </a>
            </li>
        </ul>
        <p>请升级您的浏览器以便带来更好更好的用户体验 <br/>谢谢...</p>
    </div>
    <![endif]-->



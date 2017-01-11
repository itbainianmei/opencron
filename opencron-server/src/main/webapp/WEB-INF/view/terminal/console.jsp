<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="cron" uri="http://www.opencron.org" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <link rel="shortcut icon" href="${contextPath}/img/terminal.png" />
    <script type="text/javascript" src="${contextPath}/js/jquery.min.js"></script> <!-- jQuery Library -->
    <script type="text/javascript" src="${contextPath}/js/term.js"></script>
    <script type="text/javascript" src="${contextPath}/js/opencron.term.js"></script>
    <link rel="stylesheet" href="${contextPath}/css/font-awesome.css" >
    <link rel="stylesheet" href="${contextPath}/css/font-awesome-ie7.min.css" >
    <script type="text/javascript" src="${contextPath}/js/opencron.js"></script>

    <!-- Bootstrap -->
    <link rel="stylesheet" href="${contextPath}/css/bootstrap.css" >
    <script type="text/javascript" src="${contextPath}/js/bootstrap.js"></script>
    <link rel="stylesheet" href="${contextPath}/css/opencron.term.css" >

    <title>opencron Terminal</title>
</head>

<body>

<div class="navbar navbar-default navbar-fixed-top" role="navigation">
    <div class="container">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
        </div>
        <div class="collapse navbar-collapse">
            <ul class="nav navbar-nav">
                <li><a href="javascript:void(0)" data-original-title="" title="">Opencron</a></li>

                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown" data-original-title="" title=""><i aria-hidden="true" class="fa fa-gear"></i>&nbsp;选择主题<b class="caret"></b></a>
                    <ul class="dropdown-menu theme" >
                        <li><a theme="yellow" href="javascript:void(0)"><span class="circle" style="background-color:yellow"></span>&nbsp;黄色</a></li>
                        <li><a theme="green" href="javascript:void(0)"><span class="circle" style="background-color:green"></span>&nbsp;绿色</a></li>
                        <li><a theme="black" href="javascript:void(0)"><span class="circle" style="background-color:black"></span>&nbsp;黑色</a></li>
                        <li><a theme="blue" href="javascript:void(0)"><span class="circle" style="background-color:blue"></span>&nbsp;蓝色</a></li>
                    </ul>
                </li>

                <li class="dropdown">
                    <a href="#" class="dropdown-toggle" data-toggle="dropdown" data-original-title="" title=""><i aria-hidden="true" class="fa fa-folder-open-o"></i>&nbsp;打开终端<b class="caret"></b></a>
                    <ul class="dropdown-menu">
                        <c:forEach var="t" items="${terms}">
                            <li><a href="${contextPath}/terminal/ssh2?id=${t.id}" target="_blank" data-original-title="" title="">${t.name}(${t.host})</a></li>
                        </c:forEach>
                    </ul>
                </li>
                <li><a href="${contextPath}/terminal/reopen?token=${token}" target="_blank" title=""><i aria-hidden="true" class="fa fa-window-restore"></i>&nbsp;复制终端</a></li>
                <li>
                    <div class="input-group input-group-sm" style="padding-top:8px;">
                        <span class="input-group-addon">中文输入</span>
                        <input type="text" class="form-control" id="chinese" style="width:300px;">
                        <span class="input-group-btn">
                        <button class="btn btn-default" id="chinput" type="button">发送</button>
                    </span>
                    </div>
                </li>
                <li style="float: right;margin-right: 5px;"><a href="https://github.com/wolfboys/opencron" target="_blank"><i aria-hidden="true" class="fa fa-github" style="font-size:30px;position:absolute;top:6px"></i></a></li>
            </ul>

        </div>
    </div>
</div>

<div id="term"></div>

</body>

<script type="text/javascript">
    $(document).ready(function () {
       document.title = '${name}';
       var term = new OpencronTerm();
       $(".theme").find("a").each(function (i,e) {
           var theme = $(e).attr('theme');
           $(e).click(function () {
               term.theme(theme);
           });
       });

    });
</script>
</html>
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
    <script type="text/javascript" src="${contextPath}/js/opencron.term.js?id=20170112"></script>
    <link rel="stylesheet" href="${contextPath}/css/font-awesome.css" >
    <link rel="stylesheet" href="${contextPath}/css/font-awesome-ie7.min.css" >
    <script type="text/javascript" src="${contextPath}/js/opencron.js"></script>

    <!-- Bootstrap -->
    <link rel="stylesheet" href="${contextPath}/css/bootstrap.css" >
    <script type="text/javascript" src="${contextPath}/js/bootstrap.js"></script>
    <link rel="stylesheet" href="${contextPath}/css/opencron.term.css?id=20170112" >

    <title>opencron Terminal</title>
</head>

<body>

<div class="navbar navbar-default navbar-fixed-top" role="navigation">
    <div class="container">
        <div class="collapse navbar-collapse">
            <ul class="nav navbar-nav">
                <li><a class="term-logo" href="javascript:void(0)" title="" title="">Opencron</a></li>
               <%--
                <li class="dropdown">
                    <a href="javascript:void(0)" class="dropdown-toggle" data-toggle="dropdown" title="选择主题"><i aria-hidden="true" class="fa fa-gear"></i>&nbsp;选择主题<b class="caret"></b></a>
                    <ul class="dropdown-menu theme" >
                        <li><a theme="yellow" href="javascript:void(0)"><span class="circle" style="background-color:yellow"></span>&nbsp;黄色</a></li>
                        <li><a theme="green" href="javascript:void(0)"><span class="circle" style="background-color:green"></span>&nbsp;绿色</a></li>
                        <li><a theme="black" href="javascript:void(0)"><span class="circle" style="background-color:black"></span>&nbsp;黑色</a></li>
                        <li><a theme="blue" href="javascript:void(0)"><span class="circle" style="background-color:blue"></span>&nbsp;蓝色</a></li>
                    </ul>
                </li>
               --%>
                <li class="dropdown">
                    <a href="javascript:void(0)" class="dropdown-toggle" data-toggle="dropdown" title="打开终端"><i aria-hidden="true" class="fa fa-folder-open-o"></i>&nbsp;打开终端<b class="caret"></b></a>
                    <ul class="dropdown-menu">
                        <c:forEach var="t" items="${terms}">
                            <li><a href="${contextPath}/terminal/ssh2?id=${t.id}" target="_blank">${t.name}(${t.host})</a></li>
                        </c:forEach>
                    </ul>
                </li>
                <li><a href="${contextPath}/terminal/reopen?token=${token}" target="_blank" title="复制会话"><i aria-hidden="true" class="fa fa-window-restore"></i>&nbsp;复制会话</a></li>
                <li><a href="javascript:window.close();" title="退出终端" data-toggle="tooltip"><i aria-hidden="true" class="fa fa-power-off"></i>&nbsp;退出终端</a></li>
                <li style="padding-top: 9px;margin-left: 18px;">
                    <label style="color:#777;font-weight: normal; "><i aria-hidden="true" class="fa fa-pencil"></i>&nbsp;中文输入</label>&nbsp;&nbsp;<input id="chinese" size="40" placeholder="发送中文请在这里输入" type="text">
                    &nbsp;<div class="btn btn-success btn-sm" id="chinput" style="margin-top: -3px;">发送</div>
                </li>
                <li style="float: right;margin-right: 10px;"><a href="https://github.com/wolfboys/opencron" target="_blank"><i aria-hidden="true" class="fa fa-github" style="font-size:30px;position:absolute;top:6px"></i></a></li>
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

        //去掉a点击时的虚线框
        $(".container").find("a").each(function (i,a) {
            $(a).focus(function () {
                this.blur();
            });
        });

        //主题
       $(".theme").find("a").each(function (i,e) {
           var theme = $(e).attr('theme');
           $(e).click(function () {
               term.theme(theme);
           });
       });

    });
</script>
</html>
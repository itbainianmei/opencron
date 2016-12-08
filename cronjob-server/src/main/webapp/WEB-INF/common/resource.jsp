<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String port = request.getServerPort() == 80 ? "" : (":"+request.getServerPort());
    String path = request.getContextPath().replaceAll("/$","");
    String contextPath = request.getScheme()+"://"+request.getServerName()+port+path;
    pageContext.setAttribute("contextPath",contextPath);
%>
<title>Cronjob</title>
    <meta name="format-detection" content="telephone=no">
    <meta name="description" content="Cronjob">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="keywords" content="cronjob,crontab,a better crontab,Let's crontab easy">
    <meta name="author" content="author:benjobs,wechat:wolfboys,Created by languang(http://u.languang.com) @ 2016" />

    <!-- CSS -->
    <link rel="stylesheet" href="${contextPath}/css/bootstrap.css" rel="stylesheet">
    <link rel="stylesheet" href="${contextPath}/css/animate.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${contextPath}/css/font-awesome.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${contextPath}/css/font-awesome-ie7.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${contextPath}/css/form.css" rel="stylesheet">
    <link rel="stylesheet" href="${contextPath}/css/calendar.css" rel="stylesheet">
    <link rel="stylesheet" href="${contextPath}/css/style.css" rel="stylesheet">
    <link rel="stylesheet" href="${contextPath}/css/icons.css" rel="stylesheet">
    <link rel="stylesheet" href="${contextPath}/css/generics.css" rel="stylesheet">
    <link rel="stylesheet" href='${contextPath}/css/sweetalert.css' rel='stylesheet'>
    <link rel="stylesheet" href='${contextPath}/css/cronjob.css' rel='stylesheet'>
    <link rel="stylesheet" href='${contextPath}/css/loading.css' rel='stylesheet'>
    <link rel="stylesheet" href='${contextPath}/css/morris.css' rel='stylesheet'>
    <link rel="stylesheet" href='${contextPath}/css/prettify.min.css' rel='stylesheet'>
    <link rel="stylesheet" href="${contextPath}/img/favicon.ico" rel="shortcut icon" type="image/ico">

    <!-- Javascript Libraries -->
    <!-- jQuery -->
    <script type="text/javascript" src="${contextPath}/js/jquery.min.js"></script> <!-- jQuery Library -->
    <script type="text/javascript" src="${contextPath}/js/jquery-ui.min.js"></script> <!-- jQuery UI -->
    <script type="text/javascript" src="${contextPath}/js/jquery.easing.1.3.js"></script> <!-- jQuery Easing - Requirred for Lightbox + Pie Charts-->

    <!-- Bootstrap -->
    <script type="text/javascript" src="${contextPath}/js/bootstrap.js"></script>
    <script type="text/javascript" src="${contextPath}/js/easypiechart.js"></script> <!-- EasyPieChart - Animated Pie Charts -->

    <!--  Form Related -->
    <script type="text/javascript" src="${contextPath}/js/icheck.js"></script> <!-- Custom Checkbox + Radio -->
    <script type="text/javascript" src="${contextPath}/js/select.min.js"></script> <!-- Custom Select -->

    <!-- UX -->
    <script type="text/javascript" src="${contextPath}/js/scroll.min.js"></script> <!-- Custom Scrollbar -->

    <!-- Other -->
    <script type="text/javascript" src="${contextPath}/js/calendar.min.js"></script> <!-- Calendar -->
    <script type="text/javascript" src="${contextPath}/js/raphael.2.1.2-min.js"></script>
    <script type="text/javascript" src="${contextPath}/js/prettify.min.js"></script>
    <script type="text/javascript" src="${contextPath}/js/morris.min.js"></script>
    <!-- All JS functions -->
    <script id="themeFunctions" src="${contextPath}/js/functions.js?${contextPath}"></script>

    <!--flot-->
    <script type="text/javascript" src="${contextPath}/js/flot/jquery.flot.min.js"></script>
    <script type="text/javascript" src="${contextPath}/js/flot/jquery.flot.resize.min.js"></script>
    <script type="text/javascript" src="${contextPath}/js/flot/jquery.flot.spline.min.js"></script>
    <script type="text/javascript" src="${contextPath}/js/testdevice.js"></script>

    <!-- MD5 -->
    <script type="text/javascript" src="${contextPath}/js/md5.js"></script>
    <script type="text/javascript" src="${contextPath}/js/html5/html5shiv/html5shiv.js"></script>
    <script type="text/javascript" src="${contextPath}/js/gauge.js"></script>
    <script type="text/javascript" src="${contextPath}/js/jquery.cookie.js"></script>
    <script type="text/javascript" src="${contextPath}/js/My97DatePicker/WdatePicker.js"></script>
    <script type="text/javascript" src="${contextPath}/js/sweetalert.min.js"></script>
    <script type="text/javascript" src="${contextPath}/js/cronjob.js"></script>

    <!--upfile-->
    <link rel="stylesheet" href="${contextPath}/js/cropper/cropper.main.css" type="text/css" />
    <link rel="stylesheet" href="${contextPath}/js/cropper/cropper.min.css" type="text/css" />
    <script type="text/javascript" src="${contextPath}/js/cropper/cropper.min.js" ></script>
    <script type="text/javascript" src="${contextPath}/js/cronjob.cropper.js" ></script>




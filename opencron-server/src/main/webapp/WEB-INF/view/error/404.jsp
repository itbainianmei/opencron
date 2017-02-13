<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="cron" uri="http://www.opencron.org" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <link rel="shortcut icon" href="${contextPath}/img/favicon.ico?resId=${resourceId}" />
    <title>Opencron 404</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            outline: none;
            -webkit-user-select: none;
            -moz-user-select: none;
            -ms-user-select: none;
            -khtml-user-select: none;
            user-select: none;
            cursor: default;
            font-weight: lighter;
        }

        .center {
            margin: 0 auto;
        }

        .whole {
            width: 100%;
            height: 100%;
            line-height: 100%;
            position: fixed;
            bottom: 0;
            left: 0;
            z-index: -1000;
            overflow: hidden;
        }

        .whole img {
            width: 100%;
            height: 100%;
        }

        .mask {
            width: 100%;
            height: 100%;
            position: absolute;
            top: 0;
            left: 0;
            background: #000;
            opacity: 0.6;
            filter: alpha(opacity=60);
        }

        .b {
            width: 100%;
            text-align: center;
            height: 400px;
            position: absolute;
            top: 50%;
            margin-top: -230px
        }

        .a {
            width: 150px;
            height: 50px;
            margin-top: 30px
        }

        .a a {
            display: block;
            float: left;
            width: 150px;
            height: 50px;
            background: #fff;
            text-align: center;
            line-height: 50px;
            font-size: 18px;
            border-radius: 25px;
            color: #333
        }

        .a a:hover {
            color: #000;
            box-shadow: #fff 0 0 20px
        }

        p {
            color: #fff;
            margin-top: 40px;
            font-size: 28px;
            font-weight: 300
        }

        #num {
            margin: 0 5px;
            font-weight: 400;
        }
    </style>
    <script type="text/javascript">
        var num=6;
        function redirect(){
            num--;
            document.getElementById("num").innerHTML=num;
            if(num<0){
                document.getElementById("num").innerHTML=0;
                location.href="${contextPath}";
            }
        }
        setInterval("redirect()", 1000);
    </script>
</head>

<body onLoad="redirect();">
<div class="whole">
    <img src="${contextPath}/img/back.jpg" />
    <div class="mask"></div>
</div>
<div class="b">
    <img src="${contextPath}/img/404.png" class="center"/>
    <p>
        亲爱的:您查找的页面未找到<br>
        可能输入的网址错误或此页面不存在<br>
        <span id="num"></span>秒后自动跳转到主页
    </p>
</div>

</body>
</html>

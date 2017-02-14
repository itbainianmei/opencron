<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="cron"  uri="http://www.opencron.org"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>
<html lang="en">
<head>
    <jsp:include page="/WEB-INF/common/resource.jsp"/>

    <style type="text/css">
        .error_image {
            float: left;
            margin-right: -50px;
            margin-left: 30px;
            cursor: pointer;
        }
        .error_content{
            font-size: 25px;
            font-weight: 300;
            margin-left: -60px;
            margin-top:15px;
        }
        .error_contact li {
            font-size: 40px;
            margin-left: 20px;
            margin-top: 20px;
            cursor: pointer;
        }

        #error {
            margin-top: 30px;
        }

    </style>
    <script type="text/javascript">
        $(document).ready(function () {
            $("#error_close").click(function () {
                $("#error").hide();
            });
            $(".error_image").click(function () {
                $("#error").show();
            });
        })
    </script>
</head>
<jsp:include page="/WEB-INF/common/top.jsp"/>

<!-- Content -->
<section id="content" class="container">

    <!-- Messages Drawer -->
    <jsp:include page="/WEB-INF/common/message.jsp"/>

    <!-- Breadcrumb -->
    <ol class="breadcrumb hidden-xs">
        <li class="icon">&#61753;</li>
        当前位置：
        <li><a href="">opencron</a></li>
        <li><a href="">出错啦</a></li>
    </ol>
    <h4 class="page-title"><i class="glyphicon glyphicon-remove"></i>&nbsp;出错啦</h4>
    <div class="block-area" id="defaultStyle">
        <div class="error_image"><img src="${contextPath}/img/500.png" width="400px" height="135px"></div>
        <div class="error_content">对不起,出错啦!您可能通过以下方式联系开发者</div>
        <ul class="error_contact">
            <li class="fa fa-wechat"></li>
            <a href="tencent://message/?uin=123322242&Site=121ask.com&Menu=yes"> <li class="fa fa-qq"></li></a>
            <a href="https://github.com/wolfboys/opencron/issues" target="_blank"><li class="fa fa-github"></li></a>
            <a href="mailto:benjobs@qq.com" target="_blank"><li class="fa fa-envelope-o"></li></a>
        </ul>

        <div class="tile" id="error">
            <div class="listview narrow">
                <div class="media">
                    <i class="fa fa-eye"></i>&nbsp;<a href="#">错误详情</a>
                    <span class="drawer-close" id="error_close">&times;</span>
                </div>
                <div class="media text-left whiter l-100">
                    <pre id="pre" style="font-size:11px;color:#FFF;border: none;background: none;white-space: pre-wrap;word-wrap: break-word;">${error}</pre>
                </div>
            </div>
        </div>
    </div>

</section>
<br/><br/>

<jsp:include page="/WEB-INF/common/footer.jsp"/>

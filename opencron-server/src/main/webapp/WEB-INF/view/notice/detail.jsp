<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="cron"  uri="http://www.opencron.org"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>
<html lang="en">
<head>
    <jsp:include page="/WEB-INF/common/resource.jsp"/>
    <style type="text/css">
        .email{
            height: 100%;
            overflow: visible;
            position: relative;
            top: 20px;
            z-index: 50;
            height: 968px !important;
            margin: 0 auto;
            padding: 165px 115px 100px 100px;
            width: 1256px !important;
            margin-top: 5px;
            background: url(${contextPath}/img/bg-mob.png) no-repeat 0 -1249px;
        }
        .email-title{
            height: 126px;
            margin-left: 171px;
            margin-top: 140px;
            width: 864px;
            padding-top: 1px;
            padding-left: 20px;
        }
        .email-content{
            height: 126px;
            margin-left: 171px;
            margin-top: 10px;
            width: 850px;
            padding-top: 0px;
            padding-left: 20px;
        }
        .mobile{
            height: 100%;
            overflow: visible;
            position: relative;
            top: 20px;
            z-index: 50;
            height: 720px !important;
            width: 387px !important;
            margin: 0 auto;
            padding: 125px 25px 159px 25px;
            margin-top: 20px;
            background: url(${contextPath}/img/bg-mob.png) no-repeat 0 -2217px;
        }
        .mobile-in{
            background-color: rgba(255, 255, 255, 0.30);
            height: 463px;
            margin-left: -7px;
            margin-top: -31px;
            width: 351px;
        }

        .message-border{
            width:85%;
            background-color:rgba(0,0,0,0.1);
            margin-top: 20px;
            margin-left: 10px;
            padding: 8px 8px 8px 8px;
            -moz-border-radius: 10px !important;
            -webkit-border-radius: 10px !important;
            border-radius:10px !important;
        }
        ::selection {
            background:#d3d3d3;
            color:#555;
        }
        ::-moz-selection {
            background:#d3d3d3;
            color:#555;
        }
    </style>

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
        <li><a href="">告警日志</a></li>
    </ol>
    <h4 class="page-title"><i class="glyphicon glyphicon-eye-open"></i>&nbsp;日志详情</h4>


    <div class="block-area" id="defaultStyle">
        <button type="button" onclick="history.back()" class="btn btn-sm m-t-10" style="float: right;margin-bottom: 8px;"><i class="icon">&#61740;</i>&nbsp;返回</button>
        <table class="table tile textured">
            <tbody id="tableContent">
            <tr>
                <td>
                    <c:if test="${log.type eq 0}"><i class="glyphicon glyphicons-message-plus"></i></c:if>
                    <c:if test="${log.type eq 1}"><i class="glyphicons glyphicons-chat"></i></c:if>
                    <c:if test="${log.type eq 2}"><i class="glyphicon glyphicon-tasks"></i></c:if>
                    &nbsp;通知类型
                </td>
                <td>
                    <c:if test="${log.type eq 0}">邮件</c:if>
                    <c:if test="${log.type eq 1}">短信</c:if>
                    <c:if test="${log.type eq 2}">站内信</c:if>
                </td>
            </tr>

            <tr>
                <td><i class="glyphicons glyphicons-user-conversation"></i>&nbsp;发件人</td>
                <td>${sender}</td>
            </tr>

            <tr>
                <td><i class="glyphicons glyphicons-alarm"></i>&nbsp;发送时间</td>
                <td><fmt:formatDate value="${log.sendTime}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
            </tr>

            <tr>
                <td><i class="glyphicons glyphicons-user-add"></i>&nbsp;收件人</td>
                <td>${log.receiver}</td>
            </tr>

            <tr>
                <td colspan="2">
                    <i class="glyphicons glyphicons-eye-open"></i>&nbsp;<strong>信&nbsp;&nbsp;息</strong></p>
                    <pre id="pre" style="font-size:11px;color:#FFF;border: none;background: none;white-space: pre-wrap;word-wrap: break-word;">${log.message}</pre>
                </td>
            </tr>
            </tbody>
        </table>
    </div>

</section>
<br/><br/>

<jsp:include page="/WEB-INF/common/footer.jsp"/>

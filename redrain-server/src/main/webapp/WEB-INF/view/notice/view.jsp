<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ben"  uri="ben-taglib"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%
    String path = request.getContextPath().replaceAll("/$","");
    String port = request.getServerPort() == 80 ? "" : ":"+request.getServerPort();
    String contextPath = request.getScheme()+"://"+request.getServerName()+port+path;
    pageContext.setAttribute("contextPath",contextPath);
%>

<!DOCTYPE html>
<html lang="en">
<head>
    <jsp:include page="/WEB-INF/common/resource.jsp"/>
    <script type="text/javascript">

        $(document).ready(function(){
            $("#size").change(function(){doUrl();});
            $("#workerId").change(function(){doUrl();});
        });
        function doUrl(){
            var workerId = $("#workerId").val();
            var sendTime = $("#sendTime").val();
            var pageSize = $("#size").val();
            window.location.href = "${contextPath}/notice/view?workerId="+workerId+"&sendTime="+sendTime+"&pageSize="+pageSize;
        }

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
        <li><a href="">REDRAIN</a></li>
        <li><a href="">系统设置</a></li>
    </ol>
    <h4 class="page-title">告警日志</h4>
    <div class="block-area" id="defaultStyle">

        <div>
            <div style="float: left">
                <label>
                    每页 <select size="1" class="select-self" id="size" style="width: 50px;">
                    <option value="15">15</option>
                    <option value="30" ${page.pageSize eq 30 ? 'selected' : ''}>30</option>
                    <option value="50" ${page.pageSize eq 50 ? 'selected' : ''}>50</option>
                    <option value="100" ${page.pageSize eq 100 ? 'selected' : ''}>100</option>
                </select> 条记录
                </label>
            </div>

            <div style="float: right;margin-top: -10px">
                <label for="workerId">执行器：</label>
                <select id="workerId" name="workerId" class="select-self" style="width: 120px;">
                    <option value="">全部</option>
                    <c:forEach var="w" items="${workers}">
                        <option value="${w.workerId}" ${w.workerId eq workerId ? 'selected' : ''}>${w.name}</option>
                    </c:forEach>
                </select>
                &nbsp;
                <label for="sendTime">发送时间：</label>
                <input type="text" id="sendTime" name="sendTime" value="${sendTime}" onfocus="WdatePicker({onpicked:function(){doUrl(); },dateFmt:'yyyy-MM-dd'})" class="Wdate select-self" style="width: 90px"/>
                
                <button type="button" onclick="history.back()" class="btn btn-sm m-t-10" style="margin-left: 50px;margin-bottom: 8px"><i class="icon">&#61740;</i>&nbsp;返回</button>
            </div>
        </div>


        <table class="table tile">
            <thead>
            <tr>
                <th>发送方式</th>
                <th>执行器</th>
                <th>接收人</th>
                <th>发送信息</th>
                <th>时间</th>
                <th>操作</th>
            </tr>
            </thead>

            <tbody id="tableContent">

            <c:forEach var="log" items="${page.result}" varStatus="index">
                <tr>
                    <td style="padding: 5px"><center style="font-size: 16px">
                        <c:if test="${log.type eq 0}"><i class="icon" title="邮件">&#61880;</i></c:if>
                        <c:if test="${log.type eq 1}"><i class="icon" title="短信">&#61704;</i></c:if>
                    </center></td>
                    <td>${log.workerName}</td>
                    <td>${log.receiver}</td>
                    <td title="${log.message}">${ben:substr(log.message,0,60,"...")}</td>
                    <td>${log.sendTime}</td>
                    <td>
                        <center>
                            <a href="${contextPath}/notice/detail?logId=${log.logId}" title="查看详情">
                                <i class="glyphicon glyphicon-eye-open"></i>
                            </a>
                        </center>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>

        <ben:pager href="${contextPath}/notice/view?workerId=${workerId}&sendTime=${sendTime}" id="${page.pageNo}" size="${page.pageSize}" total="${page.totalCount}"/>

    </div>

</section>
<br/><br/>

<jsp:include page="/WEB-INF/common/footer.jsp"/>

﻿<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%
	String path = request.getContextPath().replaceAll("/$","");
	String port = request.getServerPort() == 80 ? "" : request.getServerPort()+"";
	String contextPath = request.getScheme()+"://"+request.getServerName()+":"+port+path;
	pageContext.setAttribute("contextPath",contextPath);
%>

<script type="text/javascript">
	$(document).ready(function() {

		<c:if test="${fn:contains(uri,'/notice/')}">
		$("#msg-icon").remove();
		</c:if>

		if($.isMobile()){
			$("#time").remove();
		}

		var skin = $.cookie("redrain_skin");
		if(skin) {
			$('body').attr('id', skin);
		}

		$('body').on('click', '.template-skins > a', function(e){
			e.preventDefault();
			var skin = $(this).data('skin');
			$('body').attr('id', skin);
			$('#changeSkin').modal('hide');
			$.cookie("redrain_skin", skin, {
				expires : 30,
				domain:document.domain,
				path:"/"
			});
		});


		$.ajax({
			url: "${contextPath}/notice/uncount",
			dataType: "html",
			success: function (data) {
				if (data != "0"){
					$(".n-count").text(data);
					$("#msg-icon").show();
					$.ajax({
						url: "${contextPath}/notice/unread",
						dataType: "html",
						success: function (data) {
							$("#msgList").html(data);
						}
					});
				}else {
					$("#messages").remove();
					$(".n-count").remove();
					$("#msg-icon").click(function () {
						window.location.href="${contextPath}/notice/view";
					})
					$("#msg-icon").show();
				}
			}
		});

	});
</script>

<body id="skin-blur-violate">
<header id="header">
	<a href="" id="menu-toggle" style="background-image: none"><i class="icon">&#61773;</i></a>
	<a id="log1" href="${contextPath}/home" class="logo pull-left"><div style="float:left;width: 145px;margin-left:24px;margin-top:4px;">
		<img src="${contextPath}/img/redrain.png">
	</div>
	</a>
	<div class="media-body">
		<div class="media" id="top-menu" style="float:right;margin-right:15px;">
			<div class="pull-left tm-icon" id="msg-icon" style="display: none;">
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
					<img class="profile-pic animated" src="${contextPath}/img/profile-pic.jpg" alt="">
				</a>
				<h4 class="m-0">${user.userName}</h4>
				<ul class="dropdown-menu profile-menu">
					<li><a href="${contextPath}/user/detail?userId=${user.userId}">个人信息</a> <i class="icon left">&#61903;</i><i class="icon right">&#61815;</i></li>
					<li><a href="${contextPath}/notice/view">通知&nbsp;&&nbsp;消息</a> <i class="icon left">&#61903;</i><i class="icon right">&#61815;</i></li>
					<li><a href="${contextPath}/logout">退出登录</a> <i class="icon left">&#61903;</i><i class="icon right">&#61815;</i></li>
				</ul>
			</div>

			<!-- Calendar -->
			<div class="s-widget m-b-25">
				<div id="sidebar-calendar"></div>
			</div>

		</div>

		<!-- Side Menu -->
		<ul class="list-unstyled side-menu">
			<li class="<c:if test="${fn:contains(uri,'/home')}">active</c:if>">
				<a href="${contextPath}/home">
					<i aria-hidden="true" class="fa fa-tachometer"></i><span class="menu-item">效果报告</span>
				</a>
			</li>
			<li class="<c:if test="${fn:contains(uri,'/worker')}">active</c:if>">
				<a  href="${contextPath}/worker/view">
					<i aria-hidden="true" class="fa fa-desktop"></i><span class="menu-item">执行器管理</span>
				</a>
			</li>
			<li class="dropdown <c:if test="${fn:contains(uri,'/job')}">active</c:if>">
				<a href="#">
					<i aria-hidden="true" class="fa fa-tasks" aria-hidden="true"></i><span class="menu-item">作业管理</span>
				</a>
				<ul class="list-unstyled menu-item">
					<li <c:if test="${fn:contains(uri,'/job/view')}">class="active"</c:if>>
						<a href="${contextPath}/job/view" class="<c:if test="${fn:contains(uri,'/job/view')}">active</c:if>">作业列表</a>
					</li>
					<li <c:if test="${fn:contains(uri,'/exec')}">class="active"</c:if>>
						<a href="${contextPath}/job/goexec" class="<c:if test="${fn:contains(uri,'/exec')}">active</c:if>">现场执行</a>
					</li>
				</ul>
			</li>

			<li class="dropdown <c:if test="${fn:contains(uri,'/record')}">active</c:if>">
				<a href="#">
					<i class="fa fa-print" aria-hidden="true"></i><span class="menu-item">调度记录</span>
				</a>
				<ul class="list-unstyled menu-item">
					<li <c:if test="${fn:contains(uri,'/running')}">class="active"</c:if>>
						<a href="${contextPath}/record/running" class="<c:if test="${fn:contains(uri,'running')}">active</c:if>">正在运行</a>
					</li>
					<li <c:if test="${fn:contains(uri,'/done')}">class="active"</c:if>>
						<a href="${contextPath}/record/done" class="<c:if test="${fn:contains(uri,'done')}">active</c:if>">已完成</a>
					</li>
				</ul>
			</li>

			<c:if test="${permission eq true}">
				<li <c:if test="${fn:contains(uri,'/user')}">class="active"</c:if>>
					<a href="${contextPath}/user/view">
						<i class="fa fa-user" aria-hidden="true"></i></i><span class="menu-item">用户管理</span>
					</a>
				</li>
				<li <c:if test="${fn:contains(uri,'/config')}">class="active"</c:if>>
					<a href="${contextPath}/config/view">
						<i aria-hidden="true" class="fa fa-cog"></i><span class="menu-item">系统设置</span>
					</a>
				</li>
			</c:if>
		</ul>
	</aside>
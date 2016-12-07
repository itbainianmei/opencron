<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%
	String port = request.getServerPort() == 80 ? "" : (":"+request.getServerPort());
	String path = request.getContextPath().replaceAll("/$","");
	String contextPath = request.getScheme()+"://"+request.getServerName()+port+path;
	pageContext.setAttribute("contextPath",contextPath);
	request.setAttribute("uri",request.getRequestURI());
%>


<script type="text/javascript">

	$(document).ready(function() {
		<c:if test="${fn:contains(uri,'/notice/')}">
		$("#msg-icon").remove();
		</c:if>

		if($.isMobile()){
			$("#time").remove();
			$("#contactDialog").remove();
			$("#change-img").remove();
		}else {
			$("#profile-pic").mouseover(function () {
				$("#change-img").show();
			}).mouseout(function () {
				$("#change-img").hide();
			});

			$("#change-img").mouseover(function () {
				$(this).show();
			}).mouseout(function () {
				$(this).hide();
			});
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
			type:"POST",
			url: "${contextPath}/notice/uncount",
			dataType: "html",
			success: function (data) {
				if (data != "0"){
					$(".n-count").text(data);
					$("#msg-icon").show();
					$.ajax({
						type:"POST",
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

<body id="skin-cloth">

<div id="mask" class="mask"></div>

<header id="header">
	<a href="" id="menu-toggle" style="background-image: none"><i class="icon">&#61773;</i></a>
	<a id="log1" href="${contextPath}/home" class="logo pull-left"><div style="float:left;width: 145px;margin-left:24px;margin-top:4px;">
		<img src="${contextPath}/img/cronjob.png">
	</div>
	</a>
	<div class="media-body">
		<div class="media" id="top-menu" style="float:right;margin-right:15px;">
			<div class="pull-left tm-icon" id="msg-icon" style="display: none;">
				<a  class="drawer-toggle" data-drawer="messages" id="toggle_message" href="#">
					<i class="sa-top-message icon" style="background-image:none;font-size: 30px; background-size: 25px;">&#61710;</i>
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

<div class="container" id="crop-avatar">



	<!-- Cropping modal -->
	<div class="modal fade" id="avatar-modal" aria-hidden="true" aria-labelledby="avatar-modal-label" role="dialog" tabindex="-1">
		<div class="modal-dialog modal-md">
			<div class="modal-content">
				<form class="avatar-form" name="picform" action="${contextPath}/headpic/upload" enctype="multipart/form-data" method="post">
					<input name="userId" type="hidden" value="${cronjob_user.userId}">
					<div class="modal-header">
						<button class="close" data-dismiss="modal" type="button">&times;</button>
						<h4 class="modal-title" id="avatar-modal-label">更改图像</h4>
					</div>
					<div class="modal-body">
						<div class="avatar-body">

							<!-- Upload image and data -->
							<div class="avatar-upload">
								<input class="avatar-src" name="avatar_src" type="hidden">
								<input class="avatar-data" name="avatar_data" type="hidden">
								<input type="button" value="请选择本地照片" class="btn btn-default" onclick="document.picform.file.click()">
								<input class="avatar-input" id="avatarInput" name="file" type="file" style="display:none;">
							</div>

							<!-- Crop and preview -->
							<div class="row">
								<div class="col-md-8">
									<div class="avatar-wrapper"></div>
								</div>
								<div class="col-md-4">
									<div class="avatar-preview preview-lg"></div>
								</div>
							</div>

							<div class="row avatar-btns">
								<div class="col-md-8">
									<div class="btn-group"  style="margin-left:0px;">
										<button class="btn btn-sm" data-method="rotate" data-option="-90" type="button" title="Rotate -90 degrees">左旋转</button>
										<button class="btn btn-sm" data-method="rotate" data-option="-15" type="button">-15°</button>
										<button class="btn btn-sm" data-method="rotate" data-option="-30" type="button">-30°</button>
										<button class="btn btn-sm" data-method="rotate" data-option="-45" type="button">-45°</button>
									</div>
									<div class="btn-group">
										<button class="btn btn-sm" data-method="rotate" data-option="90" type="button" title="Rotate 90 degrees">右旋转</button>
										<button class="btn btn-sm" data-method="rotate" data-option="15" type="button">15°</button>
										<button class="btn btn-sm" data-method="rotate" data-option="30" type="button">30°</button>
										<button class="btn btn-sm" data-method="rotate" data-option="45" type="button">45°</button>
									</div>
								</div>
								<div class="col-md-4">
									<button class="btn btn-primary btn-block avatar-save" type="submit">上传</button>
								</div>
							</div>
						</div>
					</div>
				</form>
			</div>
		</div>
	</div><!-- /.modal -->

	<!-- Loading state -->
	<div class="loading" aria-label="Loading" role="img" tabindex="-1"></div>
</div>


<section id="main" class="p-relative" role="main">

	<!-- Sidebar -->
	<aside id="sidebar">

		<!-- Sidbar Widgets -->
		<div class="side-widgets overflow">
			<!-- Profile Menu -->
			<div class="text-center s-widget m-b-25 dropdown" id="profile-menu">
				<a href="" id="header-img" data-toggle="dropdown" class="animated a-hover">
					<img class="profile-pic" id="profile-pic" width="140px;" height="140px;"  onerror="javascript:this.src='${contextPath}/img/profile-pic.jpg'" src="${contextPath}/upload/${cronjob_user.userId}_pic${cronjob_user.picExtName}?<%=System.currentTimeMillis()%>">
					<div class="change-text" id="change-img" href="javascript:void(0);">更换头像</div>
				</a>
				<h4 class="m-0">${cronjob_user.userName}</h4>
				<ul class="dropdown-menu profile-menu">
					<li><a href="${contextPath}/user/detail?userId=${cronjob_user.userId}">个人信息</a> <i class="icon left">&#61903;</i><i class="icon right">&#61815;</i></li>
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
					<i aria-hidden="true" class="fa fa-tachometer"></i><span class="menu-item">作业报告</span>
				</a>
			</li>
			<li class="<c:if test="${fn:contains(uri,'/agent')}">active</c:if>">
				<a  href="${contextPath}/agent/view">
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
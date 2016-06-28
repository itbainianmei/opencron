﻿<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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

		$(".profile-pic").mouseover(function () {
			$(".change-text").show();
		}).mouseout(function () {
			$(".change-text").hide();
		});

		$(".change-text").mouseover(function () {
			$(this).show();
		}).mouseout(function () {
			$(this).hide();
		});

	});

	var i_flash;
	// 检测是否安装flash
	if (navigator.plugins) {
		for (var i=0; i < navigator.plugins.length; i++) {
			if (navigator.plugins[i].name.toLowerCase().indexOf("shockwave flash") >= 0) {
				i_flash = true;
			}
		}
	}
	if (!i_flash) {
		document.writeln("<span style='color:red;font-size:16px;'>此浏览器未安装Flash插件，请先下载安装！</span>")
	}

	/**关闭弹窗**/
	function closeDialog(){
		$("#contactDialog").css("display", "none");
		var headIcon=$("#iconHead").val();
		if(headIcon!=null && headIcon!=""){
			$("#headIconImg").attr("src", headIcon+"?tn="+Math.ceil(Math.random()*10000));
		}
	}
	/**显示弹窗**/
	function showDialog(){
		$("#contactDialog").css("display", "block");
	}

	jQuery(function($){
		var jcrop_api, boundx, boundy;
		$('#target').Jcrop({
			onChange: updatePreview,
			onSelect: updatePreview,
			minSize:[50,50],
			allowSelect:false,
			aspectRatio: 1
		},function(){
			jcrop_api = this;
			var rect = [0,0,300,300];
			//this.setSelect(rect);
		});

		function updatePreview(c) {
			if(parseInt(c.w)>0){
				var bounds = jcrop_api.getBounds();
				boundx = bounds[0];
				boundy = bounds[1];

				var rx=120/c.w;
				var ry=120/c.h;
				$("#preview").css({
					width:Math.round(rx*boundx)+"px",
					height:Math.round(ry*boundy)+"px",
					marginLeft:"-"+Math.round(rx*c.x)+"px",
					marginTop:"-"+Math.round(ry*c.y)+"px"
				});
			}else{
				var rect = [0,0,50,50];
				jcrop_api.setSelect(rect);
			}

			jQuery('#x').val(c.x);
			jQuery('#y').val(c.y);
			jQuery('#w').val(c.w);
			jQuery('#h').val(c.h);
		};

		jQuery('#cropButton').click(function(){
			var x = jQuery("#x").val();
			var y = jQuery("#y").val();
			var w = jQuery("#w").val();
			var h = jQuery("#h").val();

			if(w == 0 || h == 0 ){
				alert("您还没有选择图片的剪切区域,不能进行剪切图片!");
				return;
			}
			var url = "/upload/imageCut";
			var data = {
				userId:$("#userId").val(),
				x:x,
				y:y,
				w:w,
				h:h,
				oldImgPath:$("#oldImgPath").val(),
				scale:$("#scale").val()
			};
			$.ajax({
				url      : url,
				type 	   : "POST",
				data     : data,
				success  : function(result) {
					var result = jQuery.parseJSON(result);
					$("#iconHead").val(result.fileUrl);//首页头像
					alert("头像设置成功！");
					closeDialog();//保存头像成功，关闭对话框
				},
				error : function(){
					alert("头像设置失败，请重新上传！");
				}
			});
		});

		$("#btnUploadPic").uploadify({
			fileSizeLimit : '5120KB',
			fileObjName : 'file',
			method:'post',
			formData: {
				userId:${user.userId}
			},
			height : 25,
			width : 80,
			buttonImage : '',
			swf : '${contextPath}/js/upload/uploadify.swf',
			uploader : '/uploadimg',
			fileTypeExts : '*.gif; *.jpg; *.png; *.jpeg',
			buttonText : "上传图片",
			overrideEvents : [ 'onSelectError','onUploadError', 'onDialogClose' ],
			onSelectError : function(file, errorCode) {
				switch (errorCode) {
					case -100:
						alert("上传的文件数量已经超出系统限制的"
								+ $('#btnUploadPic').uploadify(
										'settings',
										'queueSizeLimit') + "个文件！");
						break;
					case -110:
						alert("文件 ["
								+ file.name
								+ "] 大小超出系统限制的"
								+ $('#btnUploadPic')
										.uploadify('settings',
												'fileSizeLimit')
								+ "大小！");
						break;
					case -120:
						alert("文件 [" + file.name + "] 大小异常！");
						break;
					case -130:
						alert("文件 [" + file.name + "] 类型不正确！");
						break;
				}
				return false;
			},
			onUploadSuccess : function(file, data, response) {
				var result = jQuery.parseJSON(data);
				if (result && result.fileUrl) {

					var imgUrl = "${contextPath}"+result.fileUrl;
					$("#headIcon").val(imgUrl);
					$("#previewImg").attr("src", imgUrl);
					$("#btnUploadPic").uploadify('settings', 'buttonText', '重传图片');

					//截图
					jcrop_api.setImage(imgUrl);
					$("#preview").attr("src",imgUrl);

					var rect = [0,0,300,300];

					if(!result.flag){
						rect = [0,0,120,120];
						jcrop_api.setSelect(rect);
						jcrop_api.allowResize=false;
						jcrop_api.allowMove=false;
					}else{
						jcrop_api.allowSelect=true;
						jcrop_api.setSelect(rect);
					}

					$("#oldImgPath").val(imgUrl);
					$("#scale").val(result.scale);

					$("#divBut").show();
				} else {
					alert("上传失败，请重试!" + result);
				}
			},
			onUploadProgress : function(file, bytesUploaded, bytesTotal, totalBytesUploaded, totalBytesTotal) {
				$("#btnUploadPic").uploadify('settings', 'buttonText', '上传中...');
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

<!-- 头像设置弹窗 -->
<div class="black-wrap" style="display: none" id="contactDialog">
	<div class="dialog-wrap" id="contentBody" style="height:550px; width:620px;z-index: 999; margin-top:-300px;margin-left: -380px;">
		<h1 style="width:620px;margin-top:0px" class="dialog-title"><span onclick="closeDialog();"></span>头像设置</h1>
		<div class="dialog-center" style="width:600px;height: 398px;">
			<div class="form-group spacing-col20">
				<div id="outer">
					<div class="jcExample" style="width:600px;margin:0 0 0 0;border:0;">
						<div>
							<form id="imageCutForm" action="/upload/imageCut" method="post">
								<input type="hidden" id="oldImgPath" name="oldImgPath" value=""/>
								<input type="hidden" id=scale name="scale" value=""/>
								<input type="hidden" id="x" name="x"/>
								<input type="hidden" id="y" name="y"/>
								<input type="hidden" id="w" name="w"/>
								<input type="hidden" id="h" name="h"/>
							</form>

							<table frame=void class="head-set">
								<tr>
									<td width="140px;">
										<input type="hidden" id="headIcon" value="" name="headIcon" class="xg_shuru">
										<input type="hidden" id="iconHead" value="" name="iconHead" class="xg_shuru">
										<div style="margin-left: 40px;">
											<div id="btnUploadPic" class="btnUpload" style="text-align: center;"></div>
										</div>
									</td>

								</tr>
							</table>

							<table style="height:300px;margin-left:42px;" frame=void class="head-set">
								<tr>
									<td style="height:300px;overflow:hidden;" valign="bottom">
										<img height="300px;" width="300px;" src="${contextPath}/img/profile-pic.jpg" id="target" alt="Flowers">
									</td>
									<td style="width: 55px;">&nbsp;&nbsp;&nbsp;</td>
									<td valign="bottom">
										<span class="l">120*120</span>
										<div style="width:120px;height:120px;position: relative; overflow: hidden;">
											<img src="${contextPath}/img/profile-pic.jpg" id="preview" alt="Preview" width="120px;" height="120px;">
										</div>
									</td>
								</tr>
							</table>

							<table frame=void class="head-set">
								<tr>
									<td height="50px;" style="padding-left: 35px;color:#969696">
										选择一张JPG/PNG/GIF格式的本地图片上传。图片大小不能超过5M。
									</td>
								</tr>
							</table>
						</div>
					</div>
				</div>

			</div>
		</div>
		<div class="dialog-describe"></div>
		<!--下一步-->
		<div class="dialog-next-wrap">
			<div class="dialog-next">
				<input id="cropButton" class="btn-submit btn-login" type="button" style="height: 26px;" value="保存头像">
				<input id="cropButton" class="btn-submit btn-login dialog-next-cancel" type="button"
					   style="height: 26px;" value="取消" onclick="closeDialog();">
			</div>
		</div>
	</div>
</div>
<!-- 底部-->



<section id="main" class="p-relative" role="main">

	<!-- Sidebar -->
	<aside id="sidebar">

		<!-- Sidbar Widgets -->
		<div class="side-widgets overflow">
			<!-- Profile Menu -->
			<div class="text-center s-widget m-b-25 dropdown" id="profile-menu">
				<a href="" id="header-img" data-toggle="dropdown" class="animated a-hover">
					<img class="profile-pic" src="${contextPath}/img/profile-pic.jpg" alt="">
					<div class="change-text"  onclick="showDialog();" href="javascript:void(0);">更换头像</div>
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
					<i aria-hidden="true" class="fa fa-tachometer"></i><span class="menu-item">作业报告</span>
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
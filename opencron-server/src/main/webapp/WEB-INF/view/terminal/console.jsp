<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="cron" uri="http://www.opencron.org" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <!--base-->
    <link rel="shortcut icon" href="${contextPath}/img/terminal.png" />
    <script type="text/javascript" src="${contextPath}/js/jquery.min.js"></script> <!-- jQuery Library -->
    <link rel="stylesheet" href="${contextPath}/css/font-awesome.css" >
    <link rel="stylesheet" href="${contextPath}/css/font-awesome-ie7.min.css" >
    <link rel="stylesheet" href='${contextPath}/css/sweetalert.css' >
    <script type="text/javascript" src="${contextPath}/js/sweetalert.min.js"></script>

    <!-- Bootstrap -->
    <link rel="stylesheet" href="${contextPath}/css/bootstrap.css" >
    <script type="text/javascript" src="${contextPath}/js/bootstrap.js"></script>
    <link rel="stylesheet" href="${contextPath}/css/opencron.term.css?id=20170117" >

    <!--fileinput-->
    <link href="${contextPath}/js/fileinput/css/fileinput.css" media="all" rel="stylesheet" type="text/css" />
    <script src="${contextPath}/js/fileinput/js/fileinput.js" type="text/javascript"></script>
    <script src="${contextPath}/js/fileinput/js/locales/zh.js" type="text/javascript"></script>

    <!--term-->
    <script type="text/javascript" src="${contextPath}/js/term.js"></script>
    <script type="text/javascript" src="${contextPath}/js/opencron.term.js?id=20170117"></script>
    <script type="text/javascript" src="${contextPath}/js/opencron.js"></script>

    <title>opencron Terminal</title>
</head>

<body>

<div id="navigation" class="navbar navbar-default" role="navigation">
    <div class="container">
        <div>
            <ul class="nav navbar-nav">
                <li><a class="term-logo" href="${contextPath}" target="_blank" title="">Opencron</a></li>
               <%-- <li class="dropdown">
                     <a href="javascript:void(0)" class="dropdown-toggle" data-toggle="dropdown" title="选择主题"><i aria-hidden="true" class="fa fa-gear"></i>&nbsp;选择主题<b class="caret"></b></a>
                     <ul class="dropdown-menu theme" >
                         <li><a theme="yellow" href="javascript:void(0)"><span class="circle" style="background-color:yellow"></span>&nbsp;黄色</a></li>
                         <li><a theme="green" href="javascript:void(0)"><span class="circle" style="background-color:green"></span>&nbsp;绿色</a></li>
                         <li><a theme="black" href="javascript:void(0)"><span class="circle" style="background-color:black"></span>&nbsp;黑色</a></li>
                         <li><a theme="blue" href="javascript:void(0)"><span class="circle" style="background-color:blue"></span>&nbsp;蓝色</a></li>
                     </ul>
                 </li>--%>
                <li class="dropdown">
                    <a href="javascript:void(0)" class="dropdown-toggle" data-toggle="dropdown" title="常用操作"><i aria-hidden="true" class="fa fa-server"></i>&nbsp;操作<b class="caret"></b></a>
                    <ul class="dropdown-menu theme" >
                        <li><a href="${contextPath}/terminal/reopen?token=${token}" target="_blank" title="复制会话">&nbsp;复制会话</a></li>
                        <li><a href="javascript:theme()" title="设置主题">&nbsp;设置主题</a></li>
                        <li><a href="javascript:upload()" title="上传文件">&nbsp;上传文件</a></li>
                    </ul>
                </li>

                <li class="dropdown">
                    <a href="javascript:void(0)" class="dropdown-toggle" data-toggle="dropdown" title="打开终端"><i aria-hidden="true" class="fa fa-folder-open-o"></i>&nbsp;打开<b class="caret"></b></a>
                    <ul class="dropdown-menu">
                        <c:forEach var="t" items="${terms}">
                            <li><a href="${contextPath}/terminal/ssh2?id=${t.id}" target="_blank">${t.name}(${t.host})</a></li>
                        </c:forEach>
                    </ul>
                </li>

                <li><a href="javascript:closeTerminal();" title="退出终端" data-toggle="tooltip"><i aria-hidden="true" class="fa fa-power-off"></i>&nbsp;退出</a></li>

                <li style="padding-top: 9px;margin-left: 18px;">
                    <label style="color:#777;font-weight: normal; "><i aria-hidden="true" class="fa fa-send"></i>&nbsp;中文输入</label>&nbsp;&nbsp;<input id="chinese" class="china-btn" size="30" placeholder="发送中文请在这里输入" type="text">
                    &nbsp;<div class="btn btn-success btn-sm" id="chinput" style="margin-top: -3px;">发送</div>
                </li>
                <li style="float: right;margin-right: 10px;"><a href="https://github.com/wolfboys/opencron" target="_blank"><i aria-hidden="true" class="fa fa-github" style="font-size:35px;position:absolute;top:6px"></i></a></li>
            </ul>
        </div>
    </div>
</div>

<div id="term"></div>

<div id="upload_push" class="modal fade" >
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
                <h4 class="modal-title">文件上传</h4>
            </div>
            <div class="modal-body">
                <form enctype="multipart/form-data">
                    <div class="input-group" style="padding-bottom:12px">
                        <span class="input-group-addon">上传路径</span>
                        <input type="text" id="path" class="form-control col-lg-13" placeholder="请输入文件上传路径,默认在当前终端所在的路径下">
                    </div>
                    <input id="file" name="file" type="file">
                </form>
            </div>
        </div>
    </div>
</div>


</body>

<script type="text/javascript">
    $(document).ready(function () {
       document.title = '${name}';
        new OpencronTerm('${token}');
        //去掉a点击时的虚线框
        $(".container").find("a").each(function (i,a) {
            $(a).focus(function () {
                this.blur();
            });
        });

        $("#term").css("padding-top",$("#navigation").outerHeight()+"px");

        $('#file').fileinput({
            language: 'zh',
            showPreview : true,
            browseOnZoneClick:false,
            uploadUrl : '${contextPath}/terminal/upload',
            removeLabel : "删除",
            showCaption: true, //是否显示标题,
            dropZoneEnabled:true,
            dropZoneTitle:"拖拽文件到这里来上传...",
            resizeImage: false,
            previewFileIcon: "<i class='glyphicon glyphicon-king'></i>",
            initialCaption: "请选择要上传的文件",
            maxFileSize:104857600,//文件最大100M
            allowedFileExtensions : null,
            uploadExtraData: function() {
                var obj = {};
                obj.token = '${token}';
                obj.path = $("#path").val();
                return obj;
            }
        });
    });

    function upload() {
        $("#upload_push").modal('show');
        $(".fileinput-remove-button").click();
        $("#path").val('');
    }

    //导入文件上传完成之后的事件
    $("#file").on("fileuploaded", function (event, data, previewId, index) {
        if (!data.response) {
            alert('文件格式类型不正确');
        }
    });

    function closeTerminal() {
        swal({
            title: "",
            text: "您确定要退出终端吗？",
            type: "warning",
            showCancelButton: true,
            closeOnConfirm: false,
            confirmButtonText: "退出"
        }, function() {
            window.close();
        });
    }
</script>
</html>
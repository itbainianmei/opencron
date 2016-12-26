<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ben" uri="ben-taglib" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<!DOCTYPE html>
<html lang="en">
<head>
    <jsp:include page="/WEB-INF/common/resource.jsp"/>
    <script type="text/javascript">

        function ssh(id, type) {
            $.ajax({
                type: "POST",
                url: "${contextPath}/terminal/ssh",
                data: "id=" + id,
                dataType: "html",
                success: function (data) {
                    var json = eval("(" + data + ")");
                    if (json.status == "authfail") {
                        if (type == 2) {
                            alert("登录失败,请确认登录口令的正确性");
                        } else {
                            editSsh(id,0);
                        }
                    } else if (json.status == "timeout") {
                        alert("连接到远端主机超时");
                    } else if (json.status == "error") {
                        alert("连接失败请重试");
                    } else if (json.status == "success") {
                        var url = '${contextPath}' + json.url;
                        swal({
                            title: "",
                            text: "您确定要打开Terminal吗？",
                            type: "warning",
                            showCancelButton: true,
                            closeOnConfirm: false,
                            confirmButtonText: "打开"
                        });

                        /**
                         *
                         * 默认打开新的弹窗浏览器会阻止,有的浏览器如Safair连询问用户是否打开新窗口的对话框都没有.
                         * 这里页面自己弹出询问框,当用户点击"打开"产生了真正的点击行为,然后利用事件冒泡就触发了包裹它的a标签,使得可以在新窗口打开a标签的连接
                         *
                         */
                        if ($("#openLink").length == 0) {
                            $(".sweet-alert").find(".confirm").wrap("<a id='openLink' href='" + url + "'  target='_blank'/></a>");
                        } else {
                            $("#openLink").attr("href", url);
                        }

                        $("#openLink").click(function () {
                            window.setTimeout(function () {
                                $("div[class^='sweet-']").remove();
                            }, 200)
                        });

                        $(".sweet-alert").find(".cancel").click(function () {
                            window.setTimeout(function () {
                                $("div[class^='sweet-']").remove();
                            }, 500)
                        });
                    }
                }
            });
        }

        function editSsh(id,type) {
            if (type == 1) {
                $("#sshTitle").text("编辑Terminal");
            }else {
                $("#sshTitle").text("SSH登陆");
            }
            $.ajax({
                type: "POST",
                url: "${contextPath}/terminal/detail",
                data: "id="+id,
                dataType: "html",
                success: function (term) {
                    var json = $.parseJSON(term);
                    $("#sshuser").val(json.user);
                    $("#sshname").val(json.name);
                    $("#sshport").val(json.port);
                    $("#sshhost").val(json.host).attr("readonly","readonly");
                    $("#sshuser")[0].focus();
                    $("#sshModal").modal("show");
                }
            });
        }


        function del(id) {
            swal({
                title: "",
                text: "您确定要删除该Terminal实例吗?",
                type: "warning",
                showCancelButton: true,
                closeOnConfirm: false,
                confirmButtonText: "删除"
            },function () {
                $.ajax({
                    type: "POST",
                    url: "${contextPath}/terminal/del",
                    data: "id="+id,
                    dataType: "html",
                    success: function (message) {
                        if (message == "success") {
                            alertMsg("删除成功!")
                            $("#tr_" + id).remove();
                        }else {
                            alert("删除失败!")
                        }
                    }
                });
            });
        }

        function saveSsh() {
            var user = $("#sshuser").val();
            var name = $("#sshname").val();
            var pwd = $("#sshpwd").val();
            var port = $("#sshport").val();
            var host = $("#sshhost").val();
            $.ajax({
                type: "POST",
                url: "${contextPath}/terminal/add",
                data: {
                    "name":name,
                    "userName": user,
                    "password": pwd,
                    "port": port,
                    "host": host
                },
                dataType: "html",
                success: function (status) {
                    $("#sshModal").modal("hide");
                    $("#sshform")[0].reset();
                    if (status == "success") {
                        alertMsg("恭喜你添加Terminal成功!");
                        location.reload();
                    } else {
                        alert("添加Terminal失败,请确认登录口令的正确性");
                    }
                }
            });
        }

        function addSSH() {
            $("#sshform")[0].reset();
            $("#sshhost").removeAttr("readonly");
            $("#sshTitle").text("添加Terminal");
            $("#sshModal").modal("show");
        }

    </script>

    <style type="text/css">
        .visible-md i {
            font-size: 15px;
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
        <li><a href="">Cronjob</a></li>
        <li><a href="">WEB终端</a></li>
    </ol>
    <h4 class="page-title"><i class="fa fa-terminal" aria-hidden="true"></i>&nbsp;WEB终端&nbsp;&nbsp;</h4>
    <div class="block-area" id="defaultStyle">
        <div>
            <div style="float: left">
                <label>
                    每页 <select size="1" class="select-self" id="size" style="width: 50px;margin-bottom: 8px">
                    <option value="15">15</option>
                    <option value="30" ${pageBean.pageSize eq 30 ? 'selected' : ''}>30</option>
                    <option value="50" ${pageBean.pageSize eq 50 ? 'selected' : ''}>50</option>
                    <option value="100" ${pageBean.pageSize eq 100 ? 'selected' : ''}>100</option>
                </select> 条记录
                </label>
            </div>
            <c:if test="${permission eq true}">
                <div style="float: right;margin-top: -10px">
                    <a href="javascript:addSSH();" class="btn btn-sm m-t-10"
                       style="margin-left: 50px;margin-bottom: 8px"><i class="icon">&#61943;</i>添加</a>
                </div>
            </c:if>
        </div>

        <table class="table tile textured">
            <thead>
            <tr>
                <th>实例名称</th>
                <th>主机地址</th>
                <th>SSH端口</th>
                <th>最后登陆</th>
                <th>
                    <center>操作</center>
                </th>
            </tr>
            </thead>

            <tbody id="tableContent">

            <c:forEach var="t" items="${pageBean.result}" varStatus="index">
                <tr id="tr_${t.id}">
                    <td id="name_${t.id}">${t.name}</td>
                    <td>${t.host}</td>
                    <td>${t.port}</td>
                    <td><fmt:formatDate value="${t.logintime}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                    <td>
                        <center>
                            <div class="visible-md visible-lg hidden-sm hidden-xs action-buttons">
                                    <a href="javascript:ssh('${t.id}')" title="登录">
                                        <i aria-hidden="true" class="fa fa-tv"></i>
                                    </a>&nbsp;&nbsp;
                                <a href="javascript:editSsh('${t.id}',1)" title="编辑">
                                    <i aria-hidden="true" class="fa fa-edit"></i>
                                </a>&nbsp;&nbsp;
                                <a href="javascript:del('${t.id}')" title="删除">
                                    <i aria-hidden="true" class="fa fa-remove"></i>
                                </a>&nbsp;&nbsp;
                            </div>
                        </center>
                    </td>
                </tr>
            </c:forEach>

            </tbody>
        </table>

        <ben:pager href="${contextPath}/terminal/view" id="${pageBean.pageNo}" size="${pageBean.pageSize}"
                   total="${pageBean.totalCount}"/>

    </div>

    <!-- 修改密码弹窗 -->
    <div class="modal fade" id="sshModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 id="sshTitle">SSH登录</h4>
                </div>
                <div class="modal-body">
                    <form class="form-horizontal" role="form" id="sshform">

                        <div class="form-group" style="margin-bottom: 4px;">
                            <label for="sshname" class="col-lab control-label"><i class="glyphicon glyphicon-leaf"></i>&nbsp;&nbsp;名&nbsp;&nbsp;称&nbsp;&nbsp;：</label>
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="sshname"
                                       placeholder="请输入该web终端的实例名字">&nbsp;&nbsp;<label id="sshname_lab"></label>
                            </div>
                        </div>

                        <div class="form-group" style="margin-bottom: 4px;">
                            <label for="sshhost" class="col-lab control-label"><i class="glyphicon glyphicon-tag"></i>&nbsp;&nbsp;地&nbsp;&nbsp;址&nbsp;&nbsp;：</label>
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="sshhost"
                                       placeholder="请输入主机地址(IP)">&nbsp;&nbsp;<label id="sshhost_lab"></label>
                            </div>
                        </div>


                        <div class="form-group" style="margin-bottom: 4px;">
                            <label for="sshport" class="col-lab control-label"><i class="glyphicon glyphicon-question-sign"></i>&nbsp;&nbsp;端&nbsp;&nbsp;口&nbsp;&nbsp;：</label>
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="sshport"
                                       placeholder="请输入端口">&nbsp;&nbsp;<label id="sshport_lab"></label>
                            </div>
                        </div>

                        <div class="form-group" style="margin-bottom: 4px;">
                            <label for="sshuser" class="col-lab control-label"><i class="glyphicon glyphicon-user"></i>&nbsp;&nbsp;帐&nbsp;&nbsp;号&nbsp;&nbsp;：</label>
                            <div class="col-md-9">
                                <input type="text" class="form-control " id="sshuser"
                                       placeholder="请输入账户">&nbsp;&nbsp;<label id="sshuser_lab"></label>
                            </div>
                        </div>

                        <div class="form-group">
                            <label for="sshpwd" class="col-lab control-label"><i class="glyphicon glyphicon-lock"></i>&nbsp;&nbsp;密&nbsp;&nbsp;码&nbsp;&nbsp;：</label>
                            <div class="col-md-9">
                                <input type="password" class="form-control " id="sshpwd" placeholder="请输入密码"/>&nbsp;&nbsp;<label
                                    id="sshpwd_lab"></label>
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <center>
                        <button type="button" class="btn btn-sm" onclick="saveSsh()">保存</button>
                        &nbsp;&nbsp;
                        <button type="button" class="btn btn-sm" data-dismiss="modal">关闭</button>
                    </center>
                </div>
            </div>
        </div>
    </div>

</section>
<br/><br/>

<jsp:include page="/WEB-INF/common/footer.jsp"/>
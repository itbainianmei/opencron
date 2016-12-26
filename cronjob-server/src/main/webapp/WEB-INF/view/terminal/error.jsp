<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ben" uri="ben-taglib" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <link rel="shortcut icon" href="${contextPath}/img/terminal.png" />

    <script src="${contextPath}/js/jquery.min.js"></script> <!-- jQuery Library -->

    <script src="${contextPath}/js/sweetalert.min.js"></script>

    <link href='${contextPath}/css/sweetalert.css' rel='stylesheet'>

    <title>Terminal Error</title>

    <script type="text/javascript">
        $(document).ready(function () {
            swal({
                title: "",
                text: "会话已结束本次连接失败,请返回登陆重试",
                type: "warning",
                showCancelButton: false,
                closeOnConfirm: false,
                confirmButtonText: "确定"
            }, function() {
                window.opener=null;
                window.close();
            });
        });
    </script>

</head>
<body></body>
</html>
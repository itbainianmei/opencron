<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ben" uri="ben-taglib" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html lang="en">
<head>


    <link rel="shortcut icon" href="${contextPath}/img/terminal.png" />

    <jsp:include page="/WEB-INF/common/resource.jsp"/>

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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ben" uri="ben-taglib" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html lang="en">
<head>

    <script type="text/javascript" src="${contextPath}/js/jquery.min.js"></script> <!-- jQuery Library -->

    <script type="text/javascript" src="${contextPath}/js/term.js"></script>

    <script type="text/javascript" src="${contextPath}/js/cronjob.term.js?t=31fdffs32"></script>

    <script type="text/javascript">

        $(document).ready(function () {
            new CronjobTerm(${instanceId}, ${hostId}, "${hostName}(${ip})",".termwrapper").open();
        });

    </script>
    <style type="text/css">
        body{
            margin: 0px;
        }
        .termwrapper{
            height: 100%;
            width:100%;
            background-color: #000000;
        }
    </style>
    <title>Cronjob Terminal</title>

</head>
<body>
<div class="termwrapper"></div>
</body>
</html>
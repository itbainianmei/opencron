<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ben" uri="ben-taglib" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html lang="en">
<head>

    <link rel="shortcut icon" href="${contextPath}/img/terminal.png" />

    <script type="text/javascript" src="${contextPath}/js/jquery.min.js"></script> <!-- jQuery Library -->

    <script type="text/javascript" src="${contextPath}/js/term.js"></script>

    <script type="text/javascript" src="${contextPath}/js/cronjob.term.js"></script>

    <script type="text/javascript">

        $(document).ready(function () {
            new CronjobTerm('${id}',"${hostName}(${ip})").open();
        });

    </script>
    <style type="text/css">
        body{
            margin: 0px;
        }
    </style>
    <title>Cronjob Terminal</title>
</head>
<body></body>
</html>
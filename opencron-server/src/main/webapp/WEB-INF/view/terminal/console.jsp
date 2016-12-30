<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="cron" uri="http://www.opencron.org" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <link rel="shortcut icon" href="${contextPath}/img/terminal.png" />
    <script type="text/javascript" src="${contextPath}/js/jquery.min.js"></script> <!-- jQuery Library -->
    <script type="text/javascript" src="${contextPath}/js/term.js"></script>
    <script type="text/javascript" src="${contextPath}/js/opencron.term.js"></script>
    <title>opencron Terminal</title>
</head>

<body style="margin: 0px;background-color: #000000"></body>

<script type="text/javascript">
    $(document).ready(function () {
       document.title = '${name}';
       new OpencronTerm();
    });
</script>
</html>
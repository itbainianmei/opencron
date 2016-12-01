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

    <title>Cronjob Terminal</title>

    <script type="text/javascript">
        document.onkeydown = function (e) {
            var ev = window.event || e;
            var code = ev.keyCode || ev.which;
            if (code == 116) {
                if(ev.preventDefault) {
                    ev.preventDefault();
                } else {
                    ev.keyCode = 0;
                    ev.returnValue = false;
                }
            }
        }
    </script>

</head>
<body style="margin: 0px;background-color: #000000"><div id="term"></div></body>
<script type="text/javascript">

    var getSize = function () {
        var text="qwertyuiopasdfghjklzxcvbnm";
        var span = $("<span>", { text: text });
        $("#term").append(span);
        var charWidth = span.width() / 26;
        span.remove();
        return {
            cols: Math.floor($(window).width() / charWidth),
            rows: Math.floor($(window).height() / 15) - 1
        };
    }

    $(document).ready(function () {

        var contextPath = (window.location.protocol === "https:"?"wss://":"ws://")+window.location.host;

        var webSocket = new WebSocket(contextPath+"/terminal.ws");

        webSocket.onerror = function(event) {
            onError(event);
        };

        webSocket.onopen = function(event) {
            onOpen(event)
        };

        webSocket.onmessage = function(event) {
            onMessage(event)
        };

        webSocket.onclose = function(event) {
            onClose(event)
        };

        function onMessage(event) {
            term.write(event.data);
        }

        function onOpen(event) {

        }

        var term = new Terminal({
            termName: "xterm",
            cols: getSize().cols,
            rows: getSize().rows,
            fontSize:12,
            lineHeight:15,
            useStyle: true,
            screenKeys: true,
            cursorBlink: true,
            convertEol: true
        });
        term.open($("#term").empty()[0]);
        term.on('data', function(data) {
            webSocket.send(data);
        });
    });
</script>
</html>
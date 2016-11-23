<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ben" uri="ben-taglib" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html lang="en">
<head>

    <script type="text/javascript" src="${contextPath}/js/jquery.min.js"></script> <!-- jQuery Library -->

    <script type="text/javascript" src="${contextPath}/js/term.js"></script>

    <link rel="stylesheet" href="${contextPath}/css/bootstrap.css"/>

    <link rel="stylesheet" href="${contextPath}/css/term.css"/>

    <script type="text/javascript">

        $(document).ready(function () {
            //get instance id list from selected terminals
            function getActiveTermsInstanceIds() {
                var ids = [];
                $(".run_cmd_active").each(function () {
                    var id = $(this).attr("id").replace("run_cmd_", "");
                    ids.push(id);
                });
                return ids;
            }


            $('#dummy').focus();
            var keys = {};

            var termFocus = true;
            $("#match").focus(function () {
                termFocus = false;
            });
            $("#match").blur(function () {
                termFocus = true;
            });

            $(document).keypress(function (e) {
                if (termFocus) {
                    var keyCode = (e.keyCode) ? e.keyCode : e.charCode;

                    if (String.fromCharCode(keyCode) && String.fromCharCode(keyCode) != ''
                        && (!e.ctrlKey || e.altKey) && !e.metaKey && !keys[27] && !keys[37]
                        && !keys[38] && !keys[39] && !keys[40] && !keys[13] && !keys[8] && !keys[9]
                        && !keys[46] && !keys[45] && !keys[33] && !keys[34] && !keys[35] && !keys[36]) {
                        var cmdStr = String.fromCharCode(keyCode);
                        connection.send(JSON.stringify({id: getActiveTermsInstanceIds(), command: cmdStr}));
                    }

                }
            });
            //function for command keys (ie ESC, CTRL, etc..)
            $(document).keydown(function (e) {
                if (termFocus) {
                    var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
                    keys[keyCode] = true;

                    //27 - ESC
                    //37 - LEFT
                    //38 - UP
                    //39 - RIGHT
                    //40 - DOWN
                    //13 - ENTER
                    //8 - BS
                    //9 - TAB
                    //17 - CTRL
                    //46 - DEL
                    //45 - INSERT
                    //33 - PG UP
                    //34 - PG DOWN
                    //35 - END
                    //36 - HOME
                    if ((e.ctrlKey && !e.altKey) ||
                        keyCode == 27 ||
                        keyCode == 37 ||
                        keyCode == 38 ||
                        keyCode == 39 ||
                        keyCode == 40 ||
                        keyCode == 13 ||
                        keyCode == 8 ||
                        keyCode == 9 ||
                        keyCode == 46 ||
                        keyCode == 45 ||
                        keyCode == 33 ||
                        keyCode == 34 ||
                        keyCode == 35 ||
                        keyCode == 36) {

                        connection.send(JSON.stringify({id: getActiveTermsInstanceIds(), keyCode: keyCode}));

                    }

                    //prevent default for unix ctrl commands
                    if (e.ctrlKey && (keyCode == 83 || keyCode == 81 || keyCode == 84 || keyCode == 220 || keyCode == 90 || keyCode == 72 || keyCode == 87 || keyCode == 85 || keyCode == 82 || keyCode == 68)) {
                        e.preventDefault();
                        e.stopImmediatePropagation();
                    }

                }

            });

            $(document).keyup(function (e) {
                var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
                delete keys[keyCode];
                if (termFocus) {
                    $('#dummy').focus();
                }
            });

            $(document).click(function (e) {
                if (termFocus && !$('body').hasClass('modal-open')) {
                    $('#dummy').focus();
                }
                //always change focus unless in match sort
                if (e.target.id != 'match') {
                    termFocus = true;
                }
            });


            //get cmd text from paste
            $(this).bind('paste', function (e) {
                $('#dummy').focus();
                $('#dummy').val('');
                setTimeout(function () {
                    var cmdStr = $('#dummy').val();
                    connection.send(JSON.stringify({id: getActiveTermsInstanceIds(), command: cmdStr}));
                }, 100);
            });


            var termMap = {};
            var loc = window.location, ws_uri;
            if (loc.protocol === "https:") {
                ws_uri = "wss:";
            } else {
                ws_uri = "ws:";
            }
            ws_uri += "//" + loc.host + '/terms.ws?t=' + new Date().getTime();

            var connection = new WebSocket(ws_uri);
            // Log errors
            connection.onerror = function (error) {
                console.log('WebSocket Error ' + error);
            };

            // Log messages from the server
            connection.onmessage = function (e) {
                var json = jQuery.parseJSON(e.data);
                $.each(json, function (key, val) {
                    if (val.output != '') {
                        if (!termMap[val.instanceId]) {
                            createTermMap(val.instanceId, val.output);
                        } else {
                            termMap[val.instanceId].write(val.output);
                        }
                    }
                });
            };

            function createTermMap(id, output) {

                termMap[id] = new Terminal({
                    screenKeys: false,
                    useStyle: true,
                    cursorBlink: true,
                    convertEol: true
                });
                termMap[id].open($("#run_cmd_" + id).find('.output'));
                termMap[id].write(output);
            }

            //function to set all terminal bindings when creating a term window
            function setTerminalEvents(element) {

                //if terminal window toggle active for commands
                element.mousedown(function (e) {
                    //check for cmd-click / ctr-click
                    if (!e.ctrlKey && !e.metaKey) {
                        $(".run_cmd").removeClass('run_cmd_active');
                    }

                    if (element.hasClass('run_cmd_active')) {
                        element.removeClass('run_cmd_active');
                    } else {
                        element.addClass('run_cmd_active')
                    }
                });

                //set focus to term
                $(".output").mouseup(function (e) {
                    if (window.getSelection().toString()) {
                        termFocus = false;
                    } else {
                        termFocus = true;
                    }
                });

                $(".output").bind('copy', function () {
                    setTimeout(function () {
                        termFocus = true;
                        window.getSelection().removeAllRanges();
                    }, 100);
                });

            }

            //returns div for newly created terminal element
            function createTermElement(instanceId, hostId, displayLabel) {
                var instance =
                    "<div id=\"run_cmd_" + instanceId + "\" class=\"run_cmd_active run_cmd\">"
                    + "<h6 class=\"term-header\">" + displayLabel + "</h6>"
                    + "<div class=\"term\">"
                    + "<div id=\"output_" + instanceId + "\" class=\"output\"></div>"
                    + "</div>"
                    + "<div data-hostId=\"" + hostId + "\" class=\"host\"></div>"
                    + "</div>";
                return instance;
            }

            $(createTermElement(${instanceId}, ${hostId}, "hadoop")).prependTo(".termwrapper");

            setTerminalEvents($("#run_cmd_" +${instanceId}));

        });

    </script>
    <title>Cronjob Terms</title>

</head>
<body>
<div class="termwrapper"></div>
</body>
</html>
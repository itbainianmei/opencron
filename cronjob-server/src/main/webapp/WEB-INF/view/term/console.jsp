<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="ben" uri="ben-taglib" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html lang="en">
<head>

    <script type="text/javascript" src="${contextPath}/js/jquery.min.js"></script> <!-- jQuery Library -->

    <script type="text/javascript" src="${contextPath}/js/term.js"></script>

    <script type="text/javascript" src="${contextPath}/js/cronjob.term.js"></script>


    <script type="text/javascript">

        $(document).ready(function () {

            //new CronjobTerm(${instanceId}, ${hostId}, "hadoop",".termwrapper").open();

            function getTermId() {
                return ${instanceId};
            }

            //disconnect terminals and remove from view
            $('#disconnect').click(function(){
                var ids = getTermId();
                var id=ids[0];
                $.ajax({url: '../admin/disconnectTerm.action?id=' + id, cache: false});
                $('#run_cmd_'+id).remove();
                termMap[id].destroy();
                delete termMap[id];
            });

            $('#focus').focus();
            var keys = {};

            var termFocus = true;

            $(document).keypress(function (e) {
                if (termFocus) {
                    var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
                    if (String.fromCharCode(keyCode) && String.fromCharCode(keyCode) != ''
                        && (!e.ctrlKey || e.altKey) && !e.metaKey && !keys[27] && !keys[37]
                        && !keys[38] && !keys[39] && !keys[40] && !keys[13] && !keys[8] && !keys[9]
                        && !keys[46] && !keys[45] && !keys[33] && !keys[34] && !keys[35] && !keys[36]) {
                        var cmdStr = String.fromCharCode(keyCode);
                        connection.send(JSON.stringify({id: getTermId(), command: cmdStr}));
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
                    //8  - BS
                    //9  - TAB
                    //17 - CTRL
                    //46 - DEL
                    //45 - INSERT
                    //33 - PAGEUP
                    //34 - PAGEDOWN
                    //35 - END
                    //36 - HOME
                    if((e.ctrlKey && !e.altKey) || keyCode == 27 || keyCode == 37 || keyCode == 38 || keyCode == 39 || keyCode == 40 || keyCode == 13 || keyCode == 8 || keyCode == 9 || keyCode == 46 || keyCode == 45 || keyCode == 33 || keyCode == 34 || keyCode == 35 || keyCode == 36) {
                        connection.send(JSON.stringify({id: getTermId(), keyCode: keyCode}));
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
                    $('#focus').focus();
                }
            });


            //get cmd text from paste
            $(this).bind('paste', function (e) {
                $('#focus').focus();
                $('#focus').val('');
                setTimeout(function () {
                    var cmdStr = $('#focus').val();
                    connection.send(JSON.stringify({id: getTermId(), command: cmdStr}));
                }, 100);
            });


            var termMap = {};

            var ws_uri = ( window.location.protocol === "https:"?"wss://":"ws://" ) +  window.location.host + '/terms.ws?jsid=' + new Date().getTime();

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
                        if(!termMap[val.instanceId]) {
                            createTermMap(val.instanceId, val.output);
                        }else {
                            termMap[val.instanceId].write(val.output);
                        }
                    }
                });
            };

            function  createTermMap(id, output){

                termMap[id] = new Terminal({
                    rows: 50,
                    fdsaf:3333,
                    fontSize:13,
                    lineHeight:16,
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
                    if(window.getSelection().toString()) {
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
            function createTermElement(instanceId, hostId, displayLabel){
                var instance =
                    "<div id=\"run_cmd_" +instanceId + "\" class=\"run_cmd_active run_cmd\">"
                    + "<h6 class=\"term-header\">" + displayLabel + "</h6>"
                    + "<div class=\"term\">"
                    +   "<div id=\"output_" + instanceId + "\" class=\"output\"></div>"
                    + "</div>"
                    + "<div data-hostId=\""+ hostId +"\" class=\"host\"></div>"
                    +"</div>";
                return instance;
            }

            $(createTermElement(${instanceId}, ${hostId}, "${hostName}(${ip})")).prependTo(".termwrapper");

            setTerminalEvents($("#run_cmd_" +${instanceId}));

        });

    </script>

    <style type="text/css">
        .terminal {
            background-color:#000;
            color:rgb(222,222,222);
            font-size: 12px;
        }
    </style>
    <title>Cronjob Terminal</title>

</head>
<body>
<!--别动,很神奇,让该框永远得到焦点,主要是阻止按删除键,触发页面退出,返回上个页面-->
<textarea id="focus" style="border:none;color:#FFFFFF;width:0px;height:0px;resize:none"></textarea>
<div class="termwrapper"></div>
</body>
</html>
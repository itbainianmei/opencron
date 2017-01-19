<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%
    String port = request.getServerPort() == 80 ? "" : (":"+request.getServerPort());
    String path = request.getContextPath().replaceAll("/$","");
    String contextPath = request.getScheme()+"://"+request.getServerName()+port+path;
    pageContext.setAttribute("contextPath",contextPath);
%>


<!-- Older IE Message -->
<!--[if lt IE 9]>
<div class="ie-block">
    <h1 class="Ops">Ooops!</h1>
    <p> 您使用的浏览器太老啦，升级到下列任何一个最新浏览器，以便您愉快的访问该网站。 </p>
    <ul class="browsers">
        <li>
            <a href="https://www.google.com/intl/en/chrome/browser/">
                <img src="${contextPath}/img/browsers/chrome.png" alt="">
                <div>Google Chrome</div>
            </a>
        </li>
        <li>
            <a href="http://www.mozilla.org/en-US/firefox/new/">
                <img src="${contextPath}/img/browsers/firefox.png" alt="">
                <div>Mozilla Firefox</div>
            </a>
        </li>
        <li>
            <a href="http://www.opera.com/computer/windows">
                <img src="${contextPath}/img/browsers/opera.png" alt="">
                <div>Opera</div>
            </a>
        </li>
        <li>
            <a href="http://safari.en.softonic.com/">
                <img src="${contextPath}/img/browsers/safari.png" alt="">
                <div>Safari</div>
            </a>
        </li>
        <li>
            <a href="http://windows.microsoft.com/en-us/internet-explorer/downloads/ie-10/worldwide-languages">
                <img src="${contextPath}/img/browsers/ie.png" alt="">
                <div>Internet Explorer(New)</div>
            </a>
        </li>
    </ul>
    <p>请升级您的浏览器以便带来更好的用户体验 <br/>谢谢...</p>
</div>
<![endif]-->

</section>

<script type="text/javascript">
    $(document).ready(function() {

        <c:if test="${fn:contains(uri,'/notice/')}">
        $("#msg-icon").remove();
        </c:if>

        if($.isMobile()){
            $("#time").remove();
            $("#change-img").remove();
        }else {
            $("#profile-pic").mouseover(function () {
                $("#change-img").show();
            }).mouseout(function () {
                $("#change-img").hide();
            });

            $("#change-img").mouseover(function () {
                $(this).show();
            }).mouseout(function () {
                $(this).hide();
            });
        }

        var skin = $.cookie("opencron_skin");
        if(skin) {
            $('body').attr('id', skin);
        }

        $('body').on('click', '.template-skins > a', function(e){
            e.preventDefault();
            var skin = $(this).data('skin');
            $('body').attr('id', skin);
            $('#changeSkin').modal('hide');
            $.cookie("opencron_skin", skin, {
                expires : 30,
                domain:document.domain,
                path:"/"
            });
        });

        $.ajax({
            type:"POST",
            url: "${contextPath}/notice/uncount",
            dataType: "html",
            success: function (data) {
                if (data != "0"){
                    $(".n-count").text(data);
                    $("#msg-icon").show();
                    $.ajax({
                        type:"POST",
                        url: "${contextPath}/notice/unread",
                        dataType: "html",
                        success: function (data) {
                            $("#msgList").html(data);
                        }
                    });
                }else {
                    $("#messages").remove();
                    $(".n-count").remove();
                    $("#toggle_message").css({"padding":"10px 0px 0"});
                    $("#msg-icon").click(function () {
                        window.location.href="${contextPath}/notice/view";
                    })
                    $("#msg-icon").show();
                }
            }
        });

        $.ajax({
            type: "POST",
            url: "${contextPath}/progress",
            dataType: "json",
            success: function (data) {
                if (data != null) {
                    //折线图
                    var dataArea = [];
                    var successSum = 0;
                    var failureSum = 0;
                    var killedSum = 0;
                    var singleton = 0;
                    var flow = 0;
                    var crontab = 0;
                    var quartz = 0;
                    var rerun = 0;
                    var auto = 0;
                    var operator = 0;

                    for (var i in data) {
                        dataArea.push({
                            date: data[i].date,
                            success: data[i].success,
                            failure: data[i].failure,
                            killed: data[i].killed
                        });
                        successSum += parseInt(data[i].success);
                        failureSum += parseInt(data[i].failure);
                        killedSum += parseInt(data[i].killed);
                        singleton += parseInt(data[i].singleton);
                        flow += parseInt(data[i].flow);
                        crontab += parseInt(data[i].crontab);
                        quartz += parseInt(data[i].quartz);
                        rerun += parseInt(data[i].rerun);
                        auto += parseInt(data[i].auto);
                        operator += parseInt(data[i].operator);
                    }

                    var progress_data = {
                        "dataArea": dataArea,
                        "success": successSum,
                        "failure": failureSum,
                        "killed": killedSum,
                        "singleton": singleton,
                        "flow": flow,
                        "crontab": crontab,
                        "quartz": quartz,
                        "rerun": rerun,
                        "auto": auto,
                        "operator": operator
                    };

                    var job_type = parseFloat(progress_data.auto / (progress_data.auto + progress_data.operator)) * 100;
                    if (isNaN(job_type)) {
                        $("#progress_type").attr("aria-valuenow", 0).css("width", "0%");
                    } else {
                        $("#progress_type").attr("aria-valuenow", job_type).css("width", job_type + "%");
                    }

                    var job_category = parseFloat(progress_data.singleton / (progress_data.singleton + progress_data.flow)) * 100;
                    if (isNaN(job_category)) {
                        $("#progress_category").attr("aria-valuenow", 0).css("width", "0%");
                    } else {
                        $("#progress_category").attr("aria-valuenow", job_category).css("width", job_category + "%");
                    }

                    var job_model = parseFloat(progress_data.crontab / (progress_data.crontab + progress_data.quartz)) * 100;
                    if (isNaN(job_model)) {
                        $("#progress_model").attr("aria-valuenow", 0).css("width", "0%");
                    } else {
                        $("#progress_model").attr("aria-valuenow", job_model).css("width", job_model + "%");
                    }

                    var job_rerun = parseFloat((progress_data.success + progress_data.failure + progress_data.killed - progress_data.rerun) / (progress_data.success + progress_data.failure + progress_data.killed)) * 100;
                    if (isNaN(job_rerun)) {
                        $("#progress_rerun").attr("aria-valuenow", 0).css("width", "0%");
                    } else {
                        $("#progress_rerun").attr("aria-valuenow", job_rerun).css("width", job_rerun + "%");
                    }

                    var job_status = parseFloat(progress_data.success / (progress_data.success + progress_data.failure + progress_data.killed)) * 100;
                    if (isNaN(job_status)) {
                        $("#progress_status").attr("aria-valuenow", 0).css("width", "0%");
                    } else {
                        $("#progress_status").attr("aria-valuenow", job_status).css("width", job_status + "%");
                    }
                }
            }
        });


        
        
    });
</script>

</body>

</html>


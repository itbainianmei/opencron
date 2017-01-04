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
    <p> 您正在使用一个过时的互联网浏览器，升级到下列任何一个网络浏览器，以访问该网站的最大功能。 </p>
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
    <p>请升级您的浏览器以便带来更好更好的用户体验 <br/>谢谢...</p>
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
            $("#contactDialog").remove();
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
    });
</script>

</body>

</html>


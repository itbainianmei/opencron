;function OpencronTerm() {
    'use strict';
    this.socket = null;
    this.term = null;
    this.termNode = null;
    this.args = arguments;
    this.contextPath = (window.location.protocol === "https:"?"wss://":"ws://")+window.location.host;
    this.open();
}

;OpencronTerm.prototype.resize = function () {
    var self = this;
   $(window).resize(function () {
       var currSize = self.size();
       if (self.display.cols != currSize.cols || self.display.rows != currSize.rows) {
           self.display = currSize;
           self.term.resize(self.display.cols,self.display.rows);
           $.ajax({
               url: '/terminal/resize?token=' + self.args[0] + '&cols=' + self.display.cols + '&rows=' + self.display.rows,
               cache: false
           });
       }
   });
}

;OpencronTerm.prototype.size = function () {
    return {
        width: $(window).innerWidth(),
        height: $(window).innerHeight()-$("#navigation").outerHeight(),
        cols: Math.floor($(window).innerWidth() / 7.2981),
        rows: Math.floor( ($(window).innerHeight()-$("#navigation").outerHeight()) / 16)
    };
}

;OpencronTerm.prototype.open = function () {
    'use strict';
    var self = this;

    self.termNode = $("#term");
    self.term = new Terminal({
        termName: "xterm",
        cols: self.size().cols,
        rows: self.size().rows,
        useStyle: true,
        screenKeys: true,
        cursorBlink: false,
        convertEol: true
    });
    self.term.open(self.termNode.empty()[0]);
    self.display = self.size();
    self.term.on('data', function(data) {
        self.socket.send(data);
    });

    //给发送按钮绑定事件
    $("#chinput").bind("click",function () {
        var chinese = $("#chinese").val();
        if ( chinese && chinese.length>0 ){
            self.socket.send(chinese);
            $("#chinese").val('');
        }
    });

    $("#chinese").focus(function () {
        self.chfocus = true;
    }).blur(function () {
        self.chfocus = false;
    });

    $(document).keypress(function (e) {
        var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
        if ( keyCode == 13 ){
            console.log(self.termClosed)

            //在中文输入框里点击Enter按钮触发发送事件.
            if (self.hasOwnProperty("chfocus") && self.chfocus ) {
                $("#chinput").click();
            }
            //(终端已经logout的情况下,再点击Enter则关闭当前页面
            if (self.hasOwnProperty("termClosed")) {
                if ( $("#unfocusinput").is(":focus")||$("#chinese").is(":focus") ){
                    self.term.close();
                    window.close();
                }
            }
        }
    });

    self.resize();

    var size = this.size();

    var url = this.contextPath+'/terminal.ws';
    var params = "?cols="+size.cols+"&rows="+size.rows+"&width="+size.width+"&height="+size.height;

    if ('WebSocket' in window) {
        self.socket = new WebSocket(url+params);
    } else if ('MozWebSocket' in window) {
        self.socket = new MozWebSocket(url+params);
    } else {
        url = "http://"+window.location.host+"/terminal.js";
        self.socket= SockJS(url+params);
    }

    self.socket.onerror = function() {
        self.term.write("Sorry! opencron terminal connect error!please try again.\n");
        window.clearInterval(self.term._blink);
    };

    self.socket.onopen = function(event) {
        //self.term.write("Welcome to opencron terminal!Connect Starting...\n");
    };

    self.socket.onmessage = function(event) {
        self.term.write(event.data);
    };

    self.socket.onclose = function() {
        self.term.write("Thank you for using opencron terminal! bye...");
        //清除光标闪烁
        window.clearInterval(self.term._blink);
        self.termClosed = true;
        //转移焦点到零时的输入框,主要是为了接管term对键盘的监听(终端已经logout的情况下,再点击Enter则关闭当前页面)
        $("<input type='text' id='unfocusinput' width='0px' height='0px' style='border:0;outline:none;position: absolute;top: -1000px;left: -1000px;'>").appendTo('body');
        document.getElementById("unfocusinput").focus();
        $("#term").bind("click",function () {
            document.getElementById("unfocusinput").focus();
        });
        $("#chinput").bind("click",function () {
            window.close();
        });
    };

}
/*

;OpencronTerm.prototype.theme = function () {
    'use strict';
    var yellow  = {
        colors: [
            '#000000',
            '#cd0000',
            '#00cd00',
            '#cdcd00',
            '#0000ee',
            '#cd00cd',
            '#00cdcd',
            '#e5e5e5',
            '#7f7f7f',
            '#ff0000',
            '#00ff00',
            '#ffff00',
            '#5c5cff',
            '#ff00ff',
            '#00ffff',
            '#ffffff'
        ],
        color256:'#FFFFFF',
        color257:'#000000'
    }
    this.term.colors = yellow.colors;
    this.term.colors[256] = yellow.color256;
    this.term.colors[257] = yellow.color257;
}
*/

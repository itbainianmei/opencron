;function OpencronTerm() {
    this.socket = null;
    this.term = null;
    this.termNode = null;
    this.args = arguments;
    this.sendLine = "";//发送的一行命令
    this.contextPath = (window.location.protocol === "https:"?"wss://":"ws://")+window.location.host;
    this.open();
}

;OpencronTerm.prototype.resize = function () {
    var self = this;
   $(window).resize(function () {
       window.setTimeout(function () {
           var currSize = self.size();
           if (self.display.cols != currSize.cols || self.display.rows != currSize.rows) {
               self.display = currSize;
               self.term.resize(self.display.cols,self.display.rows);
               $.ajax({
                   url: '/terminal/resize?token=' + self.args[0] + "&csrf=" + self.args[1] + '&cols=' + self.display.cols + '&rows=' + self.display.rows+"&width="+self.display.width+"&height="+self.display.height,
                   cache: false
               });
           }
       },1000);
   });
}

;OpencronTerm.prototype.size = function () {
    var cols = Math.floor($(window).innerWidth() / 7.2261);//基于fontSize=12的cols参考值
    var span = $("<span>");
    $('body').append(span);
    var array = ['q','w','e','r','t','y','u','i','o','p','a','s','d','f','g','h','j','k','l','z','x','c','v','b','n','m','1','2','3','4','5','6','7','8','9','0'];
    var i = 0;
    while (true){
        span.text( span.text()+ (i <array.length?array[i]:array[i % (array.length-1)]) );
        if( $(window).width() < span.width() ){
            cols = i-1;
            break;
        }
        ++i;
    }
    span.remove();
    return {
        width: $(window).innerWidth(),
        height: $(window).innerHeight()-$("#navigation").outerHeight(),
        cols: cols,
        rows: Math.floor( ($(window).innerHeight()-$("#navigation").outerHeight()- 4 )/ 16)
    };
}

;OpencronTerm.prototype.open = function () {
    var self = this;

    self.termNode = $("#term");
    self.term = new Terminal({
        termName: "xterm",
        cols: self.size().cols,
        rows: self.size().rows,
        useStyle: true,
        screenKeys: true,
        cursorBlink: false,
        convertEol: true,
        colors: [
            "#2e3436",
            "#cc0000",
            "#4e9a06",
            "#c4a000",
            "#3465a4",
            "#75507b",
            "#06989a",
            "#d3d7cf",
            "#555753",
            "#ef2929",
            "#8ae234",
            "#fce94f",
            "#729fcf",
            "#ad7fa8",
            "#34e2e2",
            "#eeeeec"
        ]
    });
    self.term.open(self.termNode.empty()[0]);
    self.display = self.size();
    self.term.on('data', function(data) {
        if (self.disconnect){
            self.socket.close();
        }else {
            self.sendData = data;
            self.sendLine += data;
            self.socket.send(data);
        }
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
            //在中文输入框里点击Enter按钮触发发送事件.
            if (self.hasOwnProperty("chfocus") && self.chfocus ) {
                $("#chinput").click();
            }
            //(终端已经logout的情况下,再点击Enter则关闭当前页面
            if (self.hasOwnProperty("termClosed")) {
                self.term.close();
                window.close();
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
        if (self.sendData == "\r") {
            if (event.data == "\r\nlogout\r\n" ){
                if (self.sendLine == "exit\r"){
                    self.disconnect = true;
                }else if(self.sendLine.sub){

                }
            }
            self.sendLine = "";
        }
    };

    self.socket.onclose = function() {
        self.term.write("Thank you for using opencron terminal! bye...");
        //清除光标闪烁
        window.clearInterval(self.term._blink);
        self.termClosed = true;
        document.title = "Terminal Disconnect";
        $('<div class="modal-backdrop in" id="backdrop">').appendTo('body');
        //转移焦点到零时的输入框,主要是为了接管term对键盘的监听(终端已经logout的情况下,再点击Enter则关闭当前页面)
        $("<input type='text' id='unfocusinput' width='0px' height='0px' style='border:0;outline:none;position: absolute;top: -1000px;left: -1000px;'>").appendTo('body');
        document.getElementById("unfocusinput").focus();
        $(".terminal-cursor").remove();
    };

};
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

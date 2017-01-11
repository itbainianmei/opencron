;function OpencronTerm() {
    'use strict';
    this.socket = null;
    this.term = null;
    this.termNode = null;
    this.contextPath = (window.location.protocol === "https:"?"wss://":"ws://")+window.location.host;
    this.open();
}

;OpencronTerm.prototype.resize = function () {
    var self = this;
   $(window).resize(function () {
       self.term.resize(self.size().cols,self.size().rows);
   });
}

;OpencronTerm.prototype.size = function () {
    var text="qwertyuiopasdfghjklzxcvbnm";
    var span = $("<span>", { text: text });
    $(this.termNode).append(span);
    var charWidth = span.width() / text.length;
    span.remove();
    return {
        cols: Math.floor($(window).width() / charWidth) + 2,
        rows: Math.floor( ($(window).height()-45) / 16)
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

    //在中文输入框里点击Enter按钮触发发送事件.
    $(document).keypress(function (e) {
        var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
        if ( keyCode == 13 && self.hasOwnProperty("chfocus") && self.chfocus ){
            $("#chinput").click();
        }
    });

    self.resize();

    var size = this.size();

    var url = this.contextPath+'/terminal.ws';
    var params = "?cols="+size.cols+"&rows="+size.rows;

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
        //清除光标闪烁
        self.term.write("Thank you for using opencron terminal! bye...");
        window.clearInterval(self.term._blink);
    };

}

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

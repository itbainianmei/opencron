;function CronjobTerm() {
    'use strict';
    this.socket = null;
    this.term = null;
    this.contextPath = (window.location.protocol === "https:"?"wss://":"ws://")+window.location.host;
    this.open();
}

;CronjobTerm.prototype.resize = function () {
    var self = this;
   $(window).resize(function () {
       self.term.resize(self.size().cols,self.size().rows);
   });
}

;CronjobTerm.prototype.size = function () {
    var text="qwertyuiopasdfghjklzxcvbnm";
    var span = $("<span>", { text: text });
    $('body').append(span);
    var charWidth = span.width() / 26;
    span.remove();
    return {
        cols: Math.floor($(window).width() / charWidth),
        rows: Math.floor($(window).height() / 15) - 1
    };
}

;CronjobTerm.prototype.open = function () {
    'use strict';
    var self = this;

    var termNode = $("<div style=\"height:100%;width:100%;background-color:#000000;\"></div>").prependTo('body');
    self.term = new Terminal({
        termName: "xterm",
        cols: self.size().cols,
        rows: self.size().rows,
        fontSize:13,
        lineHeight:15,
        useStyle: true,
        screenKeys: true,
        cursorBlink: true,
        convertEol: true
    });
    self.term.open(termNode.empty()[0]);
    self.term.on('data', function(data) {
        self.socket.send(data);
    });

    self.resize();

    var url = this.contextPath+'/terminal.ws';
    if ('WebSocket' in window) {
        self.socket = new WebSocket(url);
    } else if ('MozWebSocket' in window) {
        self.socket = new MozWebSocket(url);
    } else {
        url = "http://"+window.location.host+"/terminal.js";
        self.socket= SockJS(url);
    }

    self.socket.onerror = function() {
        self.term.write("Sorry! cronjob terminal connect error!playse try again...\n");
        window.clearInterval(self.term._blink);
    };

    self.socket.onopen = function(event) {
        //self.term.write("Welcome to cronjob terminal!Connect Starting...\n");
    };

    self.socket.onmessage = function(event) {
        self.term.write(event.data);
    };

    self.socket.onclose = function() {
        self.term.write("Thank you for using cronjob terminal!Good-bye...");
        //清除光标闪烁
        window.clearInterval(self.term._blink);
    };
}


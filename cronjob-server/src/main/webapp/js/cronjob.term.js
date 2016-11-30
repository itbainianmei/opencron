
;function CronjobTerm() {
    'use strict';
    this.id=arguments[0];
    this.termTitle = arguments[1];
    this.contextPath = (window.location.protocol === "https:"?"wss://":"ws://")+window.location.host;
    this.termFocus = true;
    this.keys = {};
    this.websocket = null;
    this.term = null;
    this.termElement = null;
    document.title=this.termTitle;
}

;CronjobTerm.prototype.open = function () {
    this.bind();
    this.create();
    this.request();
    this.focus();
}

;CronjobTerm.prototype.create = function () {
    var self = this;

    this.termElement = $("<div style=\"height:100%;width:100%;background-color:#000000;\"></div>");

    this.termElement.click(function () {
        self.focus();
    }).blur(function () {
        self.unfocus();
    }).prependTo('body');

    //set focus to term
    $(".output").mouseup(function (e) {
        if(window.getSelection().toString()) {
            self.unfocus();
        } else {
            self.focus();
        }
    });
    //绑定拷贝事件
    $(".output").bind('copy', function () {
        setTimeout(function () {
            self.focus();
            window.getSelection().removeAllRanges();
        }, 100);
    });
    return this;
}

;CronjobTerm.prototype.focus = function () {

    if( $("#focus").length === 0  ){
        <!--别动,很神奇,让该框永远得到焦点,并且接受用户的输入,阻止按删除,TAB等键,触发页面退出等...-->
        $("<textarea id='focus' size='1' style='border:none;width:1px;height:1px;position: absolute;top: -1000px;'></textarea>").insertBefore(this.termElement);
    }
    $("#focus").focus().click();
    this.termFocus = true;
    return this;
}

;CronjobTerm.prototype.unfocus = function () {
    $("#focus").blur();
    this.termFocus = false;
    return this;
}

;CronjobTerm.prototype.bind = function () {
    var self = this;
    var keys = this.keys;

    $(document).keypress(function (e) {
        if (self.termFocus) {
            var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
            if (String.fromCharCode(keyCode) && String.fromCharCode(keyCode) != ''
                && (!e.ctrlKey || e.altKey) && !e.metaKey && !keys[27] && !keys[37]
                && !keys[38] && !keys[39] && !keys[40] && !keys[13] && !keys[8] && !keys[9]
                && !keys[46] && !keys[45] && !keys[33] && !keys[34] && !keys[35] && !keys[36]) {
                var command = String.fromCharCode(keyCode);
                self.websocket.send(JSON.stringify({token: self.id, command: command}));
            }
        }
    }).keydown(function (e) {
        if (self.termFocus) {
            var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
            self.keys[keyCode] = true;
            if((e.ctrlKey && !e.altKey) || keyCode == 27 || keyCode == 37 || keyCode == 38 || keyCode == 39 || keyCode == 40 || keyCode == 13 || keyCode == 8 || keyCode == 9 || keyCode == 46 || keyCode == 45 || keyCode == 33 || keyCode == 34 || keyCode == 35 || keyCode == 36) {
                self.websocket.send(JSON.stringify({token: self.id, keyCode: keyCode}));
            }
            if (e.ctrlKey && (keyCode == 83 || keyCode == 81 || keyCode == 84 || keyCode == 220 || keyCode == 90 || keyCode == 72 || keyCode == 87 || keyCode == 85 || keyCode == 82 || keyCode == 68)) {
                e.preventDefault();
                e.stopImmediatePropagation();
            }
        }
    }).keyup(function (e) {
        var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
        delete self.keys[keyCode];
        if (self.termFocus) {
            self.focus();
        }
    });

    $(window).bind('paste', function (e) {
        self.focus();
        $('#focus').val('');
        setTimeout(function () {
            var command = $('#focus').val();
            self.websocket.send(JSON.stringify({token: self.id, command: command}));
        }, 100);
    });

    $(window).resize(function() {
        self.resize();
    });

    return this;
}

;CronjobTerm.prototype.resize = function () {
    this.term.resize(this.getSize().cols,this.getSize().rows);
}

;CronjobTerm.prototype.getSize = function () {
    var text="qwertyuiopasdfghjklzxcvbnm";
    var span = $("<span>", { text: text });
    this.termElement.append(span);
    var charWidth = span.width() / 26;
    span.remove();
    return {
        cols: Math.floor($(window).width() / charWidth),
        rows: Math.floor($(window).height() / 15) - 1
    };
}

;CronjobTerm.prototype.request = function () {
    var self = this;
    var url = this.contextPath+'/terminal.ws';
    if ('WebSocket' in window) {
        this.websocket = new WebSocket(url);
    } else if ('MozWebSocket' in window) {
        this.websocket = new MozWebSocket(url);
    } else {
        url = "http://"+window.location.host+"/terminal.wsjs";
        this.websocket = new SockJS(url);
    }

    this.websocket.onmessage = function(e) {
        var data = jQuery.parseJSON(e.data);
        if (data.error !== undefined) {
            self.websocket.onerror(data.error);
        } else {
            if (!self.term){
                console.log(self.getSize());
                self.term = new Terminal({
                    termName: "xterm",
                    cols: self.getSize().cols,
                    rows: self.getSize().rows,
                    fontSize:12,
                    lineHeight:15,
                    useStyle: true,
                    screenKeys: true,
                    cursorBlink: true,
                    convertEol: true
                });
                self.term.open(self.termElement);
            }
            $.each(data, function (key, val) {
                if (val.output != '') {
                    self.term.write(val.output);
                }
            });
        }
    };

    this.websocket.onclose = function(e) {
        self.term.destroy();
    };

    this.websocket.onerror = function(e) {
        console.log("Socket error:", e);
    };

    return this;
}


function CronjobTerm() {
    this.id=arguments[0];
    this.termTitle = arguments[1];
    this.target = arguments[2];
    this.contextPath = (window.location.protocol === "https:"?"wss://":"ws://")+window.location.host;
    this.termFocus = true;
    this.keys = {};
    this.connect = null;
    this.term = null;
    document.title=this.termTitle;
}

CronjobTerm.prototype.open = function () {
    this.bind();
    this.create();
    this.request();
    this.focus();
}

CronjobTerm.prototype.create = function () {
    var self = this;
    var term =
        "<div id=\"terminal_" +this.id + "\">"
        + "<div class=\"term\">"
        +   "<div id=\"output_" + this.id + "\" class=\"output\"></div>"
        + "</div>"
        +"</div>";

    $(term).prependTo(this.target);

    $('body').click(function () {
       this.focus();
    }).blur(function () {
        this.unfocus();
    });

    $(".output").bind('copy', function () {
        setTimeout(function () {
            self.focus();
            window.getSelection().removeAllRanges();
        }, 100);
    });
    return this;
}

CronjobTerm.prototype.focus = function () {

    if( $("#focus").length === 0  ){
        <!--别动,很神奇,让该框永远得到焦点,并且接受用户的输入,阻止按删除,TAB等键,触发页面退出等...-->
        $("<textarea id='focus' size='1' style='border:none;width:1px;height:1px;position: absolute;top: -1000px;'></textarea>").insertBefore(this.target);
    }
    $("#focus").focus().click();
    this.termFocus = true;
    return this;
}


CronjobTerm.prototype.unfocus = function () {
    $("#focus").blur();
    this.termFocus = false;
    return this;
}

CronjobTerm.prototype.bind = function () {
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
                self.connect.send(JSON.stringify({id: self.id, command: command}));
            }
        }
    }).keydown(function (e) {
        if (self.termFocus) {
            var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
            self.keys[keyCode] = true;
            if((e.ctrlKey && !e.altKey) || keyCode == 27 || keyCode == 37 || keyCode == 38 || keyCode == 39 || keyCode == 40 || keyCode == 13 || keyCode == 8 || keyCode == 9 || keyCode == 46 || keyCode == 45 || keyCode == 33 || keyCode == 34 || keyCode == 35 || keyCode == 36) {
                self.connect.send(JSON.stringify({id: self.id, keyCode: keyCode}));
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
            self.connect.send(JSON.stringify({id: self.id, command: command}));
        }, 100);
    });

    return this;
}

CronjobTerm.prototype.request = function () {
    var url = this.contextPath+'/terms.ws?t=' + new Date().getTime();
    this.connect =  new WebSocket(url);

    this.connect.onerror = function (error) {
        console.log('WebSocket Error ' + error);
    };

    var self = this;

    this.connect.onmessage = function (e) {
        var json = jQuery.parseJSON(e.data);
        $.each(json, function (key, val) {
            if (val.output != '') {
                if(!self.term) {
                    self.term = new Terminal({
                        cols: 132,
                        rows: Math.floor($(window).height()/15),
                        screenKeys: true,
                        useStyle: true,
                        cursorBlink: true,
                        convertEol: true
                    });
                    self.term.open($("#terminal_" + self.id).find('.output'));
                    self.term.write(val.output);
                }else {
                    self.term.write(val.output);
                }
            }
        });
    };
    return this;
}


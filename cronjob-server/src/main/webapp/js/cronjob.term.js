
function CronjobTerm() {
    this.id=arguments[0];
    this.hostId = arguments[1];
    this.termTitle = arguments[2];
    this.target = arguments[3];
    this.contextPath = (window.location.protocol === "https:"?"wss://":"ws://")+window.location.host;
    this.termFocus = true;
    this.keys = {};
    this.wsconn = null;
    this.term = null;
    if($("#focus").length==0) {
        $("<textarea id='focus' size='1' style='border:none;color:#FFFFFF;width:1px;height:1px'></textarea>").appendTo($('body'));
    }

    this.focus();
}

CronjobTerm.prototype.open = function () {
    this.bind();
    this.create();
    this.conn();
}

CronjobTerm.prototype.create = function () {
    var self = this;
    var term =
        "<div id=\"run_cmd_" +this.id + "\" class=\"run_cmd_active run_cmd\">"
        + "<h6 class=\"term-header\">" + this.termTitle + "</h6>"
        + "<div class=\"term\">"
        +   "<div id=\"output_" + this.id + "\" class=\"output\"></div>"
        + "</div>"
        + "<div data-hostId=\""+ this.hostId +"\" class=\"host\"></div>"
        +"</div>";

    $(term).prependTo(this.target);

    var element = $("#run_cmd_"+this.id);

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
            self.unfocus()
        } else {
            self.focus();
        }
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
    $("#focus").focus();
    this.termFocus = true;
    return this;
}

CronjobTerm.prototype.unfocus = function () {
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
                self.wsconn.send(JSON.stringify({id: self.id, command: command}));
            }
        }
    }).keydown(function (e) {
        if (self.termFocus) {
            var keyCode = (e.keyCode) ? e.keyCode : e.charCode;
            self.keys[keyCode] = true;
            if((e.ctrlKey && !e.altKey) || keyCode == 27 || keyCode == 37 || keyCode == 38 || keyCode == 39 || keyCode == 40 || keyCode == 13 || keyCode == 8 || keyCode == 9 || keyCode == 46 || keyCode == 45 || keyCode == 33 || keyCode == 34 || keyCode == 35 || keyCode == 36) {
                self.wsconn.send(JSON.stringify({id: self.id, keyCode: keyCode}));
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
            self.wsconn.send(JSON.stringify({id: self.id, command: command}));
        }, 100);
    });

    return this;
}

CronjobTerm.prototype.conn = function () {
    var url = this.contextPath+'/terms.ws?t=' + new Date().getTime();
    this.wsconn =  new WebSocket(url);

    this.wsconn.onerror = function (error) {
        console.log('WebSocket Error ' + error);
    };

    var self = this;
    this.wsconn.onmessage = function (e) {
        var json = jQuery.parseJSON(e.data);
        $.each(json, function (key, val) {
            if (val.output != '') {
                if(!self.term) {
                    self.term = new Terminal({
                        cols: Math.floor($('.output:first').innerWidth() / 7.2981), rows: 24,
                        screenKeys: false,
                        useStyle: true,
                        cursorBlink: true,
                        convertEol: true
                    });
                    self.term.open($("#run_cmd_" + self.id).find('.output'));
                    self.term.write(val.output);
                }else {
                    self.term.write(val.output);
                }
            }
        });
    };

    return this;

}


function keyboard_input(event) {
	if (event.keyCode == 13) {
		input = document.getElementById('kuromoji-input').value;
        // document.getElementById('kuromoji-input').value = '';
	    tokenize(input, mode);
	}
}

function tokenize(text, mode) {
    clear_response();
    $.getJSON('/kuromoji/rest/tokenizer/tokenize?text=' + encodeURI(text) + '&mode=' + mode, function(data){
        clear_response();
        for (i = 0; i < data.length; i++) {
            add_response_line(data[i].surface, data[i].features);
        }
    });
}

function clear_input() {
	document.getElementById('kuromoji-input').value = '';
    focus_input();
	clear_response();
}

function focus_input() {
    $('#kuromoji-input').focus();
}

function clear_response() {
	$('#kuromoji p').remove();
}

function add_response_line(surface, features) {
    if (surface == ' ' || surface == '　') {
    	surface = '␣';
    }
	html  = '<p>';
	html += '<span class="kuromoji-surface">' + surface + '</span>';
	html += '<span class="kuromoji-features">' + features + '</span>';
    html += '<div class="kuromoji-clear"></div>';
	html += '</p>';

	$("#kuromoji-response").append(html);
}


function toggle_mode() {
    mode++;
    if (mode >= modes.length) {
    	mode = 0;
    }
    set_mode(mode);
    tokenize(document.getElementById('kuromoji-input').value, mode);
}

function set_mode(mode) {
	$('#kuromoji-mode a').html(modes[mode]);
    focus_input();
}

var modes = ['normal mode', 'search model', 'extended search mode'];
var mode = 0;

$(document).ready(function() {
	set_mode(mode);
});

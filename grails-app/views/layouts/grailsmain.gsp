<html>
<head>
<title><g:layoutTitle default="Curious" /></title>
<link type="text/css" href="${resource(dir:'theme', file:'ui.all.css')}" rel= "stylesheet">
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery-1.4.4.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery.json-2.2.js')}"></script>
<script type="text/javascript">
var MAX_DUMP_DEPTH = 10;

$.fn.setAbsolute = function(options) {
	return this.each(function() {
			var el = $(this);
			var pos = el.position();
			settings = jQuery.extend({
				x: pos.left,
				y: pos.top,
				rebase: false
			}, options);
			el.css({ position: "absolute",
				marginLeft: 0, marginTop: 0,
				top: settings.y, left: settings.x });
			if (settings.rebase)
				el.remove().appendTo("body");
			});
}

$.fn.isUnderEvent = function(e) {
	var pos = this.position();
	if (!pos) return false;
	var height = this.height();
	var width = this.width();
	
	return e.pageX >= pos.left && e.pageX < pos.left + width
			&& e.pageY >= pos.top && e.pageY < pos.top + height;
}

function showAlert(text) {
	alert(text);
}

function checkData(data, status, errorMessage, successMessage) {
	if (data == 'error') {
		if (errorMessage && status != 'cached')
			showAlert(errorMessage);
		return false;
	}
	if (data == 'login') {
		if (status != 'cached') {
			showAlert("Session timed out.");
			doLogout();
			location.reload(true);
		}
		return false;
	}
	if (data == 'success') {
		if (successMessage && status != 'cached')
			showAlert(successMessage);
		return true;
	}
	if (typeof(data) == 'string') {
		if (status != 'cached') {
			showAlert(data);
			location.reload(true);
		}
		return false;
	}
	return true;
}

function removeElem(arr, elem) {
	return jQuery.grep(arr, function(v) {
		return v != elem;
	});
}

function dumpObj(obj) {
	return dumpInternalObj(obj, "", "", 0);
}

function dumpInternalObj(obj, name, indent, depth) {
	if (depth > MAX_DUMP_DEPTH) {
		return indent + name + ": <Maximum Depth Reached>\n";
	}
	if (typeof obj == "object") {
		var child = null;
		var output = indent + name + "\n";
		indent += "\t";
		for (var item in obj) {
			try {
				child = obj[item];
			} catch (e) {
				child = "<Unable to Evaluate>";
			}
			if (typeof child == "object") {
				output += dumpInternalObj(child, item, indent, depth + 1);
			} else {
				output += indent + item + ": " + child + "\n";
			}
		}
		return output;
	} else {
		return obj;
	}
}

</script>
<link type="text/css" href="${resource(dir:'theme', file:'demos.css')}" rel= "stylesheet">
<link type="text/css" href="${resource(dir:'flot', file:'layout.css')}" rel= "stylesheet">
<!--link type="text/css" href="/flot/layout.css" rel= "stylesheet"-->
<style type="text/css">
body {
	font-size: 62.5%;
}
table {
	font-size: 1em;
}
body {
	font-family: "Trebuchet MS", "Helvetica", "Arial",  "Verdana", "sans-serif";
}
.table {
	display: table;
} .row {
	display: table-row;
} .cell {
	display: table-cell;
} .rounded {
	border-color: #AAAAAA;
	border-style: solid;
	border-width: 1px;
	-moz-border-radius: 4px;
	-webkit-border-radius: 4px;
} .dotted {
	border-color: #AAAAAA;
	border-style: dotted;
	border-width: 1px;
} .amount {
	color: #888888;
}
</style>
<link rel="stylesheet" href="${resource(dir:'css', file:'grailsmain.css')}"/>
<g:if test="${templateVer == 'lhp'}">
<link rel="stylesheet" href="${resource(dir:'css', file:'grailsmainlhp.css')}"/>
</g:if>
<link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon" />
<g:layoutHead />
<g:javascript library="application" />
</head>
<body>
<g:if test="${templateVer == 'lhp'}">
	<div id="grailsLogo" class="logo"><a href="index"><img src="${resource(dir:'images',file:'logo_sm_lhp.png')}" alt="Curious" border="0" /></a></div>
</g:if>
<g:else>
	<div id="grailsLogo" class="logo"><a href="index"><img src="${resource(dir:'images',file:'logo_sm.png')}" alt="Curious" border="0" /></a></div>
</g:else>
	<g:layoutBody />
</body>
</html>

<title><g:layoutTitle default="Curious" /></title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link type="text/css" href="${resource(dir:'css/bootstrap', file:'bootstrap.min.css')}" rel= "stylesheet">
<link type="text/css" href="${resource(dir:'css/custom-theme', file:'jquery-ui-1.8.18.custom.css')}" rel= "stylesheet">
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery-1.7.2.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js/bootstrap', file:'bootstrap.min.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery.json-2.2.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery.offline.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery-ui-1.8.18.custom.js')}"></script>
<script type="text/javascript" src="/static/js/jstz-1.0.4.min.js"></script>
<script type="text/javascript" src="/static/js/curious/base.js?ver=9"></script>
<script type="text/javascript" src="/static/js/curious/curious.js?ver=9"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="/lib/flot/excanvas.pack.js"></script><![endif]-->
<link rel="stylesheet" href="${resource(dir:'css', file:'main.css?v=5')}"/>
<g:if test="${templateVer == 'lhp'}">
<link rel="stylesheet" href="${resource(dir:'css', file:'mainlhp.css')}"/>
</g:if>
<g:layoutHead />
<r:layoutResources/>
<script type="text/javascript">
function makeGetUrl(url) {
	return "/public/" + url + "?callback=?";
}

function makeGetArgs(args) {
	return args;
}

function makePostUrl(url) {
	return "/public/" + url;
}

function makePostArgs(args) {
	return args;
}

function makePlainUrl(url) {
	return "/public/" + url;
}

function initTemplate() {
}

function formatDate(d) {
	var hour = d.getHours();
	var ampm = "am";
	if (hour == 0) hour = 12;
	else if (hour > 12) { hour -= 12; ampm = "pm"; }
	var minute = d.getMinutes();
	if (minute < 10) minute = "0" + minute;
	
	return '' + (d.getMonth() + 1) +'/' + d.getDate() + '/' + d.getFullYear() + ' '
			+ hour + ':' + minute + ampm;
}

function formatShortDate(d) {
	var hour = d.getHours();
	var ampm = "am";
	if (hour == 0) hour = 12;
	else if (hour > 12) { hour -= 12; ampm = "pm"; }
	var minute = d.getMinutes();
	if (minute < 10) minute = "0" + minute;
	
	return '' + (d.getMonth() + 1) +'/' + d.getDate() + '/' + ('' + d.getFullYear()).slice(-2) + ' '
			+ hour + ':' + minute + ampm;
}
</script>

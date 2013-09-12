<title><g:layoutTitle default="Curious" /></title>
<script type="text/javascript">
</script>
<link type="text/css" href="${resource(dir:'css/custom-theme', file:'jquery-ui-1.8.18.custom.css')}" rel= "stylesheet">
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery-1.7.2.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery.json-2.2.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery.offline.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery-ui-1.8.18.custom.js')}"></script>
<script type="text/javascript" src="/static/js/curious/base.js?v=5"></script>
<script type="text/javascript" src="/static/js/curious/curious.js?v=5"></script>
<script type="text/javascript" src="/static/js/curious/autocomplete.js?v=5"></script>
<script type="text/javascript" src="/static/js/curious/treeview.js?v=5"></script>
<script type="text/javascript" src="/static/js/curious/taglist.js?v=5"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="/lib/flot/excanvas.pack.js"></script><![endif]-->
<link rel="stylesheet" href="${resource(dir:'css', file:'main.css?v=4')}"/>
<g:if test="${templateVer == 'lhp'}">
<link rel="stylesheet" href="${resource(dir:'css', file:'mainlhp.css')}"/>
</g:if>
<g:layoutHead />
<script type="text/javascript">
function makeGetUrl(url) {
	return "/home/" + url + "?callback=?";
}

function makeGetArgs(args) {
	return args;
}

function makePostUrl(url) {
	return "/home/" + url;
}

function makePostArgs(args) {
	return args;
}

function makePlainUrl(url) {
	return "/home/" + url;
}

function initTemplate() {
	$("#logoutLink").click(function() {
		doLogout();
		return true;
	});
}

function addPerson(name, username, userId, sex) {
	if (sex == 'F') sex = 'Female';
	if (sex == 'M') sex = 'Male';
	$('#displayUser').html('<a href="/home/userpreferences?userId=' + userId + '">' + username + '<\/a>');
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

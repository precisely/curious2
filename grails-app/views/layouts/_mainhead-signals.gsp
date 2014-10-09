<title><g:layoutTitle default="Curious" /></title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link type="text/css" href="/css/bootstrap/bootstrap.min.css" rel= "stylesheet">
<link type="text/css" href="/css/custom-theme/jquery-ui-1.8.18.custom.css" rel= "stylesheet">
<script type="text/javascript" src="/js/jquery/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="/js/bootstrap/bootstrap.min.js"></script>
<script type="text/javascript" src="/js/jquery/jquery-ui-1.8.18.custom.js"></script>
<script type="text/javascript" src="/js/jstz-1.0.4.min.js"></script>
<script type="text/javascript" src="/js/curious/base.js?ver=21"></script>
<script type="text/javascript" src="/js/curious/webBase.js?ver=21"></script>
<script type="text/javascript" src="/js/curious/curious.js?ver=21"></script>
<script type="text/javascript" src="/js/curious/autocomplete.js?ver=21"></script>
<script type="text/javascript" src="/js/curious/treeview.js?ver=21"></script>
<script type="text/javascript" src="/js/curious/taglist.js?ver=21"></script>
<script type="text/javascript" src="/js/curious/signals.js?ver=21"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="/lib/flot/excanvas.pack.js"></script><![endif]-->
<link rel="stylesheet" href="/css/main.css?ver=21"/>
<link rel="stylesheet" href="/css/main-responsive.css?ver=21"/>
<g:if test="${templateVer == 'lhp'}">
<link rel="stylesheet" href="/css/mainlhp.css"/>
</g:if>
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
	
	return '' + (d.getMonth() + 1) +'/' + d.getDate() + '/' + ('' + d.getFullYear()).slice(-2);
}
</script>

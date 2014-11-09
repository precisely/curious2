<title><g:layoutTitle default="Curious" /></title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link type="text/css" href="/css/custom-theme/jquery-ui-1.8.18.custom.css" rel= "stylesheet">
<script type="text/javascript" src="/js/jquery/jquery-1.7.2.min.js"></script>
<script type="text/javascript" src="/js/jquery/jquery.json-2.2.js"></script>
<script type="text/javascript" src="/js/jquery/jquery-ui-1.8.18.custom.js"></script>

<script type="text/javascript" src="/js/curious/base.js?ver=20"></script>
<script type="text/javascript" src="/js/curious/webBase.js?ver=20"></script>
<link type="text/css" href="/css/login.css" rel= "stylesheet">
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

$(function() {
	$("#logoutLink").click(function() {
		doLogout();
		return true;
	});
}
</script>

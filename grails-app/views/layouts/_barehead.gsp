<title><g:layoutTitle default="Curious" /></title>
<link type="text/css" href="${resource(dir:'css/custom-theme', file:'jquery-ui-1.8.18.custom.css')}" rel= "stylesheet">
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery-1.7.2.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery.json-2.2.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery.offline.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js/jquery', file:'jquery-ui-1.8.18.custom.js')}"></script>
<script type="text/javascript" src="${resource(dir:'js/curious', file:'base.js?ver=14')}"></script>
<script type="text/javascript" src="${resource(dir:'js/curious', file:'webBase.js?ver=14')}"></script>
<link rel="stylesheet" href="${resource(dir:'css', file:'login.css')}"/>
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
</script>

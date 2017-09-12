<!DOCTYPE html>
<html>
<head>
	<title><g:layoutTitle default="precise.ly"/></title>
	<link rel="shortcut icon" href="/images/favicon.ico" type="image/x-icon" />
	<meta name="description" content="A platform for health hackers"/>
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<link type="text/css" href="/css/bootstrap/bootstrap.min.css" rel="stylesheet">
	<link type="text/css" href="/css/custom-theme/jquery-ui.min.css" rel="stylesheet">
	<link type="text/css" href="//cdnjs.cloudflare.com/ajax/libs/font-awesome/4.2.0/css/font-awesome.min.css" rel="stylesheet">
	<script type="text/javascript" src="/js/jquery/jquery-2.1.1.min.js"></script>
	<script type="text/javascript" src="/js/jquery/jquery-migrate-1.1.0.min.js"></script>
	<script type="text/javascript" src="/js/lodash/lodash.min.js"></script>
	<script type="text/javascript" src="/js/bootstrap/bootstrap.min.js"></script>
	<script type="text/javascript" src="/js/jquery/jquery-ui.min.js"></script>
	<script type="text/javascript" src="/js/jstz-1.0.4.min.js"></script>
	<script type="text/javascript" src="/js/curious/base.js?ver=24"></script>
	<script type="text/javascript" src="/js/curious/webBase.js?ver=24"></script>
	<script type="text/javascript" src="/js/curious/auto.resize.js?ver=22"></script>
	<script type="text/javascript" src="/js/curious/curious.js?ver=23"></script>
	<script type="text/javascript" src="/js/jquery/jquery.json-2.2.js"></script>
	<script type="text/javascript" src="/js/store2.min.js"></script>
	<!--[if IE]><script language="javascript" type="text/javascript" src="/lib/flot/excanvas.pack.js"></script><![endif]-->
	<link rel="stylesheet" href="/css/core.css?ver=22"/>
	<link rel="stylesheet" href="/css/main.css?ver=22"/>
	<%--<link rel="stylesheet" href="/css/sprite.css"/>--%>
	<link rel="stylesheet" href="/css/main-responsive.css?ver=22"/>
	<g:if test="${templateVer == 'lhp'}">
		<link rel="stylesheet" href="/css/mainlhp.css"/>
	</g:if>

	<script type="text/javascript">
		function makeGetUrl(url) {
			return "/home/" + url + "?callback=?";
		}

		//_.templateSettings.interpolate = /{{([\s\S]+?)}}/g;

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

		$(function () {
			$("#logoutLink").click(function () {
				doLogout();
				return true;
			});
		});

		function addPerson(name, username, userId, sex) {
			if (sex == 'F') sex = 'Female';
			if (sex == 'M') sex = 'Male';
			console.log(username);
			$('.dropdown-toggle').html(username + '<b class="caret"></b>');
			if (username.indexOf('(anonymous)') > -1) {
				$('#displayUser').html('<a href="/home/register"> Sign Up <\/a>');
				$('#logoutLink').html('');
			} else {
				$('#displayUser').html('<a href="/home/userpreferences?userId=' + userId + '"> Edit Profile <\/a>');
			}
		}

		function formatDate(d) {
			var hour = d.getHours();
			var ampm = "am";
			if (hour == 0) hour = 12;
			else if (hour > 12) {
				hour -= 12;
				ampm = "pm";
			}
			var minute = d.getMinutes();
			if (minute < 10) minute = "0" + minute;

			return '' + (d.getMonth() + 1) + '/' + d.getDate() + '/' + d.getFullYear() + ' '
					+ hour + ':' + minute + ampm;
		}

		function formatShortDate(d) {
			var hour = d.getHours();
			var ampm = "am";
			if (hour == 0) hour = 12;
			else if (hour > 12) {
				hour -= 12;
				ampm = "pm";
			}
			var minute = d.getMinutes();
			if (minute < 10) minute = "0" + minute;

			return '' + (d.getMonth() + 1) + '/' + d.getDate() + '/' + ('' + d.getFullYear()).slice(-2);
		}

		var actionName = '${actionName}';
		var controllerName = '${controllerName}';
	</script>

	<c:setExplanationCardUserPreferences></c:setExplanationCardUserPreferences>
	<g:layoutHead/>
</head>

<body class="${pageProperty(name: 'body.class') ?: ''}">
	<i id="spinner-feedback" class="hide fa fa-spin fa-spinner" style="position: fixed; top: 10px; left: 10px;z-index: 1050;"></i>
	<g:layoutBody/>

	<footer>
		<script>
			(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
						(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
					m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
			})(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

			ga('create', 'UA-106350495-1', 'auto');
			ga('send', 'pageview');

		</script>
	</footer>

</body>
</html>

<html>
<head>
<meta name="layout" content="bare" />
<title>Login</title>
<script type="text/javascript">
$(function(){
	$("input:text:visible:first").focus();
    $('form').each(function() {
        $('input').keypress(function(e) {
            // Enter pressed?
            if(e.which == 10 || e.which == 13) {
                this.form.submit();
            }
        });

        $('input[type=submit]').hide();
    });
});
</script>
<style type="text/css">
/* FRAME */

* {
	vertical-align: baseline;
	font-weight: inherit;
	font-family: arial,helvetica,san-serif;
	font-style: inherit;
	font-size: 100%;
	border: 0 none;
	outline: 0;
	padding: 0;
	margin: 0;
}
h1 {
	font-weight:bold;
}
body {
	text-align:left;
}
a,a:link,a:hover,a:visited {
	color:#08BBF1;	
	text-decoration:none;
}

.bugs {
	position:relative;
}
.bugs a {	
	display:block;
	background:url(/images/bugs.gif) no-repeat;
	width:19px;
	height:20px;
	position:absolute;
	top:26px;
	right:0px;	
}
.bugs a span {	
	display:none;
}

/* JQUERY UI MODS */
.ui-accordion-header {
	overflow:hidden;
}
#login {
	border:0px;	
	text-align:center;
	padding:3em 5em .5em 5em;
}

#login .loginbody2 {

	border-width:2px 0px;
	border-color:rgb(239,75,58);
	border-style: solid;	
	
<g:if test="${templateVer == 'lhp'}">
	padding: 3em 2em 3em 2em;
	max-width:900px;
</g:if>
<g:else>
	padding: 3em 5em 3em 5em;
	max-width:800px;
</g:else>
	margin:0px auto .5em auto;
	text-align:right;
}

#login form {
	text-align:right;
	padding-top:7em;
}

#login form input {
	display:inline-block;
	width:225px;
	clear:right;
	padding:2px;
	margin:0px 0px 4px 0px;
	background-color:#eeeeee;
}
#loginfields {
	padding:0px 0px 1em 0px;
}
#headerbutton {
	background:url(/images/lhpdonate.gif) no-repeat;
	width:135px;
	height:50px;
	display:inline-block;
	vertical-align:middle;
	margin-left:20px;
}
</style>
</head>
<body class="login">
<div id="login" class="loginbody1">
<div class="loginbody2">
<g:if test="${flash.message}">
	<div class="loginmessage">${flash.message.encodeAsHTML()}</div>
</g:if>

<g:if test="${templateVer == 'lhp'}">
<img alt="Curious" src="/images/logo_login_lhp.png" />
</g:if>
<g:else>
<img alt="Curious" src="/images/logo_login.png" />
</g:else>

<form method="post" action="/home/dologin">

<input type="hidden" name="precontroller" value="${precontroller.encodeAsHTML()}"/>
<input type="hidden" name ="preaction" value="${preaction.encodeAsHTML()}"/>
<input type="hidden" name ="parm" value="${parm.encodeAsHTML()}"/>

<div id="loginfields">
<input type="text" id="username" name="username" value="" placeholder="username" /><br />
<input type="password" id="password" name="password" value="" placeholder="password" /><br />
<button><img src="/images/login.png" width="76" height="24" alt="Login" /></button><br />
</div>

<g:link action="forgot" params="${['precontroller':precontroller,'preaction':preaction]}">Forgot your login info?</g:link>
<br/><g:link action="register" params="${['precontroller':precontroller,'preaction':preaction]}">Create an account</g:link>

</form>


</div>

<g:if test="${templateVer == 'lhp'}">
<a href="mailto:info@LAMHealthProject.org">Send an email</a> or <a href="https://twitter.com/LAMHlthProject" target="_blank">follow us on Twitter</a> or <a href="https://www.facebook.com/LAMHealthProject" target="_blank">like us on Facebook</a>. 
</g:if>
<g:else>
<a href="mailto:contact@wearecurio.us">Send an email</a> or <a href="http://twitter.com/wearecurious">follow us on Twitter</a> to be included in the next round of alpha invites. Or just say hi. 
</g:else>
<g:if test="${templateVer == 'lhp'}">
	<a href="https://npo1.networkforgood.org/Donate/Donate.aspx?npoSubscriptionId=3737" id="headerbutton"></a>
</g:if>

</div>
</body>
</html>

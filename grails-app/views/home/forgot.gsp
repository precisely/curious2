<html>
<head>
<meta name="layout" content="grailsmain" />
<title>Login</title>
<script type="text/javascript">
$(function(){
	$("input:text:visible:first").focus();
});
</script>
</head>
<body>
<div class="body">
<h1>Recover Password</h1>
<g:if test="${flash.message}">
	<div class="loginmessage">${flash.message.encodeAsHTML()}</div>
</g:if>
<g:if test="${message}">
	<div class="loginmessage">${flash.message.encodeAsHTML()}</div>
</g:if>
<div class="logindialog">
	<g:form action="doforgot" method="post" >
		<input type="hidden" name="precontroller" value="${precontroller.encodeAsHTML()}"/>
		<input type="hidden" name ="preaction" value="${preaction.encodeAsHTML()}"/>
		<div class="loginline">
			<div style="font-size:10pt">
			<label for="Username">Please enter your username:</label>
			</div>
			<div class="loginfield">
			<input style="width:250px" "type="username" id="username" name="username"/>
			</div>
		</div>
		<br/>
		<div class="loginbuttons">
			<span class="button">
			<input class="save" type="submit" value="Recover" />
			</span>
		</div>
	</g:form>
</div>
</body>
</html>

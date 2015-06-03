<html>
<head>
<meta name="layout" content="home" />
<script type="text/javascript">
$(function(){
	$("input:text:visible:first").focus();
});
</script>
</head>
<body>
<div class="body">
<h1>Update your password</h1>
<g:if test="${flash.message}">
	<div class="loginmessage">${flash.message.encodeAsHTML()}</div>
</g:if>
<div class="logindialog">
	<g:form action="dorecover" method="post" >
		<input type="hidden" name="precontroller" value="${precontroller.encodeAsHTML()}"/>
		<input type="hidden" name ="preaction" value="${preaction.encodeAsHTML()}"/>
		<input type="hidden" name ="code" value="${code.encodeAsHTML()}"/>
		<div class="loginline">
			<div style="font-size:10pt;padding-bottom:5px">
			<label for="password">Please enter a new password:</label>
			</div>
			<div class="loginfield">
			<input style="width:235px" type="password" id="password" name="password"/>
			</div>
		</div>
		<div class="loginline" style="padding-bottom:30px">
			<div style="font-size:10pt;padding-bottom:5px;padding-top:15px">
			<label for="password">Verify your password:</label>
			</div>
			<div class="loginfield">
			<input style="width:235px" type="password" id="passwordverify" name="passwordverify"/>
			</div>
		</div>
		<div class="loginbuttons">
			<span class="button">
			<input class="save" type="submit" value="Update" />
			</span>
		</div>
	</g:form>
</div>
<div class="loginregisterline">
<div></div><g:link action="register" params="${['precontroller':precontroller,'preaction':preaction]}">Create an account</g:link></div>
</div>
</body>
</html>

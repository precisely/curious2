<html>
  <head>
    <meta name="layout" content="trialmain" />
    <title>Login</title>
    <script type="text/javascript">
	  $(function(){
		$("input:text:visible:first").focus();
	  });
	</script>
  </head>
  <body>
    <div class="body">
      <div id="grailsLogo" class="logo"><a href="index"><img src="${resource(dir:'images',file:'logomedium.png')}" alt="Curious" border="0" /></a></div>
      <g:if test="${flash.message}">
        <div class="message">${flash.message.encodeAsHTML()}<br/></div>
      </g:if>
	  <div class="trialintro">
		<h2>Help us out. We're wondering if coffee affects your resting heart rate.</h2>

		<p>Take your pulse before and after coffee for five days and we’ll let you know what we find.</p>

		<p class="tight">But first:</p>
		<ul><li>Sign in and authorize Curious to receive your private, direct messages via Twitter.</li>
		<li>Answer a few questions about yourself.</li>
		<li>Go over our guidelines for monitoring and recording your heart rate.</li>
		</ul>		
		
		<g:form action="dologin" method="post" >
		  <div class="dialog">
			<input type="hidden" name="precontroller" value="${precontroller}"/>
			<input type="hidden" name ="preaction" value="${preaction}"/>
					<label for="Username">Username:</label>
					<input type="text" id="username" name="username"/><br />
					<label for="password">Password:</label>
					<input type="password" id="password" name="password"/>
		  </div>
		  <div class="buttons">
			  <input class="save" type="submit" value="Sign in to get started &raquo;" />
		  </div>
		  <div class="links">
			  <g:link action="register" params="${['precontroller':precontroller,'preaction':preaction]}">Oops, I don't know my username or password.</g:link>
		  </div>
		</g:form>      
    </div>
	  
	  </div>	  
  </body>
</html>

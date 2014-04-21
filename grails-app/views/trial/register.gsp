<html>
  <head>
    <meta name="layout" content="trialmain" />
    <title>Register</title>
    <script type="text/javascript">
	  $(function(){
		$("input:text:visible:first").focus();
	  });
	</script>
  </head>
  <body>
    <div class="body">
      <div id="grailsLogo" class="logo"><a href="index"><img src="/images/logomedium.png" alt="Curious" border="0" /></a></div>
      <div class="trialintro">
	  <h1>New User Signup</h1>
      <g:if test="${flash.message}">
        <div class="message">${flash.message.encodeAsHTML()}<br/></div>
      </g:if>
		<p>We won't share your email address or name with any third parties.</p>
      <g:form action="doregister" method="post" >
        <div class="dialog">
          <input type="hidden" name="precontroller" value="${session.precontroller}"/>
          <input type="hidden" name="preaction" value="${session.preaction}"/>
          <table style="margin-left:auto">
            <tbody>
              <tr class="prop">
                <td class="name">
                  <label for="Username">Username:</label>
                </td>
                <td>
				  <g:if test="${session.username}">
                    ${session.username.encodeAsHTML()}
				  </g:if>
				  <g:else>
					<input type="text" id="username" name="username"/>
				  </g:else>
                </td>
              </tr>

              <tr class="prop">
                <td class="name">
                  <label for="Username">Password:</label>
                </td>
                <td>
				  <g:if test="${session.password}">
                    (already entered)
				  </g:if>
				  <g:else>
					<input type="password" id="password" name="password"/>
				  </g:else>
                </td>
              </tr>
			  
              <tr class="prop">
                <td class="name">
                  <label for="Email">Email:</label>
                </td>
                <td>
                  <input type="text" id="email" name="email"/>
                </td>
              </tr>

              <tr class="prop">
                <td class="name">
                  <label for="first" style="color:#999999">First Name (optional):</label>
                </td>
                <td>
                  <input type="text" id="first" name="first"/>
                </td>
              </tr>

              <tr class="prop">
                <td class="name">
                  <label for="last" style="color:#999999">Last Name (optional):</label>
                </td>
                <td>
                  <input type="text" id="last" name="last"/>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="buttons">
          <span class="button">
            <input class="save" type="submit" value="Register &raquo;" />
          </span>
        </div>
      </g:form>
    </div>
	</div>
  </body>
</html>

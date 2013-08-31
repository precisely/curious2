<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery"/>
<html>
<head>
  <meta name="layout" content="main" />
  <title>Curious</title>
  <meta name="description" content="A platform for health hackers" />
<style type="text/css">
.name {
	font-size:10pt;
}
.name a, .prop a, .prop input, .name input {
	font-size:10pt;
}
.savebutton {height:30px;margin:0px;padding-left:5px;padding-right:5px;background-color:#08BBF1;color:#FFFFFF;padding-top:5px;padding-bottom:5px;}
.main form {
}
</style>
<script type="text/javascript">
function refreshPage() {
}

function doLogout() {
	callLogoutCallbacks();
}

$(function(){
	initTemplate();
	
	$.getJSON("/home/getPeopleData?callback=?",
		function(data){
			if (!checkData(data))
				return;
			
			found = false;
			
			jQuery.each(data, function() {
				if (!found) {
					// set first user id as the current
					setUserId(this['id']);
					found = true;
				}
				addPerson(this['first'] + ' ' + this['last'],
					this['username'], this['id'], this['sex']);
				return true;
			});
		});
});
</script>
</head>
<body>
  <!-- MAIN -->
  <div class="main">

    <div id="area0">
      <div id="addData">
		<g:if test="${flash.message}">
		  <div class="message">${flash.message.encodeAsHTML()}</div>
		</g:if>
      <h1>Answer a few questions</h1>
      <g:form action="doupdateuserpreferences" method="post">
        <div class="dialog">
          <input type="hidden" name="precontroller" value="${precontroller}"/>
          <input type="hidden" name="preaction" value="${preaction}"/>
          <input type="hidden" name="userId" value="${user.id}"/>
          <table style="width:300px;">
            <tbody>
              <tr class="prop">
                <td class="name">
                  <label for="Username">Username:</label>
                </td>
                <td>
                  <input type="text" id="username" name="username" value="${user.username.encodeAsHTML()}"/>
                </td>
              </tr>

              <tr class="prop">
                <td class="name">
                  <label for="Password">Change password:</label>
                </td>
                <td>
                  <input type="password" id="password" name="password" value=""/>
                </td>
              </tr>

              <tr class="prop">
                <td class="name">
                  <label for="Email">Email:</label>
                </td>
                <td>
                  <input type="text" id="email" name="email" value="${user.email.encodeAsHTML()}"/>
                </td>
              </tr>

              <tr class="prop">
                <td class="name">
                  <label for="Notify Email">Email to send reminders to:</label>
                </td>
                <td>
                  <input type="text" id="remindEmail" name="remindEmail" value="${user.remindEmail ? user.remindEmail.encodeAsHTML() : ""}"/>
                </td>
              </tr>

              <tr class="prop">
                <td class="name">
                  <label for="first">First Name:</label>
                </td>
                <td>
                  <input type="text" id="first" name="first" value="${user.first.encodeAsHTML()}"/>
                </td>
              </tr>

              <tr class="prop">
                <td class="name">
                  <label for="last">Last Name:</label>
                </td>
                <td>
                  <input type="text" id="last" name="last" value="${user.last.encodeAsHTML()}"/>
                </td>
              </tr>

              <tr class="prop">
                <td class="name">
                  <label for="birthdate">Birthdate (MM/DD/YYYY):</label>
                </td>
                <td>
                  <input type="text" id="birthdate" name="birthdate" value="${user.birthdate ? new java.text.SimpleDateFormat("MM/dd/yyyy").format(user.birthdate) : ''}"/>
                </td>
              </tr>

              <tr class="prop">
                <td class="name">
                  <label for="first">Email Notifications</label>
                </td>
                <td class="name">
                  <input type="radio" name="notifyOnComments" value="off" <g:if test="${!user.getNotifyOnComments()}">checked</g:if>>No emails on comments<br>
                  <input type="radio" name="notifyOnComments" value="on" <g:if test="${user.getNotifyOnComments()}">checked</g:if>>Emails on comments<br>
                </td>
              </tr>

              <tr class="prop">
                <td class="name" colspan="3">
                  <g:radioGroup name="sex" labels="['Male','Female']" values="['M','F']" value="${user.sex}">
                    ${it.label} ${it.radio}
                  </g:radioGroup>
                </td>
              </tr>

			  <g:if test="${user.twitterAccountName == null || user.twitterAccountName.length() == 0}">
				<tr class="prop">
				  <td class="name" colspan="3">
					<g:link action="registertwitter">Link Twitter Account</g:link>
				  </td>
				</tr>
			  </g:if>
			  <g:else>
				<tr class="prop">
				  <td class="name">
					<label for="twitterAccountName">Twitter Account Name:</label>
				  </td>
				  <td>
					${user.twitterAccountName}
				  </td>
				</tr>
				<tr class="prop">
				  <td class="name" colspan="3">
					<g:link action="registertwitter">Link Other Twitter Account</g:link>
				  </td>
				</tr>
			  </g:else>

			  <tr class="prop">
			    <td class="name" colspan="3">
				  <g:link action="registerfitbit">Link FitBit Account</g:link>
			    </td>
			  </tr>

			  <tr class="prop">
			    <td class="name" colspan="3">
				  <g:link action="registerwithings">Link Withings Account</g:link>
			    </td>
			  </tr>

              <tr class="prop">
                <td class="name">
                  <label for="first">Default Twitter to Now</label>
                </td>
                <td class="name">
                  <input type="checkbox" name="twitterDefaultToNow" style="border: none;width: 20px;"<g:if test="${user.getTwitterDefaultToNow()}"> checked</g:if>>
                    Default Twitter timestamp to now
                </td>
              </tr>

		  </tbody>
          </table>
        </div>
        <div class="buttons">
          <input class="savebutton" type="submit" value="Update" />
        </div>
      </g:form>
      </div>
    </div>

  </div>
  <!-- /MAIN -->

  <!-- RIGHT NAV -->
  <div class="tagNav">

  </div>
  <!-- /RIGHT NAV -->

  <div style="clear:both;"></div>

</body>
</html>

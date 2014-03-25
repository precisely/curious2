<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<title>Curious</title>
<meta name="layout" content="main" />
<meta name="description" content="A platform for health hackers" />

<r:script>
function refreshPage() {
}
function doLogout() {
    callLogoutCallbacks();
}
var origUsername = "${user.username}";

$(function() {
    initTemplate();

    queueJSON("getting login info", "/home/getPeopleData?callback=?", function(data) {
        if (!checkData(data))
            return;

        found = false;

        jQuery.each(data, function() {
            if (!found) {
                // set first user id as the current
                setUserId(this['id']);
                found = true;
            }
            addPerson(this['first'] + ' ' + this['last'], this['username'], this['id'], this['sex']);
            return true;
        });
    });
    // defeat the damn browser autofill
    $("#username").val("${user.username}");
    $("#oldPassword").val("");
    $("#password").val("");
    $("#verify_password").val("");
	$("#updateUserPreferences").submit(function(e) {
		e.preventDefault();
		var form = this;
		var newUsername = $("#username").val();
		var newPw = $("#password").val();
		if (origUsername != newUsername) {
			if (newPw.length == 0) {
				showAlert("If you change the username, you must set the password as well");
				return;
			}
		}
		if (newPw.length > 0) {
			if (newPw != $("#verify_password").val()) {
				showAlert("New password and verification do not match");
				return;
			}
		}
		form.submit();
	});
});
</r:script>

<% randTag = "" + ((java.lang.System.currentTimeMillis() * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1)) %>

<style type="text/css">
	input.savebutton {
		padding: 5px 10px;
		background-color: #08BBF1;
		color: #FFFFFF;
	}
	.profile-container table {
		font-size: 10pt;
		width: 100%;
	}
	.profile-container table tr td {
		padding-bottom: 20px;
	}
	.profile-container table tr td:first-child {
		font-weight: bold;
		padding-right: 20px;
		text-align: right;
		text-transform: uppercase;
		width: 200px;
	}
	.profile-container input:not([type=radio]):not([type=checkbox]):not([type=submit]),
		.profile-container text-area {
		padding: 6px;
		min-width: 300px;
	}
	.logo {
		background-color: #ccc;
		min-height: 218px;
	}
</style>
</head>
<body>
	<div class="red-strip"></div>
    <g:render template="/layouts/alertMessage" />
	<div class="row">
		<div class="span3 text-center">
			<div class="logo"></div>
		</div>
		<div class="span9 profile-container">
			<div id="addData">
				<form action="/home/doupdateuserpreferences" method="post" name="updateUserPreferences" id="updateUserPreferences" autocomplete="off" >
					<g:hiddenField name="precontroller" value="${precontroller}" />
					<g:hiddenField name="preaction" value="${preaction}" />
					<g:hiddenField name="userId" value="${user.id}" />
					<table>
						<tbody>
							<tr>
								<td><label for="Username">Username</label></td>
								<td><g:textField name="username" value="${user.username}" autofocus="" required="" autocomplete="off" /></td>
							</tr>

							<tr>
								<td><label for="oldPassword">Old password</label></td>
								<td><g:passwordField name="oldPassword" value="z" autocomplete="off"/></td>
							</tr>

							<tr>
								<td><label for="Password">New password</label></td>
								<td><g:passwordField name="password" value="z" autocomplete="off" /></td>
							</tr>

							<tr>
								<td><label for="Verify Password">Verify password</label></td>
								<td><g:passwordField name="verify_password" value="" /></td>
							</tr>

							<tr>
								<td><label for="Email">Email</label></td>
								<td><g:field type="email" required="" name="email" value="${user.email}" /></td>
							</tr>

							<tr>
								<td><label for="first">First Name</label></td>
								<td><g:textField name="first" value="${user.first.encodeAsHTML()}" /></td>
							</tr>

							<tr>
								<td><label for="last">Last Name</label></td>
								<td><g:textField name="last" value="${user.last.encodeAsHTML()}" /></td>
							</tr>

							<tr>
								<td><label for="birthdate">Birthdate (MM/DD/YYYY)</label></td>
								<td><g:textField name="birthdate"
									value="${user.birthdate ? new java.text.SimpleDateFormat("MM/dd/yyyy").format(user.birthdate) : ''}" /></td>
							</tr>

							<tr>
								<td><label for="Notify Email">Email to send reminders to</label></td>
								<td><g:textField name="remindEmail"
									value="${user.remindEmail ? user.remindEmail.encodeAsHTML() : ""}" /></td>
							</tr>

							<tr>
								<td><label for="first">Email Notifications</label></td>
								<td>
									<g:radio name="notifyOnComments" value="off"
										checked="${!user.getNotifyOnComments()}" /> No emails on comments<br>
									<g:radio name="notifyOnComments" value="on"
										checked="${user.getNotifyOnComments()}" /> Emails on comments
								</td>
							</tr>

							<tr>
								<td><label for="sex">Gender</label></td>
								<td style="padding-bottom: 0px;">
									<g:radio name="sex" value="F" checked="${user.sex == 'F' }"/> Female<br>
									<g:radio name="sex" value="M" checked="${user.sex == 'M' }"/> Male<br><br>
								</td>
							</tr>

							<tr>
								<td><label>Other Accounts</label>
								<td>
									<g:if test="${!user.twitterAccountName}">
										<g:link action="registertwitter">Link Twitter Account</g:link>
									</g:if>
									<g:else>
										Twitter Account: ${user.twitterAccountName}<br>
										<g:link action="registertwitter">Link Other Twitter Account</g:link>
									</g:else><br>
									<oauth:checkSubscription userId="${user.id}" typeId="FITBIT">
										<g:if test="${it?.accessToken }">
											<g:link action="unregisterfitbit">Unlink FitBit Account</g:link><br>
										</g:if>
										<g:elseif test="${it && !it.accessToken }">
											<g:link action="registerfitbit" class="text-danger">Re-link FitBit Account</g:link><br>
										</g:elseif>
										<g:else>
											<g:link action="registerfitbit">Link FitBit Account</g:link><br>
										</g:else>
									</oauth:checkSubscription>
									<oauth:checkSubscription userId="${user.id}" typeId="WITHINGS">
										<g:if test="${it?.accessToken }">
											<g:link action="unregisterwithings">Unlink Withings Account</g:link><br>
										</g:if>
										<g:elseif test="${it && !it.accessToken }">
											<g:link action="registerwithings" class="text-danger">Re-link Withings Account</g:link><br>
										</g:elseif>
										<g:else>
											<g:link action="registerwithings">Link Withings Account</g:link><br>
										</g:else>
									</oauth:checkSubscription>
									<oauth:checkSubscription userId="${user.id}" typeId="MOVES">
										<g:if test="${it?.accessToken }">
											<g:link action="unregistermoves">Unlink Moves Account</g:link><br>
										</g:if>
										<g:elseif test="${it && !it.accessToken }">
											<g:link action="registermoves" class="text-danger">Re-link Moves Account</g:link><br>
										</g:elseif>
										<g:else>
											<g:link action="registermoves">Link Moves Account</g:link><br>
										</g:else>
									</oauth:checkSubscription>
									<g:link action="register23andme">
										<oauth:checkSubscription userId="${user.id}" typeId="TWENTY_THREE_AND_ME">
											<g:if test="${it }">
												Re-import from 23andMe
											</g:if>
											<g:else>
												Import from 23andme
											</g:else>
										</oauth:checkSubscription>
									</g:link>
								</td>
							</tr>

							<tr>
								<td><label for="first">Default Twitter to Now</label></td>
								<td><input type="checkbox" name="twitterDefaultToNow" style="border: none; width: 20px;"
									<g:if test="${user.getTwitterDefaultToNow()}"> checked</g:if>> Default Twitter timestamp to now</td>
							</tr>
							<tr>
								<td></td>
								<td class="text-right"><g:submitButton class="savebutton" name="Update" /></td>
							</tr>
						</tbody>
					</table>
				</form>
			</div>
		</div> <!-- .span9 ends -->
	</div>
</body>
</html>
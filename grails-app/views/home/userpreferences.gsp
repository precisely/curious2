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
	.profile-container {
		font-size: 10pt;
	}
	.profile-container #addData .form-group {
		margin-bottom: 20px;
	}
	.profile-container #addData .form-group label.control-label {
		padding-right: 10px;
		text-transform: uppercase;
		width: 200px;
	}
	.profile-container input:not([type=radio]):not([type=checkbox]):not([type=submit]), .profile-container text-area {
		padding: 6px;
		min-width: 300px;
	}
	.logo {
		background-color: #ccc;
		min-height: 218px;
	}
</style>
</head>
<body class="user-preference">
	<div class="red-strip"></div>
	<g:render template="/layouts/alertMessage" />
	<div class="row">
		<div class="col-sm-3 text-center">
			<div class="logo"></div>
		</div>
		<div class="col-sm-9 profile-container">
			<div id="addData">
				<g:form url="/home/doupdateuserpreferences" name="updateUserPreferences" class="form-horizontal" autocomplete="off">
					<g:hiddenField name="precontroller" value="${precontroller}" />
					<g:hiddenField name="preaction" value="${preaction}" />
					<g:hiddenField name="userId" value="${user.id}" />
					<div class="form-group">
						<label class="control-label col-md-4" for="Username">Username</label>
						<div class="col-md-5">
							<g:textField name="username" value="${user.username}" autofocus="" required="" autocomplete="off" />
						</div>
					</div>

					<div class="form-group">
						<label class="control-label col-md-4" for="oldPassword">Old password</label>
						<div class="col-md-5">
							<g:passwordField name="oldPassword" value="z" autocomplete="off" />
						</div>
					</div>

					<div class="form-group">
						<label class="control-label col-md-4" for="Password">New password</label>
						<div class="col-md-5">
							<g:passwordField name="password" value="z" autocomplete="off" />
						</div>
					</div>

					<div class="form-group">
						<label class="control-label col-md-4" for="Verify Password">Verify password</label>
						<div class="col-md-5">
							<g:passwordField name="verify_password" value="" />
						</div>
					</div>

					<div class="form-group">
						<label class="control-label col-md-4" for="Email">Email</label>
						<div class="col-md-5">
							<g:field type="email" required="" name="email" value="${user.email}" />
						</div>
					</div>

					<div class="form-group">
						<label class="control-label col-md-4" for="first">First Name</label>
						<div class="col-md-5">
							<g:textField name="first" value="${user.first.encodeAsHTML()}" />
						</div>
					</div>

					<div class="form-group">
						<label class="control-label col-md-4" for="last">Last Name</label>
						<div class="col-md-5">
							<g:textField name="last" value="${user.last.encodeAsHTML()}" />
						</div>
					</div>

					<div class="form-group">
						<label class="control-label col-md-4" for="birthdate">Birthdate (MM/DD/YYYY)</label>
						<div class="col-md-5">
							<g:textField name="birthdate"
								value="${user.birthdate ? new java.text.SimpleDateFormat("MM/dd/yyyy").format(user.birthdate) : ''}" />
						</div>
					</div>

					<div class="form-group">
						<label class="control-label col-md-4" for="Notify Email">Email to send reminders to</label>
						<div class="col-md-5">
							<g:textField name="remindEmail" value="${user.remindEmail ? user.remindEmail.encodeAsHTML() : ""}" />
						</div>
					</div>

					<div class="form-group">
						<label class="control-label col-md-4" for="first">Email Notifications</label>
						<div class="col-md-5">
							<g:radio name="notifyOnComments" value="off" checked="${!user.getNotifyOnComments()}" />
							No emails on comments<br>
							<g:radio name="notifyOnComments" value="on" checked="${user.getNotifyOnComments()}" />
							Emails on comments
						</div>
					</div>

					<div class="form-group">
						<label class="control-label col-md-4" for="sex">Gender</label>
						<div class="col-md-5">
							<g:radio name="sex" value="F" checked="${user.sex == 'F' }" />
							Female<br>
							<g:radio name="sex" value="M" checked="${user.sex == 'M' }" />
							Male<br>
						</div>
					</div>

					<div class="form-group">
						<label class="control-label col-md-4">Other Accounts</label>
						<div class="col-md-5">
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
						</div>
					</div>

					<div class="form-group">
						<label class="control-label col-md-4" for="first">Default Twitter to Now</label>
						<div class="col-md-5">
							<input type="checkbox" name="twitterDefaultToNow" style="border: none; width: 20px;"
								<g:if test="${user.getTwitterDefaultToNow()}"> checked</g:if>> Default Twitter timestamp to now
						</div>
					</div>
					<div class="form-group">
						<div class="col-md-12 text-right">
							<g:submitButton class="savebutton" name="Update" />
						</div>
					</div>
				</g:form>
			</div>
		</div><!-- .col-sm-9 ends -->
	</div>
</body>
</html>
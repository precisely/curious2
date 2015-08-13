<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="menu" />

<c:jsCSRFToken keys="addEntryCSRF, getPeopleDataCSRF, getListDataCSRF, autocompleteDataCSRF, listTagsAndTagGroupsCSRF,
showTagGroupCSRF, createTagGroupCSRF, deleteTagGroupCSRF, addTagToTagGroupCSRF, deleteGhostEntryDataCSRF, deleteEntryDataCSRF, updateEntrySDataCSRF,
removeTagFromTagGroupCSRF, addTagGroupToTagGroupCSRF, removeTagGroupFromTagGroupCSRF, pingDataCSRF,
excludeFromTagGroupDataCSRF, addBackToTagGroupDataCSRF, getInterestTagsDataCSRF, addInterestTagDataCSRF, deleteInterestTagDataCSRF, updateInterestTagDataCSRF" />

<script type="text/javascript" src="/js/curious/interestTagList.js?ver=21"></script>
<script>
function refreshPage() {
}
function doLogout() {
    callLogoutCallbacks();
}
var origUsername = "${user.username}";

var interestTagList;

$(function() {
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
            addPerson(this['name'], this['username'], this['id'], this['sex']);
            return true;
        });
    });
    this.interestTagList = new InterestTagList("interestTagInputField", "interestTagsList");
});

function editUserDetails() {
    var updatedName = $("#name").val();
    var newUsername = $("#username").val();
    var newPw = $("#password").val();
    var bio = $("#bio").val();
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
    document.getElementById("updateUserPreferences").submit();
}
</script>

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
	#addData .form-group {
		margin-bottom: 30px;
	}
	#addData .form-group label.control-label {
		padding-right: 10px;
		text-transform: uppercase;
		font-size: 14px;
		color: #B6B6B6;
		font-weight: 300;
	}
	#user-name-label{
		padding-right: 10px;
		text-transform: uppercase;
		font-size: 14px;
		color: #B6B6B6;
		font-weight: 300;
		margin-top: 14px;
	}
	.profile-container input:not([type=radio]):not([type=checkbox]):not([type=submit]), .profile-container text-area {
		padding: 6px;
		min-width: 191px;
		height:40px;
		border-radius: 3px;
	}
	.logo {
		background-color: #ccc;
		min-height: 218px;
	}
	.user-details {
		background-color: #ffffff;
		padding-top: 30px;
		border: 1px solid #e0e0e0;
	}
</style>
</head>
<body class="user-preference">
	<div class="red-strip"></div>
	<div class="main container-fluid ">
		<div class = "user-details">
			<div class="row">
				<div class="pull-right">
					<button class="save-user-details" onclick="editUserDetails()">Save Profile</button>
				</div>
				<div class="user-image pull-left">
					<img src="/images/avatar.png" alt="" class="img-circle">
					<div class="form-group">
						<button class="edit-image-button">Edit Image</button>
					</div>
				</div>
				<div class="user-name">
					<label class="control-label" for="name" id="user-name-label">Name</label>
					<g:textField class="form-control" name="name" placeholder="Enter name here" value="${user.name}"/>
					<div class="user-name-radio">
						<div class="input-affordance">
							<input type="radio" class="radio-public" name="name-visibility" id="name-public" value="public" checked="">
							<label for="name-public" class="radio-public-label">Public</label> <br>
							<input type="radio" class="radio-private" name="name-visibility" id="name-private" value="private">
							<label for="name-private" class="radio-private-label">Private</label>
						</div>
					</div>
				</div>
			</div>
			<div class="profile-container">
				<div class="row">
					<div id="addData">
						<g:form url="/home/doupdateuserpreferences" name="updateUserPreferences" id="updateUserPreferences" class="form-horizontal" autocomplete="off">
							<div class="col-xs-4" style="margin-left:80px;">
								<g:hiddenField name="precontroller" value="${precontroller}" />
								<g:hiddenField name="preaction" value="${preaction}" />
								<g:hiddenField name="userId" value="${user.id}" />
								<div class="form-group">
									<label class="control-label" for="Username">Username</label>
									<div class="input-group">
										<g:textField type="text" class="form-control" name="username" value="${user.username}" autofocus="" required="" autocomplete="off"/>
									</div>
								</div>

								<div class="form-group">
									<label class="control-label" for="Email">Email</label><br>
									<div class="input-group">
										<g:textField type="text" class="form-control" type="email" required="" name="email" value="${user.email}"/>
									</div>
								</div>

								<div class="form-group">
									<label class="control-label" for="oldPassword">Old password</label>
									<div class="input-group">
										<g:passwordField class="form-control" name="oldPassword" value="${user.password}" autocomplete="off" />
									</div>
								</div>

								<div class="form-group">
									<label class="control-label" for="New Password">New Password</label><br>
									<div class="input-group">
										<g:passwordField type="password" class="form-control" name="password" value="${user.password}" autocomplete="off"/>
									</div>
								</div>

								<div class="form-group">
									<label class="control-label" for="Verify Password">Verify password</label><br>
									<div class="input-group">
										<g:passwordField type="password" class="form-control" name="verify_password" value=""/>
									</div>
								</div>
							</div>
							<div class="col-xs-1"></div>
							<div class="col-xs-4" style="margin-top: -75px">
								<div class="form-group">
									<label class="control-label" for="bio">BIO</label>
									<textarea id="bio" name="bio" class="form-control" value="${user.bio}"></textarea>
									<div class="bio-radio">
										<input type="radio" class="radio-public" name="bio-visibility" id="bio-public" value="public" checked="">
										<label for="bio-public" class="radio-public-label">Public</label>
										<input type="radio" class="radio-private" name="bio-visibility" id="bio-private" value="private">
										<label for="bio-private" class="radio-private-label">Private</label>
									</div>
								</div>

								<!-- 
								<div class="form-group">
									<label class="control-label col-sm-3" for="birthdate">Birthdate (MM/DD/YYYY)</label>
									<div class="col-sm-5">
										<g:textField name="birthdate"
										value="${user.birthdate ? new java.text.SimpleDateFormat("MM/dd/yyyy").format(user.birthdate) : ''}" />
									</div>
								</div>
								-->

								<!--div class="form-group">
									<label class="control-label col-sm-3" for="Notify Email">Email to send reminders to</label>
									<div class="col-sm-5">
										<g:textField name="remindEmail" value="${user.remindEmail ? user.remindEmail.encodeAsHTML() : ""}" />
									</div>
								</div -->

								<div class="form-group">
									<label class="control-label" for="email notifications">Email Notifications</label><br>
									<div class="email-radio">
										<input type="radio" class="radio-public" name="visibility1" id="public1" value="public" checked="">
										<label for="public1" class="radio-public-label">Email me when someone comments</label> <br>
										<input type="radio" class="radio-private" name="visibility1" id="private1" value="private">
										<label for="private1" class="radio-private-label">Don't email me when someone comments</label>
									</div>
								</div>

								<div class="form-group">
									<label class="control-label">External Accounts</label><br>
									<!--
									<g:if test="${!user.twitterAccountName}">
									<g:link action="registertwitter">Link Twitter Account</g:link>
									</g:if>
									<g:else>
										Twitter Account: ${user.twitterAccountName}<br>
										<g:link action="registertwitter">Link Other Twitter Account</g:link>
									</g:else><br>
									 -->
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
									<oauth:checkSubscription userId="${user.id}" typeId="JAWBONE">
										<g:if test="${it?.accessToken }">
											<g:link action="unregisterJawboneUp">Unlink JawboneUp Account</g:link>
										</g:if>
										<g:elseif test="${it && !it.accessToken }">
											<g:link action="registerJawboneUp" class="text-danger">Re-link JawboneUp Account</g:link>
										</g:elseif>
										<g:else>
											<g:link action="registerJawboneUp">Link JawboneUp Account</g:link>
										</g:else>
										<br>
									</oauth:checkSubscription>
									<g:link action="register23andme">
										<oauth:checkSubscription userId="${user.id}" typeId="TWENTY_THREE_AND_ME">
											<g:if test="${it }">
												Re-import from 23andMe Account
											</g:if>
											<g:else>
												Import from 23andme Account
											</g:else>
										</oauth:checkSubscription>
									</g:link>
								</div>

								<!-- 
								<div class="form-group">
									<label class="control-label col-sm-3" for="default twitter to now">Default Twitter to Now</label>
									<div class="col-sm-5">
										<input type="checkbox" name="twitterDefaultToNow" style="border: none; width: 20px;"
										<g:if test="${user.getTwitterDefaultToNow()}"> checked</g:if>> Default Twitter timestamp to now
									</div>
								</div>
								 -->
							</div>
						</g:form>
					</div>
				</div>
			</div><!-- .col-sm-9 ends -->
		</div>
	<div class="interest-list">
		<!-- div id="autocomplete" style="position: absolute; top: 10px; right: 10px;"></div  -->
		<label class="control-label" for="interests">Interest Tags</label>
		<input type="text" class="form-control" id="interestTagInputField" name="data" value="" />
	</div>
</body>
</html>

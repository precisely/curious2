<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="menu" />

<c:jsCSRFToken keys="addEntryCSRF, getPeopleDataCSRF, getListDataCSRF, autocompleteDataCSRF, listTagsAndTagGroupsCSRF,
showTagGroupCSRF, createTagGroupCSRF, deleteTagGroupCSRF, addTagToTagGroupCSRF, deleteGhostEntryDataCSRF, deleteEntryDataCSRF, updateEntrySDataCSRF,
removeTagFromTagGroupCSRF, addTagGroupToTagGroupCSRF, removeTagGroupFromTagGroupCSRF, pingDataCSRF,
excludeFromTagGroupDataCSRF, addBackToTagGroupDataCSRF, getInterestTagsCSRF, addInterestTagCSRF, deleteInterestTagCSRF,
updateAvatarCSRF, deleteUserAccountCSRF" />

<script type="text/javascript" src="/js/curious/profileTag.js?ver=23"></script>
<script src="/js/jquery/jquery.cropit.min.js"></script>
<script>
function refreshPage() {
}
function doLogout() {
	callLogoutCallbacks();
}

function triggerChooseImageWindow() {
	$('#cropit-image-input').click();
}

$(window).load(function() {
	$('#image-cropper').cropit({ 
		imageBackground: true,
		smallImage: 'stretch',
		maxZoom: 3,
	});

	$( "#updateUserPreferences" ).keyup(function(e) {
		if (e.which == 13) {
			editUserDetails();
		}
	});

	var httpArgs = {processData: false, contentType: false, requestMethod:'POST'};
	$('.export').click(function() {
		var imageData = $('#image-cropper').cropit('export');
		if (!imageData) {
			showAlert('Please choose a file to upload.');
			return;
		}
		$('#avatarModal .export').hide()
		$('#avatarModal .wait-form-submit').show()
		var blob = dataURItoBlob(imageData);

		var formData = new FormData();
		formData.append("avatar", blob, "avatar.png");
		formData.append(App.CSRF.SyncTokenKeyName, App.CSRF.updateAvatarCSRF);
		formData.append(App.CSRF.SyncTokenUriName, 'updateAvatarCSRF');

		queueJSONAll('updating avatar', '/user/saveAvatar', 
				formData, function(data) {
			if (data.success) {
				console.log(data);
				$('.img-circle').attr('src', data.avatarURL);
			} else {
				showAlert(data.message);
			}
			$('#avatarModal').modal('hide');
			$('#avatarModal .wait-form-submit').hide()
			$('#avatarModal .export').show()
		}, function(data) {
			showAlert('Internal server error occurred.');
			$('#avatarModal .wait-form-submit').hide()
			$('#avatarModal .export').show()
		}, 0, httpArgs);
	});
});

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

	interestTagList = new InterestTagList("interestTagInputField", "interestTagsList");

	$('#add-profile-tag').click(function() {
		interestTagList.processInput();
	});
});

function deleteUserAccount() {
	showYesNo("The account will be deleted permanently. Do you want to continue?", function() {
		queuePostJSON('Deleting User Account.', '/user/deleteAccount',
				getCSRFPreventionObject('deleteUserAccountCSRF'),
				function (data) {
						if (checkData(data)) {
								if (data.success) {
									doLogout();
									window.location = "/home/logout";
								} else {
									showAlert("We have encountered an error while deleting the account. Please contact support.");
								}
							}
					}
			);
	});
}

function editUserDetails() {
	var newUsername = $("#username").val();
	var newPw = $("#password").val();
	if (origUsername != newUsername && newPw.length == 0) {
		showAlert("If you change the username, you must set the password as well");
		return false;
	}
	if (newPw.length > 0 && newPw != $("#verify_password").val()) {
		showAlert("New password and verification do not match");
		return false;
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
		margin-left: -7px;
	}
	#user-name-label{
		padding-right: 10px;
		text-transform: uppercase;
		font-size: 14px;
		color: #B6B6B6;
		font-weight: 300;
		margin-top: 4px;
		margin-left: 8px;
		padding-bottom: 5px;
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
	<div class="main container-fluid">
		<div class="user-details">
			<g:form url="/home/doupdateuserpreferences" name="updateUserPreferences" id="updateUserPreferences" class="form-horizontal" autocomplete="off">
				<div class="row">
					<div class="pull-right">
						<button class="save-user-details" type="button" onclick="editUserDetails()">Save Profile</button>
					</div>
					<div class="user-image pull-left">
						<img src="${user.avatar?.path ?: '/images/avatar.png'}" alt="" class="img-circle">
						<div class="form-group">
							<button class="edit-image-button" type="button" data-toggle="modal" data-target="#avatarModal">Edit Image</button>
						</div>
					</div>
					<div class="user-name">
						<label class="control-label" for="name" id="user-name-label">Name</label>
						<g:textField class="form-control" name="name" placeholder="Enter name here" value="${user.name}"/>
						<div class="user-name-radio">
							<div class="input-affordance">
								<input type="radio" class="radio-public" name="namePrivacy" id="name-public" value="public"
										${user.settings.isNamePublic() ? 'checked="checked"' : '' }>
								<label for="name-public" class="radio-public-label">Public</label> <br>
								<input type="radio" class="radio-private" name="namePrivacy" id="name-private" value="private"
										${!user.settings.isNamePublic() ? 'checked="checked"' : '' }>
								<label for="name-private" class="radio-private-label">Private</label>
							</div>
						</div>
					</div>
				</div>
				<div class="profile-container">
					<div class="row">
						<div id="addData">
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
										<g:textField class="form-control" type="email" required="" name="email" value="${user.email}"/>
									</div>
								</div>

								<div class="form-group">
									<label class="control-label" for="oldPassword">Old password</label>
									<div class="input-group">
										<g:passwordField class="form-control" name="oldPassword" autocomplete="off" />
									</div>
								</div>

								<div class="form-group">
									<label class="control-label" for="New Password">New Password</label><br>
									<div class="input-group">
										<g:passwordField type="password" class="form-control" name="password" autocomplete="off"/>
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
									<textarea id="bio" name="bio" maxlength="250" class="form-control" value="${user.bio}">${user.bio}</textarea>
									<div class="bio-radio">
										<input type="radio" class="radio-public" name="bioPrivacy" id="bio-public" value="public"
												${user.settings.isBioPublic() ? 'checked="checked"' : '' }>
										<label for="bio-public" class="radio-public-label">Public</label>
										<input type="radio" class="radio-private" name="bioPrivacy" id="bio-private" value="private"
												${!user.settings.isBioPublic() ? 'checked="checked"' : '' }>
										<label for="bio-private" class="radio-private-label">Private</label>
									</div>
								</div>

								<div class="form-group">
									<label class="control-label" for="notifyOnComments">Email Notifications</label><br>
									<div class="email-radio">
										<input type="radio" class="radio-public" name="notifyOnComments" id="public1"
											value="on" ${user.notifyOnComments ? "checked" : ""}>
										<label for="public1" class="radio-public-label">Email me when someone comments</label> <br>
										<input type="radio" class="radio-private" name="notifyOnComments" id="private1" value="off"
											value="on" ${user.notifyOnComments ? "" : "checked"}>
										<label for="private1" class="radio-private-label">Don't email me when someone comments</label>
									</div>
									<div>
									</div>
								</div>

								<div class="form-group">
									<label class="control-label">External Accounts</label><br>
									<oauth:checkSubscription userId="${user.id}" typeId="OURA">
										<g:if test="${it && !it.accessToken}">
											<g:link action="registerOura">Re-link Oura Account</g:link><br>
										</g:if>
										<g:elseif test="${!it}">
											<g:link action="registerOura">Link Oura Account</g:link><br>
										</g:elseif>
										<g:elseif test="${it}">
											<g:link action="unregisterOura">Unlink Oura Account</g:link><br>
										</g:elseif>
									</oauth:checkSubscription>
									<oauth:checkSubscription userId="${user.id}" typeId="WITHINGS">
										<g:if test="${it && !it.accessToken}">
											<g:link action="registerwithings">Re-link Withings Account</g:link><br>
										</g:if>
										<g:elseif test="${it}">
											<g:link action="unregisterwithings">Unlink Withings Account</g:link><br>
										</g:elseif>
										<g:else>
											<g:link action="registerwithings">Link Withings Account</g:link><br>
										</g:else>
									</oauth:checkSubscription>
									<oauth:checkSubscription userId="${user.id}" typeId="MOVES">
										<g:if test="${it && !it.accessToken}">
											<g:link action="registermoves">Re-link Moves Account</g:link><br>
										</g:if>
										<g:elseif test="${it}">
											<g:link action="unregistermoves">Unlink Moves Account</g:link><br>
										</g:elseif>
										<g:else>
											<g:link action="registermoves">Link Moves Account</g:link><br>
										</g:else>
									</oauth:checkSubscription>
									<oauth:checkSubscription userId="${user.id}" typeId="JAWBONE">
										<g:if test="${it && !it.accessToken}">
											<g:link action="registerJawboneUp">Re-link JawboneUp Account</g:link><br>
										</g:if>
										<g:elseif test="${it}">
											<g:link action="unregisterJawboneUp">Unlink JawboneUp Account</g:link>
										</g:elseif>
										<g:else>
											<g:link action="registerJawboneUp">Link JawboneUp Account</g:link>
										</g:else>
										<br>
									</oauth:checkSubscription>
								</div>
								<div class="form-group">
									<g:link action="dosendverify" class="basic-text">Resend verification email</g:link>
								</div>
								<div class="form-group">
									<a href="#" style="color: #ff0000" onclick="deleteUserAccount()"
											class="basic-text">
										Delete Account
									</a>
								</div>
							</div>
						</div>
					</div>
				</div>
			</g:form>
		</div>
		<div class="interest-list">
			<div>
				<label class="control-label interest-list-label" for="interestTagInputField">Interest Tags</label>
				<input type="text" class="form-control" id="interestTagInputField" name="data" value="" />

				<div class="profile-tag-radio-group">
					<div>
						<input type="radio" class="radio-public profile-tag-radio" name="namePrivacy" value="PUBLIC"
								id="profile-tag-public-radio" checked>
						<label for="profile-tag-public-radio" class="radio-public-label">Public</label>
					</div>
					<div>
						<input type="radio" class="radio-private profile-tag-radio" name="namePrivacy" value="PRIVATE"
								id="profile-tag-private-radio">
						<label for="profile-tag-private-radio" class="radio-private-label">Private</label>
					</div>
					<div>
						<button class="add-profile-tag" id="add-profile-tag" type="button">Add</button>
					</div>
				</div>
			</div>

			<div class="profile-tag-listing" id="interestTagsList">
				<div>
					<span class="profile-tag-list-heading">Public Tags</span>
					<div id="public-list"></div>
				</div>
				<div>
					<span class="profile-tag-list-heading">Private Tags</span>
					<div id="private-list"></div>
				</div>
			</div>
		</div>
		<div class="modal fade" id="avatarModal" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						Add/Edit Avatar
					</div>
					<div class="modal-body">
						<div class="image-editor">
							<g:uploadForm name="saveAvatar" id="saveAvatar" class="form-horizontal" autocomplete="off">
								<g:hiddenField name="userId" value="${user.id}" />
								<button type="button" class="btn btn-primary choose-image-button" onclick="triggerChooseImageWindow()">Choose Image</button>
								<div id="image-cropper">
									<input type="file" class="cropit-image-input hidden" id="cropit-image-input">
									<div class="cropit-image-preview-container">
										<div class="cropit-image-preview"></div>
									</div>
									<div class="image-size-label text-center">
										Resize image
									</div>
									<input type="range" class="cropit-image-zoom-input">
								</div>
							</g:uploadForm>
						</div>
					</div>
					<div class="modal-footer">
						<div class="wait-form-submit waiting-icon" hidden="true"></div>
						<button class="export btn btn-danger" type="button">Upload</button>
					</div>
				</div>
			</div>
		</div>
	</div>
</body>
</html>

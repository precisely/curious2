<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="menu" />

<c:jsCSRFToken keys="addEntryCSRF, getPeopleDataCSRF, getListDataCSRF, autocompleteDataCSRF, listTagsAndTagGroupsCSRF,
showTagGroupCSRF, createTagGroupCSRF, deleteTagGroupCSRF, addTagToTagGroupCSRF, deleteGhostEntryDataCSRF, deleteEntryDataCSRF, updateEntrySDataCSRF,
removeTagFromTagGroupCSRF, addTagGroupToTagGroupCSRF, removeTagGroupFromTagGroupCSRF, pingDataCSRF,
excludeFromTagGroupDataCSRF, addBackToTagGroupDataCSRF, getInterestTagsDataCSRF, addInterestTagDataCSRF, deleteInterestTagDataCSRF, updateInterestTagDataCSRF, updateAvatarCSRF" />

<script type="text/javascript" src="/js/curious/interestTagList.js?ver=21"></script>
<script src="/js/jquery/jquery.cropit.js"></script>
<script>
function refreshPage() {
}
function doLogout() {
	callLogoutCallbacks();
}

$(window).load(function() {
	function readURL(input) {
		var httpArgs ={processData: false, contentType: false, requestMethod:'POST'};

		if (input.files && input.files[0]) {
			var reader = new FileReader();
			reader.onload = function (e) {
				$('.image-editor').cropit({
					imageState: {
					src: e.target.result
					},
				});

				$('.export').click(function() {
					var imageData = $('.image-editor').cropit('export');
					var blob = dataURItoBlob(imageData);
					console.log('blob' + blob);
					var formData = new FormData();
					formData.append("avatar", blob, "avatar.png");
					formData.append(App.CSRF.SyncTokenKeyName, App.CSRF.updateAvatarCSRF);
					formData.append(App.CSRF.SyncTokenUriName, 'updateAvatarCSRF');

					queueJSONAll('updating avatar', '/home/saveAvatar', 
							formData, function(data) {
						if (data.success) {
							console.log(data);
							$('.img-circle').attr('src', data.avatarURL);
						} else {
							showAlert('Error updating avatar. Please try again.');
						}
							$('#avatarModal').modal('hide');
					}, function(data) {
						showAlert('Internal server error occurred.');
					}, 0, httpArgs);
				});
			}
			reader.readAsDataURL(input.files[0]);
		}
	}
	$("#cropit-image-input").change(function() {
		readURL(this);
	});
});

function dataURItoBlob(dataURI) {
	// convert base64/URLEncoded data component to raw binary data held in a string
	var byteString;
	if (dataURI.split(',')[0].indexOf('base64') >= 0) {
		byteString = atob(dataURI.split(',')[1]);
	} else {
		byteString = unescape(dataURI.split(',')[1]);
	}
	// separate out the mime component
	var mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0];

	// write the bytes of the string to a typed array
	var ia = new Uint8Array(byteString.length);
	for (var i = 0; i < byteString.length; i++) {
		ia[i] = byteString.charCodeAt(i);
	}
	return new Blob([ia], {type:mimeString});
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
						<button class="save-user-details" onclick="editUserDetails()">Save Profile</button>
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
										<g:textField type="text" class="form-control" type="email" required="" name="email" value="${user.email}"/>
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
									<textarea id="bio" name="bio" class="form-control" value="${user.bio}"></textarea>
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
							</div>
						</div>
					</div>
				</div>
			</g:form>
		</div>
		<div class="interest-list">
			<label class="control-label" for="interests">Interest Tags</label>
			<input type="text" class="form-control" id="interestTagInputField" name="data" value="" />
		</div>
		<div class="modal fade" id="avatarModal" role="dialog">
			<div class="modal-dialog modal-sm">
				<div class="modal-content">
					<div class="modal-header">
						Add/Edit Avatar
					</div>
					<div class="modal-body text-center">
						<div class="image-editor">
							<g:uploadForm name="saveAvatar" id="saveAvatar" class="form-horizontal" autocomplete="off">
								<g:hiddenField name="userId" value="${user.id}" />
								<input type="file" class="cropit-image-input" id="cropit-image-input">
								<div class="cropit-image-preview"></div>
								<div class="image-size-label">
									Resize image
								</div>
								<input type="range" class="cropit-image-zoom-input">
							</g:uploadForm>
						</div>
					</div>
					<div class="modal-footer">
						<button class="export btn btn-danger" type="button">Upload</button>
					</div>
				</div>
			</div>
		</div>
	</div>
</body>
</html>

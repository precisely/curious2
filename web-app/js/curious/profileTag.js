function ProfileTagList(inputFieldId, listId) {
	this.editId = inputFieldId;
	this.listId = listId;

	this.inputField = $('#' + inputFieldId);

	this.publicListDiv = $('#' + listId + ' #public-list');
	this.privateListDiv = $('#' + listId + ' #private-list');

	this.inputField.val('Enter interest tag. Ex. exercise, sleep, dry skin...');

	this.inputField.on("click", function() {
		if (!this.inputField.data('entryTextSet')) {
			this.setText('');
		}
	}.bind(this));

	// On Enter key press.
	this.inputField.keyup(function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
			this.processInput();
		}
	}.bind(this));

	this.inputField.bind("keypress", function(e) {
		if (e.keyCode == 13) {
			e.preventDefault();
		}
	});

	this.refresh();
}

ProfileTagList.prototype = Object.create({});
ProfileTagList.prototype.constructor = ProfileTagList;

ProfileTagList.prototype.clearPublic = function() {
	this.publicListDiv.html('');
};

ProfileTagList.prototype.clearPrivate = function() {
	this.privateListDiv.html('');
};

ProfileTagList.prototype.addTag = function(profileTag) {
	var description = profileTag.tag.description;
	var profileTagId = profileTag.id;

	var listItemContent =
		'<li id="interestTagList' + this.listId + profileTagId +'" class="interest-tags-container">' +
			'<span id="profileTagText' + this.listId + profileTagId + '">' +
				escapehtml(description) +
			'</span>' +
			'<a href="#" class="profile-tag-delete" id="profileTagDelete' + this.listId + profileTagId + '">' +
				'<i class="fa fa-trash" />' +
			'</a>' +
		'</li>';

	var listDiv = profileTag.status === 'PRIVATE' ? this.privateListDiv : this.publicListDiv;
	listDiv.append(listItemContent);

	$('#profileTagDelete' + this.listId + profileTagId).on("click", function(e) {
		e.preventDefault();

		this.deleteTag(profileTagId, function(data) {
			if (data.success) {
				$('#interestTagList' + this.listId + profileTagId).remove();
			}
		}.bind(this));
	}.bind(this));
};

ProfileTagList.prototype.addTags = function(tags) {
	jQuery.each(tags, function(index, profileTag) {
		this.addTag(profileTag);
	}.bind(this));
};

ProfileTagList.prototype.refreshPublicTags = function(publicTags) {
	this.clearPublic();
	this.addTags(publicTags);
};

ProfileTagList.prototype.refreshPrivateTags = function(privateTags) {
	this.clearPrivate();
	this.addTags(privateTags);
};

ProfileTagList.prototype.setText = function(text) {
	this.inputField.val(text);
};

ProfileTagList.prototype.processInput = function() {
	var $field = this.inputField;

	var text = $field.val();

	// no tag data
	if (text == "") {
		return;
	}

	$field.val("");

	var tagStatus = $('.profile-tag-radio-group').find(':radio:checked').val();

	this.addInterestTag(text, tagStatus);
};

function InterestTagList(inputFieldId, listId) {
	ProfileTagList.call(this, inputFieldId, listId);
	this.privateInterestTags = [];
	this.publicInterestTags = [];
}

InterestTagList.prototype = Object.create(ProfileTagList.prototype);
InterestTagList.prototype.constructor = InterestTagList;

InterestTagList.prototype.refresh = function() {
	queueJSON("getting interest tags", "/profileTag/getInterestTags?userId=" + currentUserId + "&callback=?",
			getCSRFPreventionObject("getInterestTagsCSRF"), function(profileTags) {
				if (checkData(profileTags)) {
					this.setText('');
	
					this.publicInterestTags = profileTags.publicInterestTags;
					this.privateInterestTags = profileTags.privateInterestTags;
	
					this.refreshPublicTags(this.publicInterestTags);
					this.refreshPrivateTags(this.privateInterestTags);
				}
			}.bind(this)
	);
};

InterestTagList.prototype.addInterestTag = function(tagName, tagStatus) {
	queueJSON("adding new interest tag", "/profileTag/addInterestTag?tagNames=" + tagName + "&" + "tagStatus=" +
			tagStatus + "&" + getCSRFPreventionURI("addInterestTagCSRF") + "&callback=?",
			function(data) {
				if (data.success) {
					if (data.profileTag.status === 'PRIVATE') {
						this.privateInterestTags.push(data.profileTag);
					} else {
						this.publicInterestTags.push(data.profileTag);
					}

					this.addTag(data.profileTag[0]);
				}
			}.bind(this)
	);
};

InterestTagList.prototype.deleteTag = function(tagId, callback) {
	queueJSON("deleting interest tag", "/profileTag/deleteInterestTag?id=" + tagId + "&callback=?",
			getCSRFPreventionObject("deleteInterestTagCSRF"), callback);
};
function handleQuestionFormData(questionId, surveyId, rowNumber, answerCount, answerType) {

	if (questionId == null || answerType === 0) {
		$('#answerListContainer').attr("style", "display:none");
		$('#answerContainer').attr("style", "display:none");
	}

	if (!$('#answerInputAffordance  > tr')[0]) {
		$('#answerListContainer').attr("style", "display:none");
	}

	$('#addAnswerRow').click(function() {
		$('#answerListContainer').attr("style", "display:block");
		var profileTagRowId = "profileTags" + rowNumber;
		var trackingTagRowId = "trackingTags" + rowNumber;
		var innerHTMLContent =
			'<tr id=answerRow' + rowNumber + '>' +

			'<td id=answerId' + rowNumber + ' class="hidden">' +
			'</td>' +

			'<td><textarea id=answerText' + rowNumber + ' placeholder="Add answer text..." ' +
			'maxlength="1000" class="answer-input" required></textarea></td>' +

			'<td width="10px"><input id=priorityNumber' + rowNumber + ' class="answer-input" type="number" ' +
			'min="0" required/></td>' +

			'<td id=profileTagsData' + rowNumber + ' class="hidden">' +
			'</td>' +

			'<td width="300px"><input type="text" placeholder="Add profile tags here..." name="associatedProfileTags"' +
			' class="answer-input associated-profile-tags" id=' + profileTagRowId + '>' +
			'</td>' +

			'<td id=trackingTagsData' + rowNumber + ' class="hidden">' +
			'</td>' +

			'<td width="300px"><input type="text" placeholder="Add tracking tags here..." name="associatedTrackingTags"' +
			' class="answer-input associated-tracking-tags" id=' + trackingTagRowId + '>' +
			'</td>' +

			'<td style="width: 10px;">' +
			'<a href=# class="margin-left">' +
			'<i class="fa fa-trash action-icon"' +
			'onclick="deleteAnswer(' + rowNumber + ')"></i>' +
			'</a>' +
			'</td>' +

			'</tr>';

		$('#answerInputAffordance').append(innerHTMLContent);
		rowNumber += 1;

		addTheme(profileTagRowId);
		addTheme(trackingTagRowId);
	});

	for (var answerRow = 0; answerRow < answerCount; answerRow ++) {
		prePopulateTags('profileTags', answerRow);
		prePopulateTags('trackingTags', answerRow);
	}

	$('#addOrUpdateQuestion').click(function(e) {
		var possibleAnswersList = [];
		var question = $('#question').val();
		var priority = $('#priority').val();
		var status = $('#status').val();
		var answerType = $('#answerType').val();
		var isRequired = $('#isRequired').prop('checked');

		$('#answerInputAffordance  > tr').each(function() {
			var answerRow = $(this).find('td');
			var profileTags = $(answerRow[4]).text().split('×');
			var trackingTags = $(answerRow[6]).text().split('×');

			var possibleAnswer = {
				answer: $(answerRow[1]).find('textarea').val(),
				priority: $(answerRow[2]).find('input').val(),
				profileTags: profileTags,
				trackingTags: trackingTags,
			};

			var answerId = $(answerRow[0]).text();
			if (answerId) {
				possibleAnswer.answerId = answerId;
			}

			possibleAnswersList.push(possibleAnswer);
		});

		var params = {question: question, priority: priority, status: status,
			answerType: answerType, isRequired: isRequired,
			possibleAnswers: JSON.stringify(possibleAnswersList)};
		var redirectLocation = "/survey/surveyDetails/" + surveyId;

		if (questionId != null) {
			if (answerType == 'DESCRIPTIVE' && $('#answerInputAffordance  > tr')[0]) {
				showAlert("To change the answer type to Descriptive, please remove all the answers first.");
				$('#answerListContainer').attr("style", "display:block");
				$('#answerContainer').attr("style", "display:block");
				e.preventDefault();

				return;
			}

			params.id = questionId;
				queuePostJSON('Updating question', '/survey/updateQuestion',
					getCSRFPreventionObject('updateQuestionCSRF', params), function(data) {
						if (checkData(data) && data.success) {
							window.location = redirectLocation;
						}
						showBootstrapAlert($('.alert'), data.message);
					}, function(xhr) {
						window.location = redirectLocation;
						showBootstrapAlert($('.alert'), data.message);
					}
				);
		} else {
			params.id = surveyId;
			queuePostJSON('Adding question to survey', '/survey/saveQuestion',
				getCSRFPreventionObject('addQuestionCSRF', params), function(data) {
					if (checkData(data) && data.success) {
						window.location = redirectLocation;
					}
					showBootstrapAlert($('.alert'), data.message);
				}, function(xhr) {
					window.location = redirectLocation;
					showBootstrapAlert($('.alert'), data.message);
				});
		}
	});

	$("#answerType").change(function() {
		var answerType = $('#answerType').val();
		if (answerType === "DESCRIPTIVE") {
			$('#answerListContainer').attr("style", "display:none");
			$('#answerContainer').attr("style", "display:none");
		} else {
			if ($('#answerInputAffordance  > tr')[0]) {
				$('#answerListContainer').attr("style", "display:block");
			}
			$('#answerContainer').attr("style", "display:block");
		}
	});
};

function addTheme(id) {
	$("#" + id).tokenInput("/data/getTagsForAutoComplete", {theme: "facebook", preventDuplicates: true});
}

// Function to prepopulate Tags data in answers row.
function prePopulateTags(tagType, rowIndex) {
	var index = rowIndex;
	var tagsList = [];
	var tagDataSelector = tagType === 'profileTags' ? $("#profileTagsData" + index) : $("#trackingTagsData" + index);
	var tagDescriptionsList = tagDataSelector.text() ? tagDataSelector.text().split(",") : [];

	tagDescriptionsList.forEach(function (tagDescription) {
		if (tagDescription.trim()) {
			var tagObject = new Object();
			tagObject.name = tagDescription.trim();

			tagsList.push(tagObject)
		}
	});

	var associatedTagsSelector = tagType === 'profileTags' ? $("#profileTags" + index) : $("#trackingTags" + index);

	associatedTagsSelector.tokenInput("/data/getTagsForAutoComplete", {
		theme: "facebook",
		preventDuplicates: true,
		prePopulate: tagsList
	});
}

function deleteAnswer(rowId, answerId, questionId) {
	if (answerId) {
		showYesNo("The answer will be deleted permanently. Do you want to continue?", function() {
			var params = {'possibleAnswerInstance.id': answerId};
			params.id = questionId;
				queuePostJSON('Deleting answer from survey question', '/survey/deleteAnswer',
					getCSRFPreventionObject('deleteAnswerCSRF', params),
					function (data) {
						if (checkData(data)) {
							if (data.success) {
								$('#answerRow' + rowId).remove();
							}
							showBootstrapAlert($('.alert'), data.message);
						}
					}
				);
		});
	} else {
		$('#answerRow' + rowId).remove();
	}

	if (!$('#answerInputAffordance  > tr')[0]) {
		$('#answerListContainer').attr("style", "display:none");
	}
}
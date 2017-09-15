<%@ page import="us.wearecurio.model.survey.QuestionStatus" %>
<%@ page import="us.wearecurio.model.survey.AnswerType" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>Survey Question</title>

		<c:jsCSRFToken keys="addQuestionCSRF, updateQuestionCSRF, deleteAnswerCSRF, autocompleteDataCSRF"/>
		<script type="text/javascript">

			var rowIdInEditMode;
			var questionId = ${questionInstance.id ?: null}

			$(document).ready(function() {
				var rowNumber = ${(questionInstance.answers.size() ?: -1) + 1};

				createAutocompleteForTags('associatedProfileTags', 'profileTagsAutocomplete');
				createAutocompleteForTags('associatedTrackingTags', 'trackingTagsAutocomplete');

				$(document).on("click", ".deleteProfileTags", function() {
					var parentLi = $(this).parents('li');
					var answerId = $('#answerId').val();
					var tagDescription = $(this).parents('li')[0].textContent;
					if (answerId) {
						queueJSON('Removing tracking tag', '/survey/removeAssociatedTagFromPossibleAnswer',
								{answerId: answerId, tagDescription: tagDescription.trim(), tagType: 'profileTag'},
								function (data) {
									if (!checkData(data)) {
										return;
									}

									if (data.success) {
										parentLi.remove();
										showAlert('Tag removed successfully from answer.');
									} else {
										showAlert('Could not remove Tag from answer.')
									}
								}, function (xhr) {
									console.log('error: ', xhr);
								});
					} else {
						parentLi.remove();
					}
					return false;
				});

				// Deleting Tag from Possible Answer
				$(document).on("click", ".deleteTrackingTags", function() {
					var parentLi = $(this).parents('li');
					var answerId = $('#answerId').val();
					var tagDescription = $(this).parents('li')[0].textContent;
					if (answerId) {
						queueJSON('Removing tracking tag', '/survey/removeAssociatedTagFromPossibleAnswer',
								{answerId: answerId, tagDescription: tagDescription.trim(), tagType: 'trackingTag'},
								function (data) {
									if (!checkData(data)) {
										return;
									}

									if (data.success) {
										parentLi.remove();
										showAlert('Tag removed successfully from answer.');
									} else {
										showAlert('Could not remove Tag from answer.')
									}
								}, function (xhr) {
									console.log('error: ', xhr);
								});
					} else {
						parentLi.remove();
					}
					return false;
				});

				$("#addAnswerOverlay").on("hidden.bs.modal",function() {
					$(this).find('form').trigger('reset');
				});

				$('#addSurveyAnswerForm').submit(function() {
					// Defined in base.js file.
					var params = $(this).serializeObject();
					var profileTags = [];
					var trackingTags = [];

					$('#associatedProfileTags-list li').each(function () {
						profileTags.push($(this).context.firstChild.data);
					});
					$('#associatedTrackingTags-list li').each(function () {
						trackingTags.push($(this).context.firstChild.data);
					});

					if (rowIdInEditMode != null) {
						var rowInEditMode = $('#answerRow' + rowIdInEditMode);
						rowInEditMode.find('#answerText' + rowIdInEditMode).text(params.answer);
						rowInEditMode.find('#priorityNumber' + rowIdInEditMode).text(params.priority);
						rowInEditMode.find('#profileTags' + rowIdInEditMode).text(profileTags);
						rowInEditMode.find('#trackingTags' + rowIdInEditMode).text(trackingTags);

						rowIdInEditMode = null;
					} else {
						var innerHTMLContent =
								'<tr id=answerRow' + rowNumber + '>' +

								'<td id=answerId' + rowNumber + ' class="hidden">' +
								(params.answerId ? params.answerId : '') +
								'</td>' +

								'<td id=answerText' + rowNumber + '>' +
								params.answer +
								'</td>' +

								'<td id=priorityNumber' + rowNumber + '>' +
								params.priority +
								'</td>' +

								'<td id=profileTags' + rowNumber + '>' +
								profileTags +
								'</td>' +

								'<td id=trackingTags' + rowNumber + '>' +
								trackingTags +
								'</td>' +

								'<td>' +
								'<a href=#>' +
								'<i class="fa fa-pencil action-icon"' +
								'onclick="editAnswer(' + rowNumber + ')"></i>' +
								'</a>' +
								'<a href=# class="margin-left">' +
								'<i class="fa fa-trash action-icon"' +
								'onclick="deleteAnswer(' + rowNumber + ')"></i>' +
								'</a>' +
								'</td>' +

								'</tr>';

						$('#answerInputAffordance').append(innerHTMLContent);
						rowNumber += 1;
					}

					$('#addAnswerOverlay').modal('toggle');

					return false;
				});

				$('#addOrUpdateQuestion').click(function() {
					var possibleAnswersList = [];
					var id = $('#id').val();
					var question = $('#question').val();
					var priority = $('#priority').val();
					var status = $('#status').val();
					var answerType = $('#answerType').val();
					var isRequired = $('#isRequired').val();

					$('#answerInputAffordance  > tr').each(function() {
						var answerRow = $(this).find('td');

						var possibleAnswer = {
							answer: $(answerRow[1]).text(),
							priority: $(answerRow[2]).text(),
							profileTags: $(answerRow[3]).text() ? $(answerRow[3]).text().split(',') : [],
							trackingTags: $(answerRow[4]).text() ? $(answerRow[4]).text().split(',') : [],
						};

						var answerId = $(answerRow[0]).text();
						if (answerId) {
							possibleAnswer.answerId = answerId;
						}

						possibleAnswersList.push(possibleAnswer);
					});

					var params = {id: id, question: question, priority: priority, status: status,
						answerType: answerType, isRequired: isRequired,
						possibleAnswers: JSON.stringify(possibleAnswersList)};

					if (questionId) {
						queuePostJSON('Updating question', '/survey/updateQuestion',
								getCSRFPreventionObject('updateQuestionCSRF', params),
								function(data) {
									if (checkData(data)) {
										showBootstrapAlert($('.alert'), data.message);
									}
								}, function(xhr) {
									console.log('error: ', xhr);
								}
						);
					} else {
						queuePostJSON('Adding question to survey', '/survey/saveQuestion',
								getCSRFPreventionObject('addQuestionCSRF', params),
								function(data) {
									if (checkData(data)) {
										showBootstrapAlert($('.alert'), data.message);
									}
								}, function(xhr) {
									console.log('error: ', xhr);
								}
						);
					}
				});
			});

			function deleteAnswer(rowId, answerId) {
				if (answerId && questionId) {
					var params = {'possibleAnswerInstance.id': answerId, 'questionInstance.id': questionId};
					queuePostJSON('Deleting answer from survey question', '/survey/deleteAnswer',
							getCSRFPreventionObject('deleteAnswerCSRF', params),
							function(data) {
								if (checkData(data)) {
									if (data.success) {
										$('#answerRow' + rowId).remove();
									}
									showBootstrapAlert($('.alert'), data.message);
								}
							}, function(xhr) {
								console.log('error: ', xhr);
							}
					);
				} else {
					$('#answerRow' + rowId).remove();
				}
			}

			function editAnswer(rowId) {
				rowIdInEditMode = rowId;

				var rowInEditMode = '#answerRow' + rowId;
				var answerModal = $('#addAnswerOverlay');
				var answerId = answerModal.find('#answerId').val($(rowInEditMode).find('#answerId' + rowId).text());
				if (answerId[0].value) {
					var profileTagsList = $("#profileTags" + rowId).html().split(',');
					for (var i = 0; i < profileTagsList.length; i++) {
						var tagDescription = profileTagsList[i];
						if (tagDescription.trim() != '') {
							addProfileOrTrackingTagsToList($("#associatedProfileTags-list"), 'deleteProfileTags', tagDescription.trim());
						}
					}
					var trackingTagsList = $("#trackingTags" + rowId).html().split(',');
					for (var i = 0; i < trackingTagsList.length; i++) {
						var tagDescription = trackingTagsList[i];
						if (tagDescription.trim() != '') {
							addProfileOrTrackingTagsToList($("#associatedTrackingTags-list"), 'deleteTrackingTags', tagDescription.trim());
						}
					}
				}
				answerModal.find('#answer').val($(rowInEditMode).find('#answerText' + rowId).text());
				answerModal.find('#priority').val($(rowInEditMode).find('#priorityNumber' + rowId).text());
				answerModal.find('.add-answer').text('Save');
				answerModal.find('h4').text('Edit PossibleAnswer');
				answerModal.modal('toggle');
			}
		</script>
	</head>
	<body>
		<div class="red-header">
			<div>
				<h1>
					<g:if test="${questionInstance.id}">
						Update Question
					</g:if>
					<g:else>
						Create Question
					</g:else>
				</h1>
			</div>
		</div>

		<div class="main container-fluid survey-factory">
			<g:form>
				<input hidden name="id" id="id" value="${questionInstance.id ?: surveyId}" />
				<div>
					<label for="question">
						Question
					</label>
					<textarea placeholder="Add question text..." maxlength="1000" name="question"
							id="question" required>${questionInstance.question}</textarea>
				</div>
				<div>
					<label for="priority">
						Priority
					</label>
					<input class="survey-input" type="number" min="0" value="${questionInstance.priority}"
							name="priority" placeholder="Priority" id="priority" required/>
				</div>
				<div>
					<label for="status">
						Status
					</label>
					<g:select class="survey-input" name="status" id="status" from="${QuestionStatus.values()}"
							optionValue="displayText" value="${questionInstance.status ?: QuestionStatus.values()[1]}"/>
				</div>
				<div>
					<label for="answerType">
						Answer Type
					</label>
					<g:select class="survey-input" name="answerType" id="answerType" from="${AnswerType.values()}"
							optionValue="displayText" value="${questionInstance.answerType ?: AnswerType.values()[0]}"/>
				</div>
				<div>
					<label for="isRequired">
						Required
					</label>
					<g:checkBox name="isRequired" id="isRequired" value="${questionInstance.isRequired}" />
				</div>

				<g:render template="answerList" model="[isViewOnly: false]" />

				<div class="margin-top">
					<button type="button" class="btn btn-default">
						<g:link controller="survey" action="surveyDetails" id="${surveyId}"
								class="cancel-button-link">
							Cancel
						</g:link>
					</button>
					<button type="submit" class="btn btn-default margin-left" id="addOrUpdateQuestion">
						${questionInstance.id ? 'Update' : 'Add'}
					</button>
				</div>

			</g:form>
		</div>

		<g:render template="addAnswersModal" />

	</body>
</html>
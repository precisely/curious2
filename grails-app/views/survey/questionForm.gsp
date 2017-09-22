<%@ page import="us.wearecurio.model.survey.QuestionStatus" %>
<%@ page import="us.wearecurio.model.survey.AnswerType" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>Survey Question</title>

		<c:jsCSRFToken keys="addQuestionCSRF, updateQuestionCSRF, deleteAnswerCSRF, autocompleteDataCSRF"/>
		<script type="text/javascript" src="/js/jquery/jquery.tokeninput.js"></script>
		<link rel="stylesheet" type="text/css" href="/css/token-input/token-input.css" />
		<link rel="stylesheet" type="text/css" href="/css/token-input/token-input-facebook.css" />
		<script type="text/javascript">

			var rowIdInEditMode;

			$(document).ready(function() {
				var rowNumber = ${(questionInstance.answers.size() ?: -1) + 1};

				if (${!questionInstance.id || questionInstance.answerType == AnswerType.DESCRIPTIVE}) {
					$('#answerListContainer').attr("style", "display:none");
					$('#answerContainer').attr("style", "display:none");
				}

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

				$('#addAnswerRow').click(function() {
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

							'<td width="300px"><input type="text" placeholder="Add profile tags here..." name="associatedProfileTags"' +
							' class="answer-input associated-profile-tags" id=' + profileTagRowId + '>' +
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

					$("#" + profileTagRowId).tokenInput("/data/getTagsForAutoComplete", {theme: "facebook", preventDuplicates: true});
					$("#" + trackingTagRowId).tokenInput("/data/getTagsForAutoComplete", {theme: "facebook", preventDuplicates: true});
				});

				$('#addOrUpdateQuestion').click(function() {
					var possibleAnswersList = [];
					var question = $('#question').val();
					var priority = $('#priority').val();
					var status = $('#status').val();
					var answerType = $('#answerType').val();
					var isRequired = $('#isRequired').prop('checked');

					$('#answerInputAffordance  > tr').each(function() {
						var answerRow = $(this).find('td');
						var profileTags = $(answerRow[3]).text().indexOf('×') >= 0 ? $(answerRow[3]).text().split('×') : $(answerRow[3]).text().split(',');
						var trackingTags = $(answerRow[4]).text().indexOf('×') >= 0 ? $(answerRow[4]).text().split('×') : $(answerRow[4]).text().split(',');

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

					if (${questionInstance.id != null}) {
						if (answerType == 'DESCRIPTIVE' && $('#answerInputAffordance  > tr')[0]) {
							alert("Please remove all the answers first.");

							return;
						}

						params.id = ${questionInstance.id}
						queuePostJSON('Updating question', '/survey/updateQuestion',
								getCSRFPreventionObject('updateQuestionCSRF', params));
					} else {
						params.id = ${surveyId};
						queuePostJSON('Adding question to survey', '/survey/saveQuestion',
								getCSRFPreventionObject('addQuestionCSRF', params));
					}
				});

				$("#answerType").change(function() {
					var answerType = $('#answerType').val();
					if (answerType == "DESCRIPTIVE" && !$('#answerInputAffordance  > tr')[0]) {
						$('#answerListContainer').attr("style", "display:none");
						$('#answerContainer').attr("style", "display:none");
					} else {
						$('#answerListContainer').attr("style", "display:block");
						$('#answerContainer').attr("style", "display:block");
					}
				});
			});

			function deleteAnswer(rowId, answerId) {
				if (answerId) {
					var params = {'possibleAnswerInstance.id': answerId};
					params.id = ${questionInstance.id}
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
				<input hidden name="id" id="id" value="${questionInstance.id}" />
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
				<div id="answerContainer">
					<label>
						Answers
					</label>
					<g>
						<button type="button" class="btn btn-default add-survey-button" id="addAnswerRow">
							<i class="fa fa-plus-circle survey-add-icon"></i> Add Answer
						</button>
					</g>

					<g:render template="answerList" model="[isViewOnly: false]" />

				</div>
				<div>
					<label for="isRequired">
						Required
					</label>
					<g:checkBox name="isRequired" id="isRequired" value="${questionInstance.isRequired}" />
				</div>

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
	</body>
</html>
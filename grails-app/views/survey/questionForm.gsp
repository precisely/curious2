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

				if (${questionInstance.id}) {
					if (${questionInstance.answerType == AnswerType.DESCRIPTIVE}) {
						$('#answerListContainer').attr("style", "display:none");
						$('#answerContainer').attr("style", "display:none");
					} else if (!$('#answerInputAffordance  > tr')[0]) {
						$('#answerListContainer').attr("style", "display:none");
					}
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

					$("#" + profileTagRowId).tokenInput("/data/getTagsForAutoComplete", {theme: "facebook", preventDuplicates: true});
					$("#" + trackingTagRowId).tokenInput("/data/getTagsForAutoComplete", {theme: "facebook", preventDuplicates: true});
				});

				// To prepopulate tags in saved answers.
				var answerCount = ${questionInstance.answers.size()};

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
						var profileTags = $(answerRow[4]).text().indexOf('×') >= 0 ? $(answerRow[4]).text().split('×') : $(answerRow[4]).text().split(',');
						var trackingTags = $(answerRow[6]).text().indexOf('×') >= 0 ? $(answerRow[6]).text().split('×') : $(answerRow[6]).text().split(',');

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
							alert("To change the answer type to Descriptive, please remove all the answers first.");
							$('#answerListContainer').attr("style", "display:block");
							$('#answerContainer').attr("style", "display:block");
							e.preventDefault();

							return;
						}

						params.id = ${questionInstance.id}
						queuePostJSON('Updating question', '/survey/updateQuestion',
								getCSRFPreventionObject('updateQuestionCSRF', params), function(data) {
									if (checkData(data)) {
										if(data.success) {
											window.location = "/survey/surveyDetails/${surveyId}";
										}
									}
									showBootstrapAlert($('.alert'), data.message);
								}, function(xhr) {
									window.location = "/survey/surveyDetails/${surveyId}";
									showBootstrapAlert($('.alert'), data.message);
								}
						);
					} else {
						params.id = ${surveyId};
						queuePostJSON('Adding question to survey', '/survey/saveQuestion',
								getCSRFPreventionObject('addQuestionCSRF', params), function(data) {
									if (checkData(data)) {
										if(data.success) {
											window.location = "/survey/surveyDetails/${surveyId}";
										}
									}
									showBootstrapAlert($('.alert'), data.message);
								}, function(xhr) {
									window.location = "/survey/surveyDetails/${surveyId}";
									showBootstrapAlert($('.alert'), data.message);
								});
					}
				});

				$("#answerType").change(function() {
					var answerType = $('#answerType').val();
					if (answerType == "DESCRIPTIVE") {
						$('#answerListContainer').attr("style", "display:none");
						$('#answerContainer').attr("style", "display:none");
					} else {
						if ($('#answerInputAffordance  > tr')[0]) {
							$('#answerListContainer').attr("style", "display:block");
						}
						$('#answerContainer').attr("style", "display:block");
					}
				});
			});

			// Function to prepopulate Tags data in answers row.
			function prePopulateTags(tagType, rowIndex) {
				var index = rowIndex;
				var tagsList = [];
				var tagDataSelector = tagType == 'profileTags' ? $("#profileTagsData" + index) : $("#trackingTagsData" + index);
				var tagDescriptionsList = tagDataSelector.text() ? tagDataSelector.text().split(",") : [];

				for (var i = 0; i < tagDescriptionsList.length; i++) {
					if (tagDescriptionsList[i].trim()) {
						var tagObject = new Object();
						tagObject.name = tagDescriptionsList[i].trim();

						tagsList.push(tagObject)
					}
				}

				var associatedTagsSelector = tagType == 'profileTags' ? $("#profileTags" + index) : $("#trackingTags" + index);

				associatedTagsSelector.tokenInput("/data/getTagsForAutoComplete", {
					theme: "facebook",
					preventDuplicates: true,
					prePopulate: tagsList
				});
			}

			function deleteAnswer(rowId, answerId) {
				if (answerId) {
					showYesNo("The answer will be deleted permanently. Do you want to continue?", function() {
						var params = {'possibleAnswerInstance.id': answerId};
						params.id = ${questionInstance.id}
						queuePostJSON('Deleting answer from survey question', '/survey/deleteAnswer',
								getCSRFPreventionObject('deleteAnswerCSRF', params),
								function (data) {
									if (checkData(data)) {
										if (data.success) {
											$('#answerRow' + rowId).remove();
										}
										showBootstrapAlert($('.alert'), data.message);
									}
								}, function (xhr) {
									console.log('error: ', xhr);
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
				<input hidden name="questionId" id="questionId" value="${questionInstance.id}" />
				<div>
					<label for="question">
						Question
					</label>
					<textarea placeholder="Add question text..." maxlength="1000" name="question"
							id="question" class="question-input" required>${questionInstance.question}</textarea>
				</div>
				<div class="row">
					<div class="col-md-3">
						<label for="priority">
							Priority
						</label>
						<input class="survey-input" type="number" min="0" value="${questionInstance.priority}"
								name="priority" placeholder="Priority" id="priority" required/>
					</div>
					<div class="col-md-3">
						<label for="status">
							Status
						</label>
						<g:select class="survey-input" name="status" id="status" from="${QuestionStatus.values()}"
								optionValue="displayText" value="${questionInstance.status ?: QuestionStatus.values()[1]}"/>
					</div>
					<div class="col-md-3">
						<label for="answerType">
							Answer Type
						</label>
						<g:select class="survey-input" name="answerType" id="answerType" from="${AnswerType.values()}"
								optionValue="displayText" value="${questionInstance.answerType ?: AnswerType.values()[0]}"/>
					</div>
					<div class="col-md-1">
						<label for="isRequired">
							Required
						</label>
						<g:checkBox class="survey-input" name="isRequired" id="isRequired" value="${questionInstance.isRequired}" />
					</div>
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
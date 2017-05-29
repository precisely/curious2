<%@ page import="us.wearecurio.model.survey.AnswerType" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>Question</title>

		<c:jsCSRFToken keys="addAnswersCSRF, deleteAnswerCSRF, autocompleteDataCSRF"/>
		<script type="text/javascript">

			var rowIdInEditMode;

			$(document).ready(function() {
				var rowNumber = ${(questionInstance.answers.size() ?: -1) + 1};

				var autocompleteWidget = new AutocompleteWidget("answerTagAutoComplete", 'tagDescription');

				$("#addAnswerOverlay").on("hidden.bs.modal",function() {
					$(this).find('form').trigger('reset');
				});

				$('#addSurveyAnswerForm').submit(function() {
					// Defined in base.js file.
					var params = $(this).serializeObject();

					if (rowIdInEditMode != null) {
						var rowInEditMode = $('#answerRow' + rowIdInEditMode);
						rowInEditMode.find('#answerText' + rowIdInEditMode).text(params.answer);
						rowInEditMode.find('#priorityNumber' + rowIdInEditMode).text(params.priority);
						rowInEditMode.find('#tagDescription' + rowIdInEditMode).text(params.tagDescription);

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
	
									'<td id=tagDescription' + rowNumber + '>' +
										params.tagDescription +
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

				$('#addAnswers').click(function() {
					var possibleAnswersList = [];

					$('#answerInputAffordance  > tr').each(function() {
						var answerRow = $(this).find('td');

						var possibleAnswer = {
							answer: $(answerRow[1]).text(),
							priority: $(answerRow[2]).text(),
							tagDescription: $(answerRow[3]).text()
						};

						var answerId = $(answerRow[0]).text();
						if (answerId) {
							possibleAnswer.answerId = answerId;
						}

						possibleAnswersList.push(possibleAnswer);
					});

					if (possibleAnswersList.length === 0) {
						showBootstrapAlert($('.alert'), 'Please add answers before saving.');

						return;
					}

					var params = {id: ${questionInstance.id}, possibleAnswers: JSON.stringify(possibleAnswersList)};

					queuePostJSON('Adding answer to survey question', '/survey/addAnswers',
							getCSRFPreventionObject('addAnswersCSRF', params),
							function(data) {
								if (checkData(data)) {
									showBootstrapAlert($('.alert'), data.message);
								}
							}, function(xhr) {
								console.log('error: ', xhr);
							}
					);
				});
			});

			function deleteAnswer(rowId, answerId) {
				if (answerId) {
					var params = {'possibleAnswerInstance.id': answerId, 'questionInstance.id': ${questionInstance.id}};
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
				answerModal.find('#answerId').val($(rowInEditMode).find('#answerId' + rowId).text());
				answerModal.find('#answer').val($(rowInEditMode).find('#answerText' + rowId).text());
				answerModal.find('#priority').val($(rowInEditMode).find('#priorityNumber' + rowId).text());
				answerModal.find('#tagDescription').val($(rowInEditMode).find('#tagDescription' + rowId).text());
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
					Question Details
				</h1>
			</div>
		</div>

		<div class="main container-fluid survey-factory">
			<div class="margin-bottom">
				<ul class="breadcrumb">
					<li>
						<g:link controller="survey" action="index" data-toggle="tooltip"
								title="Go To Survey List" data-placement="top">
							Surveys
						</g:link>
					</li>
					<li>
						<span>/ </span>
						<g:link controller="survey" action="surveyDetails" id="${questionInstance.survey.id}"
								data-toggle="tooltip" title="Go To Survey Details" data-placement="top">
							Survey Details
						</g:link>
					</li>
					<li class="active">/ Question Details</li>
				</ul>
			</div>
			<table class="table table-bordered table-hover table-striped">
				<thead>
				<tr>
					<th>Question</th>
					<th>Priority</th>
					<th>Status</th>
					<th>Answer Type</th>
				</tr>
				</thead>
				<tbody>
				<tr>
					<td>
						${questionInstance.question}
					</td>
					<td>
						${questionInstance.priority}
					</td>
					<td>
						${questionInstance.status.displayText}
					</td>
					<td>
						${questionInstance.answerType.displayText}
					</td>
				</tr>
				</tbody>
			</table>

			<g:render template="answerList" />

		</div>

		<g:render template="addAnswersModal" />

	</body>
</html>
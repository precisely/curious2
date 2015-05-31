<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<title>Survey</title>
		<c:jsCSRFToken keys="createAnswersDataCSRF"/>
		<script type="text/javascript">
		var rowIdToRemove;
		$(document).ready(function() {
			var rowNumber = ${(surveyQuestion.possibleAnswers.size() ?: -1) + 1};

			$('#addSurveyAnswerForm').submit(function(event) {
				$('.add-answer').attr('disabled','disabled');
				var params = $(this).serializeObject();		// Defined in base.js file

				var innerHTMLContent = '<div class="row survey-answers" id="' + rowNumber + '"><div class="col-md-3"> ' + 
					'<input type="text" name="possibleAnswers[' + rowNumber + '].answer" id="answer" readonly value="' + params.answer + '"/></div>' + 
					'<div class="col-md-3"><input type="text" class="answer-code" name="possibleAnswers[' + rowNumber + '].code" id="code" readonly value="' + params.code + '"/></div>' + 
					'<div class="col-md-1"><input type="number" class="answer-priority" name="possibleAnswers[' + rowNumber + '].priority" id="priority" value="' + params.priority + '" readonly/></div>' + 
					'<div class="col-md-3"><input readonly type="text" class="answer-type" name="possibleAnswers[' + rowNumber + '].answerType" value="' + params.answerType + '"></div>' + 
					'<div class="col-md-1"> <button class="edit-answer" onclick="editAnswer(' + rowNumber + ')"><i class="fa fa-pencil"></i></button></div>' + 
					'<div class="col-md-1"><button onclick="deleteAnswer(' + rowNumber + ')" class="delete-answer"> <i class="fa fa-trash"></i></button></div></div>';

				if (rowIdToRemove != null) {
					$('#' + rowIdToRemove).remove();
					rowIdToRemove = null;
				}
				$('#answerInputAffordance').append(innerHTMLContent);
				$('#addAnswerOverlay').modal('toggle');
				$('.add-answer').removeAttr('disabled');
				rowNumber += 1;
				return false;
			});

			$('#submitAnswersForm').submit(function(event) {
				params = $(this).serializeObject();
				// If form is in edit mode, final form should not be submitted
				if (rowIdToRemove == null) {
					queuePostJSON('Adding answer to survey question', '/admin/createAnswersData', 
							getCSRFPreventionObject('createAnswersDataCSRF', params),
							function(data) {
								if (checkData(data) {
									if (data.success) {
										window.location.assign('/admin/listSurveyQuestions');
									} else {
										showBootstrapAlert($('.alert'), data.message);
									}
								}
							}, function(xhr) {
								console.log('error: ', xhr);
							});
				}
				return false;
			});
		});
		function deleteAnswer(rowId) {
			$('#' + rowId).remove();
		}

		function editAnswer(rowId) {
			rowIdToRemove = rowId;
			$('#addAnswerOverlay').find('#answer').val($('#' + rowId).find('#answer').val());
			$('#addAnswerOverlay').find('#code').val($('#' + rowId).find('#code').val());
			$('#addAnswerOverlay').find('#priority').val($('#' + rowId).find('#priority').val());
			$('#addAnswerOverlay').find('.answer-type').val($('#' + rowId).find('.answer-type').val());
			$('#addAnswerOverlay').find('.add-answer').text('Save');
			$('#addAnswerOverlay').find('h4').text('Edit Answer');
			$('#addAnswerOverlay').modal('toggle');
		}
		</script>
	</head>
	<body>
	<div class="row red-header">
		<div>
			<h1 class="clearfix">
				${surveyQuestion.question}
			</h1>
		</div>
	</div>
	<div class="main container-fluid">
		<div>
			<p id="surveyAnswerLable">
				Add possible Answers to the created question: 
			</p>
			<button id="addAnswerButton" data-toggle="modal" 
				class="btn btn-default" data-target="#addAnswerOverlay"> 
				ADD 
			</button>
		</div>
		<div class="row Answer-Headers">
			<div class="col-md-3">
				Answer
			</div>
			<div class="col-md-3">
				Code
			</div>
			<div class="col-md-1">
				Priority
			</div>
			<div class="col-md-3 type">
				Type
			</div>
			<div class="col-md-1">
				Edit
			</div>
			<div class="col-md-1">
				Delete
			</div>
		</div>
		<g:each in="${surveyQuestion.possibleAnswers}" var="answerInstance">
			<div class="row saved-survey-answers">
				<div class="col col-md-3">
					${answerInstance.answer}
				</div>
				<div class="col col-md-3">
					${answerInstance.code}
				</div>
				<div class="col col-md-2">
					${answerInstance.priority}
				</div>
				<div class="col col-md-2">
					${answerInstance.answerType}
				</div>
				<div class="col col-md-1">-</div>
				<div class="col col-md-1">-</div>
			</div>
		</g:each>
		<form action="/admin/createAnswers" id="submitAnswersForm">
			<input type="hidden" name="questionId" value="${surveyQuestion.id}" />
			<div id="answerInputAffordance">
			</div>
			<button type="submit" class="btn btn-default">Save</button>
		</form>
	</div>
	<g:render template="/survey/addAnswersModal" /> 
	</body>
</html>

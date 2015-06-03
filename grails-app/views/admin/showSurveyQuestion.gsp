<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>Survey</title>
		<c:jsCSRFToken keys="deleteSurveyQuestionDataCSRF, updateSurveyAnswerDataCSRF, deletePossibleAnswerDataCSRF"/>
		<script type="text/javascript">
			$(document).ready(function() {
				$('#addSurveyAnswerForm').submit(function(event) {
					var params = $(this).serializeObject();
					queuePostJSON('Adding answer to survey question', '/admin/updateSurveyAnswerData', 
							getCSRFPreventionObject('updateSurveyAnswerDataCSRF', params),
							function(data) {
						if (checkData(data)) {
							if (data.success) {
								$('#answer-text-' + params.answerId).text(params.answer);
								$('#answer-code-' + params.answerId).text(params.code);
								$('#answer-priority-' + params.answerId).text(params.priority);
								$('#answer-type-' + params.answerId).text(params.answerType);
								$('#addAnswerOverlay').hide();
							} else {
								showBootstrapAlert($('.alert'), data.message);
							}
						}
					}, function(xhr) {
						console.log('error: ', xhr);
					});
					return false;
				});
			});
			function deletePossibleAnswer(answerId, questionId) {
				queuePostJSON('Deleting survey answer', '/admin/deletePossibleAnswerData', 
						getCSRFPreventionObject('deletePossibleAnswerDataCSRF', {answerId: answerId, questionId: questionId}),
						function(data) {
					if (checkData(data)) {
						if (data.success) {
							$("#answer-"+answerId).remove();
						} else {
							showBootstrapAlert($('.alert'), data.message);
						}
					}
				}, function(xhr) {
					console.log('error: ', xhr);
				});
			}

			function editPossibleAnswer(answerId) {
				$('#addAnswerOverlay').find('#answer').val($('#answer-text-' + answerId).data('value'));
				$('#addAnswerOverlay').find('#code').val($('#answer-code-' + answerId).data('value'));
				$('#addAnswerOverlay').find('#priority').val($('#answer-priority-' + answerId).data('value'));
				$('#addAnswerOverlay').find('.answer-type').val($('#answer-type-' + answerId).data('value'));
				$('#addSurveyAnswerForm').append('<input type="hidden" name="answerId" value="' + answerId + '"/>');
				$('#addAnswerOverlay').find('.add-answer').text('Save');
				$('#addAnswerOverlay').find('h4').text('Edit Answer');
				$('#addAnswerOverlay').css('display','block');
				$('#addAnswerOverlay').modal({show: true});
			}

			function deleteQuestion(questionId) {
				queuePostJSON('Deleting survey questions', '/admin/deleteSurveyQuestionData', 
						getCSRFPreventionObject('deleteSurveyQuestionDataCSRF', {id: questionId}),
						function(data) {
					if (checkData(data)) {
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
		</script>
	</head>
	<body>
		<div class="row red-header">
			<div>
				<h1 class="clearfix">
					Question
				</h1>
			</div>
		</div>
		<div class="main container-fluid survey-factory">
			<h2>${surveyQuestion.question }</h1>
			<h3>Code: ${surveyQuestion.code }</h3>
			<table class="table table-bordered table-hover table-striped">
				<thead>
					<tr>
						<th>
							Question Priority
						</th>
						<th>
							Question Status
						</th>
						<th>
							Edit
						</th>
						<th>
							Delete
						</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td>
							${surveyQuestion.priority }
						</td>
						<td>
							${surveyQuestion.status }
						</td>
						<td>
							<g:link class="edit-question" controller="admin"
								action="survey" params="[id: surveyQuestion.id]">
								<i class="fa fa-pencil"></i>
							</g:link>
						</td>
						<td>
							<button class="delete-question" onclick="deleteQuestion(${surveyQuestion.id})">
								<i class="fa fa-trash"></i>
							</button>
						</td>
					</tr>
				</tbody>
			</table>
			<h3 style="display: inline-block;">Possible Answers &nbsp;&nbsp;</h2>
			<g:link class="edit-question" controller="admin" 
				action="addPossibleAnswers" params="[id: surveyQuestion.id]">
				<i class="fa fa-plus-circle"></i>
			</g:link>
			<table class="table table-bordered table-hover table-striped">
				<thead>
					<tr>
						<g:sortableColumn property="code" title="Answer Code" />
	
						<g:sortableColumn property="answer" title="Answer" />
	
						<g:sortableColumn property="priority" title="Answer Priority" />
	
						<g:sortableColumn property="answerType" title="Answer Type" />

						<th>Edit</th>

						<th>Delete</th>
					</tr>
				</thead>
				<tbody>
					<g:each in="${surveyQuestion.possibleAnswers}" var="answerInstance">
						<tr id="answer-${answerInstance.id}">
							<td id="answer-code-${answerInstance.id}" data-value="${answerInstance.code}">
								${answerInstance.code}
							</td>
							<td id="answer-text-${answerInstance.id}" data-value="${answerInstance.answer}">
								${answerInstance.answer}
							</td>
							<td id="answer-priority-${answerInstance.id}" data-value="${answerInstance.priority}">
								${answerInstance.priority}
							</td>
							<td id="answer-type-${answerInstance.id}" data-value="${answerInstance.answerType}">
								${answerInstance.answerType}
							</td>
							<td>
								<button class="edit-answer" 
									onclick="editPossibleAnswer(${answerInstance.id})">
									<i class="fa fa-pencil"></i>
								</button>
							</td>
							<td>
								<button class="delete-answer" 
									onclick="deletePossibleAnswer(${answerInstance.id}, ${surveyQuestion.id})">
									<i class="fa fa-trash"></i>
								</button>
							</td>
						</tr>
					</g:each>
				</tbody>
			</table>
		</div>
		<g:render template="/survey/addAnswersModal" /> 
	</body>
</html>

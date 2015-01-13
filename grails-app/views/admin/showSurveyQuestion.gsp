<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<title>Survey Factory</title>
		<script type="text/javascript">
			$(document).ready(function() {
				$('#addSurveyAnswerForm').submit(function(event) {
					params = $(this).serializeArray();
					$.ajax({
						type: 'POST',
						url: '/admin/UpdateSurveyAnswer',
						data: params,
						success: function(data) {
							data = JSON.parse(data);
							if (data.success) {
								$('#answer-text-' + params[4].value).text(params[0].value);
								$('#answer-code-' + params[4].value).text(params[1].value);
								$('#answer-priority-' + params[4].value).text(params[2].value);
								$('#answer-type-' + params[4].value).text(params[3].value);
								$('#addAnswerOverlay').hide();
							} else {
								console.log('error');
								$('#alert').removeClass('hide').text('Error while updating answer!');
								setInterval(function() {
									$('#alert').addClass('hide');
								}, 5000);
							}
						},
						error: function(xhr) {
							console.log('xhr:', xhr);
						}
					});
					return false;
				});
			});
			function deletePossibleAnswer(answerId, questionId) {
				$.ajax({
					type: 'POST',
					url: '/admin/deletePossibleAnswer',
					data: {
							answerId: answerId,
							questionId: questionId
						},
					success: function(data) {
						data = JSON.parse(data);
						if (data.success) {
							$("#answer-"+answerId).remove();
						} else {
							console.log('error');
							$('.alert').removeClass('hide').text('Error while adding answers to the question!');
							setInterval(function() {
								$('.alert').addClass('hide');
							}, 5000);
						}
					},
					error: function(xhr) {
						console.log('xhr:', xhr);
					}
				});
			}

			function editPossibleAnswer(answerId) {
				$('#addAnswerOverlay').find('#answer').val($('#answer-text-' + answerId).text());
				$('#addAnswerOverlay').find('#code').val($('#answer-code-' + answerId).text());
				$('#addAnswerOverlay').find('#priority').val($('#answer-priority-' + answerId).text());
				$('#addAnswerOverlay').find('.answer-type').val($('#answer-type-' + answerId).text());
				$('#addSurveyAnswerForm').append('<input type="hidden" name="answerId" value="' + answerId + '"/>');
				$('#addAnswerOverlay').find('.add-answer').text('Save');
				$('#addAnswerOverlay').find('h4').text('Edit Answer');
				$('#addAnswerOverlay').css('display','block');
				$('#addAnswerOverlay').modal({show: true});
			}

			function deleteQuestion(questionId) {
				$.ajax({
					type: 'POST',
					url: '/admin/deleteSurveyQuestion',
					data: {
							questionId: questionId
						},
					success: function(data) {
						data = JSON.parse(data);
						if (data.success) {
							window.location.assign('/admin/listSurveyQuestions');
						} else {
							console.log('error');
							$('.alert').removeClass('hide').text('Could not delete the question!');
							setInterval(function() {
								$('.alert').addClass('hide');
							}, 5000);
						}
					},
					error: function(xhr) {
						console.log('xhr:', xhr);
					}
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
						<th>Question Priority</th>
						<th>Question Status</th>
						<th>Edit</th>
						<th>Delete</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td>${surveyQuestion.priority }</td>
						<td>${surveyQuestion.status }</td>
						<td><g:link class="edit-question" controller="admin" action="surveyFactory" params="[id: surveyQuestion.id]"><i class="fa fa-pencil"></i></g:link></td>
						<td><button class="delete-question" onclick="deleteQuestion(${surveyQuestion.id})"><i class="fa fa-trash"></i></button></td>
					</tr>
				</tbody>
			</table>
			<h3 style="display: inline-block;">Possible Answers &nbsp;&nbsp;</h2>
			<g:link class="edit-question" controller="admin" action="addPossibleAnswers" params="[id: surveyQuestion.id]"><i class="fa fa-plus-circle"></i></g:link>
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
						<td id="answer-code-${answerInstance.id}">${answerInstance.code}</td>
						<td id="answer-text-${answerInstance.id}">${answerInstance.answer}</td>
						<td id="answer-priority-${answerInstance.id}">${answerInstance.priority}</td>
						<td id="answer-type-${answerInstance.id}">${answerInstance.answerType}</td>
						<td><button class="edit-answer" onclick="editPossibleAnswer(${answerInstance.id}, ${surveyQuestion.id})"><i class="fa fa-pencil"></i></button></div>
						<td><button class="delete-answer" onclick="deletePossibleAnswer(${answerInstance.id}, ${surveyQuestion.id})"><i class="fa fa-trash"></i></button></div>
					</tr>
				</g:each>
				</tbody>
			</table>
		</div>
		<g:render template="/survey/addAnswersModal" /> 
	</body>
</html>

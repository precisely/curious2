<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<title>Survey Factory</title>
		<script type="text/javascript">
		var rowIdToRemove;
		var priorityList = [];
		$(document).ready(function() {
			var rowNo = 0;
			$('#addSurveyAnswerForm').submit(function(event) {
				console.log('in submit answer');
				params = $(this).serializeArray();
				console.log('modal submited....',params);
				if ($.inArray(params[2].value, priorityList) > -1) {
					$('#alert').removeClass('hide').text('Priorities of two questions can not be same!');
					setInterval(function() {
						$('#alert').addClass('hide');
					}, 5000);
				} else {
					priorityList.push(params[2].value);
					var answerType = (params[3].value == "MCQ")?"MCQ":"DESCRIPTIVE";
	
					var innerHTMLContent = '<div class="row surveyAnswers" id="' + rowNo +'"><div class="col-md-3"> ' + 
						'<input type="text" name="possibleAnswers['+ rowNo +'].answer" id="answer" readonly value="' + params[0].value + '"/></div>' + 
						'<div class="col-md-3"><input type="text" class="answer-code" name="possibleAnswers['+ rowNo +'].code" id="code" readonly value="' + params[1].value + '"/></div>' + 
						'<div class="col-md-1"><input type="number" class="answer-priority" name="possibleAnswers['+ rowNo +'].priority" id="priority" value="' + params[2].value + '" readonly/></div>' + 
						'<div class="col-md-3"><input readonly type="text" class="answer-type" name="possibleAnswers['+ rowNo +'].answerType" value="' + answerType + '"></div>' + 
						'<div class="col-md-1"> <button class="edit-answer" onclick="editAnswer(' + rowNo + ')"><i class="fa fa-pencil"></i></button></div>' + 
						'<div class="col-md-1"><button onclick="deleteAnswer(' + rowNo + ')" class="delete-answer"> <i class="fa fa-trash"></i></button></div></div>';

					if (rowIdToRemove != null) {
						$('#'+rowIdToRemove).remove();
						rowIdToRemove = null;
					}
					$('#answerInputAffordance').append(innerHTMLContent);
					$('#addAnswerOverlay').modal('toggle');
					rowNo += 1;
				}
				return false;
			});

			$('#submitAnswersForm').submit(function(event) {
				params = $(this).serializeArray();
				console.log('submited....',params);
				// If form is in edit mode, final form should not be submitted
				if (rowIdToRemove == null) {
					$.ajax({
						type: 'POST',
						url: '/admin/createAnswers',
						data: params,
						success: function(data) {
							data = JSON.parse(data);
							if(data.success) {
								console.log('success');
								window.location.assign('/admin/listSurveyQuestions');
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
				return false;
			});
		});
		function deleteAnswer(rowId) {
			$('#' + rowId).remove();
		}

		function editAnswer(rowId) {
			rowIdToRemove = rowId;
			var priorityIndex = priorityList.indexOf($('#'+rowId).find('#priority').val());
			priorityList.splice(priorityIndex, 1);
			$('#addAnswerOverlay').find('#answer').val($('#'+rowId).find('#answer').val());
			$('#addAnswerOverlay').find('#code').val($('#'+rowId).find('#code').val());
			$('#addAnswerOverlay').find('#priority').val($('#'+rowId).find('#priority').val());
			$('#addAnswerOverlay').find('.answer-type').val($('#'+rowId).find('.answer-type').val());
			$('#addAnswerOverlay').find('.add-answer').text('Save');
			$('#addAnswerOverlay').find('h4').text('Edit Answer');
			$('#addAnswerOverlay').css('display','block');
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
			<p id="surveyAnswerLable">Add possible Answers to the created question: </p>
			<button id="addAnswerButton" data-toggle="modal" class="btn btn-default" data-target="#addAnswerOverlay"> ADD </button>
		</div>
		<div class="row Answer-Headers">
			<div class="col-md-3">Answer</div>
			<div class="col-md-3">Code</div>
			<div class="col-md-1">Priority</div>
			<div class="col-md-3 type">Type</div>
			<div class="col-md-1">Edit</div>
			<div class="col-md-1">Delete</div>
		</div>
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

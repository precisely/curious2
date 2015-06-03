<%@ page import="us.wearecurio.model.SurveyQuestion" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>Survey</title>
	</head>
	<body>
	<div class="row red-header">
		<div>
			<h1 class="clearfix">
				Create new survey question
			</h1>
		</div>
	</div>
	<div class="main container-fluid survey-factory">
		<form action="/admin/createOrUpdateQuestion" method="post">
			<div class="survey-question">
				<label for="question">
					Question:
				</label>
				<textarea placeholder="New survey question..."
					name="question" id="question" maxlength="1000" required>${surveyQuestion?.question}</textarea>
			</div>
			<div class="row">
				<div class="col-md-4">
					<div class="input-affordance">
						<label for="code">
							Question Code:
						</label>
						<input class="question-code" type="text" value= "${surveyQuestion?.code}" 
							placeholder="Unique survey question code..." name="code" id="code" required />
					</div>
				</div>
				<div class="col-md-4">
					<label for="priority">
						Question Priority:
					</label>
					<input class="question-priority" type="number" value= "${surveyQuestion?.priority}" 
						min="0" name="priority" placeholder="Priority" id="priority" required/>
				</div>
				<div class="col-md-4">
					<label for="status">
						Question Status:
					</label>
					<g:select class="question-status" name="status"
						from="${SurveyQuestion.QuestionStatus.values()}" value="${surveyQuestion?.status}" />
				</div>
			</div>
			<input type="hidden" name="id" value="${surveyQuestion?.id}"/>
			<button type="submit" class="btn btn-default">
				Save
			</button>
		</form>
	</div>
	</body>
</html>

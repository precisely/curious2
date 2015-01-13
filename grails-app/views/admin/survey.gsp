<%@ page import="us.wearecurio.model.QuestionStatus" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<title>Survey Factory</title>
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
			<div class="row survey-question">
				<div class="col-md-6">
					<label for="question">Question:</label>
					<textarea placeholder="New survey question..."
						name="question" id="question" maxlength="1000" required>${surveyQuestion?.question}</textarea>
				</div>
				<div class="col-md-6">
					<label for="validator">Validator:</label>
					<textarea placeholder="Write a custom validator for user answers..."
						name="validator" id="validator">${surveyQuestion?.validator}</textarea>
				</div>
			</div>
			<div class="row">
				<div class="col-md-4">
					<div class="input-affordance">
						<label for="code">Question Code:</label>
						<input class="question-code" type="text" value= "${surveyQuestion?.code}" 
							placeholder="Unique survey question code..." name="code" id="code" required />
					</div>
				</div>
				<div class="col-md-4">
					<label for="priority">Question Priority:</label>
					<input class="question-priority" type="number" value= "${surveyQuestion?.priority}" 
						min="0" max="10" name="priority" placeholder="Priority" id="priority" required/>
				</div>
				<div class="col-md-4">
					<label for="status">Question Status:</label>
					<g:select class="question-status" name="status" 
						from="${QuestionStatus.values()}" value="${QuestionStatus}" />
				</div>
			</div>
			<input type="hidden" name="id" value="${surveyQuestion?.id}"/>
			<button type="submit" class="btn btn-default">Save</button>
		</form>
	</div>
	</body>
</html>

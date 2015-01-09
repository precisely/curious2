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
		<form action="/admin/createNewQuestion" method="post">
			<div class="survey-question">
				<label for="question">Question:</label>
				<textarea placeholder="New survey question..."
					name="question" id="question" required></textarea>
			</div>
			<div class="row">
				<div class="col-md-4">
					<div class="input-affordance">
						<label for="code">Question Code:</label>
						<input class="question-code" type="text" placeholder="Unique survey question code..." name="code" id="code" required />
					</div>
				</div>
				<div class="col-md-4">
					<label for="priority">Question Priority:</label>
					<input class="question-priority" type="number" min="0" max="10" name="priority" placeholder="Priority" id="priority" required/>
				</div>
				<div class="col-md-4">
					<label for="status">Question Status:</label>
					<g:select class="question-status" name="status" from="${QuestionStatus.values()}" value="${QuestionStatus}" />
				</div>
			</div>
			<button type="submit" class="btn btn-default">Save</button>
		</form>
	</div>
	</body>
</html>

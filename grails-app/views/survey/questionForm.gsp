<%@ page import="us.wearecurio.model.survey.QuestionStatus" %>
<%@ page import="us.wearecurio.model.survey.AnswerType" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>Survey Question</title>
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
			<g:form controller="survey" action="${questionInstance.id ? 'updateQuestion' : 'saveQuestion'}">
				<input hidden name="id" value="${questionInstance.id ?: surveyId}" />
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
					<button type="submit" class="btn btn-default margin-left">
						${questionInstance.id ? 'Update' : 'Add'}
					</button>
				</div>
			</g:form>
		</div>
	</body>
</html>
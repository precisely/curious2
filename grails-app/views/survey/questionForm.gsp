<%@ page import="us.wearecurio.model.survey.QuestionStatus" %>
<%@ page import="us.wearecurio.model.survey.AnswerType" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>Survey Question</title>

		<c:jsCSRFToken keys="addQuestionCSRF, updateQuestionCSRF, deleteAnswerCSRF, autocompleteDataCSRF"/>
		<script type="text/javascript" src="/js/jquery/jquery.tokeninput.min.js"></script>
		<script type="text/javascript" src="/js/curious/questionFormData.js"></script>
		<link rel="stylesheet" type="text/css" href="/css/token-input/token-input.css" />
		<link rel="stylesheet" type="text/css" href="/css/token-input/token-input-facebook.css" />
		<script type="text/javascript">

			var surveyId = ${surveyId};
			var questionId =  parseInt("${questionInstance.id}", 10) ? parseInt("${questionInstance.id}", 10) : null;

			$(document).ready(function() {
					handleQuestionFormData(questionId, surveyId,
							${(questionInstance.answers.size() ?: -1) + 1},
							${questionInstance.answers.size()},
							${questionInstance.answerType ? questionInstance.answerType.value() : null});
			});

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
					<div class="col-md-3">
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
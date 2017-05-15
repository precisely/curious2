<%@ page import="us.wearecurio.model.survey.Survey" %>
<%@ page import="us.wearecurio.model.survey.SurveyStatus" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>Survey</title>
	</head>
	<body>
		<div class="red-header">
			<div>
				<h1>
					<g:if test="${surveyInstance.id}">
						Update Survey
					</g:if>
					<g:else>
						Create Survey
					</g:else>
				</h1>
			</div>
		</div>

		<div class="main container-fluid survey-factory">
			<g:form controller="survey" action="${surveyInstance.id ? 'update' : 'save'}" method="post">
				<g:if test="${surveyInstance.id}">
					<input hidden name="id" value="${surveyInstance.id}" />
				</g:if>
				<div>
					<label for="title">
						Title
					</label>
					<input class="survey-input" type="text" name="title" placeholder="Title" id="title" 
							value="${surveyInstance.title}" required/>
				</div>
				<div>
					<label for="code">
						Code
					</label>
					<input class="survey-input" type="text" name="code" placeholder="Code" id="code"
							value="${surveyInstance.code}" required/>
				</div>
				<div>
					<label for="status">
						Status
					</label>
					<g:select class="survey-input" name="status" from="${SurveyStatus.values()}" 
							optionValue="displayText" value="${surveyInstance.status ?: SurveyStatus.values()[1]}"/>
				</div>

				<div class="margin-top">
					<button type="button" class="btn btn-default">
						<g:link controller="survey" action="index" class="cancel-button-link">
							Cancel
						</g:link>
					</button>
					<button type="submit" class="btn btn-default margin-left">
						${surveyInstance.id ? 'Update' : 'Add'}
					</button>
				</div>
			</g:form>
		</div>
	</body>
</html>
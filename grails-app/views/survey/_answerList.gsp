<%@ page import="us.wearecurio.model.survey.AnswerType" %>
<div class="main container-fluid survey-factory survey-factory-container">
	<div>
		<h2 class="display-inline">
			Survey Answers
		</h2>
		<g:if test="${questionInstance.answerType != AnswerType.DESCRIPTIVE}">
			<a href="#" class="btn btn-default add-survey-button pull-right" data-toggle="modal" data-target="#addAnswerOverlay">
				<i class="fa fa-plus-circle survey-add-icon"></i> Add Answer
			</a>
		</g:if>
		<g:else>
			( The AnswerType is descriptive. )
		</g:else>
	</div>
	<table class="table table-bordered table-hover table-striped remove-margin-bottom">
		<thead>
			<tr>
				<th>Answer</th>
				<th>Priority</th>
				<th>AssociatedProfileTags</th>
				<th>AssociatedTrackingTags</th>
				<th>Actions</th>
			</tr>
		</thead>
		<tbody id="answerInputAffordance">
			<g:each in="${questionInstance.answers}" var="answerInstance" status="index">
				<tr id="answerRow${index}">
					<td id="answerId${index}" class="hidden">${answerInstance.id}</td>
					<td id="answerText${index}">${answerInstance.answer}</td>
					<td id="priorityNumber${index}">${answerInstance.priority}</td>
					<td id="profileTags${index}"><g:each in="${answerInstance.associatedProfileTags}" var="tagsInstance">
						${tagsInstance.description},
					</g:each> </td>
					<td id="trackingTags${index}"><g:each in="${answerInstance.associatedTrackingTags}" var="tagsInstance">
						${tagsInstance.description},
					</g:each> </td>
					<td>
						<a href="#" data-toggle="tooltip" title="Edit Answer" data-placement="top">
							<i class="fa fa-pencil action-icon" onclick="editAnswer(${index})"></i>
						</a>
						<a href="#" class="margin-left" data-toggle="tooltip" title="Delete Answer" 
								data-placement="top">
							<i class="fa fa-trash action-icon" onclick="deleteAnswer(${index}, ${answerInstance.id})"></i>
						</a>
					</td>
				</tr>
			</g:each>
		</tbody>
	</table>
	<g:if test="${questionInstance.answerType != AnswerType.DESCRIPTIVE}">
		<div class="margin-top">
			<button type="button" class="btn btn-default">
				<g:link controller="survey" action="surveyDetails" id="${questionInstance.survey.id}"
						class="cancel-button-link">
					Cancel
				</g:link>
			</button>
			<button type="submit" class="btn btn-default margin-left" id="addAnswers">
				Save Answers
			</button>
		</div>
	</g:if>
</div>
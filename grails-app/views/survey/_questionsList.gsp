<div class="main container-fluid survey-factory survey-factory-container">
	<div>
		<h2 class="display-inline">
			Survey Questions
		</h2>
		<button class="btn btn-default add-survey-button pull-right" onclick="window.location = '/survey/addQuestion/${surveyInstance.id}'">
			<i class="fa fa-plus-circle survey-add-icon"></i> Add Question
		</button>
	</div>
	<table class="table table-bordered table-hover table-striped remove-margin-bottom">
		<thead>
			<tr>
				<th>Question</th>
				<th>Priority</th>
				<th>Status</th>
				<th>Answer Type</th>
				<th>Required</th>
				<th>No of Answers</th>
				<th>Actions</th>
			</tr>
		</thead>
		<tbody>
			<g:each in="${surveyInstance.questions}" var="questionInstance">
				<tr>
					<td>
						${questionInstance.question}
					</td>
					<td>
						${questionInstance.priority}
					</td>
					<td>
						${questionInstance.status.displayText}
					</td>
					<td>
						${questionInstance.answerType.displayText}
					</td>
					<td>
						${questionInstance.isRequired ? 'Yes' : 'No'}
					</td>
					<td>
						${questionInstance.answers.size()}
					</td>
					<td>
						<g:link controller="survey" action="editQuestion" id="${questionInstance.id}"
								params="[surveyId: surveyInstance.id]" data-toggle="tooltip" 
								title="Edit Question" data-placement="top">
							<i class="fa fa-pencil action-icon"></i>
						</g:link>
						<g:link controller="survey" action="toggleQuestionStatus" id="${questionInstance.id}"
								params="[surveyId: surveyInstance.id]" class="margin-left" data-toggle="tooltip"
								title="${questionInstance.status.displayText == 'Active' ? 'Mark inactive' : 'Mark active'}"
								data-placement="top">
							<i class="fa ${questionInstance.status.displayText == 'Active' ? 'fa-toggle-off' : 'fa-toggle-on'} action-icon"></i>
						</g:link>
					</td>
				</tr>
			</g:each>
		</tbody>
	</table>
</div>
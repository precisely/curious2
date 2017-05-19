<div class="main container-fluid survey-factory">
	<h1>
		Survey Questions
		<span>
		<span>
			<g:link controller="survey" action="addQuestion" id="${surveyInstance.id}">
				<i class="fa fa-plus-circle action-icon"></i>
			</g:link>
		</span>
		</span>
	</h1>
	<table class="table table-bordered table-hover table-striped">
		<thead>
			<tr>
				<th>Question</th>
				<th>Priority</th>
				<th>Status</th>
				<th>Answer Type</th>
				<th>Required</th>
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
						<g:link controller="survey" action="editQuestion" id="${questionInstance.id}"
								params="[surveyId: surveyInstance.id]" data-toggle="tooltip" 
								title="Edit Question" data-placement="top">
							<i class="fa fa-pencil action-icon"></i>
						</g:link>
						<g:link controller="survey" action="questionDetails" id="${questionInstance.id}"
								params="[surveyId: surveyInstance.id]" class="margin-left" data-toggle="tooltip"
								title="Click to see details" data-placement="top">
							<i class="fa fa-clipboard action-icon"></i>
						</g:link>
					</td>
				</tr>
			</g:each>
		</tbody>
	</table>
</div>
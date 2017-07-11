<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>Survey</title>
	</head>
	<body>
	<div class="red-header">
		<h1>
			Survey
		</h1>
	</div>

	<div class="main container-fluid survey-factory survey-factory-container">
		<div>
			<h2 class="display-inline">
				Survey List
			</h2>
			<button class="btn btn-default add-survey-button pull-right" onclick="window.location = '/survey/create'">
				<i class="fa fa-plus-circle survey-add-icon"></i> Add Survey
			</button>
		</div>

		<table class="table table-bordered table-hover table-striped remove-margin-bottom">
			<thead>
				<tr>
					<th>Title</th>
					<th>Code</th>
					<th>Status</th>
					<th>No of Questions</th>
					<th>Actions</th>
				</tr>
			</thead>
			<tbody>
				<g:each in="${surveyList}" var="surveyInstance">
					<tr>
						<td>
							${surveyInstance.title}
						</td>
						<td>
							${surveyInstance.code}
						</td>
						<td>
							${surveyInstance.status.displayText}
						</td>
						<td>
							${surveyInstance.questions.size()}
						</td>
						<td>
							<g:link controller="survey" action="edit" id="${surveyInstance.id}" data-toggle="tooltip"
									title="Edit Survey" data-placement="top">
								<i class="fa fa-pencil action-icon"></i>
							</g:link>
							<g:link controller="survey" action="surveyDetails" id="${surveyInstance.id}"
									data-toggle="tooltip" title="Click to see details" data-placement="top"
									class="margin-left">
								<i class="fa fa-clipboard action-icon"></i>
							</g:link>
						</td>
					</tr>
				</g:each>
			</tbody>
		</table>
	</div>
	</body>
</html>

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
					Survey Details
				</h1>
			</div>
		</div>

		<div class="main container-fluid survey-factory">
			<div class="margin-bottom">
				<ul class="breadcrumb">
					<li>
						<g:link controller="survey" action="index" data-toggle="tooltip"
								title="Go To Survey List" data-placement="top">
							Surveys
						</g:link>
					</li>
					<li class="active">/ Survey Details</li>
				</ul>
			</div>
			<table class="table table-bordered table-hover table-striped">
				<thead>
				<tr>
					<th>Title</th>
					<th>Code</th>
					<th>Status</th>
				</tr>
				</thead>
				<tbody>
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
					</tr>
				</tbody>
			</table>

			<g:render template="questionsList"/>

		</div>

	</body>
</html>
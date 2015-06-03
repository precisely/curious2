<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>Survey</title>
	</head>
	<body>
		<div class="row red-header">
			<div>
				<h1 class="clearfix">
					Survey Questions
				</h1>
			</div>
		</div>
		<div class="main container-fluid survey-factory">
			<h1>
				Question List:
			</h1>
			<table class="table table-bordered table-hover table-striped">
				<thead>
					<tr>
						<g:sortableColumn property="code" title="Question Code" />
	
						<g:sortableColumn property="question" title="Question" />
	
						<g:sortableColumn property="priority" title="Priority" />
	
						<g:sortableColumn property="status" title="status" />
					</tr>
				</thead>
				<tbody>
					<g:each in="${questions}" var="questionInstance">
						<tr>
							<td>
								<g:link controller="admin" action="showSurveyQuestion" id= "${questionInstance.id}">
									${questionInstance.code}
								</g:link>
							</td>
							<td>
								${questionInstance.question}
							</td>
							<td>
								${questionInstance.priority}
							</td>
							<td>
								${questionInstance.status}
							</td>
						</tr>
					</g:each>
				</tbody>
			</table>
		</div>
	</body>
</html>
		
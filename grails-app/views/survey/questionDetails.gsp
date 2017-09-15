<%@ page import="us.wearecurio.model.survey.AnswerType" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>Question</title>

		<script type="text/javascript">
			$(document).ready(function() {
				if (${questionInstance.answerType == AnswerType.DESCRIPTIVE}) {
					$('#answerListContainer').attr("style", "display:none");
				}
			});
		</script>
	</head>
	<body>
		<div class="red-header">
			<div>
				<h1>
					Question Details
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
					<li>
						<span>/ </span>
						<g:link controller="survey" action="surveyDetails" id="${questionInstance.survey.id}"
								data-toggle="tooltip" title="Go To Survey Details" data-placement="top">
							Survey Details
						</g:link>
					</li>
					<li class="active">/ Question Details</li>
				</ul>
			</div>
			<table class="table table-bordered table-hover table-striped">
				<thead>
				<tr>
					<th>Question</th>
					<th>Priority</th>
					<th>Status</th>
					<th>Answer Type</th>
				</tr>
				</thead>
				<tbody>
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
				</tr>
				</tbody>
			</table>

			<g:render template="answerList" model="[isViewOnly: true]" />

		</div>

	</body>
</html>
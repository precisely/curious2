<html>
<head>
		<meta name="layout" content="menu" />
		<title>Curious - Admin Dashboard</title>
		<meta name="description" content="A platform for health hackers" />
</head>
<body>
		<div class="page-header">
				<h1>Admin Dashboard</h1>
		</div>
		<ul>
				<li>
						<g:link controller="userGroup" action="list">Groups</g:link>
				</li>
				<li>
						<g:link controller="sharedTagGroup" action="list">Shared Tag Groups</g:link>
				</li>
				<li>
						<g:link controller="analyticsTask" action="index">Analytics Jobs</g:link>
				</li>
				<li>
					<g:link controller="admin" action="survey">Survey</g:link>
				</li>
				<li>
					<g:link controller="admin" action="listSurveyQuestions">Survey Questions</g:link>
				</li>
			</ul>
</body>
</html>

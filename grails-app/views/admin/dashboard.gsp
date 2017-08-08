<html>
	<head>
		<meta name="layout" content="menu" />
		<title>precise.ly - Admin Dashboard</title>
		<meta name="description" content="A platform for health hackers" />
	</head>
	<body>
		<div class="page-header">
			<h1>Admin Dashboard</h1>
		</div>
				
		<ul class="margin-left">
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
				<g:link controller="admin" action="exportStudy">Export Study</g:link>
			</li>
			<li>
				<g:link controller="admin" action="uploadTagInputTypeCSV">Import TagInputType from CSV</g:link>
			</li>
			<li>
				<g:link controller="survey" action="index">Survey</g:link>
			</li>
			<li>
				<g:link controller="admin" action="subscriptions" >Subscription</g:link>
			</li>
			<li>
				<g:link controller="admin" action="esReindex">Elastic Search Reindex</g:link>
			</li>
		</ul>
	</body>
</html>

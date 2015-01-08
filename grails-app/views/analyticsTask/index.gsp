<!DOCTYPE html>
<html>
<head>
	<meta name="layout" content="main">
	<title>Analytics Jobs</title>
	<script type="text/javascript" src="/js/jquery/jquery-1.7.2.min.js"></script>
	<script src="/js/mustache.js"></script>
	<script src="/js/lodash/lodash.min.js"></script>
	<script src="/js/jquery/ui.datepicker.js"></script>
	<script src="/js/curious/base.js"></script>
	<script src="/js/curious/analyticsTask.js?ver=1"></script>
	<link rel="stylesheet" href="/css/main.css?ver=21"/>
	<link rel="stylesheet" href="/css/admin.css?ver=21"/>
</head>
<body class="analytics-tasks container-fluid">
<h1 class="page-header">Analytics Jobs</h1>
<small>
	<a id="start-analytics-job-button" action="create" class="btn">Run on All Users</a>
</small>

<div class="row">
	<h2> Available Servers </h2>
	<table id="available-server-table" class="table table-bordered">
		<tr>
			<th>server</th>
			<th>status</th>
		</tr>
	<g:each in="${servers}" var="server">
		<tr server="${server['url']}">
			<td class="url">${server['url']}</td>
			<td class="status">${server['status']}</td>
		</tr>
	</g:each>
	</table>
</div>

<div class="row">
	<h2>Jobs</h2>
	<table id="task-list" class="table table-bordered">
		<thead>
			<tr>
				<th>id</th>
				<th>userId</th>
				<th>status</th>
				<th>%&nbsp;success</th>
				<th>server&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</th>
				<th>created&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</th>
				<th>updated&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</th>
				<th>notes</th>
				<th>error</th>
				<th></th>
			</tr>
		</thead>
		<tbody>
			<script id="task-template" type="x-tmpl-mustache">
				<tr class="task-row {{typeEn}}" task-id="{{id}}" parent-id="{{parentId}}">
					<td class="id"> {{id}} </td>
					<td class="user-id"> {{userId}} </td>
					<td class="status"> {{statusEn}} </td>
					<td class="percent-successful"> {{percentSuccessful}} </td>
					<td class="server-adress"> {{serverAddress}} </td>
					<td class="created-at"> {{createdAt}} </td>
					<td class="updated-at"> {{updatedAt}} </td>
					<td class="notes"> {{notes}} </td>
					<td class="error"> {{error}} </td>
					<td class="action">
						<button class="btn rerun" value="rerun-incomplete" >Rerun</button>
					</td>
				</tr>
			</script>
				<g:each in="${analyticsTasks}" var="task">
				<tr class="task-row parent" task-id="${task.id}" expanded="no">
					<td class="id"> ${task.id} </td>
					<td class="user-id"> ${task.userId} </td>
					<td class="status"> ${task.statusEn()} </td>
					<td class="percent-successful"> ${task.percentSuccessful()} </td>
					<td class="server-address"> ${task.serverAddress} </td>
					<td class="created-at"> ${task.createdAt.format('MM-dd-YYYY')} </td>
					<td class="updated-at"> ${task.updatedAt.format('MM-dd-YYYY')} </td>
					<td class="notes"> ${task.notes} </td>
					<td class="error"> ${task.error} </td>
					<td class="action" style="display: inline-block">
						<button class="btn rerun">Rerun</button>
						</td>
				</tr>
			</g:each>
	</tbody>
	</table>
</div>

</body>
</html>

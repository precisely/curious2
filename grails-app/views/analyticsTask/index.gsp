<!html>
<head>
	<title>Admin Page for Analytics Jobs</title>
	<script type="text/javascript" src="/js/jquery/jquery-1.7.2.min.js"></script>
	<script src="/js/mustache.js"></script>
	<script src="/js/curious/analyticsTask.js"></script>
</head>
<body class="admin-analytics-job">
	<button name="Run on all users" id="start-analytics-job-button" value="run-all" >Run on all users</button>
	<p id="start-analytics-job-status"></p>

<h2> Available Servers </h2>
<table id="available-server-table">
	<tr>
		<th>server</th>
		<th>status</th>
		<th></th>
	</tr>
<g:each in="${servers}" var="server">
	<tr server="${server}">
	<td>${server}</td>
	<td class="server-status">SHUTDOWN</td>
	<td><button class="stop-job-button">STOP JOB</button></td>
	</tr>
</g:each>
</table>

<h2> Jobs </h2>
<table>
	<tr>
		<th>id</th>
		<th>parentId</th>
		<th>type</th>
		<th>status</th>
		<th>server</th>
		<th>created</th>
		<th>updated</th>
		<th>notes</th>
		<th>error</th>
		<th>last userId</th>
	</tr>
 <g:each in="${analyticsTasks}" var="task">
	<tr>
		<td> ${task.id} </td>
		<td> ${task.parentId} </td>
		<td> ${task.type} </td>
		<td> ${task.status} </td>
		<td> ${task.serverAddress} </td>
		<td> ${task.createdAt} </td>
		<td> ${task.updatedAt} </td>
		<td> ${task.notes} </td>
		<td> ${task.error} </td>
		<td> ${task.userId} </td>
	</tr>
</g:each>
</body>
</table>
</html>

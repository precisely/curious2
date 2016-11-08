<%@ page import="us.wearecurio.model.User" %>
<!DOCTYPE html>
<html>
<head>
	<meta name="layout" content="menu">
	<title>Curious Third Party Data Dumps</title>
</head>
<body>
<div class="row red-header">
	<div>
		<h1 class="clearfix">
			Dump Files
		</h1>
	</div>
</div>
<div class="main container-fluid survey-factory">
	<table class="table table-bordered table-hover table-striped">
		<thead>
		<tr>
			<g:sortableColumn property="dumpfile" title="File Name" />

			<g:sortableColumn property="size" title="File Size(MBs)" />

			<g:sortableColumn property="uploaded" title="uploaded" />

			<g:sortableColumn property="status" title="Status" />

			<g:sortableColumn property="id" title="Owner" />

		</tr>
		</thead>
		<tbody>
		<g:each in="${dumpFileInstances}" var="dumpFileInstance">
			<tr>
				<td>
					<a href="${dumpFileInstance.dumpFile.path}">
						<i class="fa fa-download" aria-hidden="true"></i> ${dumpFileInstances.dumpFile.name}
					</a>
				</td>
				<td>
					${Math.round((dumpFileInstance.dumpFile.size / (1024 * 1024)) * 100.0) / 100.0}
				</td>
				<td>
					<g:formatDate format="dd MMM yyyy, hh:mm a" date="${dumpFileInstance.timeStamp}"/>
				</td>
				<td>
					${dumpFileInstance.status}
				</td>
				<td>
					${us.wearecurio.model.User.get(dumpFileInstance.userId).username}
				</td>
			</tr>
		</g:each>
		</tbody>
	</table>
</div>
</body>
</html>

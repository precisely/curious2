
<%@ page import="us.wearecurio.model.SharedTagGroup" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<title>Shared Tag Groups</title>
	</head>
	<body>
		<h1 class="page-header">Shared Tag Groups</h1>
		<small>
			<g:link action="create">Create New</g:link>
		</small>
		<table class="table table-bordered">
			<thead>
				<tr>
					<g:sortableColumn property="description" title="${message(code: 'sharedTagGroup.description.label', default: 'Description')}" />
					<th><g:message code="sharedTagGroup.parentTagGroup.label" default="Parent Tag Group" /></th>
					<th>Associated Group</th>
				</tr>
			</thead>
			<tbody>
			<g:each in="${sharedTagGroupInstanceList}" var="sharedTagGroupInstance">
				<tr>
					<td>
						${fieldValue(bean: sharedTagGroupInstance, field: "description")}
					</td>
					<td>${fieldValue(bean: sharedTagGroupInstance, field: "parentTagGroup")}</td>
					<td>${sharedTagGroupInstance.associatedGroups*.name.join(', ') }</td>
				</tr>
			</g:each>
			</tbody>
		</table>
		<div class="pagination">
			<g:paginate total="${sharedTagGroupInstanceTotal}" />
		</div>
	</body>
</html>

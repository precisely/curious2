
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
					<g:sortableColumn property="editorId" title="${message(code: 'sharedTagGroup.editorId.label', default: 'Editor Id')}" />
					<th><g:message code="sharedTagGroup.parentTagGroup.label" default="Parent Tag Group" /></th>
				</tr>
			</thead>
			<tbody>
			<g:each in="${sharedTagGroupInstanceList}" var="sharedTagGroupInstance">
				<tr>
					<td>
						<g:link action="edit" id="${sharedTagGroupInstance.id}">${fieldValue(bean: sharedTagGroupInstance, field: "description")}</g:link>
						[${sharedTagGroupInstance.name}]
					</td>
					<td>${fieldValue(bean: sharedTagGroupInstance, field: "editorId")}</td>
					<td>${fieldValue(bean: sharedTagGroupInstance, field: "parentTagGroup")}</td>
				</tr>
			</g:each>
			</tbody>
		</table>
		<div class="pagination">
			<g:paginate total="${sharedTagGroupInstanceTotal}" />
		</div>
	</body>
</html>

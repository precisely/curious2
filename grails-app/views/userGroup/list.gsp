<%@ page import="us.wearecurio.model.UserGroup" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>User Groups</title>
	</head>
	<body>
		<h1 class="page-header">User Groups</h1>
		<small>
			<g:link action="create">Create New</g:link>
		</small>
		<table class="table table-bordered">
			<thead>
				<tr>
					<g:sortableColumn property="name" title="${message(code: 'userGroup.name.label', default: 'Name')}" />
					<g:sortableColumn property="fullName" title="${message(code: 'userGroup.fullName.label', default: 'Full Name')}" />
					<g:sortableColumn property="description" title="${message(code: 'userGroup.description.label', default: 'Description')}" />
					<g:sortableColumn property="created" title="${message(code: 'userGroup.created.label', default: 'Created')}" />
					<g:sortableColumn property="defaultNotify" title="${message(code: 'userGroup.defaultNotify.label', default: 'Default Notify')}" />
					<g:sortableColumn property="isModerated" title="${message(code: 'userGroup.isModerated.label', default: 'Is Moderated')}" />
				</tr>
			</thead>
			<tbody>
			<g:each in="${userGroupInstanceList}" var="userGroupInstance">
				<tr>
					<td><g:link action="edit" id="${userGroupInstance.id}">${fieldValue(bean: userGroupInstance, field: "name")}</g:link></td>
					<td>${fieldValue(bean: userGroupInstance, field: "fullName")}</td>
					<td>${fieldValue(bean: userGroupInstance, field: "description")}</td>
					<td><g:formatDate date="${userGroupInstance.created}" /></td>
					<td><g:formatBoolean boolean="${userGroupInstance.defaultNotify}" /></td>
					<td><g:formatBoolean boolean="${userGroupInstance.isModerated}" /></td>
				</tr>
			</g:each>
			</tbody>
		</table>
		<div class="pagination">
			<g:paginate total="${userGroupInstanceTotal}" />
		</div>
	</body>
</html>

<%@ page import="us.wearecurio.model.SharedTagGroup" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'sharedTagGroup.label', default: 'SharedTagGroup')}" />
		<title><g:message code="default.edit.label" args="[entityName]" /></title>
	</head>
	<body>
		<h1 class="page-header"><g:message code="default.edit.label" args="[entityName]" /></h1>
		<g:form method="post">
			<g:hiddenField name="id" value="${sharedTagGroupInstance?.id}" />
			<g:hiddenField name="version" value="${sharedTagGroupInstance?.version}" />
			<fieldset class="form">
				<div class="row">
					<div class="col-sm-6">
						<g:render template="form"/>
					</div>
				</div>
				<g:actionSubmit class="btn btn-primary" action="update" value="${message(code: 'default.button.update.label')}" />
				<g:actionSubmit class="btn btn-danger" action="delete" value="${message(code: 'default.button.delete.label')}" formnovalidate="" onclick="return confirm('${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" />
			</fieldset>
		</g:form>
	</body>
</html>
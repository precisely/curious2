<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'userGroup.label')}" />
		<title><g:message code="default.edit.label" args="[entityName]" /></title>
	</head>
	<body>
		<div class="page-header">
			<h1><g:message code="default.edit.label" args="[entityName]" /></h1>
		</div>
		<g:form method="post">
			<g:hiddenField name="id" value="${userGroupInstance.id}" />
			<fieldset class="form">
				<div class="row">
					<div class="col-sm-6">
						<g:render template="form"/>
					</div>
				</div>
				<g:actionSubmit action="createOrUpdate" value="${message(code: 'default.button.update.label')}" />
				<g:actionSubmit class="delete" action="delete" value="${message(code: 'default.button.delete.label')}"
					formnovalidate="" onclick="return confirm('${message(code: 'default.button.delete.confirm.message')}');" />
			</fieldset>
		</g:form>
	</body>
</html>

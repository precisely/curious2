<%@ page import="us.wearecurio.model.UserGroup" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'userGroup.label', default: 'UserGroup')}" />
		<title><g:message code="default.create.label" args="[entityName]" /></title>
	</head>
	<body>
		<div class="page-header">
			<h1><g:message code="default.create.label" args="[entityName]" /></h1>
		</div>
		<g:form action="save">
			<fieldset>
				<div class="row">
					<div class="col-sm-6">
						<g:render template="form"/>
					</div>
				</div>
				<g:submitButton name="create" class="save" value="${message(code: 'default.button.create.label')}" />
			</fieldset>
		</g:form>
	</body>
</html>

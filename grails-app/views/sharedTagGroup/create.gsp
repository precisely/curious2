<%@ page import="us.wearecurio.model.SharedTagGroup" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<title>Create Shared Tag Group</title>
	</head>
	<body>
		<h1 class="page-header">Create Shared Tag Group</h1>
		<g:form action="save">
			<fieldset class="form">
				<div class="row">
					<div class="col-sm-6">
						<g:render template="form"/>
					</div>
				</div>
				<g:submitButton name="create" class="btn btn-default" value="${message(code: 'default.button.create.label')}" />
			</fieldset>
		</g:form>
	</body>
</html>

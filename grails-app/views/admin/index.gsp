<%@ page import="us.wearecurio.model.UpdateSubscription" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="menu">
		<title>User Subscription</title>
		<script type="text/javascript">
            $(document).ready(function() {
                $("#delete").click(function() {
                	var id = $(this).val();
                	queueJSON("adding user subscription", '/admin/deleteSubscription?id=' + id);
                    location.reload();
                });
            });
		</script>
	</head>
	<body>
		<h1 class="page-header">User Subscription</h1>
		<table class="table table-bordered">
			<thead>
				<tr>
					<g:sortableColumn property="Email" title="${message(code: 'userGroup.name.label', default: 'Email')}" />
					<g:sortableColumn property="Categories" title="${message(code: 'userGroup.fullName.label', default: 'Categories')}" />
					<g:sortableColumn property="Description" title="${message(code: 'userGroup.description.label', default: 'Description')}" />
					<g:sortableColumn property="Action" title="${message(code: 'userGroup.description.label', default: 'Action')}" />
				</tr>
			</thead>
			<tbody>
			<g:each in="${subscriptionList}" var="userGroupInstance">
				<tr>
					<td>${fieldValue(bean: userGroupInstance, field: "email")}</td>
					<td>${fieldValue(bean: userGroupInstance, field: "categories")}</td>
					<td>${fieldValue(bean: userGroupInstance, field: "description")}</td>
					<td><button id='delete' value = '${userGroupInstance.id}'><i class="fa fa-trash-o delete-action" aria-hidden="true"></i></button></td>
				</tr>
			</g:each>
			</tbody>
		</table>
		<div class="pagination">
			<g:paginate total="${detailInstanceTotal}" />
		</div>
		<div class="form-group">
			<g:link controller='admin' action='download' ><input class="btn" type="button" value="Export CSV file" ></g:link>
		</div>
	</body>
</html>

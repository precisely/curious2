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
					queueJSON("adding user subscription", '/admin/deleteSubscription?id=' + id +"&callback=?",
								function(data) {
									if(data.success) {
										var info = "Subscription deleted successfully"
										setTimeout(function() {
											location.reload();
										},1000);
									} else {
										var info = "Subscription not deleted"
									}
									showAlert(info);
								}.bind(this)
					)
				});
			});
		</script>
	</head>
	<body>
		<h1 class="page-header">User Subscription</h1>
		<table class="table table-bordered">
			<thead>
				<tr>
					<td><b>Email</b></td>
					<td><b>Categories</b></td>
					<td><b>Description</b></td>
					<td><b>Action</b></td>
				</tr>
			</thead>
			<tbody>
			<g:each in="${subscriptionList}" var="userGroupInstance">
				<tr>
					<td>${fieldValue(bean: userGroupInstance, field: "email")}</td>
					<td>${fieldValue(bean: userGroupInstance, field: "categories")}</td>
					<td>${fieldValue(bean: userGroupInstance, field: "description")}</td>
					<td><button id='delete' value = '${userGroupInstance.id}' style="background-color: #fff"><i class="fa fa-trash-o delete-action" aria-hidden="true"></i></button></td>
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

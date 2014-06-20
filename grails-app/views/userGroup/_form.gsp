<%@ page import="us.wearecurio.model.UserGroup" %>

<g:render template="/layouts/renderError" model="[instances: [userGroupInstance]]" />
<div class="form-group ${hasErrors(bean: userGroupInstance, field: 'name', 'has-error')} ">
	<label for="name">
		<g:message code="userGroup.name.label" default="Name" />
	</label>
	<g:textField name="name" value="${userGroupInstance.name}" class="form-control" autofocus="" />
</div>

<div class="form-group ${hasErrors(bean: userGroupInstance, field: 'fullName', 'has-error')} ">
	<label for="fullName">
		<g:message code="userGroup.fullName.label" default="Full Name" />
	</label>
	<g:textField name="fullName" value="${userGroupInstance.fullName}" class="form-control" />
</div>

<div class="form-group ${hasErrors(bean: userGroupInstance, field: 'description', 'has-error')} ">
	<label for="description">
		<g:message code="userGroup.description.label" default="Description" />
	</label>
	<g:textField name="description" value="${userGroupInstance.description}" class="form-control" />
</div>

<div class="checkbox">
	<label>
		<g:checkBox name="defaultNotify" value="${userGroupInstance.defaultNotify}" />
		<g:message code="userGroup.defaultNotify.label" default="Default Notify" />
	</label>
</div>

<div class="checkbox">
	<label>
		<g:checkBox name="isModerated" value="${userGroupInstance.isModerated}" />
		<g:message code="userGroup.isModerated.label" default="Is Moderated" />
	</label>
</div>

<div class="checkbox">
	<label>
		<g:checkBox name="isOpen" value="${userGroupInstance.isOpen}" />
		<g:message code="userGroup.isOpen.label" default="Is Open" />
	</label>
</div>

<div class="checkbox">
	<label>
		<g:checkBox name="isReadOnly" value="${userGroupInstance.isReadOnly}" />
		<g:message code="userGroup.isReadOnly.label" default="Is Read Only" />
	</label>
</div>
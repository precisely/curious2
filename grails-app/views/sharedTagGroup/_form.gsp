<%@ page import="us.wearecurio.model.*" %>

<g:render template="/layouts/renderError" model="[instances: [sharedTagGroupInstance]]" />

<div class="form-group ${hasErrors(bean: sharedTagGroupInstance, field: 'description', 'has-error')} ">
	<label for="description">
		<g:message code="sharedTagGroup.description.label" default="Description" />
	</label>
	<g:textField name="description" class="form-control" maxlength="100" value="${sharedTagGroupInstance?.description}"
		autofocus="" required="" />
</div>

<div class="form-group ${hasErrors(bean: sharedTagGroupInstance, field: 'name', 'has-error')} ">
	<label for="name">
		<g:message code="sharedTagGroup.name.label" default="Name" /> (Short Name)
	</label>
	<g:textField name="name" class="form-control" value="${sharedTagGroupInstance?.name}" required="" />
</div>

<div class="form-group">
	<label for="userId">
		User (Either user group or user is required)
	</label>
	<g:select name="userId" from="${User.list()}" optionKey="id" optionValue="name"
		class="form-control" noSelection="['':'Select User']" />
</div>

<div class="form-group">
	<label for="groupId">
		User Group (Either user group or user is required)
	</label>
	<g:select name="groupId" from="${UserGroup.list()}" class="form-control" noSelection="['':'Select Group']"
		optionKey="id" optionValue="fullName" />
</div>
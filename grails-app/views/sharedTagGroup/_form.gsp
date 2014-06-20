<%@ page import="us.wearecurio.model.*" %>

<g:render template="/layouts/renderError" model="[instances: [sharedTagGroupInstance]]" />

<div class="form-group ${hasErrors(bean: sharedTagGroupInstance, field: 'description', 'has-error')} ">
	<label for="description">
		<g:message code="sharedTagGroup.description.label" default="Description" />
	</label>
	<g:textField name="description" class="form-control" maxlength="100" value="${sharedTagGroupInstance?.description}"
		autofocus="" />
</div>

<div class="form-group ${hasErrors(bean: sharedTagGroupInstance, field: 'name', 'has-error')} ">
	<label for="name">
		<g:message code="sharedTagGroup.name.label" default="Name" /> (Short Name)
	</label>
	<g:textField name="name" class="form-control" value="${sharedTagGroupInstance?.name}" />
</div>

<div class="form-group ${hasErrors(bean: sharedTagGroupInstance, field: 'editorId', 'has-error')}">
	<label for="editorId">
		<g:message code="sharedTagGroup.editorId.label" default="Editor Id" />
	</label>
	<g:select name="editorId" from="${UserGroup.list()}" class="form-control" noSelection="['':'Select Editor']"
		optionKey="id" required="" optionValue="fullName" value="${sharedTagGroupInstance?.editorId}" />
</div>

<div class="form-group ${hasErrors(bean: sharedTagGroupInstance, field: 'parentTagGroup', 'has-error')}">
	<label for="parentTagGroup">
		<g:message code="sharedTagGroup.parentTagGroup.label" default="Parent Tag Group" />
	</label>
	<g:select name="parentTagGroup.id" from="${GenericTagGroup.list()}"
		optionKey="id" optionValue="description" value="${sharedTagGroupInstance?.parentTagGroup?.id}"
		class="form-control" noSelection="['':'Select Parent']" />
</div>
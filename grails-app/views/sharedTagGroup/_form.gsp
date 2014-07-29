<%@ page import="us.wearecurio.model.*" %>

<g:render template="/layouts/renderError" model="[instances: [sharedTagGroupInstance]]" />

<div class="form-group ${hasErrors(bean: sharedTagGroupInstance, field: 'description', 'has-error')} ">
	<label for="description">
		<g:message code="sharedTagGroup.description.label" default="Description" />
	</label>
	<g:textField name="description" class="form-control" maxlength="100" value="${sharedTagGroupInstance?.description}"
		autofocus="" required="" />
</div>

<div class="form-group">
	<label for="groupId">
		User Group
	</label>
	<g:select name="groupId" from="${UserGroup.list()}" class="form-control" noSelection="['':'Select Group']"
		optionKey="id" optionValue="fullName" required="" value="${sharedTagGroupInstance?.associatedGroup()?.id}" />
</div>
<%@ page import="us.wearecurio.model.survey.AnswerType" %>
<div class="main container-fluid survey-factory survey-factory-container margin-bottom" id="answerListContainer">
	<table class="table table-bordered table-hover table-striped remove-margin-bottom margin-top">
		<thead>
			<tr>
				<th>Answer</th>
				<th>Priority</th>
				<th>AssociatedProfileTags</th>
				<th>AssociatedTrackingTags</th>
				<g:if test="${!isViewOnly}">
					<th>Actions</th>
				</g:if>
			</tr>
		</thead>
		<tbody id="answerInputAffordance">
			<g:each in="${questionInstance.answers}" var="answerInstance" status="index">
				<tr id="answerRow${index}">
					<g:if test="${!isViewOnly}">
						<td id="answerId${index}" class="hidden">${answerInstance.id}</td>
						<td><textarea id="answerText${index}" class="answer-input" maxlength="1000" required>${answerInstance.answer}</textarea></td>
						<td style="width: 10px"><input id="priorityNumber${index}" class="answer-input" type="number" required min="0" value="${answerInstance.priority}"></td>

						<td class="hidden" id="profileTagsData${index}">
							<g:each in="${answerInstance.associatedProfileTags}" var="tagsInstance">
								${tagsInstance.description},
							</g:each>
						</td>
						<td width="300px"><input type="text" id="profileTags${index}" placeholder="Add
								profile tags here..." name="associatedProfileTags" class="answer-input
								associated-profile-tags">
						</td>

						<td class="hidden" id="trackingTagsData${index}">
							<g:each in="${answerInstance.associatedTrackingTags}" var="tagsInstance">
								${tagsInstance.description},
							</g:each>
						</td>
						<td width="300px"><input type="text" id="trackingTags${index}" placeholder="Add
								tracking tags here..." name="associatedTrackingTags" class="answer-input
								associated-tracking-tags">
						</td>

						<td style="width: 10px">
							<a href="#" class="margin-left" data-toggle="tooltip" title="Delete Answer"
									data-placement="top">
								<i class="fa fa-trash action-icon" onclick="deleteAnswer(${index}, ${answerInstance.id})"></i>
							</a>
						</td>
					</g:if>
					<g:else>
						<td id="answerId${index}" class="hidden">${answerInstance.id}</td>
						<td id="answerText${index}">${answerInstance.answer}</td>
						<td id="priorityNumber${index}">${answerInstance.priority}</td>
						<td id="profileTags${index}"><g:each in="${answerInstance.associatedProfileTags}" var="tagsInstance">
							${tagsInstance.description},
						</g:each> </td>
						<td id="trackingTags${index}"><g:each in="${answerInstance.associatedTrackingTags}" var="tagsInstance">
							${tagsInstance.description},
						</g:each> </td>
					</g:else>
				</tr>
			</g:each>
		</tbody>
	</table>
</div>
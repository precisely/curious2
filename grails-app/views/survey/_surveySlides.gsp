<%@ page import="us.wearecurio.model.survey.AnswerType" %>
<input hidden name="surveyCode" value="${surveyCode}">
<g:each in="${questions}" var="questionInstance" status="index">
	<div class="item ${(index == 0) ? 'active' : ''}">

		<div class="section">QUESTION</div>

		<p>${questionInstance.question}</p>

		<input hidden name="questionId${index}" value="${questionInstance.id}">

		<g:if test="${questionInstance.answerType.isMCQType()}">
			<div class="section">
				SELECT ANSWER
			</div>
			<g:if test="${questionInstance.answerType == AnswerType.MCQ_RADIO}">
				<g:each in="${questionInstance.answers}" var="answerInstance" status="answerIndex">
					<input hidden name="answerId${index}${answerIndex}" value="${answerInstance.id}">
					<input type="radio" name="answerText${index}${answerIndex}" id="answer-${answerInstance.id}"
							value="${answerInstance.answer}" ${questionInstance.isRequired ? 'required' : ''}>
					<label for="answer-${answerInstance.id}" class="radio-label">
						${answerInstance.answer}
					</label>
				</g:each>
			</g:if>
			<g:else>
				<g:each in="${questionInstance.answers}" var="answerInstance" status="answerIndex">
					<input hidden name="answerId${index}${answerIndex}" value="${answerInstance.id}">
					<span class="checkbox-orange checkbox-sm survey-answer-checkbox">
						<input type="checkbox" name="answerText${index}${answerIndex}" id="answer-${answerInstance.id}"
								value="${answerInstance.answer}" ${questionInstance.isRequired ? 'required' : ''}>
						<label for="answer-${answerInstance.id}"></label>
						<span class="survey-answer-checkbox-label">${answerInstance.answer}</span>
					</span>
				</g:each>
			</g:else>
		</g:if>
		<g:else>
			<div>
				<textarea name="answerText${index}" id="descriptiveArea" maxlength="2000" ${questionInstance.isRequired ? 'required' : ''}/>>
				</textarea>
			</div>
		</g:else>

	</div>
</g:each>
<script type = "text/javascript">
	$("input[type=radio]").click(function(){
		$("input[type=radio]").attr('checked', false);
		$(this).attr('checked', true);
	});
</script>
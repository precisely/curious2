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
				<textarea name="answerText${index}" ${questionInstance.isRequired ? 'required' : ''}></textarea>
			</div>
		</g:else>

	</div>
</g:each>
<div class="item ${(questions.size() == 0) ? 'active' : ''}">
	<div class="form-group">
		<div class="section">Type some interesting bio tags here:</div>
		<div>
			<input type="text" id="interestTagInput"/>
			<ol id="interestTagList"></ol>
		</div>
	</div>
</div>
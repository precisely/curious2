<g:set var="counter" value="${1}" />
<g:each in="${questions }" var="questionInstance">
	<g:if test="${counter == 1}">
		<div class="item active">
	</g:if>
	<g:else>
		<div class="item">
	</g:else>
		<div class="section">QUESTION:</div>
		<p>${questionInstance.question }</p>
		<g:if test="${questionInstance.possibleAnswers[0].answerType.value() == 1}">
			<div class="section">SELECT AN ANSWER</div>
			<g:each in="${questionInstance.possibleAnswers}" var="answerInstance">
					<input type="radio" name="answer.${questionInstance.code }" 
						id="answer-${answerInstance.id}" value="${answerInstance.code}">
					<label for="answer-${answerInstance.id}" class="radio-label">${answerInstance.answer}</label>
			</g:each>
		</g:if>
		<g:else>
			<div class="">
				<textarea id="${questionInstance.code }" name="answer.${questionInstance.code }"
					value="" ></textarea>
			</div>
		</g:else>
	</div>
	<g:set var="counter" value="${counter + 1}" />
</g:each>
<g:if test="${counter == 1}">
	<div class="item active">
</g:if>
<g:else>
	<div class="item">
</g:else>
	<div class="form-group">
		<div class="section" for="interests">Type some interesting bio tags here:</div>
		<div class="">
			<input type="text" id="interestTagInput" name="answer.data"
				value="" />
			<ol id="interestTagList"></ol>
		</div>
	</div>
</div>
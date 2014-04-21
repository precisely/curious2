<%
	/*
	 *	* flash.messageType- set this to display different type of alert blocks, eg.- success/danger/info/warn etc.
	 *	* flash.timeout- set the time in milli seconds to hide the alert block. Default: 10000 milli seconds. 
	 */
 %>

<g:set var="classes" value="${flash.messageType ? 'alert-' + flash.messageType : 'alert-info' } ${flash.message ? '' : 'hide' }" />
<div id="alert-message" class="alert ${classes }">
	<button type="button" class="close" onclick="$(this).parent().fadeOut()">&times;</button>
	<strong><%=flash.message %></strong>
</div>

<g:if test="${flash.message && (!flash.timeout || (flash.timeout && flash.timeout != 'clear')) }">
	<script>
		setTimeout(function() {
			$('div#alert-message').fadeOut();
		}, ${flash.timeout ?: '10000' });
	</script>
</g:if>
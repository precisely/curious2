<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />
<c:jsCSRFToken keys="getPeopleDataCSRF" />
<script type="text/javascript">

function deleteDiscussionId(id) {
	showYesNo("Are you sure you want to delete the saved discussion?", function() {
			backgroundJSON("deleting discussion", "/home/deleteDiscussionId?id=" + escape(id) + "&callback=?",
				function(entries) {
					if (checkData(entries))
						location.reload(true);
			});
	});
}

function doLogout() {
	callLogoutCallbacks();
}

function refreshPage() {
}

$(function() {
	queueJSON("getting login info", "/home/getPeopleData?callback=?",
			getCSRFPreventionObject("getPeopleDataCSRF"),
			function(data){
				if (!checkData(data))
					return;

			var found = false;

			jQuery.each(data, function() {
				if (!found) {
					// set first user id as the current
					setUserId(this['id']);
					found = true;
				}
				addPerson(this['first'] + ' ' + this['last'],
						this['username'], this['id'], this['sex']);
				return true;
			});
		});
});

</script>

<script src="/js/mustache.js"></script>
</head>

<body class="signals">
<!-- preload circle images //-->
<img src="/images/signals/empty_circle.png" class="nodisplay">
<img src="/images/signals/filled_circle.png" class="nodisplay">
<img src="/images/signals/hover_circle.png" class="nodisplay">

<div class="main container-fluid" >

<div id="tooltip-title-graph" class="nodisplay">View the Graph of these two tags</div>
<div id="tooltip-body-graph" class="nodisplay">View the time series graph of these tags.</div>
<div id="tooltip-go-graph" class="nodisplay">View graph</div>

<div id="tooltip-title-noise" class="nodisplay">Mark this interaction as noise.</div>
<div id="tooltip-body-noise" class="nodisplay">This interaction is not interesting.  It's probably a false positive.	Maybe it's just an artifact of the computation, the experimental set-up, noisy data, or possibly an outlier.	Regard this interaction of tag pairs as noise.</div>
<div id="tooltip-go-noise" class="nodisplay">Mark as noise</div>

<div id="tooltip-title-save" class="nodisplay">Save this interaction</div>
<div id="tooltip-body-save" class="nodisplay">This is interesting.	There could be something here.	Save this interaction for further investigation.</div>
<div id="tooltip-go-save" class="nodisplay">Save</div>


<img id="garbage-can-icon" src="/images/signals/garbage-bin.png" >

<div class="arrow-box">
	<h3 class='tooltip-title'>Reviewing your signal</h3>
	<p class='tooltip-body'>At vero eos et accusamus et iusto odio dignissimos corrupti quos dolores et quas molestias id est laborum et dolorum fuga.</p>
	<p class="buttons">
		<button type="button" class="btn btn-primary maybe-later-button">Maybe later</button>
		<button type="button" class="btn btn-primary tooltip-action-button">Next</button>
	</p>
</div>

<!-- MAIN -->
<div class="container-fluid" >
	<g:if test="${flash.message}">
		<div class="communityMessage">${flash.message.encodeAsHTML()}</div>
	</g:if>

	<div class="red-header"></div>

	<div id="correlation-container">

		<script id="correlation-template" type="x-tmpl-mustache">
			<div class="row signal-row signal-row-top" type="{{type}}" data-id="{{id}}">
				<div class="col-md-3 signal-name">
					<img src="/images/signals/{{type}}.png?v=3" width="150"/>
					<p class="signal-category"> ({{label}}) </p>
				</div>
				<div class="col-md-9 signal-description">

					<p>
						<span class="signal-section-question">Is {{description1}} {{relation_in_english}} {{description2}}?</span>
					</p>
					<br>
					<p>
						<span class="signal-section-title">SCORE:</span>
						<span class="signal-section-details">{{score}}</span>
					</p>
				</div>
			</div>
			<div class="row signal-row signal-row-bottom" type="{{type}}" data-id="{{id}}">
				<div class="col-md-3"></div>
				<div class="col-md-9 signal-section-mark-noise-or-signal">
					NOISE
					<img class="bubble" signal-level="0" src="/images/signals/{{bubble_0}}_circle.png?v=2" >
					<img class="bubble" signal-level="1" src="/images/signals/{{bubble_1}}_circle.png?v=2" >
					<img class="bubble" signal-level="2" src="/images/signals/{{bubble_2}}_circle.png?v=2" >
					<img class="bubble" signal-level="3" src="/images/signals/{{bubble_3}}_circle.png?v=2" >
					<img class="bubble" signal-level="4" src="/images/signals/{{bubble_4}}_circle.png?v=2" >
					SIGNAL
				</div>
			</div>
		</script>

	</div>
	<!-- id='correlation-container' //-->
</div>
<!-- /MAIN -->
</div>

<div style="clear: both;"></div>

<footer>
</footer>


</body>
</html>

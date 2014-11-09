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

	<div class="row">
		<div class="col-md-2 signal-name">
			<span class="saved-action">Saved</span>
			<!-- Preload the toggle image //-->
			<img src="/images/signals/arrow-right-sm.png" class="nodisplay">
			<img src="/images/signals/arrow-down-sm.png" class="toggle-saved toggle-arrow">
		</div>
	</div>
	<table class="row saved-carousel-layout">
		<tr>
			<td width='30' > <div class="arrow arrow-left"></div>  </td>
			<td class='nav-carousel-container'>
				<div id="secret-container">

					<script id="saved-item-template" type="x-tmpl-mustache">
						<div class="nav-box" data-id="{{id}}">
							<img class="nav-box-icon {{type}}-icon" src="/images/signals/{{type}}.png"/>
							<div class="carousel-caption">
								<span class="carousel-item-title">{{type}}:</span>
								<span class="carousel-item-body">
									Score: {{score}}<br>
									<a href="/home/graph/signals/{{description1}}/{{description2}}">{{description1}} X {{description2}}</a>
								<span>
							</div>
						</div>
					</script>

				</div> <!--- secret-container //-->

			</td>
			<td width='30' > <div class="arrow arrow-right"></div> </td>
		</tr>
	</table>

	<div class="red-header"></div>

	<div id="correlation-container">

		<script id="correlation-template" type="x-tmpl-mustache">
			<div class="row signal-row" type="triggered" data-id="{{id}}">
				<div class="col-md-2 signal-name">
					<img src="/images/signals/{{type}}.png" />
					<h2 class="signal-category"> Event<br>{{type}} </h2>
				</div>
				<div class="col-md-8 signal-description">

					<p>
						<span class="signal-section-title">TAGS:</span>
						<span class="signal-section-details">Is {{description1}} {{relation_in_english}} {{description2}}?</span>
					</p>
					<p>
						<span class="signal-section-title">SCORE:</span>
						<span class="signal-section-details">{{score}}</span>
					</p>
				</div>
				<div class="col-md-2 signal-action">
					<div class="signal-action-button" action-name="View signal graph" data-action="graph">View Graph</div>
					<div class="signal-action-button" action-name="Mark as noise" data-action="noise">Mark as Noise</div>
					<div class="signal-action-button" action-name="Save signal" data-action="save" >Save</div>
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

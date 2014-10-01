<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="main-signals" />
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

$(function(){
	initTemplate();
	
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

$(document).ready(function() {
	$(document).on("click", "a.delete-discussion", function() {
		var $this = $(this);
		showYesNo('Are you sure want to delete this?', function() {
			var discussionId = $this.data('discussionId');
			$.ajax({
				url: '/home/discuss',
				data: {
					discussionId: discussionId,
					deleteDiscussion: true
				},
				success: function(data) {
					showAlert(JSON.parse(data).message, function() {
						$this.parents('.graphItem').fadeOut();
					});
				},
				error: function(xhr) {
					var data = JSON.parse(xhr.responseText);
					showAlert(data.message);
				}
			});
		});
		return false;
	});

	$(document).on("click", "ul#discussion-pagination a", function() {
		var url = $(this).attr('href');
		$.ajax({
			url: url,
			success: function(data) {
				$('div#discussions').html(data);
				wrapPagination();
			}
		});
		return false;
	});
});
</script>


</head>

<body class="signals">

<div id="tooltip-title-graph" class="nodisplay">View the Graph of these two tags</div>
<div id="tooltip-body-graph" class="nodisplay">View the time series graph of these tags.</div>
<div id="tooltip-go-graph" class="nodisplay">View graph</div>

<div id="tooltip-title-noise" class="nodisplay">Mark this interaction as noise.</div>
<div id="tooltip-body-noise" class="nodisplay">Not interesting.  This is probably a false positive.  Maybe it's just an artifact of the computation, the experimental set-up, noisy data, or possibly an outlier.  Either way, mark this tag interaction as noise.</div>
<div id="tooltip-go-noise" class="nodisplay">Mark as noise</div>

<div id="tooltip-title-save" class="nodisplay">Save this interaction</div>
<div id="tooltip-body-save" class="nodisplay">This is interesting.	There could be something here.	Save this interaction for further investigation.</div>
<div id="tooltip-go-save" class="nodisplay">Save</div>


<img class="garbage-can-icon" src="/images/signals/garbage-bin.png" >
<div class="arrow-box">
	<h3 class='tooltip-title'>Reviewing your signal</h3>
	<p class='tooltip-body'>At vero eos et accusamus et iusto odio dignissimos corrupti quos dolores et quas molestias id est laborum et dolorum fuga.</p>
	<p class="buttons">
		<button type="button" class="btn btn-primary maybe-later-button">Maybe later</button>
		<button type="button" class="btn btn-primary tooltip-go-button">Next</button>
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
			<img src="/images/signals/arrow-right-sm.png" style="display: none">
			<img src="/images/signals/arrow-down-sm.png" class="toggle-saved toggle-arrow">
		</div>
	</div>
	<table class="row saved-carousel-layout">
		<tr>
			<td width='30' > <div class="arrow arrow-left"></div>  </td>
			<td class='nav-carousel-container'>
				<div class="secret-container">
				<div class="nav-box" data-id="1">
					<img class="nav-box-icon comment-icon" src="/images/comment-icon.png"/>
					<div class="carousel-caption">
						<span class="carousel-item-title">Title:</span>
						<span class="carousel-item-body">
							This is the body of item 1.
						<span>
					</div>
				</div>
				<div class="nav-box" data-id="33">
					<img class="nav-box-icon" src="/images/graph-icon.png"/>
					<div class="carousel-caption">
						<span class="carousel-item-title">Graph name:</span>
						<span class="carousel-item-body">
							This is the body 2.
						<span>
					</div>
				</div>

				<div class="nav-box" data-id="152">
					<img class="nav-box-icon" src="/images/signals/2-hill-series-small.png"/>
					<div class="carousel-caption">
						<span class="carousel-item-title">Signal:</span>
						<span class="carousel-item-body">
							Here is a signal description.
						<span>
					</div>
				</div>

				<div class="nav-box" data-id="153">
					<img class="nav-box-icon" src="/images/signals/2-hill-series-small.png"/>
					<div class="carousel-caption">
						<span class="carousel-item-title">Signal:</span>
						<span class="carousel-item-body">
							Here is a signal description.
						<span>
					</div>
				</div>

				<div class="nav-box" data-id="154">
					<img class="nav-box-icon" src="/images/signals/2-hill-series-small.png"/>
					<div class="carousel-caption">
						<span class="carousel-item-title">Signal:</span>
						<span class="carousel-item-body">
							Here is a signal description.
						<span>
					</div>
				</div>

				<div class="nav-box" data-id="155">
					<img class="nav-box-icon" src="/images/signals/2-hill-series-small.png"/>
					<div class="carousel-caption">
						<span class="carousel-item-title">Signal:</span>
						<span class="carousel-item-body">
							Here is a signal description.
						<span>
					</div>
				</div>

				<div class="nav-box" data-id="156">
					<img class="nav-box-icon" src="/images/signals/2-hill-series-small.png"/>
					<div class="carousel-caption">
						<span class="carousel-item-title">Signal:</span>
						<span class="carousel-item-body">
							Here is a signal description.
						<span>
					</div>
				</div>

				<div class="nav-box" data-id="157">
					<img class="nav-box-icon" src="/images/signals/2-hill-series-small.png"/>
					<div class="carousel-caption">
						<span class="carousel-item-title">Signal:</span>
						<span class="carousel-item-body">
							Here is a signal description.
						<span>
					</div>
				</div>

				</div> <!--- secret-container //-->

			</td>
			<td width='30' > <div class="arrow arrow-right"></div> </td>
		</tr>
	</table>

	<div class="red-header"></div>

	<div class="row signal-row" type="pos">
		<div class="col-md-2 signal-name">
			<img src="/images/signals/2-hill-series-medium.png" />
			<h2 class="signal-category"> Proportional </h2>
		</div>
		<div class="col-md-8 signal-description">
			<p>
				<span class="signal-section-title">TAGS:</span>
				<span class="signal-section-details">Are sleep mood and anxiety proportional to each other?</span>
			</p>
			<p>
				<span class="signal-section-title">SPAN:</span>
				<span class="signal-section-details">3/2/2014 - 3/28/2014</span>
			</p>
		</div>
		<div class="col-md-2 signal-action">
			<div class="signal-action-button" data-action="graph">View Graph</div>
			<div class="signal-action-button" data-action="noise">Mark as Noise</div>
			<div class="signal-action-button" data-action="save" >Save</div>
		</div>
	</div>

	<div class="row signal-row" type="neg">
		<div class="col-md-2 signal-name">
			<img src="/images/signals/inversely-proportional.png" />
			<h2 class="signal-category"> Inversely<br>Proportional </h2>
		</div>
		<div class="col-md-8 signal-description">
			<p>
				<span class="signal-section-title">TAGS:</span>
				<span class="signal-section-details">Are mood level and dm inversely proportional to each other?</span>
			</p>
			<p>
				<span class="signal-section-title">SPAN:</span>
				<span class="signal-section-details">1/2/2014 - 3/18/2014</span>
			</p>
		</div>
		<div class="col-md-2 signal-action">
			<div class="signal-action-button" data-action="graph">View Graph</div>
			<div class="signal-action-button" data-action="noise">Mark as Noise</div>
			<div class="signal-action-button" data-action="save" >Save</div>
		</div>
	</div>

	<div class="row signal-row" type="trig">
		<div class="col-md-2 signal-name">
			<img src="/images/signals/event-triggered.png" />
			<h2 class="signal-category"> Event<br>Triggered </h2>
		</div>
		<div class="col-md-8 signal-description">

			<p>
				<span class="signal-section-title">TAGS:</span>
				<span class="signal-section-details">Is anxiety level triggered by gluten exposure?</span>
			</p>
			<p>
				<span class="signal-section-title">SPAN:</span>
				<span class="signal-section-details">1/2/2014 - 1/7/2014</span>
			</p>
		</div>
		<div class="col-md-2 signal-action">
			<div class="signal-action-button" data-action="graph">View Graph</div>
			<div class="signal-action-button" data-action="noise">Mark as Noise</div>
			<div class="signal-action-button" data-action="save" >Save</div>
		</div>
	</div>


</div>
<!-- /MAIN -->

<div style="clear: both;"></div>

<footer>
</footer>
</body>
</html>

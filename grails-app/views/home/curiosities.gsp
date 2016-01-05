<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
	<meta name="layout" content="menu" />
	<script type="text/javascript">
		function doLogout() {
			callLogoutCallbacks();
		}
		function refreshPage() {
		}
	</script>
	
	<script src="/js/mustache.js"></script>
</head>

<body class="curiosities">
<!-- preload circle images //-->
<img src="/images/curiosities/empty_circle.png" class="nodisplay">
<img src="/images/curiosities/marked_circle.png" class="nodisplay">
<img src="/images/curiosities/hover_circle.png?v=3" class="nodisplay">

<div class="main container-fluid" >

<div id="tooltip-title-graph" class="nodisplay">View the Graph of these two tags</div>
<div id="tooltip-body-graph" class="nodisplay">View the time series graph of these tags.</div>
<div id="tooltip-go-graph" class="nodisplay">View graph</div>

<div id="tooltip-title-noise" class="nodisplay">Mark this interaction as noise.</div>
<div id="tooltip-body-noise" class="nodisplay">This interaction is not interesting.  It's probably a false positive.	Maybe it's just an artifact of the computation, the experimental set-up, noisy data, or possibly an outlier.	Regard this interaction of tag pairs as uninteresting.</div>
<div id="tooltip-go-noise" class="nodisplay">Mark no</div>

<div id="tooltip-title-save" class="nodisplay">Save this interaction</div>
<div id="tooltip-body-save" class="nodisplay">This is interesting.	There could be something here.	Save this interaction for further investigation.</div>
<div id="tooltip-go-save" class="nodisplay">Rate</div>


<img id="garbage-can-icon" src="/images/curiosities/garbage-bin.png" >

<div class="arrow-box">
	<h3 class='tooltip-title'>Reviewing your curiosity</h3>
	<p class='tooltip-body'></p>
	<p class="buttons">
		<button type="button" class="btn btn-primary maybe-later-button">Maybe later</button>
		<button type="button" class="btn btn-primary tooltip-action-button">Next</button>
	</p>
</div>

<style>
	.curiosities nav {
		margin: 0;
	}
</style>

<!-- MAIN -->
<div class="container-fluid" >
	<g:if test="${flash.message}">
		<div class="communityMessage">${flash.message.encodeAsHTML()}</div>
	</g:if>

	<div class="filter-group">
		<nav class="red-header">
			<ul class="col-md-4 filter-area disable-select">
				&nbsp;
			</ul>
			<ul class="col-md-8 search-area disable-select" >
				<li>
						<input id='search-input' class="search-input disable-select"	type="text" name="curiosities-search" placeholder="Search Curiosities" onfocus="this.placeholder = ''" onblur="this.placeholder = 'Search Curiosities'"><input id='search-image' type="image" class="disable-select" src="/images/curiosities/1418886778_685082-Search-128.png" height="25" >
				</li>
			</ul>
		</nav>

		<nav class="row disable-select" id="sort-by-row" >
			<ul class="disable-select">
				<li id="natural" class="filter disable-select curiosities-order active" data-order="natural">Unrated</li>
				<li id="rated"  class="filter disable-select curiosities-order"				 data-order="rated">Rated</li>
				<li id="score"	 class="filter disable-select curiosities-order"				 data-order="all">All</li>
				<!-- <li id="type"		 class="filter disable-select curiosities-order"				 data-order="type positive">Type</li> //-->
			</ul>
		</nav>
	</div>

	<hr>

	<div id="correlation-container">

		<script id="correlation-template" type="x-tmpl-mustache">
			<div class="row curiosities-row-container" type="{{type}}" marked="{{marked}}" data-id="{{id}}" score="{{score}}" style="display: {{display}}">
				<div class="row curiosities-row curiosities-row-top">
					<div class="col-md-3 curiosities-name">
						<img src="/images/curiosities/{{type}}.png?v=5" width="150"/>
						<p class="curiosities-category"> ({{label}}) </p>
					</div>
					<div class="col-md-9 curiosities-description">
						<p>
							<span class="curiosities-section-question">{{relation_text}}?</span>
						</p>
						<br>
						<p>
							<span class="curiosities-section-title">STRENGTH:</span>
							<span class="curiosities-section-details">{{strength}} ({{score}})</span>
						</p>
					</div>
				</div>
				<div class="row curiosities-row curiosities-row-bottom">
					<div class="col-md-3"></div>
					<div class="col-md-9 curiosities-section-mark-no-or-yes"> NOISE
						<img class="bubble" marked="{{bubble_0}}" signal-level="0" src="/images/curiosities/{{bubble_0}}_circle.png?v=4" >
						<img class="bubble" marked="{{bubble_1}}" signal-level="1" src="/images/curiosities/{{bubble_1}}_circle.png?v=4" >
						<img class="bubble" marked="{{bubble_2}}" signal-level="2" src="/images/curiosities/{{bubble_2}}_circle.png?v=4" >
						<img class="bubble" marked="{{bubble_3}}" signal-level="3" src="/images/curiosities/{{bubble_3}}_circle.png?v=4" >
						<img class="bubble" marked="{{bubble_4}}" signal-level="4" src="/images/curiosities/{{bubble_4}}_circle.png?v=4" >
						SIGNAL
					</div>
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

<c:renderJSTemplate template="/curiosity/curiosityExplanation" id="_curiosityHelp" />
</body>
</html>

<g:applyLayout name="menu">
<html>
	<head>
	<title><g:layoutTitle/></title>
	<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>
	<script src="/js/jquery/jquery.mobile.custom.min.js"></script>
	<script type="text/javascript" src="/js/curious/feeds.js?ver=22"></script>
	<script type="text/javascript" src="/js/curious/discussion.js?ver=22"></script>
	<c:jsCSRFToken keys="deleteGhostEntryDataCSRF, deleteEntryDataCSRF, addEntryCSRF, getPeopleDataCSRF, getCommentsCSRF,
			getInterestTagsDataCSRF, addInterestTagDataCSRF, autocompleteDataCSRF, fetchSprintDataCSRF, createNewSprintDataCSRF, 
			deleteSprintDataCSRF, stopSprintDataCSRF, startSprintDataCSRF, addMemberToSprintDataCSRF, addAdminToSprintDataCSRF, 
			deleteSprintMemberDataCSRF, deleteSprintAdminDataCSRF, updateSprintDataCSRF, getAutocompleteParticipantsDataCSRF, 
			deleteDiscussionDataCSRF, getSearchResultsCSRF, getFeedsDataCSRF, createDiscussionDataCSRF, addCommentCSRF" />
	<g:layoutHead />
	</head>
	<body class="${pageProperty(name: 'body.class') ?: '' }">
	<!-- MAIN -->
		<div class="row red-header">
			<div>
				<h1 class="clearfix">
					<span id="queryTitle">${groupFullname}</span>
				</h1>
			</div>
			<div class="pull-right">
				<div class="help">
					<i class="fa fa-question"></i>
				</div>
			</div>
		</div>
		<div class="feed-body">
		<div class="main container-fluid">
		<ul class="nav nav-pills">
			<li id="feed-all-tab" role="presentation">
				<a href="/home/social#all">ALL</a>
			</li>
			<li id="feed-people-tab" role="presentation">
				<a href="/home/social#people">PEOPLE</a>
			</li>
			<li id="feed-discussions-tab" role="presentation">
				<a href="/home/social#discussions">DISCUSSIONS</a>
			</li>
			<li id="feed-sprints-tab" role="presentation">
				<a href="/home/social#sprints">SPRINTS</a>
			</li>
			<li id="feed-right-tab" role="presentation">
			<li>
		</ul>
			<div id="feed">
				<g:pageProperty name="page.feedContent" />
			</div>
		</div>
		</div>
		<!-- /MAIN -->
		<div style="clear: both;"></div>
		<div id="share-dialog" class="hide" title="Share">
			<select name="shareOptions" id="shareOptions" multiple="multiple"
				class="form-control" size="8">
				<option value="isPublic">Visible to the world</option>
				<g:each in="${associatedGroups }" var="userGroup">
					<option value="${userGroup.id }">
						${userGroup.fullName }
					</option>
				</g:each>
			</select>
		</div>
		<div id="comment-dialog" class="hide" title="Comment">
			<input type="text" name="comment" id="userComment" required placeholder="Add Comment..."> 
			<input type="hidden" name="discussionHash" value="${discussionHash}">
		</div>
		<g:render template="/sprint/createSprintModal" />

		<c:renderJSTemplate template="/discussion/create" id="_createDiscussionForm" />
		<c:renderJSTemplate template="/discussion/instance" id="_discussions" />
		<c:renderJSTemplate template="/discussionPost/instance" id="_comments" />
		<c:renderJSTemplate template="/people/instance" id="_people" />
		<c:renderJSTemplate template="/sprint/instance" id="_sprints" />
	</body>
</html>
</g:applyLayout>

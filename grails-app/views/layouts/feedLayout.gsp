<g:applyLayout name="main">
<html>
	<head>
	<title>Curious</title>
	<meta name="description" content="A platform for health hackers" />
	<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>
	<script src="/js/jquery/jquery.mobile.custom.min.js"></script>
	<c:jsCSRFToken keys="deleteGhostEntryDataCSRF, deleteEntryDataCSRF, addEntryCSRF, getPeopleDataCSRF, 
			getInterestTagsDataCSRF, addInterestTagDataCSRF, autocompleteDataCSRF, fetchSprintDataCSRF, createNewSprintDataCSRF, 
			deleteSprintDataCSRF, stopSprintDataCSRF, startSprintDataCSRF, addMemberToSprintDataCSRF, addAdminToSprintDataCSRF, 
			deleteSprintMemberDataCSRF, deleteSprintAdminDataCSRF, updateSprintDataCSRF, getAutocompleteParticipantsDataCSRF, 
			deleteDiscussionDataCSRF, getSearchResultsCSRF, getDiscussionsDataCSRF, getSprintsDataCSRF" />
	<g:layoutHead />
	</head>
	<body class="feed">
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
			<li id="feed-all-tab" role="presentation" class="active">
				<a href="#" data-toggle="pill">ALL</a>
			</li>
			<li id="feed-people-tab" role="presentation">
				<a href="#" data-toggle="pill">PEOPLE</a>
			</li>
			<li id="feed-discussions-tab" role="presentation">
				<a href="#discussions">DISCUSSIONS</a>
			</li>
			<li id="feed-sprints-tab" role="presentation">
				<a href="#sprints">SPRINTS</a>
			</li>
			<li id="feed-right-tab" role="presentation">
			<li>
		</ul>
		<%--<div class="left-menu">
				<ul>
					<li id="recentSearches"><a href="#">RECENT SEARCHES</a>
						<g:if test="${searchKeywords}">
							<ul>
								<g:each in="${searchKeywords}" var="searchInstance">
									<li>
										<a href="#">${searchInstance.searchString}</a> 
									</li>
								</g:each>
							</ul>
						</g:if>
					</li>
					<li id="myThreads">
						<a href="/home/feed?userId=${userId}">MY THREADS</a>
					</li>
					<li id="sprints">
						<a>SPRINTS</a>
						<button class="sprint-button" onclick="createSprint()">
							<span class="create-sprint-label">CREATE</span>
						</button>
						<g:if test="${sprintList}">
							<ul>
								<g:each in="${sprintList}" var="sprint">
									<li>
										<a href="/home/sprint/${sprint.id}">${sprint.name}</a> 
									</li>
								</g:each>
							</ul>
						</g:if>
					</li>
				</ul>
			</div>
			--%>
				<div id="graphList">
					<g:pageProperty name="page.feedContent" />
				</div>
				<div id="getMoreDiscussions"></div>
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
			<input type="hidden" name="discussionId" value="${discussionId}">
		</div>
		<g:render template="/sprint/createSprintModal" />
	</body>
</html>
</g:applyLayout>

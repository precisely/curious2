<g:applyLayout name="main">
<html>
	<head>
	<title>Curious</title>
	<meta name="description" content="A platform for health hackers" />
	<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>
	<script src="/js/jquery/jquery.mobile.custom.min.js"></script>
	<c:jsCSRFToken keys="deleteGhostEntryDataCSRF, deleteEntryDataCSRF, addEntryCSRF, getPeopleDataCSRF, 
	getInterestTagsDataCSRF, addInterestTagDataCSRF, autocompleteDataCSRF" />
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
				<div class="search-bar left-addon">
					<form onsubmit="searchFeeds()">
						<i class="fa fa-search"></i>
						<input type="text" id="searchFeed" placeholder="Search Feed" required >
						<input type="hidden" id="userId" value="${userId}">
					</form>
				</div>
			</div>
		</div>
		<div class="feed-body">
			<div class="left-menu">
				<ul>
					<li id="allFeeds"><g:link controller='home' action="feed">FEED</g:link></li>
					<li id="recentSearches"><a href="#">RECENT SEARCHES</a>
						<g:if test="${searchKeywords}">
							<ul>
								<g:each in="${searchKeywords}" var="searchInstance">
									<li><a href="#">${searchInstance.searchString}</a> </li>
								</g:each>
							</ul>
						</g:if>
					</li>
					<li id="myThreads">
						<a href="#" onclick="getMyThreads({usetId: ${userId}})">MY THREADS</a>
					</li>
					<li id="sprints">
						<a href="#">SPRINTS</a>
						<button id="createSprint" onclick="createSprint()">
							<span class="create-sprint-label">CREATE</span>
						</button>
						<g:if test="${sprintList}">
							<ul>
								<g:each in="${sprintList}" var="sprint">
									<li><a href="/dummy/sprint?id=${sprint.id}">${sprint.title}</a> </li>
								</g:each>
							</ul>
						</g:if>
					</li>
				</ul>
			</div>
			<div class="main container-fluid">
				<div id="graphList">
					<div class="new-post">
						<form id="create-discussion" action="/discussion/createTopic" method="post">
							<div class="input-affordance left-addon">
								<i class="fa fa-pencil"></i> <input class="full-width discussion-topic-input"
									type="text" placeholder="New question or discussion topic?"
									name="name" id="discussion-topic" required />
								<input type="radio" class="radio-public" name="visibility" id="public" value="public" checked><label for="public" class="radio-public-label">Public</label>
								<input type="radio" class="radio-private" name="visibility" id="private" value="private"><label for="private" class="radio-private-label">Private</label>
								<hr class="hide">
								<input type="text" id="discussion-discription" class="full-width discussion-topic-description hide" placeholder="Enter comment/description"
								name="discussionPost">
							</div>
							<input type="hidden" name="group" value="${groupName}" />
						</form>
					</div>
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

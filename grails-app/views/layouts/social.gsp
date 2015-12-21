<g:applyLayout name="plot">
<html>
	<head>
	<title><g:layoutTitle/></title>
	<meta property="og:title" content="${discussionTitle}"/>
	<meta property="og:description" content="${firstPost?.message}"/>
	<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>
	<script src="/js/jquery/jquery.mobile.custom.min.js"></script>
	<script src="/js/zero.clipboard.min.js"></script>
	<script type="text/javascript" src="/js/curious/feeds.js?ver=22"></script>
	<script type="text/javascript" src="/js/curious/discussion.js?ver=23"></script>
	<script type="text/javascript" src="/js/curious/sprint.js"></script>
	<c:jsCSRFToken keys="deleteGhostEntryDataCSRF, deleteEntryDataCSRF, addEntryCSRF, getPeopleDataCSRF, getCommentsCSRF,
			getInterestTagsDataCSRF, addInterestTagDataCSRF, autocompleteDataCSRF, fetchSprintDataCSRF, createNewSprintDataCSRF, 
			deleteSprintDataCSRF, stopSprintDataCSRF, startSprintDataCSRF, addMemberToSprintDataCSRF, addAdminToSprintDataCSRF, 
			deleteSprintMemberDataCSRF, deleteSprintAdminDataCSRF, updateSprintDataCSRF, getAutocompleteParticipantsDataCSRF, 
			deleteDiscussionDataCSRF, getFeedsDataCSRF, createDiscussionDataCSRF, addCommentCSRF ,
			deleteDiscussionPostDataCSRF, getDiscussionList, getPlotDescDataCSRF, getSumPlotDescDataCSRF, showTagGroupCSRF,
			getSprintParticipantsDataCSRF, getUserDataCSRF,
			getSprintDiscussionsDataCSRF, addMemberCSRF, addAdminCSRF, deleteMemberCSRF, deleteAdminCSRF, joinSprintDataCSRF,
			leaveSprintDataCSRF, showsprintCSRF, followCSRF" />
	<g:layoutHead />

	</head>
	<body class="${pageProperty(name: 'body.class') ?: '' }">
		<g:set var="isSearchListingPage" value="${controllerName == "search" && actionName == "index"}" />
		<g:set var="isSocialListingPage" value="${controllerName == "home" && actionName == "social"}" />
		<g:set var="isSprintsListingPage" value="${controllerName == "home" && actionName == "sprint"}" />

	<script>
		window.fbAsyncInit = function() {
			FB.init({
				appId      : ${grailsApplication.config.facebookAppId},
				xfbml      : true,
				version    : 'v2.4'
			});
		};

		(function(d, s, id){
			var js, fjs = d.getElementsByTagName(s)[0];
			if (d.getElementById(id)) {return;}
			js = d.createElement(s); js.id = id;
			js.src = "//connect.facebook.net/en_US/sdk.js";
			fjs.parentNode.insertBefore(js, fjs);
		}(document, 'script', 'facebook-jssdk'));
	</script>
		<content tag="processUserData"><g:pageProperty name="page.processUserData"/></content>
		<div class="row red-header">
			<h1 class="clearfix">
				<span id="queryTitle">
					<g:if test="${isSearchListingPage}">
						Search Results: ${params.q}
					</g:if>
					<g:else>
						${groupFullname}
					</g:else>
				</span>
			</h1>
			<div class="pull-right">
				<div class="help">
					<i class="fa fa-question"></i>
				</div>
			</div>
		</div>

		<div class="feed-body clearfix">
			<div class="main container-fluid">
				<ul class="nav nav-pills">
					<li role="presentation">
						<a href="#all">ALL</a>
					</li>
					<g:if test="${isSocialListingPage || isSearchListingPage}">
						<li role="presentation">
							<a href="#people">PEOPLE</a>
						</li>
						<li role="presentation">
							<a href="#discussions">DISCUSSIONS</a>
						</li>
					</g:if>
					<g:if test="${isSearchListingPage}">
						<li role="presentation">
							<a href="#sprints">SPRINTS</a>
						</li>
					</g:if>
					<li role="presentation">
						<a href="#owned">OWNED</a>
					</li>
					<g:if test="${isSprintsListingPage}">
						<li id="feed-right-tab" role="presentation">
							<a class="create-new-sprint" href="#">CREATE NEW SPRINT</a>
						<g>
					</g:if>
				</ul>
				<div id="feed">
					<g:pageProperty name="page.feedContent" />
				</div>
			</div>
		</div>

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

		<g:render template="/sprint/createSprintModal" />
		<g:render template="/templates/discussion/share"/>

		<c:renderJSTemplate template="/discussion/create" id="_createDiscussionForm" />
		<c:renderJSTemplate template="/discussion/instance" id="_discussions" />
		<c:renderJSTemplate template="/discussionPost/instance" id="_comments" />
		<c:renderJSTemplate template="/people/instance" id="_people" />
		<c:renderJSTemplate template="/sprint/instance" id="_sprints" />
		<c:renderJSTemplate template="/people/show" id="_peopleDetails" />
		<c:renderJSTemplate template="/discussion/show" id="_showDiscussion" />
		<c:renderJSTemplate template="/sprint/show" id="_showSprints" />
	</body>
</html>
</g:applyLayout>

<g:applyLayout name="plot">
<g:setProvider library="jquery" />
<html>
	<head>
	<title><g:layoutTitle/></title>
	<meta property="og:title" content="${discussionTitle}"/>
	<meta property="og:description" content="${firstPost?.message}"/>
	<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>
	<script src="/js/jquery/jquery.mobile.custom.min.js"></script>
	<script src="/js/zero.clipboard.min.js"></script>
	<script type="text/javascript" src="/js/curious/feeds.js?ver=22"></script>
	<script type="text/javascript" src="/js/curious/discussion.js?ver=22"></script>
	<script type="text/javascript" src="/js/curious/sprint.js"></script>
	<c:jsCSRFToken keys="deleteGhostEntryDataCSRF, deleteEntryDataCSRF, addEntryCSRF, getPeopleDataCSRF, getCommentsCSRF,
			getInterestTagsDataCSRF, addInterestTagDataCSRF, autocompleteDataCSRF, fetchSprintDataCSRF, createNewSprintDataCSRF, 
			deleteSprintDataCSRF, stopSprintDataCSRF, startSprintDataCSRF, addMemberToSprintDataCSRF, addAdminToSprintDataCSRF, 
			deleteSprintMemberDataCSRF, deleteSprintAdminDataCSRF, updateSprintDataCSRF, getAutocompleteParticipantsDataCSRF, 
			deleteDiscussionDataCSRF, getSearchResultsCSRF, getFeedsDataCSRF, createDiscussionDataCSRF, addCommentCSRF ,
			deleteDiscussionPostDataCSRF, getDiscussionList, getPlotDescDataCSRF, getSumPlotDescDataCSRF, showTagGroupCSRF,
			deleteTagGroupDataCSRF, showTagGroupDataCSRF, getTagPropertiesCSRF, addTagToTagGroupCSRF, listTagsAndTagGroupsCSRF,
			removeTagFromTagGroupCSRF, addTagGroupToTagGroupCSRF, createTagGroupDataCSRF, removeTagGroupFromTagGroupCSRF,
			setTagPropertiesDataCSRF, addBackToTagGroupDataCSRF, removeTagFromTagGroupDataCSRF, getSprintParticipantsDataCSRF,
			getSprintDiscussionsDataCSRF, addMemberCSRF, addAdminCSRF, deleteMemberCSRF, deleteAdminCSRF, joinSprintDataCSRF,
			leaveSprintDataCSRF, showsprintCSRF" />
	<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>
	<g:layoutHead />

	<style type="text/css">
		.ui-accordion-header {
			overflow: hidden;
		}
	</style>
	</head>
	<body class="${pageProperty(name: 'body.class') ?: '' }">
	<script>
		// TODO: change facebook appId after creating final app
		window.fbAsyncInit = function() {
			FB.init({
				appId      : '714092418734227',
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

<g:each in="${discussionList }" var="discussionData">
	<g:set var="iconImage" value="${discussionData.isPlot ? 'graph-icon.png' : 'comment-icon.png' }" />
	<div class="graphItem media">
		<div class="pull-left" href="#">
			<img class="media-object" src="/images/${iconImage }" alt="...">
		</div>
		<div class="media-body">
			<div class="row">
				<div class="col-sm-5">
					<span class="uppercase"> POSTED BY ${discussionData.userName}</span>
					<span> ON <g:formatDate date="${discussionData.updated}" format="MM/dd/yyyy" /></span>
				</div>
				<div class="col-sm-4 col-sm-offset-2 text-right">
					<span> LAST COMMENT ON <g:formatDate date="${discussionData.updated}" format="MM/dd/yyyy" /></span>
				</div>
				<g:if test="${discussionData.isAdmin }">
					<div class="col-sm-1 text-right">
						<a href="#" class="delete-discussion" data-discussion-id="${discussionData.id } ">
							<img src="/images/x.gif" width="8" height="8">
						</a>
					</div>
				</g:if>
			</div>
			<h4 class="media-heading">
				<a href="/home/discuss?discussionId=${discussionData.id }">
					${discussionData.name ?: '(No Title)' }
				</a>
			</h4>
		</div>
	</div>
</g:each>

<ul class="pagination" id="discussion-pagination">
	<g:paginate total="${totalDiscussionCount }" action="community"
		params="[userGroupNames: params.userGroupNames]" />
</ul>
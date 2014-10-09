<%

def activeClass = { a ->
	if (actionName == a) {
		'active-nav-link'
	} else {
		''
	}
}

%>
<div class="body1 container">
<div class="body2">
<div class="bugs right-icon"><a href="#"><span>Bugs</span></a></div>

<!-- HEADER -->
<div class="header clearfix">
	<div class="pull-left logo-container">
		<a href="/home/index" class="logo-link">
			<img src ="/images/signals/logo2.png" />
		</a>
		<g:if test="${templateVer == 'lhp'}">
			<a href="https://npo1.networkforgood.org/Donate/Donate.aspx?npoSubscriptionId=3737" id="headerbutton">
				<img src="/images/lhpdonate.gif" />
			</a>
		</g:if>
	</div>
	<ul class="signin pull-right">
		<li><span id="displayUser"></span></li>
		<li><a href="/home/logout" id="logoutLink">sign out</a></li>
	</ul>
	<div class="text-center clearfix">
		<ul class="mainLinks headerLinks">
			<li><g:link controller='home' action="index">Track</g:link></li>
			<li><g:link controller='home' action="graph">Graph</g:link></li>
			<li><g:link controller='home' action="signals">Signals</g:link></li>
			<li><g:link controller='home' action="feed">Feed</g:link></li>
			<c:ifAdmin>
				<li><g:link controller="admin" action="dashboard">Admin</g:link></li>
			</c:ifAdmin>
		</ul>
	</div>
</div>
<script>
	$(window).load(function () {
		$('ul.mainLinks a').each(function() {
			var href = $(this).attr('href');
			if (href.indexOf(window.location.pathname) != -1) {
				$(this).parent().addClass("active");
				return false;
			}
		})
	});
</script>
<!-- /HEADER -->

<g:render template="/layouts/alertMessage" />
<g:layoutBody />

<!-- FOOTER -->
<br>
<g:render template="/layouts/footer"/>
<!-- /FOOTER -->
<div style="clear:both;"></div>
</div>
</div>
<div id="alert-message-dialog" class="hide">
	<p><p>
	<div id="alert-message-text"></div>
</div>

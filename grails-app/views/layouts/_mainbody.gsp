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

<!-- HEADER -->
<div class="header clearfix">
	<div class="pull-left logo-container">
		<a href="/home/index" class="logo-link">
			<img src ="/images/logo.png" />
		</a>
		<g:if test="${templateVer == 'lhp'}">
			<a href="https://npo1.networkforgood.org/Donate/Donate.aspx?npoSubscriptionId=3737" id="headerbutton">
				<img src="/images/lhpdonate.gif" />
			</a>
		</g:if>
	</div>
	<%--<ul class="signin pull-right">
		<li><span id="displayUser"></span></li>
		<li><a href="/home/logout" id="logoutLink">sign out</a></li>
	</ul>
	--%>
	<div class="pull-right search-bar left-addon">
		<form>
			<i class="fa fa-search"></i>
			<input type="text" placeholder="Search Curious" required >
		</form>
	</div>
	<div class="text-center clearfix">
		<ul class="mainLinks headerLinks">
			<li><g:link controller='home' action="index">Track</g:link></li>
			<li><g:link controller='home' action="graph">Chart</g:link></li>
			<li><g:link controller='home' action="feed">Feed</g:link></li>
			<li><g:link controller='home' action="signals">Signals</g:link></li>
			<li class="dropdown">
				<a href="#" data-toggle="dropdown" class="dropdown-toggle"><b class="caret"></b></a>
				<ul class="dropdown-menu" role="menu">
					<li>
						<span id="displayUser"></span>
					</li>
					<li class="divider"></li>
					<li>
						<a href="/home/logout" id="logoutLink">Logout</a>
					</li>
				</ul>
			</li>
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
	jQuery.curCSS = jQuery.css;
</script>
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

<div class="body1 container">
<div class="body2">
<div class="bugs"><a href="#"><span>Bugs</span></a></div>

<!-- HEADER -->
<div class="header">
	
	<a href="/home/index" class="logo-container"><span id="headerlogo" class="content-size"></span></a>
<g:if test="${templateVer == 'lhp'}">
	<a href="https://npo1.networkforgood.org/Donate/Donate.aspx?npoSubscriptionId=3737" id="headerbutton"></a>
</g:if>
	<ul class="signin pull-right">
		<li><span id="displayUser"></span></li>
		<li><a href="/home/logout" id="logoutLink">sign out</a></li>
	</ul>
	<span id="headerlinks">
	<ul class="mainLinks">
		<li><g:link controller='home' action="index">Track</g:link></li>
		<li><g:link controller='home' action="graph">Graph</g:link></li>
		<li><g:link controller='home' action="community">Community</g:link></li>
	</ul>
	</span>
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

<g:layoutBody />

<!-- FOOTER -->
<div class="footer">

<g:if test="${templateVer == 'lhp'}">
	<ul>
		<li><h2>About</h2></li>
		<li><a href="http://www.lamhealthproject.org/" target="_blank">LAM Health Project</a></li>
		<li><a href="mailto:info@LAMHealthProject.org">Contact</a></li>
	</ul>
	
	<ul>
		<li><h2>More</h2></li>
		<li><a href="https://twitter.com/LAMHlthProject" target="_blank">Twitter</a></li>
		<li><a href="https://www.facebook.com/LAMHealthProject" target="_blank">Facebook</a></li>
	</ul>
	
	<ul>
		<li><h2>&copy; LAM Health Project and<br/>Curious, Inc.</h2></li>
		<li><a href="/home/termsofservice">Terms of Service</a></li>
	</ul>
</g:if>
<g:else>
	<ul>
		<li><h2>About Curious</h2></li>
		<li><a href="http://www.wearecurio.us/">What's going on here?</a></li>
		<li><a href="mailto:contact@wearecurio.us">Contact</a></li>
	</ul>
	
	<ul>
		<li><h2>More</h2></li>
		<li><a href="https://twitter.com/wearecurious">Twitter</a></li>
		<li><a href="https://www.facebook.com/wearecurious">Facebook</a></li>
	</ul>
	
	<ul>
		<li><h2>&copy; Curious, Inc.</h2></li>
		<li><a href="/home/termsofservice">Terms of Service</a></li>
	</ul>
</g:else>
	<ul>
		<li><h2>Data</h2></li>
		<li><g:link controller='home' action="upload">Import</g:link></li>
		<li><g:link controller='home' action="download">Export</g:link></li>
		<li><g:link controller='home' action="polldevices">Poll Devices</g:link></li>
	</ul>
	<div style="clear:both;"></div>
</div>
<!-- /FOOTER -->
<div style="clear:both;"></div>
</div>
</div>
<r:layoutResources/>
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
			<img src ="/images/signals/logo-new.png" width="200"/>
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
			<c:ifAdmin>
				<li><g:link controller="admin" action="dashboard">Admin</g:link></li>
			</c:ifAdmin>
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
		</ul>
	</div>
</div>
<script>
	var showModal = ${(session.survey == 'compass')? true: false};
	$(function() {
		queueJSON("getting login info", "/home/getPeopleData?callback=?",
				getCSRFPreventionObject("getPeopleDataCSRF"),
				function(data) {
					if (!checkData(data))
						return;
				
				var found = false;
				
				jQuery.each(data, function() {
					if (!found) {
						// set first user id as the current
						setUserId(this['id']);
						found = true;
					}
					addPerson(this['first'] + ' ' + this['last'],
							this['username'], this['id'], this['sex']);
					return true;
				});
			});
	});
	$(window).load(function () {
		$('ul.mainLinks a').each(function() {
			var href = $(this).attr('href');
			if (href.indexOf(window.location.pathname) != -1) {
				$(this).parent().addClass("active");
				return false;
			}
		})
		if ((typeof showModal != 'undefined') && showModal) {
			var interestTagList;
			$.ajax({
				url: '/home/getSurveyData',
				success: function(data) {
					if (data != null) {
						$('#survey-carousel-content .carousel-inner').html(data);
						var questionCount = $('#survey-carousel-content .carousel-inner').find('.item').length;
						console.log(questionCount)
						if (questionCount == 1) {
							console.log('ha ha question count');
							$('#navigate-right').html('<button type="submit" class="navigate-carousel-right">SUBMIT</button>');
						}

						queueJSON("getting login info", "/home/getPeopleData?callback=?", function(data){ 
							this.interstTagList = new InterestTagList("interestTagInput", "interestTagList");
						});

						$('#takeSurveyOverlay').modal({show: true});
					} else {
						console.log('data error!');
					}
				},
				error: function(xhr) {
					console.log('xhr:', xhr);
				}
			});
		}
	});
	jQuery.curCSS = jQuery.css;
	
</script>
</script>
<!-- /HEADER -->

<g:render template="/layouts/alertMessage" />
<g:render template="/survey/takeSurveyModal" /> 
<g:render template="/help/helpWizardOverlay" /> 
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

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="public" />
<title></title>
<meta name="description" content="LGMD2I Collaborative Clinical Project" />
<style type="text/css">
ol {
	padding-left:20px;
}
ul {
	padding-left:20px;
}
.terms {
	height:700;
	overflow-y:scroll;
    font-size:10pt;
    margin:10px;
    color:#;
}
h3 {
    font-size:14pt;
	font-weight:bold;
}
strong {
	font-weight:bold;
	font-size:11pt;
}
p {
	margin-top: 14pt;
}
.main p {
	padding: 10px;
	margin: 10px;
}
.tracking_samples {
	margin: 10px;
	padding: 10px;
}
.main .section {
	margin: 10px;
	padding: 10px;
}
.main td {
	padding: 10px;
	border: 1px solid black;
}
#headerlogolgmd {
	background:url(/static/images/logo_lgmd2i.png) no-repeat;
	width: 650px;
	height:70px;
	display:inline-block;
	padding:0px 0px 5px 0px;
	vertical-align:middle;
}
</style>

<script type="text/javascript">

$(function(){
	initTemplate();
});
</script>
</head>
<body>
<!-- HEADER -->
<div class="header">
	<ul class="signin pull-right">
		<li><span id="displayUser"></span></li>
	</ul>
	
	<a href="/home/index"><span id="headerlogolgmd" class="content-size"></span></a>
<g:if test="${templateVer == 'lhp'}">
	<a href="https://npo1.networkforgood.org/Donate/Donate.aspx?npoSubscriptionId=3737" id="headerbutton"></a>
</g:if>
	<nav>
	<span id="headerlinks">
	<ul class="mainLinks">
		<li><a href="#project_desc">PROJECT DESCRIPTION</a></li>
		<li><a href="#participant_instructions">PARTICIPANT INSTRUCTIONS</a></li>
		<li><a href="#tracking">TRACKING</a></li>
	</ul>
	</span>
	</nav>
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
<!-- MAIN -->
<div class="main" id="contentmain">

<article>




<hgroup>
<h1 id="project_desc">Project Description</a></h1>
<p id = "who_is_curious">Curious Inc. is currently developing a platform for gathering and visualizing personal data, especially as it relates to health and well-being. As part of a collaborative project with the <a href="http://www.lgmd2ifund.org/">LGMD2I Research Fund</a>, we are inviting you to explore this platform as part of our ongoing software user experience studies. </p>
<p id = "what_is_the_project">
As a research participant, you will be interacting with an early version of the platform. You may be asked to track health-related issues (e.g. sleep or rest), exercise habits, or health-related events. We will refer to these health-related issues as "tags" in the instructions. In addition, you will be urged to use the platform’s data visualization features, such as the graphing tool.  During interviews with our researcher, you will be encouraged to give candid feedback about features, screens, and basic functionality. </p>

<img  src="/static/images/lgmd2i/ScreenShot1.jpg" width="758	" height="219" border="0" alt="Screen Shot">


<p id = "thank_you">
The Curious software team and the LGMD2i fund would like to thank you in advance for your time. We deeply appreciate your taking part in this program, and we genuinely value your commentary throughout the process-- not just during the interviews. If you have questions or want to discuss anything about the project, please do not hesitate to contact us. 
</p>

<div class="section">
<h2>Project Goals</h2>
<p id = "goals">
The goals of our collaborative effort are the following:
</p>
<ul>
<li>To obtain vital user feedback about usability problems, design defects, help omissions, etc.</li>
<li>To expose the platform to various types of users, including those in patient communities, improving our design and service to a variety of potential users.</li>
<li>To identify and understand new and exciting potential patient usage models.</li>
<li>To assess motivation and compliance to tracking routine.</li>
<li>To assess the feasibility of tracking specific tags in LGMD2I community.</li>
<li>To identify new trackable symptoms and therapies important for LGMD2I.</li>
<li>To get feedback from patients and caregivers about the platform.</li>
<li>To assess motivation and compliance to a tracking routine.</li>
</ul>
</div>

<h1 id="participant_instructions">Research Participant Instructions</a></h1>

<p id = "interview">To kick-off your participation in the research, the researchers will set up an initial interview timeslot. The LGMD2i Fund will coordinate this with you. The interview will likely occur over the phone unless you live in the Phoenix metropolitan area. A representative from LGMD2i fund may be present during the interview. The interview will cover a range of topics from living with limb girdle to the technologies used in your home.</p>

<p id = "skype">After your initial interview, the researcher will do a walk-through of the platform with you using Skype. We ask that you install Skype before the session and supply your Skype name when requested. If you are unfamiliar with Skype, it is a good idea to practice using it a few times with a friend or family member. Skype enables screen sharing so that the researcher can watch your screen interactions. It is important that it is working properly before the walk-through.
 </p>
 
<p id = "walk_thru">
The researcher may ask you to perform a set of tasks using the Curious platform.  It's very important that you keep talking, even as you are performing these tasks, especially when you become stuck. We call this process "thinking aloud." When you remember to think aloud, you play a crucial role in the software development. We get quality information from participants that helps us improve the product.</p>


<p id = "exit_interview">You will use the product by entering (i.e. tracking) your tags daily. After you have used the product for a period of time, the researchers will schedule an exit interview. This interview helps us understand your experience using the software. Please remember that throughout this process there are <b>NO WRONG ANSWERS</b>. You are not being evaluated-- the software is. You can be as candid as you prefer. We don't mind. In fact, we encourage it!</p>

<h1 id="tracking">Tracking</a></h1>
<p id = "what_is_tracking">Tracking is an important part of what you'll be doing with the platform. Sometimes it can become a little difficult to remember the tracking tags and units of measure. We've included this handy table below to help remind you what we'd like you to track.</p>

<div class="tracking_samples">
<table cellpadding=10 cellspacing=10>
<tr>
<th>
TAG LABEL
</th>
<th>
SCALE/UNITS
</th>

<th>
DESCRIPTION
</th>
<th>
EXAMPLE ENTRY
</th>
</tr>


<tr>
<td>
mood
</td>
<td>
1-10, 10=best, 5=neutral, 1=worst
</td>

<td>
Subjective measurement of your general mood or outlook
</td>
<td>
mood 3 4pm
</td>


<tr>
<td>
sleep
</td>
<td>
hours
</td>

<td>
Number of hours of evening rest
</td>
<td>
sleep 8 hours 9am 
</td>

</tr>


<tr>
<td>
fall
</td>
<td>

</td>

<td>
Falling event (slip, fall down, stumble, etc.)
</td>
<td>
fall 9am (getting out of car)
</td>

</tr>

<tr>
<td>
exercise (be specific)
</td>
<td>
minutes
</td>

<td>
Any type of cardio, resistance, or stretching
</td>
<td>
walk 30 minutes 6pm or 
yoga 20 minutes 5pm
</td>

</tr>

<tr>
<td>
fatigue
</td>
<td>
1-10, 10=extremely tired
</td>

<td>
Your assessment of your fatigue level
</td>
<td>
fatigue 5 4pm
</td>

</tr>



<tr>
<td>
climb steps
</td>
<td>
minutes or secs
</td>

<td>
Time to climb 3 steps</td>
<td>
climb steps 60 secs
</td>

</tr>

<tr>
<td>
supplements / medications 
</td>
<td>
mg, mcg, IU, etc.
</td>

<td>
Supplements and medications you are taking</td>
<td>
vitamin d 5000 IU 7am
</td>

</tr>

<tr>
<td>
muscle pain 
</td>
<td>
1-10, 10=extreme pain
</td>

<td>
Pain you are experiencing</td>
<td>
muscle pain 7 2pm (shoulders)
</td>

</tr>

<tr>
<td>
roll over
</td>
<td>
seconds, minutes
</td>

<td>
ability to roll over while lying down</td>
<td>
roll over 45 seconds 9pm
</td>

</tr>

<tr>
<td>
knee lift
</td>
<td>
seconds, minutes
</td>

<td>
Ability to lift 3 knee three times in a row (knee at 90 degree angle)</td>
<td>
knee lift 30 seconds 2pm
</td>

</tr>


<tr>
<td>
daily steps 
</td>
<td>

</td>

<td>
Your total number of steps for the day as calculated by a pedometer, Fitbit, Pulse, etc.
</td>
<td>
daily steps 10,345 9:45pm
</td>

</tr>

</table>
</div>


</hgroup>
</article>


</div>
<div style="clear: both;"></div>

<!-- /MAIN -->


</body>
</html>

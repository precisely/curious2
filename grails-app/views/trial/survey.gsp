<html>
  <head>
    <meta name="layout" content="trialmain" />
    <title>User Preferences</title>
  </head>
  <body>
    <div class="body"><div class="innerbody">
      <div id="logo" class="logoalone"><a href="index"><img src="/images/logo_alone_sm.gif" alt="Curious" border="0" /></a></div>
	  <div class="textbody">
      <g:if test="${flash.message}">
        <div class="message">${flash.message.encodeAsHTML()}<br/></div>
      </g:if>
	  <h2> Please answer a few questions:</h2>
      <g:form action="doupdatesurvey" method="post">
		<input type="hidden" name="precontroller" value="${precontroller}"/>
		<input type="hidden" name="preaction" value="${preaction}"/>

		<div class="errorbox-good">
		<div class="ss-item  ss-select"><div class="ss-form-entry"><label class="ss-q-title" for="big caffeine drinker">Do you normally have 1 or more caffeine drinks per day? 
		</label>
		<br/><label class="ss-q-help" for="entry_1">Coffee, tea, soft drinks, energy drinks.</label>
		<br/><select name="tag big caffeine drinker" id="entry_1"><option value="">[no answer]</option> <option value="1">Yes, I normally consume at least 1 caffeinated drink per day.</option> <option value="0">Nope, I don&#39;t normally touch the stuff.</option></select></div></div></div>
		<br> <div class="errorbox-good">
		<div class="ss-item  ss-select"><div class="ss-form-entry"><label class="ss-q-title" for="sugar in coffee">Will you be using sugar in your coffee for this study?
		</label>
		<br/><label class="ss-q-help" for="entry_2"></label>
		<br/><select name="tag sugar in coffee" id="entry_2"><option value="">[no answer]</option> <option value="1">Yes, probably.</option> <option value="0">No, probably not.</option></select></div></div></div>
		<br> <div class="errorbox-good">
		<div class="ss-item  ss-select"><div class="ss-form-entry"><label class="ss-q-title" for="daily commute">How long is your daily commute?
		</label>
		<br/><label class="ss-q-help" for="entry_6"></label>
		<br/><select name="tag daily commute" id="entry_6"><option value="">[no answer]</option> <option value="20 minutes">Less than 20 minutes.</option> <option value="40 minutes">20-40 minutes.</option> <option value="60 minutes">40 minutes to 1 hour.</option> <option value="90 minutes">More than 1 hour.</option> <option value="0 minutes">I don&#39;t commute at all.</option></select></div></div></div>

		<br> <div class="errorbox-good">
		<div class="ss-item  ss-select"><div class="ss-form-entry"><label class="ss-q-title" for="heavy exerciser">Do you currently work out three or more times per week?
		</label>
		<br/><label class="ss-q-help" for="entry_4"></label>
		<br/><select name="tag heavy exerciser" id="1"><option value="">[no answer]</option> <option value="Yes, at least 3 times per week.">Yes, at least 3 times per week.</option> <option value="0">Nope.</option></select></div></div></div>
		<br> <div class="errorbox-good">
		<div class="ss-item  ss-select"><div class="ss-form-entry"><label class="ss-q-title" for="atrial fibrillation">Have you been diagnosed with atrial fibrillation or have you experienced heart palpitations?
		</label>
		<br/><label class="ss-q-help" for="entry_5"></label>
		<br/><select name="tag atrial fibrillation" id="entry_5"><option value="">[no answer]</option> <option value="1">Yes.</option> <option value="0">No, never.</option> <option value="Not sure.">Not sure.</option></select></div></div></div>

		<br> <div class="errorbox-good">
		<div class="ss-item  ss-select"><div class="ss-form-entry"><label class="ss-q-title" for="rs762551">Do you know your genotype for rs762551, the SNP associated with caffeine metabolism?
		</label>
		<br/><label class="ss-q-help" for="entry_0">If you have genomic data from a company like 23andme, you&#39;ll be able to answer this, otherwise no worries.</label>
		<br/><br/><select name="tag rs762551" id="entry_0"><option value="">[no answer]</option> <option value="none">No idea.</option> <option value="AA">AA - I&#39;m a fast caffeine metabolizer.</option> <option value="AC">AC - I&#39;m a slow caffeine metabolizer.</option> <option value="CC">CC - I&#39;m a slow caffeine metabolizer.</option></select></div></div></div>

		<br> <div class="errorbox-good">
		<div class="ss-item  ss-select"><div class="ss-form-entry"><label class="ss-q-title" for="rs713598">Do you know your genotype for rs713598, the snip associated with the ability to taste bitter flavors?
		</label>
		<br/><label class="ss-q-help" for="entry_7">If you have genomic data from a company like 23andme, you&#39;ll be able to answer this, otherwise no worries.</label>
		<br/><br/><select name="tag rs713598" id="entry_7"><option value="">[no answer]</option> <option value="none">No idea.</option> <option value="GG">GG</option> <option value="CG">CG</option> <option value="CC">CC</option></select></div></div></div>
		<br>

		<div class="buttons">
		  <span class="button">
			<input class="save" type="submit" value="Submit &raquo;" />
		  </span>
		</div>
      </g:form>
		</div>
	  </div></div>
  </body>
</html>

<html>
  <head>
    <meta name="layout" content="trialmain" />
    <title>Link Twitter Account</title>
  </head>
  <body>
    <div class="body"><div class="innerbody">
      <g:if test="${flash.message}">
        <div class="message">${flash.message.encodeAsHTML()}<br/></div>
      </g:if>
      <div id="logo" class="logoalone"><a href="index"><img src="/images/logo_alone_sm.gif" alt="Curious" border="0" /></a></div>
	  <div class="textbody">
	  <h2>Now, authorize Curious to receive your private, direct messages via Twitter.</h2>

	  <p style="text-align: center"><g:link action="registertwitter" class="save">Okay, set me up!</g:link></p>

	  <p><br/>If you don’t already have a Twitter account, go to twitter.com, sign up, come back here, and get set up. We’ll tell you exactly how to communicate with us.</p>
	
	  </div>
    </div></div>
  </body>
</html>

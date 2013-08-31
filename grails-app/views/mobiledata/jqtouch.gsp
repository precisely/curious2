<!DOCTYPE html>
<g:setProvider library="jquery"/>
<html>
<head>
  <meta name="layout" content="offline" />
  <title>Curious</title>
  <meta name="description" content="A platform for health hackers" />
  <meta name="viewport" content="user-scalable=no, width=device-width, initial-scale=1.0, maximum-scale=1.0"/>
  <meta name="apple-mobile-web-app-capable" content="yes" />

  <meta name="apple-mobile-web-app-status-bar-style" content="black" />
  <link rel="apple-touch-icon" href="/images/jqt_startup.png"/>
  <link rel="apple-touch-startup-image" href="/images/jqtouch.png" />
  
  <script type="text/javascript" charset="utf-8">
	var jQT = new $.jQTouch({
		icon: '/images/jqtouch.png',
		addGlossToIcon: false,
		fullScreen: false
	});

	$(function(){
		console.log('Document.ready test');
	});
  </script>
</head>
<body>
  <div id="jqt">
	  <div>
		  <div class="toolbar">
			  <h1>Offline</h1>
		  </div>
		  <div class="info">This page saves all of its resources locally on your device, and can run even when offline.</div>
		  <div><img src="/css/jqtouch/themes/apple/img/greenButton.png"/></div>
	  </div>
  </div>
</body>
</html>

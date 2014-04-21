<html>
    <head>
        <title><g:layoutTitle default="Curious" /></title>
        <link type="text/css" href="/theme/ui.all.css" rel= "stylesheet">
        <script type="text/javascript" src="/js/jquery/jquery-1.4.4.js"></script>
        <script type="text/javascript" src="/js/jquery/jquery.json-2.2.js"></script>
        <!--script type="text/javascript" src="/js/jquery/jquery-ui-1.8.9.custom.min.js"></script-->
        <!--[if IE]><script language="javascript" type="text/javascript" src="/lib/flot/excanvas.pack.js"></script><![endif]-->
        <!--script type="text/javascript" src="/js/flot/jquery.flot.js"></script-->
        <script type="text/javascript">
        var MAX_DUMP_DEPTH = 10;

        $.fn.setAbsolute = function(options) {
          return this.each(function() {
            var el = $(this);
            var pos = el.position();
            settings = jQuery.extend({
               x: pos.left,
               y: pos.top,
               rebase: false
            }, options);
            el.css({ position: "absolute",
                marginLeft: 0, marginTop: 0,
                top: settings.y, left: settings.x });
            if (settings.rebase)
              el.remove().appendTo("body");
          });
        }

        $.fn.isUnderEvent = function(e) {
          var pos = this.position();
      	  if (!pos) return false;
          var height = this.height();
          var width = this.width();

          return e.pageX >= pos.left && e.pageX < pos.left + width
            && e.pageY >= pos.top && e.pageY < pos.top + height;
        }

        function checkData(data, status, errorMessage, successMessage) {
        	if (data == 'error') {
        		if (errorMessage && status != 'cached')
        			alert(errorMessage);
        		return false;
        	}
        	if (data == 'login') {
        		if (status != 'cached') {
        			alert("Session timed out.");
        			doLogout();
        			location.reload(true);
        		}
        		return false;
        	}
        	if (data == 'success') {
        		if (successMessage && status != 'cached')
        			alert(successMessage);
        		return true;
        	}
        	if (typeof(data) == 'string') {
        		if (status != 'cached') {
        			alert(data);
        			location.reload(true);
        		}
        		return false;
        	}
        	return true;
        }

        function removeElem(arr, elem) {
          return jQuery.grep(arr, function(v) {
            return v != elem;
          });
        }

        function dumpObj(obj) {
          return dumpInternalObj(obj, "", "", 0);
        }

        function dumpInternalObj(obj, name, indent, depth) {
          if (depth > MAX_DUMP_DEPTH) {
            return indent + name + ": <Maximum Depth Reached>\n";
          }
          if (typeof obj == "object") {
            var child = null;
            var output = indent + name + "\n";
            indent += "\t";
            for (var item in obj) {
              try {
                child = obj[item];
              } catch (e) {
                child = "<Unable to Evaluate>";
              }
              if (typeof child == "object") {
                output += dumpInternalObj(child, item, indent, depth + 1);
              } else {
                output += indent + item + ": " + child + "\n";
              }
            }
            return output;
          } else {
            return obj;
          }
        }

        function dateToTimeStr(d, shortForm) {
          var ap = "";
          var hour = d.getHours();
          if (hour < 12)
             ap = "am";
          else
             ap = "pm";
          if (hour == 0)
             hour = 12;
          if (hour > 12)
             hour = hour - 12;

          var min = d.getMinutes();

          if (shortForm && min == 0) {
            return hour + ap;
          }

          min = min + "";

          if (min.length == 1)
             min = "0" + min;

          return hour + ":" + min + ap;
        }

        </script>
        <link type="text/css" href="/theme/demos.css" rel= "stylesheet">
        <link type="text/css" href="/flot/layout.css" rel= "stylesheet">
        <style type="text/css">
          body {
            font-size: 62.5%;
          }
          table {
            font-size: 1em;
          }
          body {
            font-family: "Trebuchet MS", "Helvetica", "Arial",  "Verdana", "sans-serif";
          }
          .table {
              display: table;
          } .row {
              display: table-row;
          } .cell {
              display: table-cell;
          } .rounded {
              border-color: #AAAAAA;
              border-style: solid;
              border-width: 1px;
              -moz-border-radius: 4px;
              -webkit-border-radius: 4px;
          } .dotted {
              border-color: #AAAAAA;
              border-style: dotted;
              border-width: 1px;
          } .amount {
              color: #888888;
          }
        </style>
        <link rel="stylesheet" href="/css/trialmain.css"/>
        <link rel="shortcut icon" href="/images/favicon.ico" type="image/x-icon" />
        <g:layoutHead />
        <g:javascript library="application" />
    </head>
    <body>
      <g:layoutBody />
    </body>
</html>

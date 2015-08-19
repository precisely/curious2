<html>
<head>
<meta name="layout" content="home" />
<link type="text/css" href="/css/bootstrap/bootstrap.min.css"
    rel="stylesheet">
<script type="text/javascript" src="/js/bootstrap/bootstrap.min.js"></script>
</head>
<script type="text/javascript">
    $(function() {
        $("input:text:visible:first").focus();
    });
</script>
<body>
    <br>
    <br>
    <div class="row">
        <div class="col-sm-push-8 text-right col-sm-3 thumbnails">
            <img class="" src="/images/home/home-logo.jpg" />
            <p
                style="font-family: Tahoma; font-size: 34px; color: #f14a42; text-align: right; margin-bottom: 0px; white-space: nowrap;">
                We've got questions.</p>
            <p
                style="font-size: 31px; color: #f14a42; text-align: right;">
                <strong>Do you? </strong>
            </p>
            <br> <br>
            <g:link action="register"
                params="${['precontroller':precontroller,'preaction':preaction]}">
                <button type="button" class="btn"
                    style="-webkit-border-radius: 0 !important; -moz-border-radius: 0 !important; border-radius: 0 !important; background-color: #f14a42; color: white;">
                    <p
                        style="margin-bottom: 3px; margin-top: 3px; margin-left: 12px; margin-right: 12px; font-size: 18px">
                        Get Started</p>
                </button>
            </g:link>
        </div>
    </div>
    <div class="shape-wrapper features">
        <div class="features-background"></div>
        <div class="row" id="features">
            <div class="col-sm-4 col-sm-offset-1" style="color: white;">
                <p style="font-size: 26px;">Recover Password</p>
            </div>
        </div>
        <div class="row">
            <div class="col-sm-2 col-sm-offset-1 text-center text-teal">
                <div>

                    <form role="form" action="doforgot" method="post">
                        <div class="form-group">
                            <label for="exampleInputEmail1"
                                style="color: white;">Please
                                enter your username or email:</label><input
                                style="width: 250px; color:black;"  type="username"
                                id="username" name="username" />
                        </div>
                        <input type="hidden" name="preaction"
                            value="${precontroller.encodeAsHTML()}" />
                        <input type="hidden" name="preaction"
                            value="${preaction.encodeAsHTML()}" />
                        <div class="form-group">

                            <input class="btn pull-left" type="submit"
                                value="Recover" />
                        </div>
                    </form>
                </div>
            </div>
        </div>
        <br> <br> <br> <br> <br> <br> <br>
        <br> <br> <br>
    </div>

    

</body>
</html>

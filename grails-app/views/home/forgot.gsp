<html>
<head>
    <meta name="layout" content="home" />
    <title>Recover Password - precise.ly</title>
</head>
<body>
    <br>
    <br>
    <div class="row">
        <div class="col-sm-push-4 text-right col-sm-6" style="margin-left: 20px">
            <img class="" src="/images/home/home-logo-wide.jpg" width="583" height="88"/>
            <p style=" font-size: 34px; color: #f14a42; margin-bottom: 0px; white-space: nowrap;">
                We've all got questions.</p>
            <p style="font-size: 31px; color: #f14a42;">
                <strong>Do you?</strong>
            </p>
            <br> <br>
            <g:link action="register" params="${['precontroller':precontroller,'preaction':preaction]}"
                class="btn btn-red btn-lg btn-flat">
                Get Started
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
            <div class="col-sm-2 col-sm-offset-1">
                <form role="form" action="doforgot" method="post">
                    <div class="form-group">
                        <label for="username" style="color: white;">
                            Please enter your username or email:
                        </label>
                        <input type="text" id="username" name="username" class="form-control" />
                    </div>
                    <input type="hidden" name="preaction" value="${precontroller.encodeAsHTML()}" />
                    <input type="hidden" name="preaction" value="${preaction.encodeAsHTML()}" />
                    <div class="form-group">
                        <input class="btn pull-left" type="submit" value="Recover" />
                    </div>
                </form>
            </div>
        </div>
        <br> <br> <br> <br> <br> <br> <br>
        <br> <br> <br>
    </div>
</body>
</html>
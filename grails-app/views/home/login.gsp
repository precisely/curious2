<html>
<head>
<meta name="layout" content="home" />
<title>Home</title>
<link type="text/css" href="/css/bootstrap/bootstrap.min.css"
    rel="stylesheet">
<script type="text/javascript" src="/js/bootstrap/bootstrap.min.js"></script>
</head>
<body>

    <br>
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
            <g:link action="registerOld"
                params="${['precontroller':precontroller,'preaction':preaction]}">
                <button type="button" class="btn"
                    style="-webkit-border-radius: 0 !important; -moz-border-radius: 0 !important; border-radius: 0 !important; background-color: #f14a42; color: white;">
                    <p
                        style="margin-bottom: 3px; margin-top: 3px; margin-left: 12px; margin-right: 12px; font-size: 18px">
                        Join Curious</p>
                </button>
            </g:link>
        </div>
    </div>
    <div class="shape-wrapper features">
        <div class="features-background"></div>
        <div class="row" id="features">
            <div class="col-sm-1 col-sm-offset-1" style="color: white;">
                <p style="font-size: 26px;">Login</p>
            </div>
        </div>
        <div class="row">
            <g:if test="${!params.login}">
                <br>
                <br>
                <br>
                <div
                    class="col-sm-2 col-sm-offset-1 text-center text-teal">
                    <div class="thumbnail">
                        <img class="img-responsive"
                            style="margin: 0 auto;"
                            src="/images/home/home-second-icon.png" />
                        <div class="caption" style="color: white;">
                            <p style="font-size: 26px;">Track</p>
                            Flexible, tag-based, adaptable Easy-to-use
                            mobile app Integrated device and app data
                            (Fitbit, Withings, Jawbone, Moves, and more)
                            Environment, weather
                        </div>
                    </div>
                </div>
                <div
                    class="col-sm-2 col-sm-offset-1 text-center text-teal"
                    style="margin-left: 60px;">
                    <div class="thumbnail">
                        <img class="img-responsive"
                            style="margin: 0 auto;"
                            src="/images/home/home-first-icon.png" />
                        <div class="caption" style="color: white;">
                            <p style="font-size: 26px;">Signals</p>
                            Automated algorithms detect patterns and
                            correlations, or ‘signals’—you decide what’s
                            a valid signal and what's noise. Graph it
                            yourself to explore and map your own
                            intuitions. Plot, share, explore with
                            others.
                        </div>
                    </div>
                </div>
                <div
                    class="col-sm-2 col-sm-offset-1 text-center text-teal"
                    style="margin-left: 60px;">
                    <div class="thumbnail">
                        <img class="img-responsive"
                            style="margin: 0 auto;"
                            src="/images/home/home-third-icon.png" />
                        <div class="caption" style="color: white;">
                            <p style="font-size: 26px;">Chart</p>
                            Sed ut perspiciatis, unde omnis iste natus
                            error sit voluptatem accusantium doloremque
                            laudantium, totam rem aperiam eaque ipsa,
                            quae ab illo inventore veritatis et quasi
                            architecto beatae vitae dicta sunt,
                            explicabo.
                        </div>
                    </div>
                </div>
                <div
                    class="col-sm-2 col-sm-offset-1 text-center text-teal"
                    style="margin-left: 60px;">
                    <div class="thumbnail">
                        <img class="img-responsive"
                            style="margin: 0 auto;"
                            src="/images/home/home-fourth-icon.png" />
                        <div class="caption" style="color: white;">
                            <p style="font-size: 26px;">Community</p>
                            Find others who share your questions, tags,
                            interests. Post your stories, signals, and
                            graphs. Initiate group tracking studies.
                        </div>
                    </div>
                </div>
            </g:if>
            <g:else>
                <div class="col-sm-2 col-sm-offset-1 text-teal">
                    <form method="post" action="/home/dologin">
                        <input type="hidden" name="precontroller"
                            value="${precontroller.encodeAsHTML()}" />
                        <input type="hidden" name="preaction"
                            value="${preaction.encodeAsHTML()}" /> <input
                            type="hidden" name="parm"
                            value="${parm.encodeAsHTML()}" />

                        <div class="form-group">
                            <label>Username:</label> <input
                                class="form-control" type="text"
                                id="username" name="username" value="" />
                        </div>

                        <div class="form-group">
                            <label>Password:</label> <input
                                class="form-control" type="password"
                                id="password" name="password" value="" />
                        </div>

                        <div class="form-group">
                            <input class="btn"
                                style="margin-right: 5px;" type="submit"
                                value="Login" />
                        </div>


                        <div class="form-group">
                            <g:link action="forgot"
                                params="${['precontroller':precontroller,'preaction':preaction]}">Forgot your login info?</g:link>
                            <br>
                            <g:link action="register"
                                params="${['precontroller':precontroller,'preaction':preaction]}">Create an account</g:link>
                        </div>


                    </form>
                </div>
            </g:else>

        </div>
        <br> <br> <br> <br> <br> <br> <br>
    </div>

    <div>
        <div class="row">
            <div class="col-sm-1 col-sm-offset-1 " style="color: white;">
                <p
                    style="font-size: 24px; color: #c04f7f; padding-top: 30px;">LEARN</p>
            </div>
        </div>
        <br> <br> <br>
        <div class="row">
            <div class="col-sm-7 col-sm-offset-1 text-left">
                <ul class="">
                    <li
                        style="list-style: none; padding-left: 77px; background-image: url(/images/home/sleep-study.png); background-repeat: no-repeat; background-position: 0 .5em; padding-top: 10px; padding-bottom: 40px;">
                        <span
                        style="font-family: Tahoma; font-size: 30px;">The
                            Sleep Study <span
                            style="font-family: Tahoma; font-size: 14px; color: #ff935f; margin-left: 10px;">+13</span>
                    </span>
                    </li>
                    <li
                        style="list-style: none; padding-left: 80px; background-image: url(/images/home/migraines.png); background-repeat: no-repeat; background-position: 0 .5em; padding-top: 10px; padding-bottom: 40px;">
                        <span
                        style="font-family: Tahoma; font-size: 30px;">What
                            causes migraines? <span
                            style="font-family: Tahoma; font-size: 14px; color: #ff935f; margin-left: 10px;">+24</span>
                    </span>
                    </li>
                    <li
                        style="list-style: none; padding-left: 80px; background-image: url(/images/home/tracked-tag.png); background-repeat: no-repeat; background-position: 0 .5em; padding-top: 10px; padding-bottom: 40px;">
                        <span
                        style="font-family: Tahoma; font-size: 30px;">View
                            your most tracked tag <span
                            style="font-family: Tahoma; font-size: 14px; color: #ff935f; margin-left: 10px;">+49</span>
                    </span>
                    </li>
                </ul>

                <div style="position: absolute; z-index: -2;">
                    <ul>
                        <li
                            style="list-style: none; padding-left: 80px; background-image: url(/images/home/sleep-study.png); background-repeat: no-repeat; background-position: 0 .5em; padding-top: 10px; padding-bottom: 40px; opacity: 0.3;">
                            <span
                            style="font-family: Tahoma; font-size: 30px;">How
                                do I improve my energy level?</span>
                        </li>
                        <li
                            style="list-style: none; padding-left: 80px; background-image: url(/images/home/sleep-study.png); background-repeat: no-repeat; background-position: 0 .5em; padding-top: 10px; padding-bottom: 40px; opacity: 0.3;">
                            <span
                            style="font-family: Tahoma; font-size: 30px;">Learn
                                to maintain focus throughtout the day? </span>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </div>

</body>
</html>

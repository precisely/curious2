<html>
<head>
<meta name="layout" content="home" />
<title>Home</title>
<link type="text/css" href="/css/bootstrap/bootstrap.min.css"
    rel="stylesheet">
<script type="text/javascript" src="/js/bootstrap/bootstrap.min.js"></script>
</head>
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
            <div class="col-sm-3 col-sm-offset-1" style="color: white;">
                <p style="font-size: 26px;">New User Signup</p>
            </div>
        </div>
        <div class="row">
            <div class="col-sm-3 col-sm-offset-1 text-teal">
                <form action="doregister" method="post" role="form">
                    <input class="form-control" type="hidden" name="precontroller"
                        value="${precontroller}" /> <input
                        type="hidden" name="preaction"
                        value="${preaction}" />

                    <div class="form-group">
                        <label for="username"
                            class="registerlabel ${hasErrors(bean:flash.user,field:'username','registererror')}">Username*:</label>
                        <input class="form-control" type="text" name="username"
                            value="${fieldValue(bean:flash.user,field:'username')}" />
                    </div>

                    <div class="form-group">
                        <label for="password"
                            class="registerlabel ${flash.user?.hasErrors() ? 'registererror':''}">Password*:</label>
                        <input class="form-control"  type="password" name="password" value="" />
                    </div>

                    <div class="form-group">
                        <label for="email"
                            class="registerlabel ${hasErrors(bean:flash.user,field:'email','registererror')}">Email*:</label>
                        <input class="form-control" type="text" name="email"
                            value="${fieldValue(bean:flash.user,field:'email')}" />
                    </div>

                    <div class="form-group">
                        <label for="first"
                            class="registerlabel ${hasErrors(bean:flash.user,field:'first','registererror')}">First
                            Name:</label> <input class="form-control" type="text" name="first"
                            value="${fieldValue(bean:flash.user,field:'first')}" />
                    </div>

                    <div class="form-group">
                        <label for="last"
                            class="registerlabel ${hasErrors(bean:flash.user,field:'last','registererror')}">Last
                            Name:</label> <input class="form-control" type="text" name="last"
                            value="${fieldValue(bean:flash.user,field:'last')}" />
                    </div>

                    <div class="form-group">
                        <label for="birthdate"
                            class="${hasErrors(bean:flash.user,field:'birthdate','registererror')}">Birthdate
                            (MM/DD/YYYY):</label> <input class="form-control" type="text"
                            name="birthdate"
                            value="${fieldValue(bean:flash.user,field:'birthdate')}" />
                    </div>

                    <div class="form-group">
                        <label for="sex"
                            class="${hasErrors(bean:flash.user,field:'sex','registererror')}">Sex:</label>
                        <g:radioGroup name="sex"
                            labels="['Male','Female']"
                            values="['M','F']"
                            value="${fieldValue(bean:flash.user,field:'sex')}">
                            ${it.label}
                            ${it.radio}
                        </g:radioGroup>
                    </div>

                    <g:if test="${templateVer == 'lhp'}">
                        <div class="form-group">
                            <label for="agree">Agree to share
                                de-identified data for LAM research*:</label> <input
                                type="hidden" name="metaTagName1"
                                value="lhpresearchconsent"> <input
                                type="hidden" name="metaTagName2"
                                value="lhpmember"> <input
                                type="hidden" name="metaTagValue2"
                                value="true">
                            <g:radioGroup name="metaTagValue1" class="form-control" 
                                labels="['Y','N']" values="['Y','N']"
                                value="Y">
                                ${it.label}
                                ${it.radio}
                            </g:radioGroup>
                        </div>
                    </g:if>

                    <g:if test="${templateVer == 'lhp'}">
                        <input type="hidden" name="groups" 
                            value="['announce','lhp','lhp announce']">
                    </g:if>
                    <g:else>
                        <input type="hidden" name="groups"
                            value="['announce','curious','curious announce']">
                    </g:else>

                    <input type="submit" name="cancel" class="btn pull-right" 
                        value="Cancel" /> <input class="btn pull-right" style="margin-right:5px;"
                        type="submit" name="register" value="Register" />
                </form>
            </div>

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

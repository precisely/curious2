<html>
<head>
<meta name="layout" content="home" />
<link type="text/css" href="/css/bootstrap/bootstrap.min.css"
    rel="stylesheet">
<script type="text/javascript" src="/js/bootstrap/bootstrap.min.js"></script>
<script type="text/javascript" src="/js/jquery/jquery.validate.min.js"></script>
<script type="text/javascript" src="/js/jquery/signup.form.js"></script>
</head>
</head>
<body>

    <br>
    <br>
    <br>
    <div class="row">
        <div class="col-sm-push-4 text-right col-sm-6 thumbnails" style="margin-left: 20px">
            <img class="" src="/images/home/home-logo.jpg" />
            <p
                style=" font-size: 34px; color: #f14a42; text-align: right; margin-bottom: 0px; white-space: nowrap;">
                Track data, chart experience, find meaning.</p>
            <p
                style="font-size: 31px; color: #f14a42; text-align: right;">
                <strong>We’ve got questions. </strong>
            </p>
            <br> <br>&nbsp;<br/>&nbsp;
        </div>
    </div>
    <div class="shape-wrapper features">
        <div class="features-background"></div>
        <div class="row" id="features">
            <div class="col-sm-3 col-sm-offset-1" style="color: white;">
                <p style="font-size: 26px; text-transform: uppercase;">New User Signup</p>
            </div>
        </div>
        <div class="row">
            <div class="col-sm-3 col-sm-offset-1 text-teal">
                <form action="doregister" method="post" role="form" id="signupForm">
                    <input class="form-control" type="hidden" name="precontroller"
                        value="${precontroller}" /> <input
                        type="hidden" name="preaction"
                        value="${preaction}" />

                    <div class="form-group">
                        <label for="username"
                            class="registerlabel ${hasErrors(bean:flash.user,field:'username','registererror')}">Social Username*:</label>
                        <input class="form-control" type="text" name="username" id="username" required
                            value="${fieldValue(bean:flash.user,field:'username')}" />
                    </div>

                    <div class="form-group">
                        <label for="password"
                            class="registerlabel ${flash.user?.hasErrors() ? 'registererror':''}">Password*:</label>
                        <input class="form-control"  type="password" name="password" id="password" value="" required/>
                    </div>

                    <div class="form-group">
                        <label for="email"
                            class="registerlabel ${hasErrors(bean:flash.user,field:'email','registererror')}">Email*:</label>
                        <input class="form-control" type="email" name="email" id="email" required
                            value="${fieldValue(bean:flash.user,field:'email')}" />
                    </div>

                    <div class="form-group">
                        <label for="confirm_email"
                            class="registerlabel ${hasErrors(bean:flash.user,field:'confirm_email','registererror')}">Confirm
                            Email*:</label> 
                        <input class="form-control" type="email" name="confirm_email" id="confirm_email"
                            value="${fieldValue(bean:flash.user,field:'confirm_email')}" />
                    </div>

                    <div class="form-group">
                        <label for="name"
                            class="registerlabel ${hasErrors(bean:flash.user,field:'name','registererror')}">Full
                            Name:</label> <input class="form-control" type="text" name="name" id="name"
                            value="${fieldValue(bean:flash.user,field:'name')}" />
                    </div>

                    <!-- div class="form-group">
                        <label for="birthdate"
                            class="${hasErrors(bean:flash.user,field:'birthdate','registererror')}">Birthdate
                            (MM/DD/YYYY):</label> <input class="form-control" type="text"
                            name="birthdate" id="birthdate"
                            value="${fieldValue(bean:flash.user,field:'birthdate')}" />
                    </div>

                    <div class="form-group">
                        <label for="sex"
                            class="${hasErrors(bean:flash.user,field:'sex','registererror')}">Sex:</label>
                        <g:radioGroup name="sex" id="sex"
                            labels="['Male','Female']"
                            values="['M','F']"
                            value="${fieldValue(bean:flash.user,field:'sex')}">
                            ${it.label}
                            ${it.radio}
                        </g:radioGroup>
                    </div  -->

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

   

</body>
</html>

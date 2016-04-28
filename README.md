We Are Curious
=======

Developer documentation is all housed in the main GitHub wiki for this repo:

https://github.com/syntheticzero/curious2/wiki

Please look there for most recent development instructions and coding conventions.

For scripts meant to run on deployment servers, look in /deploymentscripts

These folders are meant to be copied to the root of vanilla CentOS 7 servers, for example /deploymentscripts/installwebapp folder and all subfolders and files should be copied to a CentOS 7 server as /root/installwebapp. You can run the script by invoking /root/installwebapp/install from the shell, as root. It will create a curious user if it doesn't exist already, and install and configure the software for that particular server.

For more detail please see the wiki referenced above.

This file used to contain vagrant/docker instructions which were not up to date. We may add vagrant/docker instructions again, in future, but for now we are using the scripts noted above.

## Bootstrap Customization

Following are some bootstrap customization which needs to be made before downloading new bootstrap resources:

Bootstrap customization URL: http://getbootstrap.com/customize/?id=496152844f6e1fea5e63 (Also present in bootstrap.min.css)

This customization ID sets: 
1. Uncheck all Less Components except **Grid system, form, typography, Basic utilities, Responsive utilities**,
2. @containerlg width to 1000px,
3. @gridgutterwidth to 20px.

After downloading the customized files, remove **!important** in **.hide** class CSS from bootstrap.min.css    
This is required because, we're using **hide** class in most portions & jQuery's **show()** method can't display element 
due to important mark in css.

We Are Curious
=======

Developer documentation is all housed in the main GitHub wiki for this repo:

https://github.com/syntheticzero/curious2/wiki

Please look there for most recent development instructions and coding conventions.

For scripts meant to run on deployment servers, look in /deploymentscripts

These folders are meant to be copied to the root of vanilla CentOS 7 servers, for example /deploymentscripts/installwebapp folder and all subfolders and files should be copied to a CentOS 7 server as /root/installwebapp. You can run the script by invoking /root/installwebapp/install from the shell, as root. It will create a curious user if it doesn't exist already, and install and configure the software for that particular server.

For more detail please see the wiki referenced above.

This file used to contain vagrant/docker instructions which were not up to date. We may add vagrant/docker instructions again, in future, but for now we are using the scripts noted above.

#Create environment for AltruHelp
FROM centos:centos6

#Installing basic tools

RUN rpm --import /etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-6
RUN yum install wget -y
RUN yum install unzip -y
RUN yum install tar -y
RUN yum install perl -y
RUN yum install gcc -y
RUN yum install vim -y

#Installing java7
RUN curl -LO 'http://download.oracle.com/otn-pub/java/jdk/7u51-b13/jdk-7u51-linux-x64.rpm' -H 'Cookie: oraclelicense=accept-securebackup-cookie' && rpm -i jdk-7u51-linux-x64.rpm

RUN yum install ruby -y

#Installing MySQL 5.1.73
RUN curl -LO 'https://downloads.mariadb.com/archives/mysql-5.1/MySQL-server-5.1.73-1.glibc23.x86_64.rpm'
RUN yum install MySQL-server-5.1.73-1.glibc23.x86_64.rpm -y
RUN curl -LO 'https://downloads.mariadb.com/archives/mysql-5.1/MySQL-client-5.1.73-1.glibc23.x86_64.rpm'
RUN yum install MySQL-client-5.1.73-1.glibc23.x86_64.rpm -y

ADD startup.sh /etc/profile.d/
ADD post-docker.sh /post-docker.sh
RUN chmod +x /*.sh
RUN echo 'root:pass' | chpasswd

#Install GVM & Grails
RUN curl -s get.gvmtool.net | bash
RUN mkdir -p ~/.gvm/archives
ADD grails-2.4.3.zip ~/.gvm/archives/

#Dockerising ssh service (This is required)
RUN yum install -y openssh-server
RUN ssh-keygen -q -N "" -t dsa -f /etc/ssh/ssh_host_dsa_key
RUN ssh-keygen -q -N "" -t rsa -f /etc/ssh/ssh_host_rsa_key
RUN sed -i "s/#UsePrivilegeSeparation.*/UsePrivilegeSeparation no/g" /etc/ssh/sshd_config
RUN sed -i "s/UsePAM.*/UsePAM no/g" /etc/ssh/sshd_config

EXPOSE 22
CMD ["/post-docker.sh"]

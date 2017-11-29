FROM   java:8-jre-alpine

COPY build/distributions/mon.zip /mon.zip
RUN /bin/sh -c 'mkdir -p /mon; cd /mon; unzip /mon.zip; mv aws*/* .'
CMD ["sh", "/mon/aws_mon_robot"]

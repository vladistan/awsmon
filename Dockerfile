FROM   java:8-jre-alpine

ADD build/distributions/aws_mon_robot-0.0.1-SNAPSHOT.tar /mon/
CMD ["sh", "/mon/aws_mon_robot"]

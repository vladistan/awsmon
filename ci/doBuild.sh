set -e

echo Building build image
cd ci
./ciBuild.sh
cd ..

USE_UID=$UID

if [ ${USE_UID} -eq 500345588 ]; then
    USE_UID=1000
fi

echo Using UID ${USE_UID}

mkdir -p \?
mkdir -p \?/\.m2
mkdir -p .m2
mkdir -p .sonar
mkdir -p .gradle
mkdir -p build

echo Setup version
git fetch --tags --all

git checkout src/main/java/org/vlad/awsresourcemonitor/autorevision.java

/usr/local/bin/autorevision.sh -t javaprop > autorevision.prop
/opt/btools/bin/prebuild .

/usr/local/bin/autorevision.sh -t java > build/autorevision.java
( echo "package org.vlad.awsresourcemonitor;" ; cat build/autorevision.java ) | cat > src/main/java/org/vlad/awsresourcemonitor/autorevision.java


echo Building aws_mon_robot
docker run -u ${UID:-1001}:${UID:-1001} \
       -v $(pwd):/awsmon \
       -w /awsmon \
       --entrypoint /bin/bash \
       local/aws_mon_builder \
       /awsmon/ci/build-and-test.sh

ls -l build/distributions/

mv build/distributions/awsmon*.tar build/distributions/mon.tar
mv build/distributions/awsmon*.zip build/distributions/mon.zip

$(aws ecr get-login  --region us-east-1 --no-include-email )

VER=$(cat gradle.properties | grep "version=" | cut -d= -f2)

echo "Built Version ${VER}"

docker build  -t awsmon  .
docker tag awsmon:latest 543533956684.dkr.ecr.us-east-1.amazonaws.com/awsmon:latest
docker tag awsmon:latest 543533956684.dkr.ecr.us-east-1.amazonaws.com/awsmon:${VER}
docker tag awsmon:latest vladistan/awsmon:latest
docker tag awsmon:latest vladistan/awsmon:${VER}
docker push 543533956684.dkr.ecr.us-east-1.amazonaws.com/awsmon:latest
docker push 543533956684.dkr.ecr.us-east-1.amazonaws.com/awsmon:${VER}
docker push vladistan/awsmon:latest
docker push vladistan/awsmon:${VER}


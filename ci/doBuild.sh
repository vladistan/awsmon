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

echo Building aws_mon_robot
docker run -u ${UID:-1001}:${UID:-1001} \
       -v $(pwd):/app \
       --entrypoint /bin/bash \
       local/aws_mon_builder \
       /app/ci/build-and-test.sh


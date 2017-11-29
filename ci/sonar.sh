set -e

echo Building build image
cd ci
./ciBuild.sh
cd ..

USE_UID=${UID:-go}

if [ ${USE_UID} -eq 500345588 ]; then
    USE_UID=1000
fi

echo Using UID ${USE_UID}

mkdir -p \?
mkdir -p .m2
mkdir -p .sonar
mkdir -p .gradle

docker run -u $USE_UID:$USE_UID \
    -e TZ=America/New_York \
    -e HOME=/awsmon \
    -v $(pwd):/awsmon \
    -v ${HOME}/.m2:/awsmon/?/.m2:rw \
    -v ${HOME}/.m2:/awsmon/.m2 \
    -v ${HOME}/.gradle:/awsmon/.gradle \
    -w /awsmon \
    --entrypoint /awsmon/gradlew \
    local/aws_mon_builder \
    clean cobertura sonarqube

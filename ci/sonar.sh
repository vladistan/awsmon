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


docker run -u go:go \
    -v /Users/vlad/Proj/aws_mon_robot:/app \
    -e TZ=America/New_York \
    -e HOME=/app \
    -v $(pwd)/.m2:/app/?/.m2:rw \
    -v $(pwd):/app \
    -v $(pwd):/app/.m2 \
    -v ${HOME}/gradle.properties:/app/gradle.properties \
    -w /app \
    --entrypoint /app/gradlew \
    local/aws_mon_builder \
    clean cobertura sonarqube

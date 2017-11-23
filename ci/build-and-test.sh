#!/usr/bin/env bash
set -e -v
./gradlew clean build test distZip distTar

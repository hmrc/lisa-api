#!/usr/bin/env bash

sbt clean scalastyleAll compile coverage test coverageOff coverageReport dependencyUpdates
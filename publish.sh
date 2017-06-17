#!/bin/bash

# Publishes the library to bintray. Don't forget to increment the
# version in publish.gradle!

./gradlew install bintrayUpload

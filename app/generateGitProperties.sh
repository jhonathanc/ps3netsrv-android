#!/bin/bash

GIT_COMMIT=$(git log -1 --pretty=format:%H)
echo "git.commit.id=$GIT_COMMIT" > src/main/assets/git.properties

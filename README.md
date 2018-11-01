# SYFO Sykmelding Apprec


## Getting started
1. Checkout the code from git
2. Update the application name (the entries with the value syfosmapprec) in Dockerfile, nais.yaml and settings.gradle and
change the entrypoint in Dockerfile to mirror this name
3. When using the other branches to cherry pick you probably need to do some changes in Environment.kt to reflect your
nais environment variables
4. You should now be able to build the application using `./gradlew clean installDist`
5. Build and verify that the docker image works by running `docker build -t syfosmapprec .` and
`docker run -p 8080:8080 syfosmapprec`

## Contact us

### Code/project related questions can be sent to
Joakim Kartveit, `joakim.kartveit@nav.no`

### For NAV employees
We are available at the Slack channel #barken

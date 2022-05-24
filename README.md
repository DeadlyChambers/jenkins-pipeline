# Setup
There is a .vscode file that will be included so that can be passed around.
It includes the vs code extenisons w/configurations
There is also a script for extracting jenkins plugins

## Intellisense
Install anything recommend from editorconfig
[npm-groovy-lint](https://github.com/nvuillam/npm-groovy-lint#configuration)
[groovy docker](https://hub.docker.com/_/groovy/)
'''
npm install -g npm-groovy-lint
docker volume create --name grapes-cache
docker run --rm -it -v grapes-cache:/home/groovy/.groovy/grapes groovy
'''
With all those installed. You should be able to run
```
npm-groovy-lint
```
Which should output issues. You can `npm-groovy-lint --fix` if you want an auto fix

https://plugins.jenkins.io/delivery-pipeline-plugin
6.0.0.${BUILD_NUMBER}

